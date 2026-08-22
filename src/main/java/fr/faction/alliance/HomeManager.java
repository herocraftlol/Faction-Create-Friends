package fr.faction.alliance;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Système de homes personnels.
 *
 * Limites de homes (par joueur) :
 *   Sans faction     → 1 home
 *   Avec faction     → 2 homes
 *   Avec 1+ allié    → 3 homes
 *
 * Contrainte de distance :
 *   Impossible de poser un home à moins de 10 chunks d'un home d'un autre joueur,
 *   SAUF si ce joueur est dans la même faction OU dans une faction alliée autorisée.
 *
 * Cooldown de téléportation : 5 secondes de chargement (annulé si on bouge).
 */
public class HomeManager {

    private static final int MIN_CHUNK_DISTANCE = 10; // 160 blocs
    private static final int WARMUP_SECONDS = 5;
    private static final int TP_COOLDOWN_SECONDS = 30;

    private final JavaPlugin plugin;
    private final FactionManager factionManager;

    // UUID joueur → List<NamedLocation>
    private final Map<UUID, List<NamedHome>> homes = new HashMap<>();

    // Cooldowns TP home : UUID → fin cooldown (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private File dataFile;

    public HomeManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    public static class NamedHome {
        public final String name;
        public final Location location;
        public NamedHome(String name, Location location) { this.name = name; this.location = location; }
    }

    // ─── LIMITES ────────────────────────────────────────────────────────────────

    public int getMaxHomes(UUID playerUUID) {
        Faction faction = factionManager.getPlayerFaction(playerUUID);
        if (faction == null) return 1;
        if (!faction.getAllies().isEmpty()) return 3;
        return 2;
    }

    // ─── SETHOME ────────────────────────────────────────────────────────────────

    public enum SetHomeResult {
        SUCCESS, TOO_MANY_HOMES, NAME_TAKEN, TOO_CLOSE_TO_OTHER_HOME
    }

    public SetHomeResult setHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        List<NamedHome> playerHomes = homes.computeIfAbsent(uuid, k -> new ArrayList<>());
        int max = getMaxHomes(uuid);

        // Vérifier si le nom existe déjà → mise à jour
        for (int i = 0; i < playerHomes.size(); i++) {
            if (playerHomes.get(i).name.equalsIgnoreCase(name)) {
                playerHomes.set(i, new NamedHome(name, player.getLocation().clone()));
                save();
                return SetHomeResult.SUCCESS;
            }
        }

        if (playerHomes.size() >= max) return SetHomeResult.TOO_MANY_HOMES;

        // Vérifier distance avec tous les autres homes
        Location loc = player.getLocation();
        String tooCloseOwner = findTooCloseHome(uuid, loc);
        if (tooCloseOwner != null) return SetHomeResult.TOO_CLOSE_TO_OTHER_HOME;

        playerHomes.add(new NamedHome(name, loc.clone()));
        save();
        return SetHomeResult.SUCCESS;
    }

    /**
     * Renvoie le nom du joueur propriétaire du home trop proche, ou null si OK.
     * Exclut les membres de la même faction ET des factions alliées.
     */
    private String findTooCloseHome(UUID playerUUID, Location loc) {
        Faction myFaction = factionManager.getPlayerFaction(playerUUID);

        for (Map.Entry<UUID, List<NamedHome>> entry : homes.entrySet()) {
            if (entry.getKey().equals(playerUUID)) continue;

            // Même faction → autorisé
            if (myFaction != null) {
                if (myFaction.isMember(entry.getKey())) continue;

                // Faction alliée → autorisé
                Faction otherFaction = factionManager.getPlayerFaction(entry.getKey());
                if (otherFaction != null && myFaction.isAlly(otherFaction.getName())) continue;
            }

            for (NamedHome h : entry.getValue()) {
                if (!h.location.getWorld().equals(loc.getWorld())) continue;
                int chunkDist = chunkDistance(loc, h.location);
                if (chunkDist < MIN_CHUNK_DISTANCE) {
                    OfflinePlayerName ownerName = new OfflinePlayerName(entry.getKey());
                    return ownerName.name;
                }
            }
        }
        return null;
    }

    private int chunkDistance(Location a, Location b) {
        int cx = (a.getBlockX() >> 4) - (b.getBlockX() >> 4);
        int cz = (a.getBlockZ() >> 4) - (b.getBlockZ() >> 4);
        return (int) Math.sqrt(cx * cx + cz * cz);
    }

    // ─── DELHOME ────────────────────────────────────────────────────────────────

    public boolean deleteHome(UUID uuid, String name) {
        List<NamedHome> h = homes.get(uuid);
        if (h == null) return false;
        boolean removed = h.removeIf(nh -> nh.name.equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    // ─── TELEPORTATION ──────────────────────────────────────────────────────────

    public enum TpHomeResult { SUCCESS, NOT_FOUND, ON_COOLDOWN }

    public TpHomeResult teleportHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        List<NamedHome> h = homes.get(uuid);
        if (h == null || h.isEmpty()) return TpHomeResult.NOT_FOUND;

        NamedHome target = h.stream()
                .filter(nh -> nh.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (target == null) return TpHomeResult.NOT_FOUND;

        // Cooldown
        long now = System.currentTimeMillis();
        Long cd = cooldowns.get(uuid);
        if (cd != null && now < cd) {
            long remaining = (cd - now) / 1000 + 1;
            player.sendMessage(prefix() + "§cCooldown : encore §e" + remaining + "s §cavant de pouvoir te téléporter.");
            return TpHomeResult.ON_COOLDOWN;
        }

        // Warmup
        Location before = player.getLocation().clone();
        player.sendMessage(prefix() + "§aTéléportation dans §e" + WARMUP_SECONDS + "s§a... Ne bouge pas !");

        final Location dest = target.location;
        new BukkitRunnable() {
            int ticks = WARMUP_SECONDS;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (player.getLocation().distanceSquared(before) > 1) {
                    player.sendMessage(prefix() + "§cTéléportation annulée (tu as bougé).");
                    cancel(); return;
                }
                if (--ticks <= 0) {
                    player.teleport(dest);
                    player.sendMessage(prefix() + "§aTéléporté à §e" + name + "§a !");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                    cooldowns.put(uuid, System.currentTimeMillis() + TP_COOLDOWN_SECONDS * 1000L);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return TpHomeResult.SUCCESS;
    }

    // ─── LISTAGE ────────────────────────────────────────────────────────────────

    public List<NamedHome> getHomes(UUID uuid) {
        return homes.getOrDefault(uuid, Collections.emptyList());
    }

    public List<String> getHomeNames(UUID uuid) {
        List<String> names = new ArrayList<>();
        for (NamedHome h : getHomes(uuid)) names.add(h.name);
        return names;
    }

    // ─── PERSISTANCE ────────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<NamedHome>> entry : homes.entrySet()) {
            String base = "homes." + entry.getKey().toString();
            int i = 0;
            for (NamedHome h : entry.getValue()) {
                cfg.set(base + "." + i + ".name", h.name);
                cfg.set(base + "." + i + ".world", h.location.getWorld().getName());
                cfg.set(base + "." + i + ".x", h.location.getX());
                cfg.set(base + "." + i + ".y", h.location.getY());
                cfg.set(base + "." + i + ".z", h.location.getZ());
                cfg.set(base + "." + i + ".yaw",   (double) h.location.getYaw());
                cfg.set(base + "." + i + ".pitch", (double) h.location.getPitch());
                i++;
            }
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Erreur sauvegarde homes : " + e.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("homes")) return;
        for (String uuidStr : Objects.requireNonNull(cfg.getConfigurationSection("homes")).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<NamedHome> list = new ArrayList<>();
                if (cfg.getConfigurationSection("homes." + uuidStr) == null) continue;
                for (String idx : Objects.requireNonNull(cfg.getConfigurationSection("homes." + uuidStr)).getKeys(false)) {
                    String base = "homes." + uuidStr + "." + idx;
                    String name = cfg.getString(base + ".name", "home");
                    World world = Bukkit.getWorld(Objects.requireNonNull(cfg.getString(base + ".world")));
                    if (world == null) continue;
                    double x = cfg.getDouble(base + ".x");
                    double y = cfg.getDouble(base + ".y");
                    double z = cfg.getDouble(base + ".z");
                    float yaw   = (float) cfg.getDouble(base + ".yaw");
                    float pitch = (float) cfg.getDouble(base + ".pitch");
                    list.add(new NamedHome(name, new Location(world, x, y, z, yaw, pitch)));
                }
                if (!list.isEmpty()) homes.put(uuid, list);
            } catch (Exception e) { /* ignore bad entries */ }
        }
        plugin.getLogger().info("Homes : " + homes.values().stream().mapToInt(List::size).sum() + " chargé(s).");
    }

    private String prefix() { return "§8[§a🏠 Home§8] §r"; }

    // Helper interne
    private static class OfflinePlayerName {
        String name;
        OfflinePlayerName(UUID uuid) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            this.name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
        }
    }
}
