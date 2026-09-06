package fr.faction.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ShopManager {

    private final JavaPlugin plugin;
    private final List<ShopListing> listings = new ArrayList<>();
    private File shopFile;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        load();
    }

    // ── Création d'annonce ───────────────────────────────────────────────────────

    /** Mode monnaie fixe */
    public ShopListing createCurrencyListing(Player seller, ItemStack itemForSale,
                                              ShopListing.Currency currency, int amount) {
        ShopListing l = new ShopListing(seller.getUniqueId(), seller.getName(),
                itemForSale, currency, amount);
        listings.add(l);
        save();
        return l;
    }

    /** Mode troc — priceItem contient déjà la quantité demandée */
    public ShopListing createBarterListing(Player seller, ItemStack itemForSale, ItemStack priceItem) {
        ShopListing l = new ShopListing(seller.getUniqueId(), seller.getName(), itemForSale, priceItem);
        listings.add(l);
        save();
        return l;
    }

    // ── Achat ────────────────────────────────────────────────────────────────────

    public enum BuyResult { SUCCESS, NOT_FOUND, ALREADY_SOLD, NOT_ENOUGH_PAYMENT, OWN_LISTING }

    public BuyResult buy(Player buyer, String listingId) {
        ShopListing listing = findById(listingId);
        if (listing == null)                                       return BuyResult.NOT_FOUND;
        if (listing.isSold())                                      return BuyResult.ALREADY_SOLD;
        if (listing.getSellerUUID().equals(buyer.getUniqueId()))   return BuyResult.OWN_LISTING;

        if (listing.getPriceMode() == ShopListing.PriceMode.CURRENCY) {
            int need = listing.getCurrencyAmount();
            if (countMaterial(buyer, listing.getCurrency().getMaterial()) < need)
                return BuyResult.NOT_ENOUGH_PAYMENT;
            removeMaterial(buyer, listing.getCurrency().getMaterial(), need);
            giveOrDrop(buyer, listing.getItemForSale().clone());
            paySellerCurrency(listing, buyer.getName(),
                    new ItemStack(listing.getCurrency().getMaterial(), need));
        } else {
            // Mode troc : l'acheteur doit avoir l'item de prix en quantité suffisante
            ItemStack need = listing.getPriceItem();
            if (countMaterial(buyer, need.getType()) < need.getAmount())
                return BuyResult.NOT_ENOUGH_PAYMENT;
            // Retirer le prix de l'acheteur
            removeItemSimilar(buyer, need.getType(), need.getAmount());
            giveOrDrop(buyer, listing.getItemForSale().clone());
            // Donner le prix au vendeur
            paySellerItem(listing, buyer.getName(), need.clone());
        }

        listing.setSold(true);
        save();
        return BuyResult.SUCCESS;
    }

    // ── Récupération ─────────────────────────────────────────────────────────────

    public enum RecoverResult { SUCCESS, NOT_FOUND, NOT_OWNER, ALREADY_SOLD }

    public RecoverResult recover(Player seller, String listingId) {
        ShopListing listing = findById(listingId);
        if (listing == null)                                          return RecoverResult.NOT_FOUND;
        if (!listing.getSellerUUID().equals(seller.getUniqueId()))    return RecoverResult.NOT_OWNER;
        if (listing.isSold())                                         return RecoverResult.ALREADY_SOLD;
        giveOrDrop(seller, listing.getItemForSale().clone());
        listings.remove(listing);
        save();
        return RecoverResult.SUCCESS;
    }

    // ── Paiements en attente (vendeur hors-ligne) ─────────────────────────────────

    public void deliverPendingPayments(Player player) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        String key = "pending." + player.getUniqueId();
        if (!cfg.contains(key)) return;
        List<?> raw = cfg.getList(key, new ArrayList<>());
        for (Object o : raw) {
            if (o instanceof ItemStack is) giveOrDrop(player, is.clone());
        }
        cfg.set(key, null);
        try { cfg.save(shopFile); } catch (IOException ignored) {}
        player.sendMessage("§8[§6Shop§8] §aTu as reçu des paiements en attente !");
    }

    // ── Filtres ───────────────────────────────────────────────────────────────────

    public List<ShopListing> getActiveListings() {
        return listings.stream().filter(l -> !l.isSold()).collect(Collectors.toList());
    }

    public List<ShopListing> searchListings(String query) {
        String q = query.toLowerCase();
        return getActiveListings().stream()
                .filter(l -> l.getItemForSale().getType().name().toLowerCase().contains(q)
                        || l.getSellerName().toLowerCase().contains(q)
                        || l.getId().toLowerCase().contains(q)
                        || (l.getPriceMode() == ShopListing.PriceMode.BARTER
                            && l.getPriceItem().getType().name().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    public List<ShopListing> getSellerListings(UUID sellerUUID) {
        return getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(sellerUUID))
                .collect(Collectors.toList());
    }

    public ShopListing findById(String id) {
        return listings.stream().filter(l -> l.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    // ── Utils ─────────────────────────────────────────────────────────────────────

    public static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));
            player.sendMessage("§8[§6Shop§8] §eInventaire plein — item(s) dropé(s) à tes pieds !");
        }
    }

    private int countMaterial(Player player, Material mat) {
        int total = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == mat) total += is.getAmount();
        return total;
    }

    private void removeMaterial(Player player, Material mat, int amount) {
        int rem = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && rem > 0; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() != mat) continue;
            int take = Math.min(is.getAmount(), rem);
            is.setAmount(is.getAmount() - take);
            if (is.getAmount() <= 0) contents[i] = null;
            rem -= take;
        }
        player.getInventory().setContents(contents);
    }

    /** Retire count items du type mat (peu importe enchantements/meta) */
    private void removeItemSimilar(Player player, Material mat, int amount) {
        removeMaterial(player, mat, amount);
    }

    private void paySellerCurrency(ShopListing listing, String buyerName, ItemStack payment) {
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            giveOrDrop(seller, payment);
            seller.sendMessage("§8[§6Shop§8] §aVente ! §e" + buyerName
                    + " §aa acheté " + listing.getItemLine()
                    + " §apour " + listing.getPriceLine() + "§a.");
        } else {
            savePendingPayment(listing.getSellerUUID(), payment);
        }
    }

    private void paySellerItem(ShopListing listing, String buyerName, ItemStack payment) {
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            giveOrDrop(seller, payment);
            seller.sendMessage("§8[§6Shop§8] §aVente ! §e" + buyerName
                    + " §aa échangé " + listing.getPriceLine()
                    + " §acontre tes " + listing.getItemLine() + "§a.");
        } else {
            savePendingPayment(listing.getSellerUUID(), payment);
        }
    }

    private void savePendingPayment(UUID sellerUUID, ItemStack payment) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        String key = "pending." + sellerUUID;
        List<ItemStack> pending = new ArrayList<>();
        List<?> raw = cfg.getList(key, new ArrayList<>());
        for (Object o : raw) if (o instanceof ItemStack is) pending.add(is);
        pending.add(payment);
        cfg.set(key, pending);
        try { cfg.save(shopFile); } catch (IOException ignored) {}
    }

    public static String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    // ── Persistance ───────────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        // Préserver les pending
        FileConfiguration existing = shopFile.exists()
                ? YamlConfiguration.loadConfiguration(shopFile) : new YamlConfiguration();
        if (existing.contains("pending")) cfg.set("pending", existing.get("pending"));

        int i = 0;
        for (ShopListing l : listings) {
            if (l.isSold()) continue;
            String base = "listings." + i;
            cfg.set(base + ".id",           l.getId());
            cfg.set(base + ".seller",       l.getSellerUUID().toString());
            cfg.set(base + ".sellerName",   l.getSellerName());
            cfg.set(base + ".item",         l.getItemForSale());
            cfg.set(base + ".priceMode",    l.getPriceMode().name());
            if (l.getPriceMode() == ShopListing.PriceMode.CURRENCY) {
                cfg.set(base + ".currency",       l.getCurrency().name());
                cfg.set(base + ".currencyAmount", l.getCurrencyAmount());
            } else {
                cfg.set(base + ".priceItem", l.getPriceItem());
            }
            i++;
        }
        try { cfg.save(shopFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde shop : " + e.getMessage());
        }
    }

    public void load() {
        listings.clear();
        if (!shopFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        if (!cfg.contains("listings")) return;
        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("listings")).getKeys(false)) {
            try {
                String base = "listings." + key;
                String id     = cfg.getString(base + ".id");
                UUID seller   = UUID.fromString(Objects.requireNonNull(cfg.getString(base + ".seller")));
                String sName  = cfg.getString(base + ".sellerName", "?");
                ItemStack item = cfg.getItemStack(base + ".item");
                String pmStr  = cfg.getString(base + ".priceMode", "CURRENCY");
                ShopListing.PriceMode pm = ShopListing.PriceMode.valueOf(pmStr);
                if (item == null) continue;

                if (pm == ShopListing.PriceMode.CURRENCY) {
                    ShopListing.Currency currency = ShopListing.Currency.valueOf(
                            Objects.requireNonNull(cfg.getString(base + ".currency")));
                    int amount = cfg.getInt(base + ".currencyAmount", 1);
                    listings.add(new ShopListing(id, seller, sName, item, pm, currency, amount, null, false));
                } else {
                    ItemStack priceItem = cfg.getItemStack(base + ".priceItem");
                    if (priceItem == null) continue;
                    listings.add(new ShopListing(id, seller, sName, item, pm, null, 0, priceItem, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement listing shop #" + key + " : " + e.getMessage());
            }
        }
        plugin.getLogger().info("Shop : " + listings.size() + " annonce(s) chargée(s).");
    }
}
