package fr.faction.managers;

import fr.faction.models.Faction;
import fr.faction.sort.SortMenuGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    // Injecté après construction pour éviter le cycle
    private SortMenuGUI sortMenuGUI;

    public SharedInventoryManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "shared_inventories.yml");
        loadInventories();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setSortMenuGUI(SortMenuGUI gui) { this.sortMenuGUI = gui; }

    // ─── Slot réservé au bouton de tri ──────────────────────────────────────────
    /** Le slot 53 (dernier) est réservé au bouton de tri — on ne le sauvegarde pas */
    private static final int SORT_BUTTON_SLOT = 53;

    private ItemStack buildSortButton() {
        ItemStack btn = new ItemStack(Material.HOPPER);
        ItemMeta meta = btn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6⬡ §e§lOrganiser le coffre");
            meta.setLore(Arrays.asList(
                    "§7Trie et regroupe les items",
                    "§7selon plusieurs critères.",
                    "",
                    "§7Modes disponibles :",
                    "§8• §bSimilaires  §8• §aCatégorie",
                    "§8• §eAlphabétique  §8• §6Quantité",
                    "§8• §dRareté",
                    "",
                    "§e➤ Clic pour ouvrir"
            ));
            btn.setItemMeta(meta);
        }
        return btn;
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

        // Toujours placer le bouton de tri
        inv.setItem(SORT_BUTTON_SLOT, buildSortButton());

        openInventories.put(player.getUniqueId(), key);
        player.openInventory(inv);
    }

    /** Appeler lors du disband pour vider la référence */
    public void deleteFactionInventory(String factionName) {
        sharedInventories.remove(factionName.toLowerCase());
        saveInventories();
    }

    /** Retourne ou crée l'inventaire partagé d'une faction */
    public Inventory getOrCreateSharedInventory(String factionName) {
        String key = factionName.toLowerCase();
        return sharedInventories.computeIfAbsent(key, k ->
                Bukkit.createInventory(null, 54,
                        ChatColor.GOLD + "⬡ " + ChatColor.YELLOW + factionName
                                + ChatColor.GOLD + " — Coffre Partagé"));
    }

    // ─── Events ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openInventories.containsKey(uuid)) return;
        openInventories.remove(uuid);
        saveInventories();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.containsKey(player.getUniqueId())) return;

        // Empêcher de prendre/déplacer le bouton de tri
        if (event.getRawSlot() == SORT_BUTTON_SLOT) {
            event.setCancelled(true);
            // Ouvrir le menu de tri
            if (sortMenuGUI != null) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> sortMenuGUI.openForSharedChest(player));
            }
        }
        // Les autres clics sont libres
    }

    // ─── Persistance ─────────────────────────────────────────────────────────────

    public void saveInventories() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Inventory> entry : sharedInventories.entrySet()) {
            String key = entry.getKey();
            Inventory inv = entry.getValue();
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (i == SORT_BUTTON_SLOT) continue; // ne pas persister le bouton
                if (contents[i] != null && contents[i].getType() != Material.AIR) {
                    cfg.set("inventories." + key + ".slot-" + i, contents[i]);
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
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
                if (i == SORT_BUTTON_SLOT) continue;
                ItemStack item = cfg.getItemStack("inventories." + key + ".slot-" + i);
                if (item != null) inv.setItem(i, item);
            }
            // Placer le bouton de tri au chargement aussi
            inv.setItem(SORT_BUTTON_SLOT, buildSortButton());
            sharedInventories.put(key, inv);
        }
        plugin.getLogger().info(sharedInventories.size() + " inventaire(s) partagé(s) chargé(s).");
    }
}
