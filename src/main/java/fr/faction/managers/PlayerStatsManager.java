package fr.faction.managers;

import fr.faction.models.PlayerStats;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les statistiques de tous les joueurs, sauvegardées dans stats.yml.
 * Intègre également les classements multi-catégories (ex-plugin FactionStats).
 */
public class PlayerStatsManager {

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();
    private File dataFile;

    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "stats.yml");
        loadAll();
    }

    // ════════════════════════════════════════════════════════════════════
    // ACCÈS
    // ════════════════════════════════════════════════════════════════════

    /** Récupère ou crée les stats d'un joueur (UUID seul, nom inconnu pour l'instant) */
    public PlayerStats getStats(UUID uuid) {
        return statsMap.computeIfAbsent(uuid, PlayerStats::new);
    }

    /** Récupère ou crée les stats d'un joueur en renseignant/actualisant son nom */
    public PlayerStats getOrCreateStats(UUID uuid, String playerName) {
        PlayerStats stats = statsMap.computeIfAbsent(uuid, u -> new PlayerStats(u, playerName));
        stats.setPlayerName(playerName);
        return stats;
    }

    /** Recherche les stats d'un joueur par son nom (insensible à la casse), null si introuvable en cache */
    public PlayerStats getStatsByName(String name) {
        for (PlayerStats stats : statsMap.values()) {
            if (name.equalsIgnoreCase(stats.getPlayerName())) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Résout les stats d'un joueur par son nom en cherchant dans le cache, les joueurs en ligne,
     * puis les OfflinePlayers connus du serveur. Retourne null si jamais vu.
     */
    @SuppressWarnings("deprecation")
    public PlayerStats resolveStats(String name) {
        PlayerStats stats = getStatsByName(name);
        if (stats != null) return stats;

        org.bukkit.entity.Player online = plugin.getServer().getPlayer(name);
        if (online != null) {
            return getOrCreateStats(online.getUniqueId(), online.getName());
        }

        OfflinePlayer offline = plugin.getServer().getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.getName() != null) {
            UUID uuid = offline.getUniqueId();
            PlayerStats existing = statsMap.get(uuid);
            if (existing != null) return existing;
            String resolvedName = offline.getName() != null ? offline.getName() : name;
            return getOrCreateStats(uuid, resolvedName);
        }
        return null;
    }

    public Collection<PlayerStats> getAllStats() {
        return Collections.unmodifiableCollection(statsMap.values());
    }

    // ════════════════════════════════════════════════════════════════════
    // SAUVEGARDE / CHARGEMENT
    // ════════════════════════════════════════════════════════════════════

    public void saveAll() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String k = entry.getKey().toString();
            PlayerStats s = entry.getValue();
            cfg.set(k + ".name",       s.getPlayerName());
            cfg.set(k + ".kills",      s.getKills());
            cfg.set(k + ".deaths",     s.getDeaths());
            cfg.set(k + ".damage",     s.getDamageDealt());
            cfg.set(k + ".damageTaken", s.getDamageTaken());
            cfg.set(k + ".mobsKilled", s.getMobsKilled());
            cfg.set(k + ".broken",     s.getBlocksBroken());
            cfg.set(k + ".placed",     s.getBlocksPlaced());
            cfg.set(k + ".distance",   s.getDistanceTravelled());
            cfg.set(k + ".advances",   s.getAdvancements());
            cfg.set(k + ".crafted",    s.getItemsCrafted());
            cfg.set(k + ".ticks",      s.getTicksPlayed());
            cfg.set(k + ".picked",     s.getItemsPickedUp());
            cfg.set(k + ".firstJoin",  s.getFirstJoin());
            cfg.set(k + ".lastJoin",   s.getLastJoin());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde stats : " + e.getMessage());
        }
    }

    public void loadAll() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = cfg.getString(key + ".name", null);
                PlayerStats s = name != null ? new PlayerStats(uuid, name) : new PlayerStats(uuid);

                for (int i = 0; i < cfg.getInt(key + ".kills", 0); i++)    s.addKill();
                for (int i = 0; i < cfg.getInt(key + ".deaths", 0); i++)   s.addDeath();
                s.addDamage(cfg.getDouble(key + ".damage", 0));
                s.addDamageTaken(cfg.getDouble(key + ".damageTaken", 0));
                for (long i = 0; i < cfg.getLong(key + ".mobsKilled", 0); i++) s.addMobKill();
                for (long i = 0; i < cfg.getLong(key + ".broken", 0); i++) s.addBlockBroken();
                for (long i = 0; i < cfg.getLong(key + ".placed", 0); i++) s.addBlockPlaced();
                s.addDistance(cfg.getDouble(key + ".distance", 0));
                for (int i = 0; i < cfg.getInt(key + ".advances", 0); i++) s.addAdvancement();
                s.addCraft(cfg.getInt(key + ".crafted", 0));
                s.addTicks(cfg.getLong(key + ".ticks", 0));
                for (long i = 0; i < cfg.getLong(key + ".picked", 0); i++) s.addItemPickedUp();
                s.setFirstJoin(cfg.getLong(key + ".firstJoin", System.currentTimeMillis()));
                s.setLastJoin(cfg.getLong(key + ".lastJoin", System.currentTimeMillis()));

                statsMap.put(uuid, s);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info(statsMap.size() + " profil(s) de stats chargé(s).");
    }

    /** Sauvegarde périodique toutes les 5 minutes */
    public void startAutoSave() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                this::saveAll, 6000L, 6000L);
    }

    // ════════════════════════════════════════════════════════════════════
    // CLASSEMENTS (ex-plugin FactionStats)
    // ════════════════════════════════════════════════════════════════════

    public List<PlayerStats> getTopMobsKilled(int limit) {
        return getTopPlayers(limit, Comparator.comparingLong(PlayerStats::getMobsKilled).reversed());
    }

    public List<PlayerStats> getTopKills(int limit) {
        return getTopPlayers(limit, Comparator.comparingInt(PlayerStats::getKills).reversed());
    }

    public List<PlayerStats> getTopAdvancements(int limit) {
        return getTopPlayers(limit, Comparator.comparingInt(PlayerStats::getAdvancements).reversed());
    }

    public List<PlayerStats> getTopDeaths(int limit) {
        return getTopPlayers(limit, Comparator.comparingInt(PlayerStats::getDeaths).reversed());
    }

    public List<PlayerStats> getTopBlocksBroken(int limit) {
        return getTopPlayers(limit, Comparator.comparingLong(PlayerStats::getBlocksBroken).reversed());
    }

    public List<PlayerStats> getTopPlaytime(int limit) {
        return getTopPlayers(limit, Comparator.comparingLong(PlayerStats::getTicksPlayed).reversed());
    }

    public List<PlayerStats> getTopDamageDealt(int limit) {
        return getTopPlayers(limit, Comparator.comparingDouble(PlayerStats::getDamageDealt).reversed());
    }

    public List<PlayerStats> getTopKDR(int limit) {
        return getTopPlayers(limit, Comparator.comparingDouble(PlayerStats::getKDR).reversed());
    }

    private List<PlayerStats> getTopPlayers(int limit, Comparator<PlayerStats> comparator) {
        List<PlayerStats> list = new ArrayList<>(statsMap.values());
        list.sort(comparator);
        return list.subList(0, Math.min(limit, list.size()));
    }

    /**
     * Retourne le rang d'un joueur (1-based) dans une catégorie de classement donnée.
     * Catégories : mobs, pvp, advancements, morts, blocs, temps, dommages, kd
     */
    public int getRank(UUID uuid, String categorie) {
        List<PlayerStats> classement = switch (categorie.toLowerCase()) {
            case "mobs"                    -> getTopMobsKilled(Integer.MAX_VALUE);
            case "pvp", "joueurs", "kills" -> getTopKills(Integer.MAX_VALUE);
            case "advancements", "progres" -> getTopAdvancements(Integer.MAX_VALUE);
            case "morts", "deaths"         -> getTopDeaths(Integer.MAX_VALUE);
            case "blocs"                   -> getTopBlocksBroken(Integer.MAX_VALUE);
            case "temps"                   -> getTopPlaytime(Integer.MAX_VALUE);
            case "dommages"                -> getTopDamageDealt(Integer.MAX_VALUE);
            case "kd"                      -> getTopKDR(Integer.MAX_VALUE);
            default                        -> getTopMobsKilled(Integer.MAX_VALUE);
        };

        for (int i = 0; i < classement.size(); i++) {
            if (classement.get(i).getUuid().equals(uuid)) return i + 1;
        }
        return -1;
    }
}
