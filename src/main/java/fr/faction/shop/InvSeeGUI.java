package fr.faction.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /invsee <joueur> — Permet aux admins (permission faction.admin) de voir
 * l'inventaire complet d'un joueur en lecture seule.
 *
 * Layout (6 rangées) :
 *   Rangées 0-3 (36 slots) → inventaire principal du joueur (slots 9-44 Bukkit)
 *   Rangée 4 (9 slots)     → hotbar du joueur (slots 0-8 Bukkit)
 *   Rangée 5 (9 slots)     → armure (0-3) + offhand (4) + vitraux déco (5-8)
 */
public class InvSeeGUI implements Listener {

    private static final String TITLE_PREFIX = "§8[§cInvSee§8] §f";
    private final JavaPlugin plugin;

    public InvSeeGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean openInvSee(Player admin, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage("§8[§cInvSee§8] §cJoueur introuvable ou hors ligne : §f" + targetName);
            return false;
        }
        if (!admin.hasPermission("faction.admin")) {
            admin.sendMessage("§8[§cInvSee§8] §cTu n'as pas la permission (faction.admin).");
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + target.getName());

        // Inventaire principal slots 9-44 → GUI slots 0-35
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 9; i <= 44; i++) {
            inv.setItem(i - 9, contents[i]);
        }
        // Hotbar slots 0-8 → GUI slots 36-44
        for (int i = 0; i < 9; i++) {
            inv.setItem(36 + i, contents[i]);
        }
        // Armure → GUI slots 45-48
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            inv.setItem(45 + i, armor[i]);
        }
        // Offhand → GUI slot 49
        inv.setItem(49, target.getInventory().getItemInOffHand());

        admin.openInventory(inv);
        admin.sendMessage("§8[§cInvSee§8] §7Inventaire de §f" + target.getName() + " §7(lecture seule).");
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (title.startsWith(TITLE_PREFIX)) {
            // Lecture seule — on annule tous les clics
            e.setCancelled(true);
        }
    }
}
