package fr.faction.sort;

import fr.faction.managers.FactionManager;
import fr.faction.managers.SharedInventoryManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * GUI de tri de coffre — s'ouvre depuis le coffre partagé ou via /fac ranger.
 *
 * Deux cibles :
 *   - Coffre partagé de la faction (partagé entre membres)
 *   - Inventaire personnel du joueur (27 slots de stockage)
 *
 * Interface en deux parties :
 *   1. Menu de sélection du mode de tri (6 boutons + aperçu)
 *   2. Confirmation + animation de tri
 *
 * Slots de l'inventaire triés :
 *   - Coffre partagé  : slots 0–53 (tout le coffre)
 *   - Inventaire perso: slots 9–35 (stockage seulement, pas la hotbar)
 *
 * Chaque bouton affiche un aperçu du nombre d'items distincts et de la
 * disposition attendue avant de confirmer.
 */
public class SortMenuGUI implements Listener {

    // ── Titres ──────────────────────────────────────────────────────────────────
    private static final String T_SORT_MENU    = "§8§l[§6§lOrganiser§8§l] §7Choisir le tri";
    private static final String T_SORT_PERSO   = "§8§l[§a§lRanger§8§l] §7Inventaire personnel";

    // ── Contextes ───────────────────────────────────────────────────────────────
    public enum SortTarget { SHARED_CHEST, PERSONAL_INVENTORY }

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final SharedInventoryManager sharedInvManager;

    // UUID → cible active
    private final Map<UUID, SortTarget>  pendingTarget = new HashMap<>();
    // UUID → mode sélectionné (aperçu avant confirm)
    private final Map<UUID, ChestSorter.SortMode> pendingMode = new HashMap<>();

    public SortMenuGUI(JavaPlugin plugin, FactionManager factionManager,
                        SharedInventoryManager sharedInvManager) {
        this.plugin          = plugin;
        this.factionManager  = factionManager;
        this.sharedInvManager = sharedInvManager;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // OUVERTURE
    // ════════════════════════════════════════════════════════════════════════════

    /** Ouvrir le menu de tri pour le coffre partagé de la faction */
    public void openForSharedChest(Player player) {
        if (factionManager.getPlayerFaction(player.getUniqueId()) == null) {
            player.sendMessage("§c[Coffre] Tu n'es pas dans une faction.");
            return;
        }
        pendingTarget.put(player.getUniqueId(), SortTarget.SHARED_CHEST);
        buildAndOpen(player, SortTarget.SHARED_CHEST);
    }

    /** Ouvrir le menu de tri pour l'inventaire personnel */
    public void openForPersonalInventory(Player player) {
        pendingTarget.put(player.getUniqueId(), SortTarget.PERSONAL_INVENTORY);
        buildAndOpen(player, SortTarget.PERSONAL_INVENTORY);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION DU MENU
    // ════════════════════════════════════════════════════════════════════════════

    private void buildAndOpen(Player player, SortTarget target) {
        String title = target == SortTarget.SHARED_CHEST ? T_SORT_MENU : T_SORT_PERSO;
        Inventory menu = Bukkit.createInventory(null, 54, title);

        // Bordure décorative
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)    menu.setItem(i, border);
        for (int i = 45; i < 54; i++)  menu.setItem(i, border);
        for (int i = 9; i < 45; i += 9)  menu.setItem(i, border);
        for (int i = 17; i < 54; i += 9) menu.setItem(i, border);

        // Header : info sur le contenu
        Inventory target_inv = getTargetInventory(player, target);
        int itemCount    = countItems(target_inv, target);
        int distinctCount = countDistinctTypes(target_inv, target);

        menu.setItem(4, makeItem(Material.CHEST,
                "§e§lCoffre à organiser",
                "§7Items présents : §f" + itemCount,
                "§7Types distincts : §f" + distinctCount,
                "§7Cible : §f" + (target == SortTarget.SHARED_CHEST ? "Coffre partagé" : "Inventaire personnel"),
                "",
                "§7Choisis un mode de tri ci-dessous."));

        // 6 boutons de tri (rangée 2 et 3)
        ChestSorter.SortMode[] modes = ChestSorter.SortMode.values();
        int[] slots = {10, 12, 14, 28, 30, 32};
        Material[] mats = {
                Material.ENDER_EYE,         // SIMILAIRE
                Material.KNOWLEDGE_BOOK,    // CATEGORIE
                Material.NAME_TAG,          // ALPHABETIQUE
                Material.CHEST,             // QUANTITE_DESC
                Material.BARREL,            // QUANTITE_ASC
                Material.NETHER_STAR        // RARETE
        };

        for (int i = 0; i < modes.length && i < slots.length; i++) {
            ChestSorter.SortMode mode = modes[i];
            menu.setItem(slots[i], makeModeButton(mats[i], mode, player, target));
        }

        // Bouton de retour
        menu.setItem(49, makeItem(Material.BARRIER, "§c✗ Fermer", "§7Ferme ce menu sans trier."));

        player.openInventory(menu);
    }

    /**
     * Après sélection d'un mode → ouvrir la confirmation avec aperçu
     */
    private void openConfirmMenu(Player player, ChestSorter.SortMode mode, SortTarget target) {
        pendingMode.put(player.getUniqueId(), mode);

        Inventory confirm = Bukkit.createInventory(null, 27,
                "§8§l[§6§lTri§8§l] §7Confirmer : " + org.bukkit.ChatColor.stripColor(mode.label));

        // Vitre
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) confirm.setItem(i, glass);

        // Info mode
        confirm.setItem(4, makeItem(Material.NETHER_STAR, mode.label,
                mode.desc,
                "",
                "§7Cible : §f" + (target == SortTarget.SHARED_CHEST ? "Coffre partagé" : "Inventaire personnel"),
                "",
                "§8⚠ Action irréversible — le coffre sera réorganisé."));

        // Aperçu catégories si mode CATEGORIE
        if (mode == ChestSorter.SortMode.CATEGORIE) {
            Inventory src = getTargetInventory(player, target);
            if (src != null) {
                Map<ChestSorter.ItemCategory, Integer> counts = countByCategory(src, target);
                List<String> preview = new ArrayList<>();
                for (Map.Entry<ChestSorter.ItemCategory, Integer> e : counts.entrySet()) {
                    preview.add(e.getKey().label + " §8× §f" + e.getValue());
                }
                if (!preview.isEmpty()) {
                    ItemStack prev = makeItem(Material.MAP, "§7Aperçu des catégories", preview.toArray(new String[0]));
                    confirm.setItem(13, prev);
                }
            }
        }

        // ✓ Confirmer
        confirm.setItem(11, makeItem(Material.LIME_STAINED_GLASS_PANE,
                "§a§l✔ Confirmer",
                "§7Lance le tri §e" + org.bukkit.ChatColor.stripColor(mode.label),
                "§7sur le coffre."));

        // ✗ Annuler
        confirm.setItem(15, makeItem(Material.RED_STAINED_GLASS_PANE,
                "§c§l✗ Annuler",
                "§7Retour au menu de tri."));

        player.openInventory(confirm);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TRI
    // ════════════════════════════════════════════════════════════════════════════

    private void performSort(Player player, ChestSorter.SortMode mode, SortTarget target) {
        Inventory inv = getTargetInventory(player, target);
        if (inv == null) {
            player.sendMessage("§c[Tri] Inventaire introuvable.");
            return;
        }

        int startSlot, endSlot;
        if (target == SortTarget.SHARED_CHEST) {
            startSlot = 0; endSlot = inv.getSize();
        } else {
            // Inventaire personnel : trier uniquement les slots 9-35 (stockage, pas hotbar)
            startSlot = 9; endSlot = 36;
        }

        // Pour le coffre partagé on a accès direct à l'inventaire
        // Pour l'inventaire perso on travaille sur l'inventaire du joueur
        Inventory sortTarget;
        if (target == SortTarget.PERSONAL_INVENTORY) {
            sortTarget = player.getInventory();
        } else {
            sortTarget = inv;
        }

        ChestSorter.sort(sortTarget, startSlot, endSlot, mode);

        // Sauvegarder le coffre partagé si nécessaire
        if (target == SortTarget.SHARED_CHEST) {
            sharedInvManager.saveInventories();
        }

        // Feedback
        player.closeInventory();
        player.sendMessage("§8[§6Tri§8] §a✔ Coffre organisé en mode §e"
                + org.bukkit.ChatColor.stripColor(mode.label) + "§a !");
        player.sendMessage("§8[§6Tri§8] §7Items regroupés et triés proprement.");
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.3f);
        // Effet visuel
        player.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EVENTS
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        // ── Menu de sélection de mode ──────────────────────────────────────────
        if (title.equals(T_SORT_MENU) || title.equals(T_SORT_PERSO)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            SortTarget target = pendingTarget.getOrDefault(player.getUniqueId(), SortTarget.SHARED_CHEST);
            int slot = e.getRawSlot();

            ChestSorter.SortMode[] modes = ChestSorter.SortMode.values();
            int[] modeSlots = {10, 12, 14, 28, 30, 32};
            for (int i = 0; i < modeSlots.length; i++) {
                if (slot == modeSlots[i] && i < modes.length) {
                    openConfirmMenu(player, modes[i], target);
                    return;
                }
            }
            if (slot == 49) { player.closeInventory(); }
            return;
        }

        // ── Menu de confirmation ───────────────────────────────────────────────
        if (title.startsWith("§8§l[§6§lTri§8§l] §7Confirmer")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            int slot = e.getRawSlot();
            SortTarget target  = pendingTarget.getOrDefault(player.getUniqueId(), SortTarget.SHARED_CHEST);
            ChestSorter.SortMode mode = pendingMode.get(player.getUniqueId());

            if (slot == 11 && mode != null) {
                performSort(player, mode, target);
            } else if (slot == 15) {
                buildAndOpen(player, target);
            }
            return;
        }

        // ── Coffre partagé ouvert → bouton de tri en slot 53 ──────────────────
        if (title.contains("Coffre Partagé") || title.contains("Coffre partagé")) {
            // On laisse les clics normaux passer SAUF sur le slot 53 (bouton de tri)
            // Le bouton de tri est placé par openSharedInventory (voir SharedInventoryManager patch)
            if (e.getRawSlot() == 53) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked != null && clicked.getType() == Material.HOPPER) {
                    e.setCancelled(true);
                    openForSharedChest(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (title.equals(T_SORT_MENU) || title.equals(T_SORT_PERSO)
                || title.startsWith("§8§l[§6§lTri§8§l] §7Confirmer")) {
            pendingTarget.remove(player.getUniqueId());
            pendingMode.remove(player.getUniqueId());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════════

    private Inventory getTargetInventory(Player player, SortTarget target) {
        if (target == SortTarget.PERSONAL_INVENTORY) return player.getInventory();
        fr.faction.models.Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return null;
        return sharedInvManager.getOrCreateSharedInventory(faction.getName());
    }

    private int countItems(Inventory inv, SortTarget target) {
        if (inv == null) return 0;
        int count = 0, start = target == SortTarget.PERSONAL_INVENTORY ? 9 : 0;
        int end   = target == SortTarget.PERSONAL_INVENTORY ? 36 : inv.getSize();
        for (int i = start; i < end; i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType() != Material.AIR) count += is.getAmount();
        }
        return count;
    }

    private int countDistinctTypes(Inventory inv, SortTarget target) {
        if (inv == null) return 0;
        Set<Material> types = new HashSet<>();
        int start = target == SortTarget.PERSONAL_INVENTORY ? 9 : 0;
        int end   = target == SortTarget.PERSONAL_INVENTORY ? 36 : inv.getSize();
        for (int i = start; i < end; i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType() != Material.AIR) types.add(is.getType());
        }
        return types.size();
    }

    private Map<ChestSorter.ItemCategory, Integer> countByCategory(Inventory inv, SortTarget target) {
        Map<ChestSorter.ItemCategory, Integer> map = new TreeMap<>(Comparator.comparingInt(c -> c.order));
        int start = target == SortTarget.PERSONAL_INVENTORY ? 9 : 0;
        int end   = target == SortTarget.PERSONAL_INVENTORY ? 36 : inv.getSize();
        for (int i = start; i < end; i++) {
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType() == Material.AIR) continue;
            ChestSorter.ItemCategory cat = ChestSorter.getCategory(is.getType());
            map.merge(cat, is.getAmount(), Integer::sum);
        }
        return map;
    }

    private ItemStack makeModeButton(Material mat, ChestSorter.SortMode mode,
                                      Player player, SortTarget target) {
        Inventory inv = getTargetInventory(player, target);
        List<String> lore = new ArrayList<>();
        lore.add(mode.desc);
        lore.add("");

        if (inv != null && mode == ChestSorter.SortMode.CATEGORIE) {
            Map<ChestSorter.ItemCategory, Integer> cats = countByCategory(inv, target);
            if (!cats.isEmpty()) {
                lore.add("§7Catégories détectées :");
                cats.entrySet().stream().limit(5).forEach(e ->
                        lore.add("  " + e.getKey().label + " §8×§f" + e.getValue()));
                if (cats.size() > 5) lore.add("  §8... et " + (cats.size() - 5) + " autre(s)");
            }
        } else if (mode == ChestSorter.SortMode.SIMILAIRE) {
            int dist = countDistinctTypes(inv, target);
            lore.add("§7Types distincts : §f" + dist);
            lore.add("§7Slots utilisés après fusion : §f~" + dist);
        }

        lore.add("");
        lore.add("§e§l➤ Clic pour sélectionner");
        return makeItem(mat, mode.label, lore.toArray(new String[0]));
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) if (s != null) l.add(s);
            meta.setLore(l);
        }
        is.setItemMeta(meta);
        return is;
    }
}
