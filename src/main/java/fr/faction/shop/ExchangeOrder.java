package fr.faction.shop;

import org.bukkit.Material;

import java.util.UUID;

/**
 * Représente un "ordre d'échange" : un joueur dépose une quantité de monnaie
 * (fer / or / diamant / émeraude) en attente, et demande en retour un item
 * précis à un certain taux ("lot").
 *
 * Exemple : un joueur dépose 60 fer et demande 32 pierre pour 5 fer.
 * Chaque fois qu'un autre joueur fournit 32 pierre, il reçoit 5 fer,
 * et la pierre est stockée en attente pour le créateur de l'ordre.
 * Cela continue jusqu'à ce que le stock de fer déposé soit épuisé
 * (ici : 12 lots possibles, 60 / 5 = 12).
 */
public class ExchangeOrder {

    private final String id;
    private final UUID creatorUUID;
    private final String creatorName;

    // ─── Ce qui est demandé (fourni par les autres joueurs) ────────────────────
    private final Material requestedMaterial;
    private final int requestedAmountPerBatch;

    // ─── Ce qui est payé en retour, par lot ────────────────────────────────────
    private final ShopListing.Currency currency;
    private final int pricePerBatch;

    // ─── État courant ───────────────────────────────────────────────────────────
    private int remainingCurrency; // monnaie encore disponible pour payer des lots
    private int collectedAmount;   // quantité de l'item demandé en attente de collecte par le créateur

    public ExchangeOrder(UUID creatorUUID, String creatorName, Material requestedMaterial,
                          int requestedAmountPerBatch, ShopListing.Currency currency,
                          int pricePerBatch, int totalCurrency) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(), creatorUUID, creatorName,
                requestedMaterial, requestedAmountPerBatch, currency, pricePerBatch, totalCurrency, 0);
    }

    // Pour la désérialisation depuis le fichier
    public ExchangeOrder(String id, UUID creatorUUID, String creatorName, Material requestedMaterial,
                          int requestedAmountPerBatch, ShopListing.Currency currency,
                          int pricePerBatch, int remainingCurrency, int collectedAmount) {
        this.id = id;
        this.creatorUUID = creatorUUID;
        this.creatorName = creatorName;
        this.requestedMaterial = requestedMaterial;
        this.requestedAmountPerBatch = requestedAmountPerBatch;
        this.currency = currency;
        this.pricePerBatch = pricePerBatch;
        this.remainingCurrency = remainingCurrency;
        this.collectedAmount = collectedAmount;
    }

    public String getId()                      { return id; }
    public UUID getCreatorUUID()                { return creatorUUID; }
    public String getCreatorName()              { return creatorName; }
    public Material getRequestedMaterial()      { return requestedMaterial; }
    public int getRequestedAmountPerBatch()     { return requestedAmountPerBatch; }
    public ShopListing.Currency getCurrency()   { return currency; }
    public int getPricePerBatch()               { return pricePerBatch; }
    public int getRemainingCurrency()           { return remainingCurrency; }
    public int getCollectedAmount()             { return collectedAmount; }

    public void removeCurrency(int amount)      { this.remainingCurrency -= amount; }
    public void addCollected(int amount)        { this.collectedAmount += amount; }
    public void clearCollected()                { this.collectedAmount = 0; }
    public void clearRemainingCurrency()        { this.remainingCurrency = 0; }

    /** L'ordre peut-il encore payer au moins un lot ? */
    public boolean isOpen() {
        return remainingCurrency >= pricePerBatch;
    }

    /** Nombre de lots encore finançables par le stock de monnaie restant. */
    public int getRemainingBatches() {
        if (pricePerBatch <= 0) return 0;
        return remainingCurrency / pricePerBatch;
    }

    /**
     * Nombre maximum de lots qu'un fournisseur possédant {@code availableItems}
     * de l'item demandé peut honorer, limité par le stock de monnaie restant.
     */
    public int maxFulfillableBatches(int availableItems) {
        if (pricePerBatch <= 0 || requestedAmountPerBatch <= 0) return 0;
        int byBudget = remainingCurrency / pricePerBatch;
        int byItems = availableItems / requestedAmountPerBatch;
        return Math.max(0, Math.min(byBudget, byItems));
    }

    public String getShortDesc() {
        String mat = requestedMaterial.name().toLowerCase().replace("_", " ");
        return id + " (" + requestedAmountPerBatch + "× " + mat + " → " + pricePerBatch + " "
                + currency.getDisplayName() + ")";
    }
}
