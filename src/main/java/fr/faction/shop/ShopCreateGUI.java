package fr.faction.shop;

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
 * GUI de création d'annonce Shop — version claire et simple.
 *
 * Layout 4 rangées (36 slots) :
 *
 *  Rangée 0 : [instructions] [ITEM] [•] [CONTRE] [PRIX/MONNAIE] [•] [aperçu]
 *  Rangée 1 : [◀ qté item] [qté item affiché] [▶ qté item] [SEP] [◀ qté prix] [qté prix affiché] [▶ qté prix]
 *  Rangée 2 : [mode MONNAIE] [fer|or|diamant|emeraude] [mode TROC]
 *  Rangée 3 : [✔ CONFIRMER] [✗ ANNULER]
 *
 * Simplifié vs ancienne version :
 *  - Slot ITEM (slot 10) : le joueur shift-clique depuis son inventaire OU dépose l'item avec le curseur
 *  - Slot PRIX (slot 14) : pareil
 *  - Boutons −10 / −1 / +1 / +10 directs
 *  - Preview en slot 16 : résumé de l'annonce
 *  - Instructions claires en slot 4
 */
public class ShopCreateGUI implements Listener {

    private static final String TITLE = "§8§l[ §6§lNouvelle Annonce §8§l]";

    // Layout slots
    private static final int SL_INSTRUCTIONS = 4;
    private static final int SL_ITEM         = 10; // zone dépôt item en vente
    private static final int SL_SEPARATOR    = 13;
    private static final int SL_PRICE_ZONE   = 16; // zone dépôt item prix (mode troc) / monnaie
    private static final int SL_PREVIEW      = 22;
    // Qty item
    private static final int SL_ITEM_MM  = 19, SL_ITEM_M = 20, SL_ITEM_QTY = 21, SL_ITEM_P = 22, SL_ITEM_PP = 23;
    // Qty prix
    private static final int SL_PRICE_MM = 25, SL_PRICE_M = 26, SL_PRICE_QTY = 27, SL_PRICE_P = 28, SL_PRICE_PP = 29;
    // Modes
    private static final int SL_MODE_CURR   = 30;
    private static final int SL_CURR_IRON   = 31, SL_CURR_GOLD = 32, SL_CURR_DIAMOND = 33, SL_CURR_EMERALD = 34;
    private static final int SL_MODE_BARTER = 35;
    // Actions
    private static final int SL_CONFIRM = 27; // re-utilisé dans la 4e rangée
    private static final int SL_CANCEL  = 35; // re-utilisé dans la 4e rangée

    // On va utiliser un layout à 5 rangées (45 slots) pour avoir de l'espace
    // Rangée 0 (0-8)    : header/info
    // Rangée 1 (9-17)   : item à vendre (slot 11) | sep (13) | item prix (slot 15)
    // Rangée 2 (18-26)  : qté item | sep | qté prix
    // Rangée 3 (27-35)  : modes de paiement (monnaie: fer/or/dia/eme, troc)
    // Rangée 4 (36-44)  : confirmer | annuler

    private final int SLOT_ITEM   = 11;
    private final int SLOT_PRICE  = 15;
    private final int SLOT_ITEM_M10 = 18, SLOT_ITEM_M1 = 19, SLOT_ITEM_QTY_D = 20, SLOT_ITEM_P1 = 21, SLOT_ITEM_P10 = 22;
    private final int SLOT_PRICE_M10 = 24, SLOT_PRICE_M1 = 25, SLOT_PRICE_QTY_D = 26, SLOT_PRICE_P1 = 27, SLOT_PRICE_P10 = 28;
    private final int SLOT_CURR_IRON    = 29;
    private final int SLOT_CURR_GOLD    = 30;
    private final int SLOT_CURR_DIAMOND = 31;
    private final int SLOT_CURR_EMERALD = 32;
    private final int SLOT_MODE_BARTER  = 33;
    private final int SLOT_CONFIRM = 38;
    private final int SLOT_CANCEL  = 40;

    private static class State {
        ItemStack sellItem;
        int sellQty = 1;
        ItemStack priceItem; // null si mode monnaie
        int priceQty = 1;
        ShopListing.PriceMode mode = ShopListing.PriceMode.CURRENCY;
        ShopListing.Currency currency = ShopListing.Currency.EMERALD;
    }

    private final JavaPlugin plugin;
    private final ShopManager shopManager;
    private final Map<UUID, State> states = new HashMap<>();
    // Inventaire ouvert par UUID (pour pouvoir le modifier sans le fermer/rouvrir)
    private final Map<UUID, Inventory> openInvs = new HashMap<>();

    public ShopCreateGUI(JavaPlugin plugin, ShopManager shopManager) {
        this.plugin      = plugin;
        this.shopManager = shopManager;
    }

    // ── Ouverture ────────────────────────────────────────────────────────────────

    public void open(Player player) {
        states.put(player.getUniqueId(), new State());
        Inventory inv = buildInv(player);
        openInvs.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    // ── Construction ─────────────────────────────────────────────────────────────

    private Inventory buildInv(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        fillInv(inv, player);
        return inv;
    }

    /** Met à jour l'inventaire existant sans le fermer (pas de bug client drag) */
    private void refresh(Player player) {
        Inventory inv = openInvs.get(player.getUniqueId());
        if (inv == null) return;
        fillInv(inv, player);
    }

    private void fillInv(Inventory inv, Player player) {
        State s = states.get(player.getUniqueId());
        if (s == null) return;

        // Vider tout
        inv.clear();

        // ── Rangée 0 — header ────────────────────────────────────────────────────
        ItemStack bgDark = glass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, bgDark);

        inv.setItem(0, label(Material.PAPER, "§e§l📋 Mode d'emploi",
                "§71. §fShift-clique §7l'item à vendre depuis ton inventaire",
                "§7   → il apparaît dans §e[ Item en vente ]",
                "§72. §fAjuste la quantité §7avec §e- §7et §e+",
                "§73. §fChoisis le prix §7(monnaie ou troc)",
                "§74. §fClique §a§l✔ CONFIRMER §7pour mettre en vente",
                "",
                "§8L'item est retiré de ton inventaire.",
                "§8Tu peux le récupérer avec §7/fac recuperer <ID>§8."));

        inv.setItem(4, label(Material.GOLD_INGOT, "§6§l⬡ Annonce en cours",
                s.sellItem == null ? "§cAucun item sélectionné" :
                    "§fVente : §e" + s.sellQty + "× " + ShopListing.displayName(s.sellItem),
                s.sellItem == null ? "" :
                    "§fPrix : " + getPriceDesc(s)));

        inv.setItem(8, label(Material.BARRIER, "§c§l✗ ANNULER",
                "§7Ferme ce menu.",
                "§8Aucun item retiré."));

        // ── Rangée 1 — zones de dépôt ────────────────────────────────────────────
        // Item en vente
        inv.setItem(9,  label(Material.LIME_STAINED_GLASS_PANE,  "§a§l▼ ITEM À VENDRE",   "§7Shift-clic depuis ton inventaire"));
        inv.setItem(10, label(Material.LIME_STAINED_GLASS_PANE,  "§a§l▼ ITEM À VENDRE",   "§7Shift-clic depuis ton inventaire"));
        inv.setItem(SLOT_ITEM, s.sellItem != null
                ? annotate(s.sellItem, s.sellQty, "§aItem en vente", true)
                : placeholder(Material.LIME_DYE, "§a§l[ Item en vente ]",
                        "§7Shift-clique un item depuis",
                        "§7ton inventaire ci-dessous.", "", "§8Tous types acceptés."));
        inv.setItem(12, label(Material.LIME_STAINED_GLASS_PANE,  "§a§l▼ ITEM À VENDRE",   "§7Shift-clic depuis ton inventaire"));

        // Séparateur
        inv.setItem(13, label(Material.WHITE_STAINED_GLASS_PANE, "§f⟷ CONTRE"));

        // Prix
        inv.setItem(14, label(Material.YELLOW_STAINED_GLASS_PANE,  "§6§l▼ PRIX",             "§7Monnaie ou item de troc"));
        if (s.mode == ShopListing.PriceMode.BARTER) {
            inv.setItem(SLOT_PRICE, s.priceItem != null
                    ? annotate(s.priceItem, s.priceQty, "§6Prix (troc)", false)
                    : placeholder(Material.GOLD_NUGGET, "§6§l[ Item prix ]",
                            "§7Shift-clique l'item que tu veux",
                            "§7recevoir en échange.", "", "§8Ex: 2× steak, 4× pain…"));
        } else {
            // Monnaie sélectionnée
            ItemStack cur = new ItemStack(s.currency.getMaterial(), Math.min(64, s.priceQty));
            ItemMeta cm = cur.getItemMeta();
            if (cm != null) {
                cm.setDisplayName("§e" + s.priceQty + "× §f" + s.currency.getDisplayName());
                cm.setLore(Arrays.asList("§7Mode : §aMonnaie fixe", "§7Clic pour changer de monnaie →"));
                cur.setItemMeta(cm);
            }
            inv.setItem(SLOT_PRICE, cur);
        }
        inv.setItem(16, label(Material.YELLOW_STAINED_GLASS_PANE,  "§6§l▼ PRIX",             "§7Monnaie ou item de troc"));
        inv.setItem(17, label(Material.YELLOW_STAINED_GLASS_PANE,  "§6§l▼ PRIX",             "§7Monnaie ou item de troc"));

        // ── Rangée 2 — quantités ─────────────────────────────────────────────────
        inv.setItem(18, qtyBtn(Material.RED_TERRACOTTA,   "§c§l−10", "§7Retire 10"));
        inv.setItem(19, qtyBtn(Material.ORANGE_TERRACOTTA,"§6§l−1",  "§7Retire 1  §8│ §7Clic droit: −5"));
        inv.setItem(20, qtyDisplay(s.sellQty, "§aQté en vente", s.sellItem));
        inv.setItem(21, qtyBtn(Material.LIME_TERRACOTTA,  "§a§l+1",  "§7Ajoute 1  §8│ §7Clic droit: +5"));
        inv.setItem(22, qtyBtn(Material.GREEN_TERRACOTTA, "§2§l+10", "§7Ajoute 10"));

        inv.setItem(23, glass(Material.GRAY_STAINED_GLASS_PANE));

        inv.setItem(24, qtyBtn(Material.RED_TERRACOTTA,   "§c§l−10", "§7Retire 10 du prix"));
        inv.setItem(25, qtyBtn(Material.ORANGE_TERRACOTTA,"§6§l−1",  "§7Retire 1  §8│ §7Clic droit: −5"));
        inv.setItem(26, qtyDisplay(s.priceQty, "§6Qté prix", s.priceItem != null ? s.priceItem :
                new ItemStack(s.currency.getMaterial())));

        // pas de +1/+10 pour le prix en rangée 2 (manque de place) - scroll aussi
        inv.setItem(27, qtyBtn(Material.LIME_TERRACOTTA,  "§a§l+1",  "§7Ajoute 1  §8│ §7Clic droit: +5"));
        inv.setItem(28, qtyBtn(Material.GREEN_TERRACOTTA, "§2§l+10", "§7Ajoute 10 au prix"));

        // ── Rangée 3 — modes de paiement ─────────────────────────────────────────
        boolean isCurr = s.mode == ShopListing.PriceMode.CURRENCY;
        inv.setItem(29, modeItem(Material.EMERALD, isCurr, "§a§l💰 Mode Monnaie",
                "§7Prix en item fixe :", "§8Lingot fer/or, diamant, émeraude",
                isCurr ? "§a✔ Actif" : "§7Clic pour activer"));

        // Sélecteurs monnaie (gris si mode troc actif)
        ShopListing.Currency[] curs = ShopListing.Currency.values();
        int[] curSlots = {30, 31, 32, 33};
        for (int ci = 0; ci < curs.length; ci++) {
            boolean sel = isCurr && curs[ci] == s.currency;
            ItemStack ci_item = new ItemStack(curs[ci].getMaterial());
            ItemMeta cm = ci_item.getItemMeta();
            if (cm != null) {
                cm.setDisplayName((sel ? "§a§l✔ " : (isCurr ? "§7" : "§8")) + curs[ci].getDisplayName());
                if (sel) cm.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                cm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                        org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                cm.setLore(Collections.singletonList(sel ? "§a✔ Sélectionné" : (isCurr ? "§7Clic pour choisir" : "§8Activer Mode Monnaie")));
                ci_item.setItemMeta(cm);
            }
            inv.setItem(curSlots[ci], isCurr ? ci_item : glass(Material.GRAY_STAINED_GLASS_PANE));
            if (!isCurr) { // montrer le nom quand même
                inv.setItem(curSlots[ci], label(Material.GRAY_STAINED_GLASS_PANE,
                        "§8" + curs[ci].getDisplayName(), "§8(mode troc actif)"));
            }
        }

        inv.setItem(34, modeItem(Material.GOLD_INGOT, !isCurr, "§6§l🔄 Mode Troc",
                "§7Prix en n'importe quel item.",
                "§8Ex: 16 cobble contre 2 steak",
                !isCurr ? "§a✔ Actif" : "§7Clic pour activer"));

        // ── Rangée 4 — confirmer/annuler ─────────────────────────────────────────
        ItemStack bgGlass = glass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 36; i < 45; i++) inv.setItem(i, bgGlass);

        boolean ready = canConfirm(s);
        inv.setItem(37, ready
                ? label(Material.LIME_STAINED_GLASS_PANE, "§a§l✔  CONFIRMER",
                        "§fVente : §e" + (s.sellItem != null ? s.sellQty + "× " + ShopListing.displayName(s.sellItem) : "?"),
                        "§fPrix : " + getPriceDesc(s),
                        "", "§aClic pour mettre en vente !")
                : label(Material.GRAY_STAINED_GLASS_PANE, "§8✔ Confirmer",
                        "§cDépose d'abord un item à vendre."));
        inv.setItem(38, ready
                ? label(Material.LIME_STAINED_GLASS_PANE, "§a§l✔  CONFIRMER",
                        "§aClic pour mettre en vente !")
                : label(Material.GRAY_STAINED_GLASS_PANE, "§8✔ Confirmer",
                        "§cItem manquant."));
        inv.setItem(39, label(Material.RED_STAINED_GLASS_PANE, "§c§l✗  ANNULER",
                "§7Ferme le menu.", "§8Aucun item retiré de ton inventaire."));
    }

    // ── Events ────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        State s = states.get(player.getUniqueId());
        if (s == null) { e.setCancelled(true); return; }

        int raw = e.getRawSlot();
        int invSize = e.getView().getTopInventory().getSize(); // 45

        // ── Shift-clic depuis l'inventaire du joueur ─────────────────────────────
        if (raw >= invSize && e.isShiftClick()) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (s.sellItem == null) {
                s.sellItem = stripToType(clicked);
                s.sellQty  = clicked.getAmount();
            } else if (s.mode == ShopListing.PriceMode.BARTER && s.priceItem == null) {
                s.priceItem = stripToType(clicked);
                s.priceQty  = clicked.getAmount();
            } else {
                player.sendMessage("§c[Shop] Les deux zones sont déjà remplies. Retire un item en cliquant dessus.");
            }
            refresh(player);
            return;
        }

        // Clic dans la partie basse → laisser passer (l'inventaire joueur)
        if (raw >= invSize) return;

        e.setCancelled(true); // bloquer tout le reste dans le GUI

        // ── Slot item en vente ────────────────────────────────────────────────────
        if (raw == SLOT_ITEM) {
            if (s.sellItem != null) {
                // Rendre l'item
                giveBack(player, s.sellItem, s.sellQty);
                s.sellItem = null; s.sellQty = 1;
                refresh(player);
            } else {
                // Essayer de prendre depuis le curseur
                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    s.sellItem = stripToType(cursor);
                    s.sellQty  = cursor.getAmount();
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    refresh(player);
                } else {
                    player.sendMessage("§7[Shop] Shift-clique un item depuis ton inventaire pour le déposer ici.");
                }
            }
            return;
        }

        // ── Slot prix (troc) ──────────────────────────────────────────────────────
        if (raw == SLOT_PRICE && s.mode == ShopListing.PriceMode.BARTER) {
            if (s.priceItem != null) {
                giveBack(player, s.priceItem, s.priceQty);
                s.priceItem = null; s.priceQty = 1;
                refresh(player);
            } else {
                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    s.priceItem = stripToType(cursor);
                    s.priceQty  = cursor.getAmount();
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    refresh(player);
                }
            }
            return;
        }

        // ── Sélecteurs de monnaie ─────────────────────────────────────────────────
        if (s.mode == ShopListing.PriceMode.CURRENCY) {
            ShopListing.Currency[] curs = ShopListing.Currency.values();
            int[] slots = {30, 31, 32, 33};
            for (int ci = 0; ci < slots.length; ci++) {
                if (raw == slots[ci]) { s.currency = curs[ci]; refresh(player); return; }
            }
        }

        // ── Quantité item vendu ───────────────────────────────────────────────────
        if (raw == 18) { s.sellQty = Math.max(1, s.sellQty - 10); refresh(player); return; }
        if (raw == 19) { s.sellQty = Math.max(1, s.sellQty - (e.isRightClick() ? 5 : 1)); refresh(player); return; }
        if (raw == 21) { s.sellQty = Math.min(2304, s.sellQty + (e.isRightClick() ? 5 : 1)); refresh(player); return; }
        if (raw == 22) { s.sellQty = Math.min(2304, s.sellQty + 10); refresh(player); return; }

        // ── Quantité prix ─────────────────────────────────────────────────────────
        if (raw == 24) { s.priceQty = Math.max(1, s.priceQty - 10); refresh(player); return; }
        if (raw == 25) { s.priceQty = Math.max(1, s.priceQty - (e.isRightClick() ? 5 : 1)); refresh(player); return; }
        if (raw == 27) { s.priceQty = Math.min(2304, s.priceQty + (e.isRightClick() ? 5 : 1)); refresh(player); return; }
        if (raw == 28) { s.priceQty = Math.min(2304, s.priceQty + 10); refresh(player); return; }

        // ── Modes ─────────────────────────────────────────────────────────────────
        if (raw == 29) { // Monnaie
            s.mode = ShopListing.PriceMode.CURRENCY;
            if (s.priceItem != null) { giveBack(player, s.priceItem, s.priceQty); s.priceItem = null; }
            refresh(player); return;
        }
        if (raw == 34) { // Troc
            s.mode = ShopListing.PriceMode.BARTER;
            refresh(player); return;
        }

        // ── Confirmer ─────────────────────────────────────────────────────────────
        if (raw == 37 || raw == 38) {
            if (!canConfirm(s)) {
                player.sendMessage("§c[Shop] Dépose d'abord un item à vendre !");
                return;
            }
            // Vérifier que le joueur possède l'item
            int inInv = countInInv(player, s.sellItem);
            if (inInv < s.sellQty) {
                player.sendMessage("§c[Shop] Tu n'as que §e" + inInv + "× " + ShopListing.displayName(s.sellItem)
                        + " §cdans ton inventaire (besoin : §e" + s.sellQty + "§c).");
                return;
            }
            removeFromInv(player, s.sellItem, s.sellQty);
            ItemStack forSale = s.sellItem.clone();
            forSale.setAmount(s.sellQty);
            ShopListing listing;
            if (s.mode == ShopListing.PriceMode.CURRENCY) {
                listing = shopManager.createCurrencyListing(player, forSale, s.currency, s.priceQty);
                player.sendMessage("§8[§6Shop§8] §aAnnonce créée !");
                player.sendMessage("§7  Vente  : §e" + s.sellQty + "× " + ShopListing.displayName(forSale));
                player.sendMessage("§7  Prix   : §e" + s.priceQty + "× " + s.currency.getDisplayName());
            } else {
                ItemStack price = s.priceItem.clone();
                price.setAmount(s.priceQty);
                listing = shopManager.createBarterListing(player, forSale, price);
                player.sendMessage("§8[§6Shop§8] §aAnnonce de troc créée !");
                player.sendMessage("§7  Vente  : §e" + s.sellQty + "× " + ShopListing.displayName(forSale));
                player.sendMessage("§7  Contre : §e" + s.priceQty + "× " + ShopListing.displayName(price));
            }
            player.sendMessage("§8ID : §7" + listing.getId() + " §8— §7/fac recuperer " + listing.getId());
            states.remove(player.getUniqueId());
            openInvs.remove(player.getUniqueId());
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            return;
        }

        // ── Annuler ───────────────────────────────────────────────────────────────
        if (raw == 8 || raw == 39) {
            cancelAndClose(player, s);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;
        State s = states.remove(player.getUniqueId());
        openInvs.remove(player.getUniqueId());
        if (s != null) {
            // Rendre les items déposés
            if (s.sellItem != null)  giveBack(player, s.sellItem, s.sellQty);
            if (s.priceItem != null) giveBack(player, s.priceItem, s.priceQty);
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────────

    private ItemStack glass(Material mat) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        if (m != null) { m.setDisplayName(" "); is.setItemMeta(m); }
        return is;
    }
    private ItemStack label(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta(); if (m == null) return is;
        m.setDisplayName(name);
        if (lore.length > 0) m.setLore(Arrays.asList(lore));
        m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        is.setItemMeta(m); return is;
    }
    private ItemStack placeholder(Material mat, String name, String... lore) {
        return label(mat, name, lore);
    }
    private ItemStack qtyBtn(Material mat, String name, String lore) {
        return label(mat, name, lore);
    }
    private ItemStack modeItem(Material mat, boolean active, String name, String... lore) {
        ItemStack is = label(mat, name, lore);
        if (active) {
            ItemMeta m = is.getItemMeta();
            if (m != null) {
                m.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                is.setItemMeta(m);
            }
        }
        return is;
    }
    private ItemStack qtyDisplay(int qty, String label, ItemStack ref) {
        Material mat = (ref != null && ref.getType() != Material.AIR) ? ref.getType() : Material.PAPER;
        ItemStack is = new ItemStack(mat, Math.max(1, Math.min(qty, mat.getMaxStackSize())));
        ItemMeta m = is.getItemMeta(); if (m == null) return is;
        m.setDisplayName(label);
        int stacks = qty / 64, rem = qty % 64;
        List<String> lore = new ArrayList<>();
        lore.add("§7Quantité : §e§l" + qty);
        if (stacks > 0) lore.add("§8= " + stacks + " stack(s)" + (rem > 0 ? " + " + rem : ""));
        m.setLore(lore); is.setItemMeta(m); return is;
    }
    private ItemStack annotate(ItemStack type, int qty, String header, boolean isSell) {
        ItemStack copy = type.clone();
        copy.setAmount(Math.max(1, Math.min(qty, type.getType().getMaxStackSize())));
        ItemMeta m = copy.getItemMeta(); if (m == null) return copy;
        if (!m.hasDisplayName()) m.setDisplayName("§f" + ShopListing.displayName(copy));
        List<String> lore = new ArrayList<>();
        lore.add("§8§m──────────");
        lore.add(header);
        lore.add("§7Quantité : §e" + qty);
        lore.add(isSell ? "§8Clic gauche : retirer" : "§8Clic gauche : retirer (mode troc)");
        m.setLore(lore); copy.setItemMeta(m); return copy;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Garde uniquement le type (et la meta), pas la quantité */
    private ItemStack stripToType(ItemStack is) {
        ItemStack copy = is.clone(); copy.setAmount(1); return copy;
    }

    private boolean canConfirm(State s) {
        if (s.sellItem == null || s.sellQty < 1) return false;
        if (s.mode == ShopListing.PriceMode.CURRENCY) return s.currency != null && s.priceQty > 0;
        return s.priceItem != null && s.priceQty > 0;
    }

    private String getPriceDesc(State s) {
        if (s.mode == ShopListing.PriceMode.CURRENCY)
            return "§e" + s.priceQty + "× §f" + (s.currency != null ? s.currency.getDisplayName() : "?");
        if (s.priceItem != null) return "§e" + s.priceQty + "× §f" + ShopListing.displayName(s.priceItem);
        return "§cNon défini";
    }

    private void giveBack(Player player, ItemStack type, int qty) {
        ItemStack give = type.clone(); give.setAmount(qty);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);
        leftover.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));
    }

    private int countInInv(Player player, ItemStack type) {
        if (type == null) return 0;
        int total = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == type.getType()) total += is.getAmount();
        return total;
    }

    private void removeFromInv(Player player, ItemStack type, int qty) {
        int rem = qty;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && rem > 0; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() != type.getType()) continue;
            int take = Math.min(is.getAmount(), rem);
            is.setAmount(is.getAmount() - take);
            if (is.getAmount() <= 0) contents[i] = null;
            rem -= take;
        }
        player.getInventory().setContents(contents);
    }

    private void cancelAndClose(Player player, State s) {
        states.remove(player.getUniqueId());
        openInvs.remove(player.getUniqueId());
        player.closeInventory();
    }
}
