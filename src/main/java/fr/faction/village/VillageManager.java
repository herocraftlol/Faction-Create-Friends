package fr.faction.village;

import fr.faction.claim.ClaimManager;
import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.villager.RecruitedVillager;
import fr.faction.villager.VillagerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les villages fondés par les factions : fondation depuis une zone de claims,
 * listing, population/niveau (dérivés des villageois recrutés qui en font partie),
 * persistance dans villages.yml.
 */
public class VillageManager implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private ClaimManager claimManager;
    private VillagerManager villagerManager;

    private final Map<UUID, Village> villages = new HashMap<>();
    private final Map<UUID, PendingFoundation> pendingFoundations = new HashMap<>();
    private final Map<UUID, PendingPost> pendingPosts = new HashMap<>();
    private final File dataFile;

    public VillageManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "villages.yml");
        load();
    }

    public void setClaimManager(ClaimManager claimManager)     { this.claimManager = claimManager; }
    public void setVillagerManager(VillagerManager villagerManager) { this.villagerManager = villagerManager; }

    // ════════════════════════════════════════════════════════════════════════
    // ACCESSEURS
    // ════════════════════════════════════════════════════════════════════════

    public Village getById(UUID id) { return villages.get(id); }

    public List<Village> getFactionVillages(String factionName) {
        List<Village> list = new ArrayList<>();
        for (Village v : villages.values()) if (v.getFactionName().equalsIgnoreCase(factionName)) list.add(v);
        return list;
    }

    public Village getByName(String factionName, String name) {
        for (Village v : getFactionVillages(factionName)) if (v.getName().equalsIgnoreCase(name)) return v;
        return null;
    }

    /** Population = nombre de villageois recrutés de la faction actuellement rattachés à ce village. */
    public int getPopulation(Village village) {
        if (villagerManager == null) return 0;
        int count = 0;
        for (RecruitedVillager rv : villagerManager.getFactionVillagers(village.getFactionName())) {
            if (village.getName().equalsIgnoreCase(rv.getVillageName())) count++;
        }
        return count;
    }

    public int getLevel(Village village) {
        return Village.levelForPopulation(getPopulation(village));
    }

    /** Village dont la zone de fondation contient ce point, pour cette faction (ou null). */
    public Village findVillageContaining(String factionName, Location loc) {
        for (Village v : getFactionVillages(factionName)) {
            if (v.contains(loc)) return v;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // FONDATION (sélection de 2 coins dans le monde, comme les autres zones)
    // ════════════════════════════════════════════════════════════════════════

    public enum FoundResult { SUCCESS, NOT_IN_FACTION, NO_PERMISSION, NAME_TAKEN, NAME_INVALID }

    private static class PendingFoundation {
        final String factionName;
        final String villageName;
        Location corner1;
        PendingFoundation(String factionName, String villageName) { this.factionName = factionName; this.villageName = villageName; }
    }

    public FoundResult startFoundation(Player player, String name) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return FoundResult.NOT_IN_FACTION;
        if (!faction.canManage(player.getUniqueId())) return FoundResult.NO_PERMISSION;
        if (name == null || name.isBlank() || name.length() > 24) return FoundResult.NAME_INVALID;
        if (getByName(faction.getName(), name) != null) return FoundResult.NAME_TAKEN;

        pendingFoundations.put(player.getUniqueId(), new PendingFoundation(faction.getName(), name));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du village §7(doit être dans tes claims)§e.");
        return FoundResult.SUCCESS;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        PendingPost postSel = pendingPosts.get(player.getUniqueId());
        if (postSel != null) {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
            event.setCancelled(true);
            handlePostClick(player, postSel, event.getClickedBlock().getLocation());
            return;
        }

        PendingFoundation sel = pendingFoundations.get(player.getUniqueId());
        if (sel == null) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        event.setCancelled(true);

        Block block = event.getClickedBlock();
        Location loc = block.getLocation();

        if (!claimBelongsToFaction(loc, sel.factionName)) {
            player.sendMessage(prefix() + "§cCe bloc doit être dans un chunk claimé par ta faction.");
            return;
        }

        if (sel.corner1 == null) {
            sel.corner1 = loc;
            player.sendMessage(prefix() + "§aCoin A défini. §eClique-droit sur le §b2e coin§e.");
            return;
        }

        if (!java.util.Objects.equals(sel.corner1.getWorld(), loc.getWorld())) {
            player.sendMessage(prefix() + "§cLes deux coins doivent être dans le même monde. Fondation annulée.");
            pendingFoundations.remove(player.getUniqueId());
            return;
        }

        pendingFoundations.remove(player.getUniqueId());

        Location center = sel.corner1.clone().add(loc).multiply(0.5);
        center.setWorld(loc.getWorld());
        World world = loc.getWorld();
        center.setY(world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ()) + 1);

        Village village = new Village(UUID.randomUUID(), sel.villageName, sel.factionName,
                sel.corner1.clone(), loc.clone(), center);
        villages.put(village.getId(), village);

        // Les villageois déjà recrutés dont l'emplacement d'origine n'est plus connu ne sont pas
        // rattachés rétroactivement ; seuls les nouveaux recrutés dans cette zone le seront.
        save();
        player.sendMessage(prefix() + "§a✔ Village §e" + village.getName() + " §afondé !");
    }

    private boolean claimBelongsToFaction(Location loc, String factionName) {
        if (claimManager == null) return true;
        var chunk = loc.getChunk();
        if (!claimManager.isClaimed(chunk)) return false;
        var data = claimManager.getClaim(chunk);
        return data != null && data.getFactionName().equalsIgnoreCase(factionName);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PORT / GARE (sélection d'un seul point, comme les autres postes)
    // ════════════════════════════════════════════════════════════════════════

    private static class PendingPost {
        final UUID villageId;
        final PostType type;
        PendingPost(UUID villageId, PostType type) { this.villageId = villageId; this.type = type; }
    }

    public boolean startPostSelection(Player player, Village village, PostType type) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.getName().equalsIgnoreCase(village.getFactionName())) return false;
        if (!faction.canManage(player.getUniqueId())) return false;
        pendingPosts.put(player.getUniqueId(), new PendingPost(village.getId(), type));
        player.sendMessage(prefix() + "§eClique-droit à l'endroit du " + (type == PostType.PORT ? "port" : "de la gare")
                + " §7(doit être dans tes claims)§e.");
        return true;
    }

    private void handlePostClick(Player player, PendingPost sel, Location loc) {
        pendingPosts.remove(player.getUniqueId());
        Village village = villages.get(sel.villageId);
        if (village == null) return;
        if (!claimBelongsToFaction(loc, village.getFactionName())) {
            player.sendMessage(prefix() + "§cCe bloc doit être dans un chunk claimé par ta faction.");
            return;
        }
        if (sel.type == PostType.PORT) village.setPort(loc.clone());
        else village.setStation(loc.clone());
        save();
        player.sendMessage(prefix() + "§a✔ " + sel.type.displayName() + " défini pour §e" + village.getName() + "§a. "
                + "Place un coffre à cet endroit : c'est l'entrepôt utilisé pour le commerce.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // DISSOLUTION
    // ════════════════════════════════════════════════════════════════════════

    public boolean disband(Player player, Village village) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.getName().equalsIgnoreCase(village.getFactionName())) return false;
        if (!faction.canManage(player.getUniqueId())) return false;

        villages.remove(village.getId());
        if (villagerManager != null) {
            for (RecruitedVillager rv : villagerManager.getFactionVillagers(village.getFactionName())) {
                if (village.getName().equalsIgnoreCase(rv.getVillageName())) {
                    villagerManager.setVillage(rv, null);
                }
            }
        }
        save();
        return true;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERSISTANCE
    // ════════════════════════════════════════════════════════════════════════

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Village v : villages.values()) {
            String key = "villages." + v.getId();
            cfg.set(key + ".name", v.getName());
            cfg.set(key + ".faction", v.getFactionName());
            cfg.set(key + ".zoneA", locToString(v.getZoneA()));
            cfg.set(key + ".zoneB", locToString(v.getZoneB()));
            cfg.set(key + ".center", locToString(v.getCenter()));
            if (v.hasPort()) cfg.set(key + ".port", locToString(v.getPort()));
            if (v.hasStation()) cfg.set(key + ".station", locToString(v.getStation()));
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde villages : " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("villages")) return;
        var section = cfg.getConfigurationSection("villages");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "villages." + key;
            UUID id;
            try { id = UUID.fromString(key); } catch (Exception e) { continue; }

            String name = cfg.getString(path + ".name");
            String factionName = cfg.getString(path + ".faction");
            Location zoneA = stringToLoc(cfg.getString(path + ".zoneA"));
            Location zoneB = stringToLoc(cfg.getString(path + ".zoneB"));
            Location center = stringToLoc(cfg.getString(path + ".center"));
            if (name == null || factionName == null || zoneA == null || zoneB == null || center == null) continue;

            villages.put(id, new Village(id, name, factionName, zoneA, zoneB, center));
            Village loaded = villages.get(id);
            if (cfg.contains(path + ".port")) loaded.setPort(stringToLoc(cfg.getString(path + ".port")));
            if (cfg.contains(path + ".station")) loaded.setStation(stringToLoc(cfg.getString(path + ".station")));
        }
        plugin.getLogger().info(villages.size() + " village(s) chargé(s).");
    }

    private String locToString(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    private Location stringToLoc(String s) {
        if (s == null) return null;
        String[] p = s.split(";");
        if (p.length != 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }
}
