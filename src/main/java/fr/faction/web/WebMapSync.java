package fr.faction.web;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WebMapSync — intégré directement dans FactionPlugin.
 * Remplace le plugin FactionWebMap séparé.
 *
 * Envoie vers le site :
 *  - Chunks explorés (flush toutes les 30s)
 *  - Positions des joueurs (toutes les 2s)
 *  - Snapshot des factions (toutes les 60s)
 */
public class WebMapSync implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;
    private fr.faction.claim.ClaimManager claimManager;

    public void setClaimManager(fr.faction.claim.ClaimManager cm) { this.claimManager = cm; }
    private final Logger log;

    private final String siteUrl;
    private final String apiKey;
    private final boolean debug;
    private final Set<String> trackedWorlds;

    // UUID → set de "cx,cz,biome" en attente de flush
    private final Map<UUID, Set<String>> pendingChunks = new ConcurrentHashMap<>();
    // UUID → dernier chunk (pour détecter le changement)
    private final Map<UUID, Long> lastChunkKey = new ConcurrentHashMap<>();
    // UUID → [x, y, z] dernière position
    private final Map<UUID, double[]> lastPos = new ConcurrentHashMap<>();

    private boolean enabled = false;

    public WebMapSync(JavaPlugin plugin, FactionManager factionManager,
                      FactionPowerManager powerManager) {
        this.plugin         = plugin;
        this.factionManager = factionManager;
        this.powerManager   = powerManager;
        this.log            = plugin.getLogger();

        this.siteUrl = plugin.getConfig().getString("site-url", "").replaceAll("/$", "");
        this.apiKey  = plugin.getConfig().getString("faction-api-key", "");

        Set<String> worlds = new HashSet<>(plugin.getConfig().getStringList("webmap.worlds-tracked"));
        if (worlds.isEmpty()) worlds.add("world");
        this.trackedWorlds = worlds;
        this.debug = plugin.getConfig().getBoolean("webmap.debug", false);

        if (siteUrl.isBlank() || apiKey.isBlank()) {
            log.warning("[WebMap] site-url ou faction-api-key absent du config.yml — sync désactivée.");
            return;
        }

        enabled = true;
        log.info("[WebMap] Sync activée → " + siteUrl);

        // Flush chunks toutes les 30s
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::flushAllChunks, 200L, 600L);

        // Positions toutes les 2s
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::pushPositions, 40L, 40L);

        // Snapshot toutes les 60s
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::pushSnapshot, 300L, 1200L);
    }

    // ── Events ────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!trackedWorlds.contains(p.getWorld().getName())) return;

        // Chunk changé ?
        if (e.getFrom().getBlockX() >> 4 != e.getTo().getBlockX() >> 4
         || e.getFrom().getBlockZ() >> 4 != e.getTo().getBlockZ() >> 4) {
            trackChunk(p, e.getTo().getChunk());
        }

        // Position (si bougé d'au moins 1 bloc)
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
         || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            lastPos.put(p.getUniqueId(), new double[]{
                e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()
            });
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        trackChunk(e.getPlayer(), e.getPlayer().getLocation().getChunk());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Flush immédiat
        flushPlayer(uuid, e.getPlayer().getName(), e.getPlayer().getWorld().getName());
        pendingChunks.remove(uuid);
        lastChunkKey.remove(uuid);
        lastPos.remove(uuid);
        // Signaler déconnexion
        sendAsync("/api/faction/push/positions",
            "{\"positions\":[{\"uuid\":\"" + uuid + "\",\"pseudo\":\"" + e.getPlayer().getName()
            + "\",\"online\":false}]}");
    }

    // ── Chunk tracking ────────────────────────────────────────────────────────────

    private void trackChunk(Player player, Chunk chunk) {
        if (!trackedWorlds.contains(chunk.getWorld().getName())) return;
        int cx = chunk.getX(), cz = chunk.getZ();
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        Long last = lastChunkKey.get(player.getUniqueId());
        if (last != null && last == key) return;
        lastChunkKey.put(player.getUniqueId(), key);

        String biome = getBiomeSafe(chunk, cx, cz);

        pendingChunks.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                     .add(cx + "," + cz + "," + biome);
    }

    /**
     * Récupère le nom du biome sans référencer org.bukkit.block.Biome directement.
     * Utilise la réflexion pour éviter l'IncompatibleClassChangeError de Paper 1.21
     * (Biome est passé de class à interface entre 1.20 et 1.21).
     */
    private String getBiomeSafe(Chunk chunk, int cx, int cz) {
        try {
            // Appeler world.getBiome(x, y, z) via réflexion
            Object biomeObj = chunk.getWorld().getClass()
                .getMethod("getBiome", int.class, int.class, int.class)
                .invoke(chunk.getWorld(), cx * 16 + 8, 64, cz * 16 + 8);
            if (biomeObj == null) return "PLAINS";
            // Appeler getKey() sur le résultat (fonctionne que Biome soit class ou interface)
            Object key = biomeObj.getClass().getMethod("getKey").invoke(biomeObj);
            String keyStr = key.getClass().getMethod("getKey").invoke(key).toString();
            return keyStr.toUpperCase().replace(":", "_");
        } catch (Exception e) {
            return "PLAINS";
        }
    }

    private void flushAllChunks() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            flushPlayer(p.getUniqueId(), p.getName(), p.getWorld().getName());
        }
    }

    private void flushPlayer(UUID uuid, String pseudo, String world) {
        if (!enabled) return;
        Set<String> chunks = pendingChunks.remove(uuid);
        if (chunks == null || chunks.isEmpty()) return;

        // Construire le JSON manuellement (pas de dépendance Gson nécessaire)
        StringBuilder sb = new StringBuilder();
        sb.append("{\"uuid\":\"").append(uuid).append("\",");
        sb.append("\"pseudo\":\"").append(pseudo).append("\",");
        sb.append("\"world\":\"").append(world).append("\",");
        sb.append("\"chunks\":[");
        boolean first = true;
        for (String entry : chunks) {
            String[] parts = entry.split(",", 3);
            if (parts.length < 2) continue;
            if (!first) sb.append(",");
            sb.append("{\"cx\":").append(parts[0])
              .append(",\"cz\":").append(parts[1])
              .append(",\"biome\":\"").append(parts.length > 2 ? parts[2] : "PLAINS").append("\"}");
            first = false;
        }
        sb.append("]}");

        sendAsync("/api/faction/push/chunks", sb.toString());
    }

    // ── Positions ─────────────────────────────────────────────────────────────────

    private void pushPositions() {
        if (!enabled) return;
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        StringBuilder sb = new StringBuilder("{\"positions\":[");
        boolean first = true;
        for (Player p : online) {
            double[] pos = lastPos.getOrDefault(p.getUniqueId(), new double[]{
                p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ()
            });
            if (!first) sb.append(",");
            sb.append("{\"uuid\":\"").append(p.getUniqueId()).append("\",")
              .append("\"pseudo\":\"").append(p.getName()).append("\",")
              .append("\"x\":").append((int) pos[0]).append(",")
              .append("\"y\":").append((int) pos[1]).append(",")
              .append("\"z\":").append((int) pos[2]).append(",")
              .append("\"world\":\"").append(p.getWorld().getName()).append("\",")
              .append("\"online\":true}");
            first = false;
        }
        sb.append("]}");
        sendAsync("/api/faction/push/positions", sb.toString());
    }

    // ── Snapshot factions ─────────────────────────────────────────────────────────

    private String buildClaimsJson() {
        if (claimManager == null) return "[]";
        try {
            java.util.Map<?, ?> allClaims = claimManager.getAllClaims();
            if (allClaims == null || allClaims.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (java.util.Map.Entry<?, ?> entry : allClaims.entrySet()) {
                try {
                    Object key  = entry.getKey();
                    Object data = entry.getValue();
                    int cx    = (int) key.getClass().getField("cx").get(key);
                    int cz    = (int) key.getClass().getField("cz").get(key);
                    String world = (String) key.getClass().getField("world").get(key);
                    String fac = (String) data.getClass().getMethod("getFactionName").invoke(data);
                    if (!first) sb.append(",");
                    sb.append("{\"cx\":").append(cx)
                      .append(",\"cz\":").append(cz)
                      .append(",\"world\":\"").append(esc(world)).append("\"")
                      .append(",\"faction\":\"").append(esc(fac)).append("\"}");
                    first = false;
                } catch (Exception ignored) {}
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            if (debug) log.warning("[WebMap] buildClaimsJson : " + e.getMessage());
            return "[]";
        }
    }

    private void pushSnapshot() {
        if (!enabled || factionManager == null) return;
        try {
            StringBuilder sb = new StringBuilder("{\"factions\":[");
            boolean first = true;
            for (Faction faction : factionManager.getAllFactions().values()) {
                if (!first) sb.append(",");
                FactionRank rank = powerManager != null
                        ? powerManager.getFactionRank(faction.getName())
                        : FactionRank.PIERRE;
                double power = powerManager != null
                        ? powerManager.getFactionPower(faction.getName()) : 0;

                // Barycentre des claims (calculé depuis ClaimManager via FactionPlugin)
                sb.append("{\"name\":\"").append(esc(faction.getName())).append("\",")
                  .append("\"rank\":\"").append(rank.nom).append("\",")
                  .append("\"power\":").append(power).append(",")
                  .append("\"members\":").append(uuidList(faction.getMembers())).append(",")
                  .append("\"allies\":").append(strList(new ArrayList<>(faction.getAllies()))).append(",")
                  .append("\"chef\":\"").append(faction.getChef()).append("\"");

                // Spawn
                if (faction.hasSpawn()) {
                    Location sp = faction.getFactionSpawn();
                    int scx = sp.getBlockX() >> 4, scz = sp.getBlockZ() >> 4;
                    sb.append(",\"spawnCx\":").append(scx).append(",\"spawnCz\":").append(scz);
                }
                sb.append("}");
                first = false;
            }
            sb.append("],\"claims\":");
            // Claims réels depuis ClaimManager
            sb.append(buildClaimsJson());
            sb.append(",\"homes\":[]}");
            sendAsync("/api/faction/push/snapshot", sb.toString());
        } catch (Exception e) {
            if (debug) log.warning("[WebMap] Snapshot erreur : " + e.getMessage());
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────────

    private void sendAsync(String path, String json) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> send(path, json));
    }

    private void send(String path, String json) {
        try {
            HttpURLConnection con = (HttpURLConnection)
                URI.create(siteUrl + path).toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("X-Faction-Key", apiKey);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = con.getResponseCode();
            if (debug) log.info("[WebMap] POST " + path + " → " + code);
            con.disconnect();
        } catch (Exception e) {
            if (debug) log.warning("[WebMap] Erreur " + path + " : " + e.getMessage());
        }
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────────

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String uuidList(List<UUID> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String strList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(esc(list.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    public boolean isEnabled() { return enabled; }
}
