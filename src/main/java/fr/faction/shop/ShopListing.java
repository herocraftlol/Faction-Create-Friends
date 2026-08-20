package fr.faction.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Représente une annonce de vente dans le shop global.
 */
public class ShopListing {

    public enum Currency {
        IRON_INGOT(Material.IRON_INGOT, "Lingot de fer"),
        GOLD_INGOT(Material.GOLD_INGOT, "Lingot d'or"),
        DIAMOND(Material.DIAMOND, "Diamant"),
        EMERALD(Material.EMERALD, "Émeraude");

        private final Material material;
        private final String displayName;

        Currency(Material material, String displayName) {
            this.material = material;
            this.displayName = displayName;
        }

        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }

        public static Currency fromMaterial(Material m) {
            for (Currency c : values()) if (c.material == m) return c;
            return null;
        }
    }

    private final String id;           // identifiant unique de l'annonce
    private final UUID sellerUUID;
    private final String sellerName;
    private ItemStack item;            // l'item en vente (peut être null si vendu)
    private final int price;
    private final Currency currency;
    private boolean sold;

    public ShopListing(UUID sellerUUID, String sellerName, ItemStack item, int price, Currency currency) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.currency = currency;
        this.sold = false;
    }

    // Pour la désérialisation depuis le fichier
    public ShopListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
                        int price, Currency currency, boolean sold) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.currency = currency;
        this.sold = sold;
    }

    public String getId()              { return id; }
    public UUID getSellerUUID()        { return sellerUUID; }
    public String getSellerName()      { return sellerName; }
    public ItemStack getItem()         { return item; }
    public int getPrice()              { return price; }
    public Currency getCurrency()      { return currency; }
    public boolean isSold()            { return sold; }

    public void setSold(boolean sold)  { this.sold = sold; }

    /** Sous-total en monnaie : prix × quantité de l'item */
    public int getTotalPrice() {
        return price * item.getAmount();
    }

    /** Description courte pour identifier l'annonce dans les commandes */
    public String getShortDesc() {
        String mat = item.getType().name().toLowerCase().replace("_", " ");
        return id + " (" + item.getAmount() + "× " + mat + ")";
    }
}
