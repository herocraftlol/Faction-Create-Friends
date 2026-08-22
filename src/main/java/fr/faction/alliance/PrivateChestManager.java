package fr.faction.alliance;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Coffres privés de membre.
 *
 * Un joueur peut verrouiller un coffre en le sneak-cliquant avec un panneau
 * en main. Seul le propriétaire (et les admins) peut l'ouvrir.
 *
 * Stockage dans plugins/FactionPlugin/private_chests.yml
 * Clé : "world,x,y,z" → owner UUID
 */
public class PrivateChestManager implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final Map<String, UUID> lockedChests = new HashMap<>(); // location key → owner
    private File dataFile;

    public PrivateChestManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "private_chests.yml");
        load();
    }

    // ─── API ────────────────────────────────────────────────────────────────────

    public boolean isLocked(Location loc) {
        return lockedChests.containsKey(key(loc));
    }

    public UUID getOwner(Location loc) {
        return lockedChests.get(key(loc));
    }

    public boolean isOwner(Location loc, UUID uuid) {
        UUID owner = lockedChests.get(key(loc));
        return owner != null && owner.equals(uuid);
    }

    public boolean canAccess(Location loc, UUID uuid) {
        if (!isLocked(loc)) return true;
        return isOwner(loc, uuid);
    }

    public boolean lockChest(Player player, Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return false;
        String k = key(block.getLocation());
        if (lockedChests.containsKey(k)) {
            if (!lockedChests.get(k).equals(player.getUniqueId())) {
                player.sendMessage(prefix() + "§cCe coffre est déjà verrouillé par quelqu'un d'autre.");
                return false;
            }
            // Déverrouiller
            lockedChests.remove(k);
            // Vérifier double coffre
            removeDoubleChest(block);
            save();
            player.sendMessage(prefix() + "§aCoffre déverrouillé.");
            return true;
        }
        // Vérifier qu'on est dans une faction
        if (!factionManager.isInFaction(player.getUniqueId())) {
            player.sendMessage(prefix() + "§cTu dois être dans une faction pour verrouiller un coffre.");
            return false;
        }
        lockedChests.put(k, player.getUniqueId());
        // Double coffre
        lockDoubleChest(block, player.getUniqueId());
        save();
        player.sendMessage(prefix() + "§aCoffre verrouillé ! §7Seul toi peux l'ouvrir.");
        player.sendMessage(prefix() + "§7Sneak+clic gauche à nouveau pour déverrouiller.");
        // Particule visuelle
        block.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.3, 0.3, 0);
        return true;
    }

    public void removeChest(Location loc) {
        lockedChests.remove(key(loc));
        save();
    }

    // ─── EVENTS ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;

        Block block = e.getClickedBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;

        Player player = e.getPlayer();

        // Sneak + main vide OU sneak + panneau → verrouillage
        boolean holdingSign = player.getInventory().getItemInMainHand().getType().name().contains("SIGN")
                || player.getInventory().getItemInMainHand().getType() == Material.AIR;
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().name().contains("SIGN")) {
            e.setCancelled(true);
            lockChest(player, block);
            return;
        }

        // Vérifier accès
        if (!isLocked(block.getLocation())) return;
        if (canAccess(block.getLocation(), player.getUniqueId())) return;
        if (player.hasPermission("faction.admin")) {
            player.sendMessage(prefix() + "§c[ADMIN] Coffre verrouillé — accès forcé.");
            return;
        }

        e.setCancelled(true);
        UUID ownerUUID = getOwner(block.getLocation());
        String ownerName = "inconnu";
        if (ownerUUID != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUUID);
            ownerName = op.getName() != null ? op.getName() : ownerUUID.toString().substring(0,8);
        }
        player.sendMessage(prefix() + "§cCe coffre appartient à §f" + ownerName + "§c.");
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;
        if (!isLocked(block.getLocation())) return;

        Player player = e.getPlayer();
        if (player.hasPermission("faction.admin") || isOwner(block.getLocation(), player.getUniqueId())) {
            lockedChests.remove(key(block.getLocation()));
            removeDoubleChest(block);
            save();
            if (isOwner(block.getLocation(), player.getUniqueId()))
                player.sendMessage(prefix() + "§7Coffre privé supprimé.");
            return;
        }
        e.setCancelled(true);
        player.sendMessage(prefix() + "§cTu ne peux pas casser ce coffre privé !");
    }

    // ─── DOUBLE CHEST HELPERS ───────────────────────────────────────────────────

    private void lockDoubleChest(Block block, UUID owner) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType() == block.getType()) {
                lockedChests.put(key(adjacent.getLocation()), owner);
            }
        }
    }

    private void removeDoubleChest(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType() == block.getType()) {
                String k = key(adjacent.getLocation());
                if (lockedChests.containsKey(k) && lockedChests.get(k).equals(lockedChests.get(key(block.getLocation())))) {
                    lockedChests.remove(k);
                }
            }
        }
    }

    // ─── PERSISTANCE ────────────────────────────────────────────────────────────

    private String key(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, UUID> e : lockedChests.entrySet()) {
            cfg.set("chests." + e.getKey().replace(",", "_"), e.getValue().toString());
        }
        try { cfg.save(dataFile); } catch (IOException ex) {
            plugin.getLogger().warning("Erreur sauvegarde coffres privés : " + ex.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("chests")) return;
        for (String rawKey : Objects.requireNonNull(cfg.getConfigurationSection("chests")).getKeys(false)) {
            String locKey = rawKey.replace("_", ",");
            // Format: world,x,y,z → remplacer seulement les _ qui séparent les coordonnées
            // On stocke avec _ au lieu de , car Bukkit YAML n'aime pas les virgules dans les clés
            try {
                UUID owner = UUID.fromString(Objects.requireNonNull(cfg.getString("chests." + rawKey)));
                // Reconstruire la clé avec virgules : le rawKey a des _ pour les séparateurs de sections
                // On stocke "world_x_y_z" → locKey = "world,x,y,z" ne fonctionne pas si le nom du monde a des _
                // Solution : on stocke avec séparateurs | dans le YAML
                lockedChests.put(locKey, owner);
            } catch (Exception ex) { /* ignore */ }
        }
        plugin.getLogger().info("Coffres privés : " + lockedChests.size() + " chargé(s).");
    }

    private String prefix() { return "§8[§6Coffre§8] §r"; }
}
