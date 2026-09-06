package fr.faction.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Annonce du shop global.
 *
 * Deux modes de prix :
 *  - CURRENCY  : prix en items de monnaie fixe (fer, or, diamant, émeraude)
 *  - BARTER    : prix en n'importe quel item (troc libre)
 *
 * Exemple troc : 16× cobblestone contre 2× steak
 *   → itemForSale = cobblestone×16, priceItem = steak×2, mode = BARTER
 */
public class ShopListing {

    // ── Mode de prix ────────────────────────────────────────────────────────────
    public enum PriceMode { CURRENCY, BARTER }

    // ── Monnaies fixes (rétro-compatibilité) ────────────────────────────────────
    public enum Currency {
        IRON_INGOT (Material.IRON_INGOT,  "Lingot de fer"),
        GOLD_INGOT (Material.GOLD_INGOT,  "Lingot d'or"),
        DIAMOND    (Material.DIAMOND,     "Diamant"),
        EMERALD    (Material.EMERALD,     "Émeraude");

        private final Material material;
        private final String displayName;
        Currency(Material m, String d) { this.material = m; this.displayName = d; }
        public Material getMaterial()  { return material; }
        public String getDisplayName() { return displayName; }
        public static Currency fromMaterial(Material m) {
            for (Currency c : values()) if (c.material == m) return c;
            return null;
        }
    }

    // ── Champs ──────────────────────────────────────────────────────────────────
    private final String id;
    private final UUID   sellerUUID;
    private final String sellerName;

    /** L'item mis en vente (avec sa quantité) */
    private final ItemStack itemForSale;

    /** Mode de prix */
    private final PriceMode priceMode;

    /** Prix en monnaie fixe (priceMode = CURRENCY) */
    private final Currency currency;   // null si BARTER
    private final int      currencyAmount;

    /** Prix en item de troc (priceMode = BARTER) */
    private final ItemStack priceItem; // null si CURRENCY — quantité incluse dans l'ItemStack

    private boolean sold;

    // ── Constructeur CURRENCY ───────────────────────────────────────────────────
    public ShopListing(UUID sellerUUID, String sellerName,
                        ItemStack itemForSale, Currency currency, int currencyAmount) {
        this.id             = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sellerUUID     = sellerUUID;
        this.sellerName     = sellerName;
        this.itemForSale    = itemForSale.clone();
        this.priceMode      = PriceMode.CURRENCY;
        this.currency       = currency;
        this.currencyAmount = currencyAmount;
        this.priceItem      = null;
        this.sold           = false;
    }

    // ── Constructeur BARTER ─────────────────────────────────────────────────────
    public ShopListing(UUID sellerUUID, String sellerName,
                        ItemStack itemForSale, ItemStack priceItem) {
        this.id             = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sellerUUID     = sellerUUID;
        this.sellerName     = sellerName;
        this.itemForSale    = itemForSale.clone();
        this.priceMode      = PriceMode.BARTER;
        this.currency       = null;
        this.currencyAmount = 0;
        this.priceItem      = priceItem.clone();
        this.sold           = false;
    }

    // ── Constructeur désérialisation ────────────────────────────────────────────
    public ShopListing(String id, UUID sellerUUID, String sellerName,
                        ItemStack itemForSale, PriceMode priceMode,
                        Currency currency, int currencyAmount, ItemStack priceItem,
                        boolean sold) {
        this.id             = id;
        this.sellerUUID     = sellerUUID;
        this.sellerName     = sellerName;
        this.itemForSale    = itemForSale;
        this.priceMode      = priceMode;
        this.currency       = currency;
        this.currencyAmount = currencyAmount;
        this.priceItem      = priceItem;
        this.sold           = sold;
    }

    // ── Getters ─────────────────────────────────────────────────────────────────
    public String    getId()             { return id; }
    public UUID      getSellerUUID()     { return sellerUUID; }
    public String    getSellerName()     { return sellerName; }
    public ItemStack getItem()           { return itemForSale; }
    public ItemStack getItemForSale()    { return itemForSale; }
    public PriceMode getPriceMode()      { return priceMode; }
    public Currency  getCurrency()       { return currency; }
    public int       getCurrencyAmount() { return currencyAmount; }
    public ItemStack getPriceItem()      { return priceItem; }
    public boolean   isSold()           { return sold; }
    public void      setSold(boolean s)  { this.sold = s; }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Ligne de prix formatée pour l'affichage */
    public String getPriceLine() {
        if (priceMode == PriceMode.CURRENCY) {
            return "§e" + currencyAmount + " §f" + currency.getDisplayName() + "(s)";
        } else {
            return "§e" + priceItem.getAmount() + "× §f" + displayName(priceItem);
        }
    }

    /** Ligne de l'item en vente */
    public String getItemLine() {
        return "§e" + itemForSale.getAmount() + "× §f" + displayName(itemForSale);
    }

    /** ID court + description pour les commandes texte */
    public String getShortDesc() {
        return id + " (" + itemForSale.getAmount() + "× " + itemForSale.getType().name().toLowerCase() + ")";
    }

    /** Nom affiché d'un ItemStack */
    public static String displayName(ItemStack is) {
        if (is == null) return "?";
        ItemMeta meta = is.getItemMeta();
        if (meta != null && meta.hasDisplayName())
            return org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        String name = is.getType().name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" "))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }
}
