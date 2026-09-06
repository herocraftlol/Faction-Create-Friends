package fr.faction.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
 * GUI du shop global — paginé, avec barre de recherche (signe de dialogue).
 *
 * Layout (6 rangées × 9 = 54 slots) :
 *  Slots 0–44  → items en vente (5 rangées × 9 = 45 max par page)
 *  Slot 45     → Page précédente
 *  Slot 46     → Recherche (clic → ferme GUI, demande texte dans le chat)
 *  Slot 48     → Trier par prix ↑
 *  Slot 49     → Info (page courante)
 *  Slot 50     → Trier par prix ↓
 *  Slot 52     → Mes annonces
 *  Slot 53     → Page suivante
 */
public class ShopGUI implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final String GUI_TITLE_PREFIX = "§8§l[§6§lShop Global§8§l] ";

    private final JavaPlugin plugin;
    private final ShopManager shopManager;

    // État par joueur
    private final Map<UUID, Integer> playerPage       = new HashMap<>();
    private final Map<UUID, String>  playerSearch     = new HashMap<>();
    private final Map<UUID, Boolean> awaitingSearch   = new HashMap<>();
    private final Map<UUID, SortMode> playerSort      = new HashMap<>();

    public enum SortMode { NONE, PRICE_ASC, PRICE_DESC }

    private ShopCreateGUI createGUI;

    public ShopGUI(JavaPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    public void setCreateGUI(ShopCreateGUI gui) { this.createGUI = gui; }

    // ─── OUVERTURE ──────────────────────────────────────────────────────────────

    public void openShop(Player player) {
        openShop(player, 0, playerSearch.getOrDefault(player.getUniqueId(), ""),
                playerSort.getOrDefault(player.getUniqueId(), SortMode.NONE));
    }

    public void openShop(Player player, int page, String search, SortMode sort) {
        playerPage.put(player.getUniqueId(), page);
        playerSearch.put(player.getUniqueId(), search);
        playerSort.put(player.getUniqueId(), sort);

        List<ShopListing> listings = search.isEmpty()
                ? shopManager.getActiveListings()
                : shopManager.searchListings(search);

        // Tri
        if (sort == SortMode.PRICE_ASC)  listings.sort(Comparator.comparingInt(l -> l.getPriceMode() == ShopListing.PriceMode.CURRENCY ? l.getCurrencyAmount() : l.getPriceItem().getAmount()));
        if (sort == SortMode.PRICE_DESC) listings.sort(Comparator.<ShopListing>comparingInt(l -> l.getPriceMode() == ShopListing.PriceMode.CURRENCY ? l.getCurrencyAmount() : l.getPriceItem().getAmount()).reversed());

        int maxPages = Math.max(1, (int) Math.ceil((double) listings.size() / PAGE_SIZE));
        page = Math.min(page, maxPages - 1);
        playerPage.put(player.getUniqueId(), page);

        String title = GUI_TITLE_PREFIX + (search.isEmpty() ? "" : "§7[" + search + "] ")
                + "§8(" + (page + 1) + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Items
        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, listings.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildListingItem(listings.get(i), player));
        }

        // Contrôles
        inv.setItem(45, page > 0 ? makeControl(Material.ARROW, "§a◀ Page précédente", "") : makeGlass(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(46, makeControl(Material.OAK_SIGN, "§e🔍 Recherche",
                search.isEmpty() ? "§7Clic : saisir un mot-clé" : "§7Actuelle : §f" + search + "\n§7Clic : changer"));
        inv.setItem(47, makeControl(Material.WRITABLE_BOOK, "§a§l+ Créer une annonce", "§7Mets n'importe quel item en vente.\n§7Mode monnaie ou troc libre.\n§eClic pour ouvrir"));
        inv.setItem(48, makeControl(Material.GOLD_NUGGET, "§6Trier : prix §a↑",
                sort == SortMode.PRICE_ASC ? "§a✔ Actif" : "§7Clic pour activer"));
        inv.setItem(49, makeControl(Material.PAPER, "§fPage " + (page + 1) + " / " + maxPages,
                "§7" + listings.size() + " annonce(s)"));
        inv.setItem(50, makeControl(Material.IRON_NUGGET, "§6Trier : prix §c↓",
                sort == SortMode.PRICE_DESC ? "§a✔ Actif" : "§7Clic pour activer"));
        inv.setItem(51, makeGlass(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(52, makeControl(Material.CHEST, "§b📦 Mes annonces", "§7Voir tes annonces actives"));
        inv.setItem(53, page < maxPages - 1 ? makeControl(Material.ARROW, "§aPage suivante ▶", "") : makeGlass(Material.GRAY_STAINED_GLASS_PANE));

        player.openInventory(inv);
    }

    /** Ouvre la vue "Mes annonces" du joueur */
    public void openMyListings(Player player) {
        List<ShopListing> mine = shopManager.getSellerListings(player.getUniqueId());
        String title = "§8§l[§b§lMes Annonces§8§l] §8(" + mine.size() + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (int i = 0; i < Math.min(mine.size(), 45); i++) {
            inv.setItem(i, buildMyListingItem(mine.get(i)));
        }
        inv.setItem(49, makeControl(Material.ARROW, "§aRetour au shop", ""));
        player.openInventory(inv);
    }

    // ─── CONSTRUCTION D'ITEMS ───────────────────────────────────────────────────

    private ItemStack buildListingItem(ShopListing listing, Player viewer) {
        ItemStack display = listing.getItemForSale().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String originalName = meta.hasDisplayName()
                ? meta.getDisplayName()
                : ShopListing.displayName(listing.getItemForSale());

        meta.setDisplayName("§e" + listing.getItemForSale().getAmount() + "× §f" + originalName);
        List<String> lore = new ArrayList<>();
        lore.add("§8ID: §7" + listing.getId());
        lore.add("§7Vendeur: §f" + listing.getSellerName());
        lore.add("");

        if (listing.getPriceMode() == ShopListing.PriceMode.CURRENCY) {
            lore.add("§7Prix : " + listing.getPriceLine());
            lore.add("§8Mode : §7Monnaie");
        } else {
            lore.add("§7Contre : " + listing.getPriceLine());
            lore.add("§8Mode : §7Troc 🔄");
            // Petite icône visuelle du prix
            if (listing.getPriceItem() != null) {
                lore.add("§8(" + ShopListing.displayName(listing.getPriceItem()) + " ×" + listing.getPriceItem().getAmount() + ")");
            }
        }

        lore.add("");
        if (listing.getSellerUUID().equals(viewer.getUniqueId())) {
            lore.add("§c[Clic gauche] Récupérer l'annonce");
        } else {
            lore.add("§a[Clic gauche] Acheter / Échanger");
            if (listing.getPriceMode() == ShopListing.PriceMode.CURRENCY) {
                lore.add("§7(tu as besoin de : " + listing.getPriceLine() + "§7)");
            } else {
                lore.add("§7(tu dois donner : " + listing.getPriceLine() + "§7)");
            }
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildMyListingItem(ShopListing listing) {
        ItemStack display = listing.getItemForSale().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String originalName = meta.hasDisplayName()
                ? meta.getDisplayName()
                : ShopListing.displayName(listing.getItemForSale());

        meta.setDisplayName("§b" + listing.getItemForSale().getAmount() + "× §f" + originalName);
        List<String> lore = new ArrayList<>();
        lore.add("§8ID: §7" + listing.getId());
        lore.add("§7" + (listing.getPriceMode() == ShopListing.PriceMode.BARTER ? "Troc 🔄 : " : "Prix : ")
                + listing.getPriceLine());
        lore.add("");
        lore.add("§c[Clic gauche] Récupérer l'item");
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
            for (String line : loreStr.split("\n")) lore.add(line);
            meta.setLore(lore);
        }
        is.setItemMeta(meta);
        return is;
    }

    private ItemStack makeGlass(Material mat) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); is.setItemMeta(meta); }
        return is;
    }

    // ─── EVENTS ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        // ── Shop global ──────────────────────────────────────────────────────────
        if (title.startsWith(GUI_TITLE_PREFIX)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            int slot = e.getRawSlot();
            int page = playerPage.getOrDefault(player.getUniqueId(), 0);
            String search = playerSearch.getOrDefault(player.getUniqueId(), "");
            SortMode sort = playerSort.getOrDefault(player.getUniqueId(), SortMode.NONE);

            if (slot < 45) {
                // Clic sur un item en vente
                List<ShopListing> listings = search.isEmpty()
                        ? shopManager.getActiveListings()
                        : shopManager.searchListings(search);
                if (sort == SortMode.PRICE_ASC)  listings.sort(Comparator.comparingInt(l -> l.getPriceMode() == ShopListing.PriceMode.CURRENCY ? l.getCurrencyAmount() : l.getPriceItem().getAmount()));
                if (sort == SortMode.PRICE_DESC) listings.sort(Comparator.<ShopListing>comparingInt(l -> l.getPriceMode() == ShopListing.PriceMode.CURRENCY ? l.getCurrencyAmount() : l.getPriceItem().getAmount()).reversed());

                int idx = page * PAGE_SIZE + slot;
                if (idx >= listings.size()) return;
                ShopListing listing = listings.get(idx);

                if (listing.getSellerUUID().equals(player.getUniqueId())) {
                    // Récupérer sa propre annonce
                    handleRecover(player, listing.getId());
                } else {
                    handleBuy(player, listing.getId());
                }

            } else if (slot == 45) {
                if (page > 0) openShop(player, page - 1, search, sort);
            } else if (slot == 46) {
                // Recherche
                player.closeInventory();
                awaitingSearch.put(player.getUniqueId(), true);
                player.sendMessage("§8[§6Shop§8] §eTape ton mot-clé dans le chat (ou §7annuler §epour fermer) :");
            } else if (slot == 47) {
                // Créer une annonce
                player.closeInventory();
                if (createGUI != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> createGUI.open(player));
                }
            } else if (slot == 48) {
                openShop(player, 0, search, sort == SortMode.PRICE_ASC ? SortMode.NONE : SortMode.PRICE_ASC);
            } else if (slot == 50) {
                openShop(player, 0, search, sort == SortMode.PRICE_DESC ? SortMode.NONE : SortMode.PRICE_DESC);
            } else if (slot == 52) {
                openMyListings(player);
            } else if (slot == 53) {
                openShop(player, page + 1, search, sort);
            }
            return;
        }

        // ── Mes annonces ─────────────────────────────────────────────────────────
        if (title.startsWith("§8§l[§b§lMes Annonces")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            int slot = e.getRawSlot();

            if (slot == 49) {
                openShop(player);
                return;
            }
            if (slot < 45) {
                List<ShopListing> mine = shopManager.getSellerListings(player.getUniqueId());
                if (slot >= mine.size()) return;
                ShopListing listing = mine.get(slot);
                handleRecover(player, listing.getId());
                openMyListings(player); // refresh
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Rien à faire, l'état est conservé pour la prochaine ouverture
    }

    // ─── LOGIQUE ────────────────────────────────────────────────────────────────

    private void handleBuy(Player buyer, String id) {
        ShopManager.BuyResult result = shopManager.buy(buyer, id);
        switch (result) {
            case SUCCESS -> {
                ShopListing l = shopManager.findById(id);
                buyer.sendMessage("§8[§6Shop§8] §aAchat effectué ! (" + id + ")");
                // refresh GUI
                Bukkit.getScheduler().runTask(plugin, () -> openShop(buyer));
            }
            case NOT_ENOUGH_PAYMENT -> buyer.sendMessage("§8[§6Shop§8] §cTu n'as pas assez d'argent !");
            case ALREADY_SOLD     -> { buyer.sendMessage("§8[§6Shop§8] §cCet article a déjà été vendu !"); openShop(buyer); }
            case NOT_FOUND        -> { buyer.sendMessage("§8[§6Shop§8] §cAnnonce introuvable."); openShop(buyer); }
            case OWN_LISTING      -> buyer.sendMessage("§8[§6Shop§8] §cTu ne peux pas acheter ton propre article !");
        }
    }

    private void handleRecover(Player seller, String id) {
        ShopManager.RecoverResult result = shopManager.recover(seller, id);
        switch (result) {
            case SUCCESS    -> seller.sendMessage("§8[§6Shop§8] §aItem récupéré !");
            case NOT_FOUND  -> seller.sendMessage("§8[§6Shop§8] §cAnnonce introuvable.");
            case NOT_OWNER  -> seller.sendMessage("§8[§6Shop§8] §cCe n'est pas ton annonce.");
            case ALREADY_SOLD -> seller.sendMessage("§8[§6Shop§8] §cL'item a déjà été vendu, tu aurais dû recevoir le paiement.");
        }
    }

    // ─── RECHERCHE CHAT ─────────────────────────────────────────────────────────

    public boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.getOrDefault(uuid, false);
    }

    public void handleSearchInput(Player player, String input) {
        awaitingSearch.remove(player.getUniqueId());
        if (input.equalsIgnoreCase("annuler") || input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§8[§6Shop§8] §7Recherche annulée.");
            return;
        }
        playerSearch.put(player.getUniqueId(), input.toLowerCase());
        openShop(player, 0, input.toLowerCase(), playerSort.getOrDefault(player.getUniqueId(), SortMode.NONE));
    }

    private static String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }
}
