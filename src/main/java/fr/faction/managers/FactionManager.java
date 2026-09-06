package fr.faction.managers;

import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

    public FactionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "factions.yml");
        loadFactions();
    }

    // ─── CRUD FACTION ───────────────────────────────────────────────────────────

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
        // Rompre les alliances
        for (String ally : new HashSet<>(faction.getAllies())) {
            Faction allyFaction = factions.get(ally);
            if (allyFaction != null) allyFaction.removeAlly(name.toLowerCase());
        }
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
            if (faction.getMembers().isEmpty()) { disbandFaction(factionName); return true; }
            else faction.setChef(faction.getMembers().get(0));
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

    // ─── SOUS-CHEFS ─────────────────────────────────────────────────────────────

    public Faction.SousChefResult addSousChef(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return Faction.SousChefResult.NOT_MEMBER;
        Faction.SousChefResult result = faction.addSousChef(player);
        if (result == Faction.SousChefResult.SUCCESS) saveFactions();
        return result;
    }

    public Faction.SousChefResult removeSousChef(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return Faction.SousChefResult.NOT_SOUS_CHEF;
        Faction.SousChefResult result = faction.removeSousChef(player);
        if (result == Faction.SousChefResult.SUCCESS) saveFactions();
        return result;
    }

    public boolean setMaxSousChefs(String factionName, int max) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction == null) return false;
        faction.setMaxSousChefs(max);
        // Si la nouvelle limite est inférieure au nombre de sous-chefs actuels,
        // on retire les excédentaires (les plus récemment ajoutés en premier n'étant pas trackés,
        // on retire arbitrairement jusqu'à revenir dans la limite).
        while (faction.getSousChefCount() > faction.getMaxSousChefs()) {
            UUID toRemove = faction.getSousChefs().iterator().next();
            faction.removeSousChef(toRemove);
        }
        saveFactions();
        return true;
    }

    public void addInvite(String factionName, UUID player) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction != null) faction.addInvite(player);
    }

    public boolean renameFaction(String oldName, String newName) {
        String oldKey = oldName.toLowerCase(), newKey = newName.toLowerCase();
        if (!factions.containsKey(oldKey) || factions.containsKey(newKey)) return false;
        Faction faction = factions.remove(oldKey);
        faction.setName(newName);
        factions.put(newKey, faction);
        for (Map.Entry<UUID, String> e : playerFactionMap.entrySet())
            if (e.getValue().equals(oldKey)) e.setValue(newKey);
        // Mettre à jour les listes d'allies des autres factions
        for (Faction f : factions.values()) {
            if (f.getAllies().remove(oldKey)) f.addAlly(newKey);
        }
        saveFactions();
        return true;
    }

    // ─── SPAWN ──────────────────────────────────────────────────────────────────

    /** Définit le spawn principal (slot 1). */
    public void setFactionSpawn(String factionName, Location loc) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction != null) { faction.setFactionSpawn(loc); saveFactions(); }
    }

    /** Définit un spawn par numéro de slot (1 ou 2). */
    public void setFactionSpawn(String factionName, Location loc, int slot) {
        Faction faction = factions.get(factionName.toLowerCase());
        if (faction != null) { faction.setSpawnBySlot(slot, loc); saveFactions(); }
    }

    // ─── ALLIANCES ──────────────────────────────────────────────────────────────

    /** Envoie une invitation d'alliance à une faction cible */
    public boolean sendAllianceInvite(String fromFaction, String toFaction) {
        Faction from = factions.get(fromFaction.toLowerCase());
        Faction to   = factions.get(toFaction.toLowerCase());
        if (from == null || to == null) return false;
        to.addPendingAlliance(fromFaction.toLowerCase());
        saveFactions();
        return true;
    }

    /** Accepte une invitation d'alliance : les deux factions deviennent alliées */
    public boolean acceptAlliance(String acceptorFaction, String inviterFaction) {
        Faction acceptor = factions.get(acceptorFaction.toLowerCase());
        Faction inviter  = factions.get(inviterFaction.toLowerCase());
        if (acceptor == null || inviter == null) return false;
        if (!acceptor.hasPendingAllianceFrom(inviterFaction)) return false;
        acceptor.removePendingAlliance(inviterFaction);
        acceptor.addAlly(inviterFaction);
        inviter.addAlly(acceptorFaction);
        saveFactions();
        return true;
    }

    /** Rompt une alliance entre deux factions */
    public boolean breakAlliance(String factionA, String factionB) {
        Faction a = factions.get(factionA.toLowerCase());
        Faction b = factions.get(factionB.toLowerCase());
        if (a == null || b == null) return false;
        a.removeAlly(factionB);
        b.removeAlly(factionA);
        saveFactions();
        return true;
    }

    public boolean areAllied(String factionA, String factionB) {
        Faction a = factions.get(factionA.toLowerCase());
        return a != null && a.isAlly(factionB);
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────────

    public Faction getFaction(String name)          { return factions.get(name.toLowerCase()); }
    public Faction getPlayerFaction(UUID player)    { String n = playerFactionMap.get(player); return n == null ? null : factions.get(n); }
    public boolean isInFaction(UUID player)         { return playerFactionMap.containsKey(player); }
    public Map<String, Faction> getAllFactions()    { return Collections.unmodifiableMap(factions); }

    // ─── PERSISTANCE ────────────────────────────────────────────────────────────

    public void saveFactions() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Faction> entry : factions.entrySet()) {
            String key = "factions." + entry.getKey();
            Faction f = entry.getValue();
            cfg.set(key + ".name", f.getName());
            cfg.set(key + ".chef", f.getChef().toString());
            List<String> ms = new ArrayList<>();
            for (UUID u : f.getMembers()) ms.add(u.toString());
            cfg.set(key + ".members", ms);

            // Sous-chefs
            List<String> scs = new ArrayList<>();
            for (UUID u : f.getSousChefs()) scs.add(u.toString());
            cfg.set(key + ".sousChefs", scs);
            cfg.set(key + ".maxSousChefs", f.getMaxSousChefs());

            // Spawn
            if (f.hasSpawn()) {
                Location s = f.getFactionSpawn();
                cfg.set(key + ".spawn.world", s.getWorld().getName());
                cfg.set(key + ".spawn.x", s.getX());
                cfg.set(key + ".spawn.y", s.getY());
                cfg.set(key + ".spawn.z", s.getZ());
                cfg.set(key + ".spawn.yaw",   (double) s.getYaw());
                cfg.set(key + ".spawn.pitch", (double) s.getPitch());
            }
            if (f.hasSpawn2()) {
                Location s2 = f.getFactionSpawn2();
                cfg.set(key + ".spawn2.world", s2.getWorld().getName());
                cfg.set(key + ".spawn2.x", s2.getX());
                cfg.set(key + ".spawn2.y", s2.getY());
                cfg.set(key + ".spawn2.z", s2.getZ());
                cfg.set(key + ".spawn2.yaw",   (double) s2.getYaw());
                cfg.set(key + ".spawn2.pitch", (double) s2.getPitch());
            }
            // Allies
            cfg.set(key + ".allies", new ArrayList<>(f.getAllies()));
            cfg.set(key + ".pendingAlliances", new ArrayList<>(f.getPendingAllianceInvites()));
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde factions : " + e.getMessage());
        }
    }

    public void loadFactions() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("factions")) return;
        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("factions")).getKeys(false)) {
            String path = "factions." + key;
            String name = cfg.getString(path + ".name");
            UUID chef = UUID.fromString(Objects.requireNonNull(cfg.getString(path + ".chef")));
            Faction faction = new Faction(name, chef);
            faction.getMembers().clear();
            for (String s : cfg.getStringList(path + ".members")) {
                UUID u = UUID.fromString(s);
                faction.addMember(u);
                playerFactionMap.put(u, key);
            }
            // Sous-chefs
            if (cfg.contains(path + ".maxSousChefs")) faction.setMaxSousChefs(cfg.getInt(path + ".maxSousChefs"));
            for (String s : cfg.getStringList(path + ".sousChefs")) {
                faction.addSousChef(UUID.fromString(s));
            }

            // Spawn
            if (cfg.contains(path + ".spawn")) {
                try {
                    World world = Bukkit.getWorld(Objects.requireNonNull(cfg.getString(path + ".spawn.world")));
                    if (world != null) {
                        double x = cfg.getDouble(path + ".spawn.x");
                        double y = cfg.getDouble(path + ".spawn.y");
                        double z = cfg.getDouble(path + ".spawn.z");
                        float yaw   = (float) cfg.getDouble(path + ".spawn.yaw");
                        float pitch = (float) cfg.getDouble(path + ".spawn.pitch");
                        faction.setFactionSpawn(new Location(world, x, y, z, yaw, pitch));
                    }
                } catch (Exception e) { /* ignore */ }
            }
            if (cfg.contains(path + ".spawn2")) {
                try {
                    World world2 = Bukkit.getWorld(Objects.requireNonNull(cfg.getString(path + ".spawn2.world")));
                    if (world2 != null) {
                        double x2 = cfg.getDouble(path + ".spawn2.x");
                        double y2 = cfg.getDouble(path + ".spawn2.y");
                        double z2 = cfg.getDouble(path + ".spawn2.z");
                        float yaw2   = (float) cfg.getDouble(path + ".spawn2.yaw");
                        float pitch2 = (float) cfg.getDouble(path + ".spawn2.pitch");
                        faction.setFactionSpawn2(new Location(world2, x2, y2, z2, yaw2, pitch2));
                    }
                } catch (Exception e) { /* ignore */ }
            }
            // Allies
            for (String ally : cfg.getStringList(path + ".allies")) faction.addAlly(ally);
            for (String pa : cfg.getStringList(path + ".pendingAlliances")) faction.addPendingAlliance(pa);

            factions.put(key, faction);
        }
        plugin.getLogger().info(factions.size() + " faction(s) chargée(s).");
    }
}
