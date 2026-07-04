package fr.faction.claim;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * GUI pour gérer les permissions d'un chunk claimé.
 * Ouvrir via openPermGUI(player, chunk).
 * Affiche les joueurs en ligne + membres de la faction, toggle autorisé/refusé.
 */
public class ClaimPermissionGUI implements Listener {

    private static final String TITLE_PREFIX = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Claim — Permissions";

    private final JavaPlugin plugin;
    private final ClaimManager claimManager;
    private final FactionManager factionManager;

    /** UUID du viewer → chunk en cours de gestion */
    private final Map<UUID, org.bukkit.Chunk> openChunks = new HashMap<>();

    public ClaimPermissionGUI(JavaPlugin plugin, ClaimManager claimManager, FactionManager factionManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.factionManager = factionManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openPermGUI(Player player, org.bukkit.Chunk chunk) {
        ClaimManager.ClaimData data = claimManager.getClaim(chunk);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "Ce chunk n'est pas claimé.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX);
        openChunks.put(player.getUniqueId(), chunk);

        // Bordure
        ItemStack border = makeItem(Material.GREEN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)   inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)  inv.setItem(i, border);
        for (int i = 17; i < 54; i += 9) inv.setItem(i, border);

        // Info du claim
        inv.setItem(4, makeItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "Chunk [" + chunk.getX() + ", " + chunk.getZ() + "]",
                ChatColor.GRAY + "Monde : " + chunk.getWorld().getName(),
                ChatColor.GRAY + "Faction : §e" + data.getFactionName(),
                "",
                ChatColor.YELLOW + "Cliquez sur un joueur pour",
                ChatColor.YELLOW + "autoriser / révoquer l'accès."));

        // Joueurs disponibles : en ligne + membres faction (sans doublons)
        Set<UUID> candidates = new LinkedHashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) candidates.add(online.getUniqueId());
        }
        Faction faction = factionManager.getFaction(data.getFactionName());
        if (faction != null) candidates.addAll(faction.getMembers());

        int slot = 10;
        for (UUID uuid : candidates) {
            if (slot > 44 || slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
            boolean allowed = data.isAllowed(uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                meta.setDisplayName((allowed ? ChatColor.GREEN : ChatColor.RED) + name);
                List<String> lore = new ArrayList<>();
                lore.add(allowed ? ChatColor.GREEN + "✔ Autorisé" : ChatColor.RED + "✘ Refusé");
                lore.add("");
                lore.add(ChatColor.GRAY + "Clic : basculer la permission");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8) slot++; // sauter la bordure droite
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        org.bukkit.Chunk chunk = openChunks.get(player.getUniqueId());
        if (chunk == null) return;

        ClaimManager.ClaimData data = claimManager.getClaim(chunk);
        if (data == null) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        UUID target = meta.getOwningPlayer().getUniqueId();
        String targetName = meta.getOwningPlayer().getName();
        if (targetName == null) targetName = target.toString().substring(0, 8);

        if (data.isAllowed(target)) {
            claimManager.denyPlayer(chunk, target);
            player.sendMessage(ChatColor.RED + "✘ Accès retiré à §e" + targetName);
        } else {
            claimManager.allowPlayer(chunk, target);
            player.sendMessage(ChatColor.GREEN + "✔ Accès accordé à §e" + targetName);
        }

        // Rafraîchir le GUI
        Bukkit.getScheduler().runTask(plugin, () -> openPermGUI(player, chunk));
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
