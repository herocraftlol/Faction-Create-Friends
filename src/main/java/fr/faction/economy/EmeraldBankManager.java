package fr.faction.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Banque d'émeraudes :
 *  - Compte personnel par joueur (UUID → solde)
 *  - Compte de faction (nom faction → solde)
 * Persistance via bank.yml
 */
public class EmeraldBankManager {

    private final JavaPlugin plugin;
    private final File dataFile;

    private final Map<UUID, Long> playerBalances   = new HashMap<>();
    private final Map<String, Long> factionBalances = new HashMap<>();

    public EmeraldBankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "bank.yml");
        load();
    }

    // ── Solde joueur ──────────────────────────────────────────────────────────

    public long getPlayerBalance(UUID uuid) {
        return playerBalances.getOrDefault(uuid, 0L);
    }

    /** Dépose des émeraudes sur le compte joueur. */
    public void depositPlayer(UUID uuid, long amount) {
        playerBalances.merge(uuid, amount, Long::sum);
        save();
    }

    /**
     * Retire des émeraudes du compte joueur.
     * @return false si solde insuffisant
     */
    public boolean withdrawPlayer(UUID uuid, long amount) {
        long current = getPlayerBalance(uuid);
        if (current < amount) return false;
        playerBalances.put(uuid, current - amount);
        save();
        return true;
    }

    // ── Solde faction ─────────────────────────────────────────────────────────

    public long getFactionBalance(String factionName) {
        return factionBalances.getOrDefault(factionName.toLowerCase(), 0L);
    }

    public void depositFaction(String factionName, long amount) {
        factionBalances.merge(factionName.toLowerCase(), amount, Long::sum);
        save();
    }

    public boolean withdrawFaction(String factionName, long amount) {
        long current = getFactionBalance(factionName);
        if (current < amount) return false;
        factionBalances.put(factionName.toLowerCase(), current - amount);
        save();
        return true;
    }

    /** Appelé quand une faction est dissoute. */
    public void deleteFactionAccount(String factionName) {
        factionBalances.remove(factionName.toLowerCase());
        save();
    }

    // ── Transfert joueur → faction ────────────────────────────────────────────

    public boolean transferToFaction(UUID uuid, String factionName, long amount) {
        if (!withdrawPlayer(uuid, amount)) return false;
        depositFaction(factionName, amount);
        return true;
    }

    public boolean transferToPlayer(String factionName, UUID uuid, long amount) {
        if (!withdrawFaction(factionName, amount)) return false;
        depositPlayer(uuid, amount);
        return true;
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        playerBalances.forEach((u, v) -> cfg.set("players." + u.toString(), v));
        factionBalances.forEach((n, v) -> cfg.set("factions." + n, v));
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Erreur sauvegarde banque : " + e.getMessage()); }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (cfg.contains("players")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("players")).getKeys(false))
                playerBalances.put(UUID.fromString(key), cfg.getLong("players." + key));
        }
        if (cfg.contains("factions")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("factions")).getKeys(false))
                factionBalances.put(key, cfg.getLong("factions." + key));
        }
    }
}
