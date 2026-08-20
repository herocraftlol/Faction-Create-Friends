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

/**
 * Gère les annonces du shop global.
 * Persistance dans plugins/FactionPlugin/shop.yml
 */
public class ShopManager {

    private final JavaPlugin plugin;
    private final List<ShopListing> listings = new ArrayList<>();
    private File shopFile;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        load();
    }

    // ─── MISE EN VENTE ──────────────────────────────────────────────────────────

    /**
     * Met en vente l'item tenu en main par le joueur.
     * @return l'annonce créée, ou null si le joueur n'a rien en main.
     */
    public ShopListing createListing(Player seller, int price, ShopListing.Currency currency) {
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return null;

        ItemStack toSell = hand.clone();
        // Retirer l'item de la main
        seller.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        ShopListing listing = new ShopListing(seller.getUniqueId(), seller.getName(), toSell, price, currency);
        listings.add(listing);
        save();
        return listing;
    }

    // ─── ACHAT ──────────────────────────────────────────────────────────────────

    public enum BuyResult { SUCCESS, NOT_FOUND, ALREADY_SOLD, NOT_ENOUGH_MONEY, OWN_LISTING }

    /**
     * Effectue l'achat d'une annonce par un acheteur.
     * - Vérifie que l'acheteur a assez de monnaie dans son inventaire
     * - Retire la monnaie de l'acheteur
     * - Donne l'item à l'acheteur (ou drop)
     * - Donne la monnaie au vendeur (ou drop)
     */
    public BuyResult buy(Player buyer, String listingId) {
        ShopListing listing = findById(listingId);
        if (listing == null)          return BuyResult.NOT_FOUND;
        if (listing.isSold())         return BuyResult.ALREADY_SOLD;
        if (listing.getSellerUUID().equals(buyer.getUniqueId())) return BuyResult.OWN_LISTING;

        int totalCost = listing.getTotalPrice();
        ItemStack currencyItem = new ItemStack(listing.getCurrency().getMaterial(), 1);

        // Compter la monnaie dans l'inventaire de l'acheteur
        int buyerBalance = countMaterial(buyer, listing.getCurrency().getMaterial());
        if (buyerBalance < totalCost) return BuyResult.NOT_ENOUGH_MONEY;

        // Débiter l'acheteur
        removeMaterial(buyer, listing.getCurrency().getMaterial(), totalCost);

        // Donner l'item à l'acheteur
        giveOrDrop(buyer, listing.getItem().clone());

        // Payer le vendeur
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        ItemStack payment = new ItemStack(listing.getCurrency().getMaterial(), totalCost);
        if (seller != null && seller.isOnline()) {
            giveOrDrop(seller, payment);
            seller.sendMessage("§8[§6Shop§8] §aVente ! §e" + buyer.getName() + " §aa acheté §e"
                    + listing.getItem().getAmount() + "× " + formatMat(listing.getItem().getType())
                    + " §apour §e" + totalCost + " " + listing.getCurrency().getDisplayName() + "(s)§a.");
        } else {
            // Vendeur hors ligne : on sauvegarde la monnaie dans une file d'attente (dans shop.yml)
            savePendingPayment(listing.getSellerUUID(), payment);
        }

        listing.setSold(true);
        save();
        return BuyResult.SUCCESS;
    }

    // ─── RÉCUPÉRATION ───────────────────────────────────────────────────────────

    public enum RecoverResult { SUCCESS, NOT_FOUND, NOT_OWNER, ALREADY_SOLD }

    /**
     * Récupère une annonce non vendue (le vendeur reprend son item).
     */
    public RecoverResult recover(Player seller, String listingId) {
        ShopListing listing = findById(listingId);
        if (listing == null)            return RecoverResult.NOT_FOUND;
        if (!listing.getSellerUUID().equals(seller.getUniqueId())) return RecoverResult.NOT_OWNER;
        if (listing.isSold())           return RecoverResult.ALREADY_SOLD;

        giveOrDrop(seller, listing.getItem().clone());
        listings.remove(listing);
        save();
        return RecoverResult.SUCCESS;
    }

    // ─── PAIEMENTS EN ATTENTE ───────────────────────────────────────────────────

    /** Remettre les paiements stockés quand un vendeur se reconnecte. */
    public void deliverPendingPayments(Player player) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        String key = "pending." + player.getUniqueId().toString();
        if (!cfg.contains(key)) return;

        List<?> raw = cfg.getList(key, new ArrayList<>());
        for (Object o : raw) {
            if (o instanceof ItemStack is) {
                giveOrDrop(player, is.clone());
            }
        }
        cfg.set(key, null);
        try { cfg.save(shopFile); } catch (IOException ignored) {}
        player.sendMessage("§8[§6Shop§8] §aTu as reçu des paiements en attente !");
    }

    // ─── RECHERCHE / FILTRES ────────────────────────────────────────────────────

    /** Toutes les annonces actives (non vendues). */
    public List<ShopListing> getActiveListings() {
        return listings.stream()
                .filter(l -> !l.isSold())
                .collect(Collectors.toList());
    }

    /** Annonces actives filtrées par mot-clé (nom du matériau). */
    public List<ShopListing> searchListings(String query) {
        String q = query.toLowerCase();
        return getActiveListings().stream()
                .filter(l -> l.getItem().getType().name().toLowerCase().contains(q)
                        || l.getSellerName().toLowerCase().contains(q)
                        || l.getId().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /** Annonces d'un vendeur spécifique (actives seulement). */
    public List<ShopListing> getSellerListings(UUID sellerUUID) {
        return getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(sellerUUID))
                .collect(Collectors.toList());
    }

    public ShopListing findById(String id) {
        return listings.stream()
                .filter(l -> l.getId().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }

    // ─── UTILITAIRES ────────────────────────────────────────────────────────────

    private int countMaterial(Player player, Material mat) {
        int total = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) total += is.getAmount();
        }
        return total;
    }

    private void removeMaterial(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == mat) {
                int take = Math.min(is.getAmount(), remaining);
                is.setAmount(is.getAmount() - take);
                if (is.getAmount() <= 0) contents[i] = null;
                remaining -= take;
            }
        }
        player.getInventory().setContents(contents);
    }

    public static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§8[§6Shop§8] §eInventaire plein — item(s) dropé(s) à tes pieds !");
        }
    }

    private void savePendingPayment(UUID sellerUUID, ItemStack payment) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        String key = "pending." + sellerUUID.toString();
        List<ItemStack> pending = new ArrayList<>();
        List<?> raw = cfg.getList(key, new ArrayList<>());
        for (Object o : raw) {
            if (o instanceof ItemStack is) pending.add(is);
        }
        pending.add(payment);
        cfg.set(key, pending);
        try { cfg.save(shopFile); } catch (IOException ignored) {}
    }

    public static String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    // ─── PERSISTANCE ────────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();

        // On recharge les pending d'abord pour ne pas les écraser
        FileConfiguration existing = shopFile.exists()
                ? YamlConfiguration.loadConfiguration(shopFile)
                : new YamlConfiguration();
        if (existing.contains("pending")) {
            cfg.set("pending", existing.get("pending"));
        }

        int i = 0;
        for (ShopListing l : listings) {
            if (l.isSold()) continue; // on ne persiste pas les vendus
            String base = "listings." + i;
            cfg.set(base + ".id", l.getId());
            cfg.set(base + ".seller", l.getSellerUUID().toString());
            cfg.set(base + ".sellerName", l.getSellerName());
            cfg.set(base + ".item", l.getItem());
            cfg.set(base + ".price", l.getPrice());
            cfg.set(base + ".currency", l.getCurrency().name());
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
            String base = "listings." + key;
            try {
                String id = cfg.getString(base + ".id");
                UUID seller = UUID.fromString(Objects.requireNonNull(cfg.getString(base + ".seller")));
                String sellerName = cfg.getString(base + ".sellerName", "?");
                ItemStack item = cfg.getItemStack(base + ".item");
                int price = cfg.getInt(base + ".price", 1);
                ShopListing.Currency currency = ShopListing.Currency.valueOf(
                        Objects.requireNonNull(cfg.getString(base + ".currency")));
                if (item != null) {
                    listings.add(new ShopListing(id, seller, sellerName, item, price, currency, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement listing shop #" + key + " : " + e.getMessage());
            }
        }
        plugin.getLogger().info("Shop global : " + listings.size() + " annonce(s) chargée(s).");
    }
}
