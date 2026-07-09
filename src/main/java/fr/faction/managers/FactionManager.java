package fr.faction.managers;

import fr.faction.models.Faction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FactionManager {

    private final JavaPlugin plugin;
    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactionMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public FactionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "factions.yml");
        loadFactions();
    }

    public boolean createFaction(String name, UUID chef) {
        if (factions.containsKey(name.toLowerCase())) return false;
        Faction faction = new Faction(name, chef);
        factions.put(name.toLowerCase(), faction);
        playerFactionMap.put(chef, name.toLowerCase());
        saveFactions();
        return true;
    }

    public boolean disbandFaction(String name) {
        Faction faction = factions.get(name.toLowerCase());
        if (faction == null) return false;
        for (UUID member : faction.getMembers()) playerFactionMap.remove(member);
        factions.remove(name.toLowerCase());
        saveFactions();
        return true;
    }

    public boolean addMember(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return false;
        faction.addMember(player);
        playerFactionMap.put(player, factionName.toLowerCase());
        faction.removeInvite(player);
        saveFactions();
        return true;
    }

    public boolean removeMember(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return false;
        faction.removeMember(player);
        playerFactionMap.remove(player);
        if (faction.isChef(player)) {
            if (faction.getMembers().isEmpty()) {
                disbandFaction(factionName);
            } else {
                faction.setChef(faction.getMembers().get(0));
            }
        }
        saveFactions();
        return true;
    }

    public boolean setChef(String factionName, UUID newChef) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null || !faction.isMember(newChef)) return false;
        faction.setChef(newChef);
        saveFactions();
        return true;
    }

    public void addInvite(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction != null) faction.addInvite(player);
    }

    public Faction getFaction(String name) {
        return factions.get(name.toLowerCase());
    }

    /**
     * Renomme une faction.
     * @return false si le nouveau nom est déjà pris ou invalide
     */
    public boolean renameFaction(String oldName, String newName) {
        String oldKey = oldName.toLowerCase();
        String newKey = newName.toLowerCase();
        if (!factions.containsKey(oldKey)) return false;
        if (factions.containsKey(newKey)) return false;

        Faction faction = factions.remove(oldKey);
        faction.setName(newName);
        factions.put(newKey, faction);

        // Mettre à jour playerFactionMap pour tous les membres
        for (Map.Entry<UUID, String> e : playerFactionMap.entrySet()) {
            if (e.getValue().equals(oldKey)) e.setValue(newKey);
        }

        saveFactions();
        return true;
    }

    public Faction getPlayerFaction(UUID player) {
        String name = playerFactionMap.get(player);
        return name == null ? null : factions.get(name);
    }

    public boolean isInFaction(UUID player) {
        return playerFactionMap.containsKey(player);
    }

    public Map<String, Faction> getAllFactions() {
        return Collections.unmodifiableMap(factions);
    }

    public void saveFactions() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        dataConfig = new YamlConfiguration();
        for (Map.Entry<String, Faction> entry : factions.entrySet()) {
            String key = "factions." + entry.getKey();
            Faction f = entry.getValue();
            dataConfig.set(key + ".name", f.getName());
            dataConfig.set(key + ".chef", f.getChef().toString());
            List<String> memberStrings = new ArrayList<>();
            for (UUID uuid : f.getMembers()) memberStrings.add(uuid.toString());
            dataConfig.set(key + ".members", memberStrings);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde factions : " + e.getMessage());
        }
    }

    public void loadFactions() {
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("factions")) return;
        for (String key : Objects.requireNonNull(dataConfig.getConfigurationSection("factions")).getKeys(false)) {
            String path = "factions." + key;
            String name = dataConfig.getString(path + ".name");
            UUID chef = UUID.fromString(Objects.requireNonNull(dataConfig.getString(path + ".chef")));
            List<String> memberStrings = dataConfig.getStringList(path + ".members");
            Faction faction = new Faction(name, chef);
            faction.getMembers().clear();
            for (String uuidStr : memberStrings) {
                UUID uuid = UUID.fromString(uuidStr);
                faction.addMember(uuid);
                playerFactionMap.put(uuid, key);
            }
            factions.put(key, faction);
        }
        plugin.getLogger().info(factions.size() + " faction(s) chargée(s).");
    }
}
