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
 * Gère les "ordres d'échange" : un joueur dépose de la monnaie (fer/or/diamant/émeraude)
 * tenue en main, et demande en retour un item précis à un taux fixé ("lot").
 * D'autres joueurs peuvent fournir cet item pour recevoir la monnaie correspondante,
 * jusqu'à épuisement du stock déposé.
 *
 * Persistance dans plugins/FactionPlugin/exchange.yml
 */
public class ExchangeManager {

    private final JavaPlugin plugin;
    private final List<ExchangeOrder> orders = new ArrayList<>();
    private final File exchangeFile;

    public ExchangeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.exchangeFile = new File(plugin.getDataFolder(), "exchange.yml");
        load();
    }

    // ─── CRÉATION ───────────────────────────────────────────────────────────────

    public enum CreateStatus { SUCCESS, NO_CURRENCY_IN_HAND, INVALID_CURRENCY, BUDGET_TOO_LOW }

    public static class CreateResult {
        public final CreateStatus status;
        public final ExchangeOrder order;
        private CreateResult(CreateStatus status, ExchangeOrder order) { this.status = status; this.order = order; }
    }

    /**
     * Crée un ordre d'échange en déposant TOUT le stack de monnaie tenu en main du créateur.
     * @param requestedMaterial   l'item demandé en échange (ex : STONE)
     * @param requestedAmountPerBatch quantité de cet item demandée par lot (ex : 32)
     * @param pricePerBatch       quantité de monnaie payée par lot (ex : 5)
     */
    public CreateResult createOrder(Player creator, Material requestedMaterial,
                                     int requestedAmountPerBatch, int pricePerBatch) {
        ItemStack hand = creator.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return new CreateResult(CreateStatus.NO_CURRENCY_IN_HAND, null);
        }

        ShopListing.Currency currency = ShopListing.Currency.fromMaterial(hand.getType());
        if (currency == null) {
            return new CreateResult(CreateStatus.INVALID_CURRENCY, null);
        }

        int totalCurrency = hand.getAmount();
        if (totalCurrency < pricePerBatch) {
            return new CreateResult(CreateStatus.BUDGET_TOO_LOW, null);
        }

        // Retirer la monnaie de la main du créateur
        creator.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        ExchangeOrder order = new ExchangeOrder(creator.getUniqueId(), creator.getName(),
                requestedMaterial, requestedAmountPerBatch, currency, pricePerBatch, totalCurrency);
        orders.add(order);
        save();
        return new CreateResult(CreateStatus.SUCCESS, order);
    }

    // ─── ALIMENTATION (FOURNIR L'ITEM DEMANDÉ) ─────────────────────────────────

    public enum FulfillStatus { SUCCESS, NOT_FOUND, OWN_ORDER, DEPLETED, NOT_ENOUGH_ITEMS }

    public static class FulfillResult {
        public final FulfillStatus status;
        public final int batches;
        public final int itemsGiven;
        public final int currencyReceived;
        public final ExchangeOrder order;
        private FulfillResult(FulfillStatus status, int batches, int itemsGiven, int currencyReceived, ExchangeOrder order) {
            this.status = status; this.batches = batches; this.itemsGiven = itemsGiven;
            this.currencyReceived = currencyReceived; this.order = order;
        }
    }

    /**
     * Le joueur {@code supplier} fournit l'item demandé par l'ordre {@code id}, depuis
     * l'ensemble de son inventaire, et reçoit la monnaie correspondante.
     * Honore automatiquement autant de lots que possible (limité par son inventaire
     * et par le stock de monnaie restant de l'ordre), sauf si maxBatches > 0 auquel cas
     * le nombre de lots est plafonné à cette valeur.
     */
    public FulfillResult fulfill(Player supplier, String id, int maxBatches) {
        ExchangeOrder order = findById(id);
        if (order == null) return new FulfillResult(FulfillStatus.NOT_FOUND, 0, 0, 0, null);
        if (order.getCreatorUUID().equals(supplier.getUniqueId()))
            return new FulfillResult(FulfillStatus.OWN_ORDER, 0, 0, 0, order);
        if (!order.isOpen()) return new FulfillResult(FulfillStatus.DEPLETED, 0, 0, 0, order);

        int available = countMaterial(supplier, order.getRequestedMaterial());
        int batches = order.maxFulfillableBatches(available);
        if (maxBatches > 0) batches = Math.min(batches, maxBatches);

        if (batches <= 0) {
            if (available < order.getRequestedAmountPerBatch()) {
                return new FulfillResult(FulfillStatus.NOT_ENOUGH_ITEMS, 0, 0, 0, order);
            }
            return new FulfillResult(FulfillStatus.DEPLETED, 0, 0, 0, order);
        }

        int itemsToTake = batches * order.getRequestedAmountPerBatch();
        int currencyToGive = batches * order.getPricePerBatch();

        removeMaterial(supplier, order.getRequestedMaterial(), itemsToTake);
        giveOrDrop(supplier, new ItemStack(order.getCurrency().getMaterial(), currencyToGive));

        order.removeCurrency(currencyToGive);
        order.addCollected(itemsToTake);
        save();

        Player creatorPlayer = Bukkit.getPlayer(order.getCreatorUUID());
        if (creatorPlayer != null && creatorPlayer.isOnline()) {
            creatorPlayer.sendMessage("§8[§dÉchange§8] §e" + supplier.getName() + " §aa fourni §e"
                    + itemsToTake + "× " + formatMat(order.getRequestedMaterial())
                    + " §asur ton ordre §7[" + order.getId() + "]§a. §7(/faction collecter " + order.getId() + ")");
        }

        return new FulfillResult(FulfillStatus.SUCCESS, batches, itemsToTake, currencyToGive, order);
    }

    // ─── COLLECTE (le créateur récupère les items reçus) ───────────────────────

    public enum CollectStatus { SUCCESS, NOT_FOUND, NOT_OWNER, NOTHING_TO_COLLECT }

    /**
     * Le créateur récupère les items actuellement en attente sur son ordre,
     * sans clôturer l'ordre s'il reste de la monnaie à distribuer.
     */
    public CollectStatus collect(Player creator, String id) {
        ExchangeOrder order = findById(id);
        if (order == null) return CollectStatus.NOT_FOUND;
        if (!order.getCreatorUUID().equals(creator.getUniqueId())) return CollectStatus.NOT_OWNER;
        if (order.getCollectedAmount() <= 0) return CollectStatus.NOTHING_TO_COLLECT;

        giveOrDrop(creator, new ItemStack(order.getRequestedMaterial(), order.getCollectedAmount()));
        order.clearCollected();

        // Si l'ordre est épuisé et qu'il n'y a plus rien à collecter, on le retire.
        if (!order.isOpen()) {
            orders.remove(order);
        }
        save();
        return CollectStatus.SUCCESS;
    }

    // ─── ANNULATION (rembourse monnaie restante + items reçus, ferme l'ordre) ──

    public enum CancelStatus { SUCCESS, NOT_FOUND, NOT_OWNER }

    public CancelStatus cancel(Player creator, String id) {
        ExchangeOrder order = findById(id);
        if (order == null) return CancelStatus.NOT_FOUND;
        if (!order.getCreatorUUID().equals(creator.getUniqueId())) return CancelStatus.NOT_OWNER;

        if (order.getRemainingCurrency() > 0) {
            giveOrDrop(creator, new ItemStack(order.getCurrency().getMaterial(), order.getRemainingCurrency()));
        }
        if (order.getCollectedAmount() > 0) {
            giveOrDrop(creator, new ItemStack(order.getRequestedMaterial(), order.getCollectedAmount()));
        }
        orders.remove(order);
        save();
        return CancelStatus.SUCCESS;
    }

    // ─── RECHERCHE / FILTRES ────────────────────────────────────────────────────

    /** Ordres encore ouverts (capables de payer au moins un lot). */
    public List<ExchangeOrder> getActiveOrders() {
        return orders.stream().filter(ExchangeOrder::isOpen).collect(Collectors.toList());
    }

    /** Ordres ouverts filtrés par mot-clé (item demandé, monnaie, créateur, ID). */
    public List<ExchangeOrder> searchOrders(String query) {
        String q = query.toLowerCase();
        return getActiveOrders().stream()
                .filter(o -> o.getRequestedMaterial().name().toLowerCase().contains(q)
                        || o.getCreatorName().toLowerCase().contains(q)
                        || o.getId().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /** Tous les ordres d'un créateur (ouverts, ou épuisés avec des items en attente de collecte). */
    public List<ExchangeOrder> getCreatorOrders(UUID creatorUUID) {
        return orders.stream()
                .filter(o -> o.getCreatorUUID().equals(creatorUUID))
                .collect(Collectors.toList());
    }

    public ExchangeOrder findById(String id) {
        return orders.stream().filter(o -> o.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
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

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§8[§dÉchange§8] §eInventaire plein — item(s) dropé(s) à tes pieds !");
        }
    }

    public static String formatMat(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    // ─── PERSISTANCE ────────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();

        int i = 0;
        for (ExchangeOrder o : orders) {
            String base = "orders." + i;
            cfg.set(base + ".id", o.getId());
            cfg.set(base + ".creator", o.getCreatorUUID().toString());
            cfg.set(base + ".creatorName", o.getCreatorName());
            cfg.set(base + ".requestedMaterial", o.getRequestedMaterial().name());
            cfg.set(base + ".requestedAmount", o.getRequestedAmountPerBatch());
            cfg.set(base + ".currency", o.getCurrency().name());
            cfg.set(base + ".pricePerBatch", o.getPricePerBatch());
            cfg.set(base + ".remainingCurrency", o.getRemainingCurrency());
            cfg.set(base + ".collectedAmount", o.getCollectedAmount());
            i++;
        }
        try { cfg.save(exchangeFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde exchange : " + e.getMessage());
        }
    }

    public void load() {
        orders.clear();
        if (!exchangeFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(exchangeFile);
        if (!cfg.contains("orders")) return;
        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("orders")).getKeys(false)) {
            String base = "orders." + key;
            try {
                String id = cfg.getString(base + ".id");
                UUID creator = UUID.fromString(Objects.requireNonNull(cfg.getString(base + ".creator")));
                String creatorName = cfg.getString(base + ".creatorName", "?");
                Material requestedMaterial = Material.valueOf(Objects.requireNonNull(cfg.getString(base + ".requestedMaterial")));
                int requestedAmount = cfg.getInt(base + ".requestedAmount", 1);
                ShopListing.Currency currency = ShopListing.Currency.valueOf(
                        Objects.requireNonNull(cfg.getString(base + ".currency")));
                int pricePerBatch = cfg.getInt(base + ".pricePerBatch", 1);
                int remainingCurrency = cfg.getInt(base + ".remainingCurrency", 0);
                int collectedAmount = cfg.getInt(base + ".collectedAmount", 0);
                orders.add(new ExchangeOrder(id, creator, creatorName, requestedMaterial, requestedAmount,
                        currency, pricePerBatch, remainingCurrency, collectedAmount));
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement ordre d'échange #" + key + " : " + e.getMessage());
            }
        }
        plugin.getLogger().info("Comptoir d'échange : " + orders.size() + " ordre(s) chargé(s).");
    }
}
