package fr.faction.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * GUI du comptoir d'échange — paginé.
 *
 * Layout (6 rangées × 9 = 54 slots) :
 *  Slots 0–44  → ordres actifs (5 rangées × 9 = 45 max par page)
 *  Slot 45     → Page précédente
 *  Slot 49     → Info (page courante)
 *  Slot 52     → Mes ordres
 *  Slot 53     → Page suivante
 *
 * Clic sur un ordre (vue principale) : fournit l'item demandé depuis l'inventaire du joueur
 * (autant de lots que possible en une fois).
 * Clic gauche sur un de "Mes ordres" : collecter les items reçus en attente.
 * Clic droit sur un de "Mes ordres" : annuler l'ordre (rembourse monnaie restante + items reçus).
 */
public class ExchangeGUI implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final String GUI_TITLE_PREFIX = "§8§l[§d§lÉchanges§8§l] ";
    private static final String MY_ORDERS_TITLE_PREFIX = "§8§l[§d§lMes Ordres§8§l] ";

    private final JavaPlugin plugin;
    private final ExchangeManager exchangeManager;

    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, Boolean> awaitingDeposit = new HashMap<>();

    public ExchangeGUI(JavaPlugin plugin, ExchangeManager exchangeManager) {
        this.plugin = plugin;
        this.exchangeManager = exchangeManager;
    }

    // ─── OUVERTURE ──────────────────────────────────────────────────────────────

    public void openExchange(Player player) {
        openExchange(player, playerPage.getOrDefault(player.getUniqueId(), 0));
    }

    public void openExchange(Player player, int page) {
        List<ExchangeOrder> orders = exchangeManager.getActiveOrders();

        int maxPages = Math.max(1, (int) Math.ceil((double) orders.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, maxPages - 1));
        playerPage.put(player.getUniqueId(), page);

        String title = GUI_TITLE_PREFIX + "§8(" + (page + 1) + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, orders.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildOrderItem(orders.get(i), player));
        }

        inv.setItem(45, page > 0 ? makeControl(Material.ARROW, "§a◀ Page précédente", "") : makeGlass());
        inv.setItem(46, makeControl(Material.EMERALD, "§a➕ Créer un ordre",
                "§7Dépose la monnaie tenue en main\n§7contre un item demandé, par lot.\n§7Clic pour commencer."));
        inv.setItem(47, makeGlass());
        inv.setItem(48, makeGlass());
        inv.setItem(49, makeControl(Material.PAPER, "§fPage " + (page + 1) + " / " + maxPages,
                "§7" + orders.size() + " ordre(s) actif(s)"));
        inv.setItem(50, makeGlass());
        inv.setItem(51, makeGlass());
        inv.setItem(52, makeControl(Material.CHEST, "§d📦 Mes ordres", "§7Voir / gérer tes ordres d'échange"));
        inv.setItem(53, page < maxPages - 1 ? makeControl(Material.ARROW, "§aPage suivante ▶", "") : makeGlass());

        player.openInventory(inv);
    }

    public void openMyOrders(Player player) {
        List<ExchangeOrder> mine = exchangeManager.getCreatorOrders(player.getUniqueId());
        String title = MY_ORDERS_TITLE_PREFIX + "§8(" + mine.size() + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (int i = 0; i < Math.min(mine.size(), 45); i++) {
            inv.setItem(i, buildMyOrderItem(mine.get(i)));
        }
        inv.setItem(49, makeControl(Material.ARROW, "§aRetour au comptoir d'échange", ""));
        player.openInventory(inv);
    }

    // ─── CONSTRUCTION D'ITEMS ───────────────────────────────────────────────────

    private ItemStack buildOrderItem(ExchangeOrder order, Player viewer) {
        ItemStack display = new ItemStack(order.getRequestedMaterial(), Math.min(64, order.getRequestedAmountPerBatch()));
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        meta.setDisplayName("§e" + order.getRequestedAmountPerBatch() + "× §f" + formatMat(order.getRequestedMaterial()));
        List<String> lore = new ArrayList<>();
        lore.add("§8ID: §7" + order.getId());
        lore.add("§7Créateur: §f" + order.getCreatorName());
        lore.add("");
        lore.add("§7Demande: §e" + order.getRequestedAmountPerBatch() + "× " + formatMat(order.getRequestedMaterial()) + " §7par lot");
        lore.add("§7Paye:    §6" + order.getPricePerBatch() + " " + order.getCurrency().getDisplayName() + "(s) §7par lot");
        lore.add("§7Lots restants: §a" + order.getRemainingBatches()
                + " §7(" + order.getRemainingCurrency() + " " + order.getCurrency().getDisplayName() + "(s) dispo)");
        lore.add("");
        if (order.getCreatorUUID().equals(viewer.getUniqueId())) {
            lore.add("§7C'est ton ordre — gère-le via §e/faction mesordres");
        } else {
            lore.add("§a[Clic] Fournir avec les items de ton inventaire");
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildMyOrderItem(ExchangeOrder order) {
        ItemStack display = new ItemStack(order.getRequestedMaterial(), Math.min(64, order.getRequestedAmountPerBatch()));
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        meta.setDisplayName("§b" + order.getRequestedAmountPerBatch() + "× §f" + formatMat(order.getRequestedMaterial()));
        List<String> lore = new ArrayList<>();
        lore.add("§8ID: §7" + order.getId());
        lore.add("§7Demande: §e" + order.getRequestedAmountPerBatch() + "× " + formatMat(order.getRequestedMaterial()) + " §7par lot");
        lore.add("§7Paye:    §6" + order.getPricePerBatch() + " " + order.getCurrency().getDisplayName() + "(s) §7par lot");
        lore.add("§7Stock restant: §a" + order.getRemainingCurrency() + " " + order.getCurrency().getDisplayName() + "(s) "
                + "§7(" + order.getRemainingBatches() + " lot(s))");
        lore.add("§7Items reçus en attente: §e" + order.getCollectedAmount() + "× " + formatMat(order.getRequestedMaterial()));
        lore.add("");
        lore.add("§a[Clic gauche] Collecter les items reçus");
        lore.add("§c[Clic droit] Annuler l'ordre §7(rembourse monnaie + items)");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack makeControl(Material mat, String name, String loreStr) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(name);
        if (!loreStr.isEmpty()) {
            List<String> lore = new ArrayList<>();
            Collections.addAll(lore, loreStr.split("\n"));
            meta.setLore(lore);
        }
        is.setItemMeta(meta);
        return is;
    }

    private ItemStack makeGlass() {
        ItemStack is = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); is.setItemMeta(meta); }
        return is;
    }

    // ─── EVENTS ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        // ── Comptoir d'échange ───────────────────────────────────────────────────
        if (title.startsWith(GUI_TITLE_PREFIX)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            int slot = e.getRawSlot();
            int page = playerPage.getOrDefault(player.getUniqueId(), 0);

            if (slot < 45) {
                List<ExchangeOrder> orders = exchangeManager.getActiveOrders();
                int idx = page * PAGE_SIZE + slot;
                if (idx >= orders.size()) return;
                ExchangeOrder order = orders.get(idx);

                if (order.getCreatorUUID().equals(player.getUniqueId())) {
                    player.sendMessage("§8[§dÉchange§8] §eC'est ton ordre — utilise §f/faction mesordres §epour le gérer.");
                } else {
                    handleFulfill(player, order.getId());
                }
            } else if (slot == 45) {
                if (page > 0) openExchange(player, page - 1);
            } else if (slot == 46) {
                player.closeInventory();
                awaitingDeposit.put(player.getUniqueId(), true);
                player.sendMessage("§8[§dÉchange§8] §eTiens la monnaie à déposer (fer/or/diamant/émeraude) dans ta main,");
                player.sendMessage("§8[§dÉchange§8] §epuis tape dans le chat : §f<item> <quantité_par_lot> <prix_par_lot>");
                player.sendMessage("§8[§dÉchange§8] §7Exemple (avec 60 fer en main) : §fpierre 32 5" + "§7  (ou §fannuler§7)");
            } else if (slot == 52) {
                openMyOrders(player);
            } else if (slot == 53) {
                openExchange(player, page + 1);
            }
            return;
        }

        // ── Mes ordres ────────────────────────────────────────────────────────────
        if (title.startsWith(MY_ORDERS_TITLE_PREFIX)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            int slot = e.getRawSlot();

            if (slot == 49) {
                openExchange(player);
                return;
            }
            if (slot < 45) {
                List<ExchangeOrder> mine = exchangeManager.getCreatorOrders(player.getUniqueId());
                if (slot >= mine.size()) return;
                ExchangeOrder order = mine.get(slot);

                if (e.getClick() == ClickType.RIGHT) {
                    handleCancel(player, order.getId());
                } else {
                    handleCollect(player, order.getId());
                }
                openMyOrders(player); // refresh
            }
        }
    }

    // ─── LOGIQUE ────────────────────────────────────────────────────────────────

    private void handleFulfill(Player supplier, String id) {
        ExchangeManager.FulfillResult result = exchangeManager.fulfill(supplier, id, 0);
        switch (result.status) {
            case SUCCESS -> {
                supplier.sendMessage("§8[§dÉchange§8] §aTu as fourni §e" + result.itemsGiven + "× "
                        + ExchangeManager.formatMat(result.order.getRequestedMaterial())
                        + " §aet reçu §6" + result.currencyReceived + " " + result.order.getCurrency().getDisplayName() + "(s)§a !");
                Bukkit.getScheduler().runTask(plugin, () -> openExchange(supplier));
            }
            case NOT_ENOUGH_ITEMS -> supplier.sendMessage("§8[§dÉchange§8] §cTu n'as pas assez de "
                    + ExchangeManager.formatMat(result.order.getRequestedMaterial()) + " (besoin d'au moins "
                    + result.order.getRequestedAmountPerBatch() + ").");
            case DEPLETED -> { supplier.sendMessage("§8[§dÉchange§8] §cCet ordre n'a plus de stock disponible."); openExchange(supplier); }
            case NOT_FOUND -> { supplier.sendMessage("§8[§dÉchange§8] §cOrdre introuvable."); openExchange(supplier); }
            case OWN_ORDER -> supplier.sendMessage("§8[§dÉchange§8] §cTu ne peux pas fournir ton propre ordre !");
        }
    }

    private void handleCollect(Player creator, String id) {
        ExchangeManager.CollectStatus result = exchangeManager.collect(creator, id);
        switch (result) {
            case SUCCESS -> creator.sendMessage("§8[§dÉchange§8] §aItems collectés !");
            case NOTHING_TO_COLLECT -> creator.sendMessage("§8[§dÉchange§8] §7Rien à collecter pour l'instant.");
            case NOT_FOUND -> creator.sendMessage("§8[§dÉchange§8] §cOrdre introuvable.");
            case NOT_OWNER -> creator.sendMessage("§8[§dÉchange§8] §cCe n'est pas ton ordre.");
        }
    }

    private void handleCancel(Player creator, String id) {
        ExchangeManager.CancelStatus result = exchangeManager.cancel(creator, id);
        switch (result) {
            case SUCCESS -> creator.sendMessage("§8[§dÉchange§8] §aOrdre annulé — monnaie restante et items reçus rendus.");
            case NOT_FOUND -> creator.sendMessage("§8[§dÉchange§8] §cOrdre introuvable.");
            case NOT_OWNER -> creator.sendMessage("§8[§dÉchange§8] §cCe n'est pas ton ordre.");
        }
    }

    private static String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    // ─── SAISIE CHAT (création d'un ordre depuis le GUI) ───────────────────────

    public boolean isAwaitingDeposit(UUID uuid) {
        return awaitingDeposit.getOrDefault(uuid, false);
    }

    /**
     * Traite la saisie chat "<item> <quantité_par_lot> <prix_par_lot>" déclenchée
     * par le bouton "➕ Créer un ordre" du GUI.
     */
    public void handleDepositInput(Player player, String input) {
        awaitingDeposit.remove(player.getUniqueId());

        if (input.equalsIgnoreCase("annuler") || input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§8[§dÉchange§8] §7Création annulée.");
            return;
        }

        String[] parts = input.trim().split("\\s+");
        if (parts.length < 3) {
            player.sendMessage("§8[§dÉchange§8] §cFormat invalide. Attendu : §f<item> <quantité_par_lot> <prix_par_lot>");
            player.sendMessage("§8[§dÉchange§8] §7Exemple : §fpierre 32 5");
            return;
        }

        Material requestedMaterial = ItemAliasUtil.resolve(parts[0]);
        if (requestedMaterial == null || !requestedMaterial.isItem()) {
            player.sendMessage("§8[§dÉchange§8] §cItem '" + parts[0] + "' inconnu.");
            return;
        }

        int amountPerBatch;
        int pricePerBatch;
        try {
            amountPerBatch = Integer.parseInt(parts[1]);
            pricePerBatch = Integer.parseInt(parts[2]);
            if (amountPerBatch <= 0 || pricePerBatch <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§8[§dÉchange§8] §cLa quantité et le prix par lot doivent être des entiers positifs.");
            return;
        }

        ExchangeManager.CreateResult result = exchangeManager.createOrder(player, requestedMaterial, amountPerBatch, pricePerBatch);
        switch (result.status) {
            case SUCCESS -> {
                ExchangeOrder o = result.order;
                player.sendMessage("§8[§dÉchange§8] §aOrdre créé ! §e" + o.getRequestedAmountPerBatch() + "× "
                        + formatMat(o.getRequestedMaterial()) + " §a→ §6" + o.getPricePerBatch() + " "
                        + o.getCurrency().getDisplayName() + "(s) §apar lot §7[ID: " + o.getId() + "]");
                player.sendMessage("§8[§dÉchange§8] §7Stock déposé : §e" + o.getRemainingCurrency() + " "
                        + o.getCurrency().getDisplayName() + "(s) §7(" + o.getRemainingBatches() + " lot(s)).");
                Bukkit.getScheduler().runTask(plugin, () -> openExchange(player));
            }
            case NO_CURRENCY_IN_HAND -> player.sendMessage("§8[§dÉchange§8] §cTu ne tiens rien dans ta main !");
            case INVALID_CURRENCY -> player.sendMessage("§8[§dÉchange§8] §cSeuls le fer, l'or, le diamant et l'émeraude "
                    + "peuvent être déposés comme monnaie.");
            case BUDGET_TOO_LOW -> player.sendMessage("§8[§dÉchange§8] §cTu dois déposer au moins " + pricePerBatch
                    + " pour financer un lot.");
        }
    }
}
