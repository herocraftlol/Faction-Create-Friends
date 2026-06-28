package fr.faction.managers;

import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SharedInventoryManager implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;

    // factionName (lowercase) → inventaire partagé
    private final Map<String, Inventory> sharedInventories = new HashMap<>();
    // UUID joueur → nom faction (pour savoir quel inventaire fermer/sauvegarder)
    private final Map<UUID, String> openInventories = new HashMap<>();

    private File dataFile;

    public SharedInventoryManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "shared_inventories.yml");
        loadInventories();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ─── API publique ────────────────────────────────────────────────────────────

    public void openSharedInventory(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(ChatColor.RED + "[Faction] Tu n'es pas dans une faction.");
            return;
        }
        String key = faction.getName().toLowerCase();
        Inventory inv = sharedInventories.computeIfAbsent(key, k ->
                Bukkit.createInventory(null, 54,
                        ChatColor.GOLD + "⬡ " + ChatColor.YELLOW + faction.getName()
                                + ChatColor.GOLD + " — Coffre Partagé"));

        openInventories.put(player.getUniqueId(), key);
        player.openInventory(inv);
    }

    /** Appeler lors du disband pour vider la référence */
    public void deleteFactionInventory(String factionName) {
        sharedInventories.remove(factionName.toLowerCase());
        saveInventories();
    }

    // ─── Events ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!openInventories.containsKey(uuid)) return;
        openInventories.remove(uuid);
        // Sauvegarder après chaque fermeture
        saveInventories();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!openInventories.containsKey(player.getUniqueId())) return;
        // Laisser passer librement : le joueur peut mettre/prendre des items
        // La sauvegarde se fait à la fermeture
    }

    // ─── Persistance ─────────────────────────────────────────────────────────────

    public void saveInventories() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Inventory> entry : sharedInventories.entrySet()) {
            String key = entry.getKey();
            Inventory inv = entry.getValue();
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    cfg.set("inventories." + key + ".slot-" + i, contents[i]);
                }
            }
        }
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde inventaires partagés : " + e.getMessage());
        }
    }

    public void loadInventories() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("inventories")) return;

        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("inventories")).getKeys(false)) {
            Inventory inv = Bukkit.createInventory(null, 54,
                    ChatColor.GOLD + "⬡ " + ChatColor.YELLOW + key + ChatColor.GOLD + " — Coffre Partagé");
            for (int i = 0; i < 54; i++) {
                ItemStack item = cfg.getItemStack("inventories." + key + ".slot-" + i);
                if (item != null) inv.setItem(i, item);
            }
            sharedInventories.put(key, inv);
        }
        plugin.getLogger().info(sharedInventories.size() + " inventaire(s) partagé(s) chargé(s).");
    }
}
