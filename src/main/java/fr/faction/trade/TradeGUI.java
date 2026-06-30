package fr.faction.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI de troc entre deux joueurs.
 *
 * Layout (54 slots) :
 *  Colonne 0 : bordure gauche
 *  Slots 1-3  (lignes 0-5, col 1-3) : offre du joueur courant   (21 slots = 3×7 non-border)
 *  Colonne 4  : séparateur central
 *  Slots 5-7  (lignes 0-5, col 5-7) : offre de l'autre joueur   (lecture seule)
 *  Colonne 8  : bordure droite
 *  Ligne 6 bas : bouton Confirmer (slot 45), bouton Annuler (slot 53)
 *
 * Mapping réel des 54 slots Minecraft :
 *  Offre perso   : 10,11,12, 19,20,21, 28,29,30, 37,38,39  (4×3=12 slots)
 *  Offre adverse : 14,15,16, 23,24,25, 32,33,34, 41,42,43  (4×3=12 slots)
 *  Séparateur col 4 : 13,22,31,40
 *  Bordure row 0  : 0-8
 *  Bordure row 5  : 45-53
 *  Bordure col 0  : 9,18,27,36
 *  Bordure col 8  : 17,26,35,44
 *  Bouton confirm : 49
 *  Bouton annuler : 45 (coin BG)
 *  Statut        : 4 (rangée 0 centre)
 */
public class TradeGUI implements Listener {

    private static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Troc";

    // Slots où chaque joueur peut poser ses items
    private static final int[] MY_SLOTS    = {10,11,12, 19,20,21, 28,29,30, 37,38,39};
    private static final int[] THEIR_SLOTS = {14,15,16, 23,24,25, 32,33,34, 41,42,43};
    private static final int[] SEP_SLOTS   = {13,22,31,40};
    private static final int SLOT_CONFIRM  = 49;
    private static final int SLOT_CANCEL   = 53;
    private static final int SLOT_INFO     = 4;

    private final JavaPlugin plugin;
    private final TradeManager tradeManager;

    /** UUID → l'inventory ouvert pour ce joueur */
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public TradeGUI(JavaPlugin plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Ouverture ─────────────────────────────────────────────────────────────

    public void openTradeGUI(Player player) {
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        UUID other = session.getOther(player.getUniqueId());
        Player otherPlayer = Bukkit.getPlayer(other);
        String otherName = otherPlayer != null ? otherPlayer.getName() : "???";

        String title = TITLE_PREFIX + " §8— §7" + otherName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Bordure
        ItemStack border = makeItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, border);

        // Séparateur
        ItemStack sep = makeItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "│");
        for (int s : SEP_SLOTS) inv.setItem(s, sep);

        // Info central
        boolean myConfirm = session.hasConfirmed(player.getUniqueId());
        boolean theirConfirm = session.hasConfirmed(other);
        inv.setItem(SLOT_INFO, makeItem(Material.BOOK,
                ChatColor.LIGHT_PURPLE + "Troc avec §e" + otherName,
                ChatColor.GRAY + "Gauche : tes objets  |  Droite : ses objets",
                "",
                (myConfirm ? ChatColor.GREEN + "✔ Tu as confirmé" : ChatColor.YELLOW + "⏳ En attente de confirmation"),
                (theirConfirm ? ChatColor.GREEN + "✔ " + otherName + " a confirmé" : ChatColor.RED + "✘ " + otherName + " n'a pas encore confirmé")));

        // Offre du joueur (éditable)
        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
        for (int i = 0; i < MY_SLOTS.length && i < myOffer.size(); i++) {
            if (myOffer.get(i) != null) inv.setItem(MY_SLOTS[i], myOffer.get(i).clone());
        }

        // Offre de l'autre (lecture seule)
        List<ItemStack> theirOffer = session.getTheirOffer(player.getUniqueId());
        for (int i = 0; i < THEIR_SLOTS.length && i < theirOffer.size(); i++) {
            if (theirOffer.get(i) != null) inv.setItem(THEIR_SLOTS[i], theirOffer.get(i).clone());
        }
        // Étiquette "lecture seule" sur les slots adverses vides
        ItemStack readOnly = makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Offre de " + otherName,
                ChatColor.DARK_GRAY + "(lecture seule)");
        for (int s : THEIR_SLOTS) {
            if (inv.getItem(s) == null) inv.setItem(s, readOnly);
        }

        // Boutons bas
        inv.setItem(SLOT_CONFIRM, myConfirm
                ? makeItem(Material.RED_CONCRETE, ChatColor.RED + "✘ Annuler ma confirmation",
                           ChatColor.GRAY + "Clique pour retirer ta validation.")
                : makeItem(Material.LIME_CONCRETE, ChatColor.GREEN + "✔ Confirmer l'échange",
                           ChatColor.GRAY + "Les deux joueurs doivent confirmer."));
        inv.setItem(SLOT_CANCEL, makeItem(Material.BARRIER,
                ChatColor.RED + "✘ Annuler le troc",
                ChatColor.GRAY + "Ferme la session et rend les items."));

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    // ── Listener clic ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;

        event.setCancelled(true);

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        int slot = event.getRawSlot();

        // Bouton Confirmer
        if (slot == SLOT_CONFIRM) {
            handleConfirm(player, session);
            return;
        }
        // Bouton Annuler
        if (slot == SLOT_CANCEL) {
            cancelTrade(player, session, true);
            return;
        }

        // Slots "leur offre" ou bordure → rien
        if (isTheirSlot(slot) || isBorderSlot(slot) || slot == SLOT_INFO) return;

        // Slots "mon offre" → géré par onInventoryClickMySlot
        if (isMySlot(slot)) {
            return;
        }

        // Inventaire bas du joueur (slots 54+) → il veut ajouter un item à son offre
        // Le slot ≥ 54 couvre l'inventaire (54-80) et la barre de raccourci (81-89)
        boolean isPlayerInv = slot >= 54;
        // Shift-click depuis l'inventaire joueur : getRawSlot() peut renvoyer le slot dans
        // l'inventaire joueur même si < 54 quand clickedInventory != topInventory
        if (!isPlayerInv && event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())) {
            isPlayerInv = true;
        }

        if (isPlayerInv) {
            if (session.hasConfirmed(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier l'offre.");
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Ajouter à l'offre si place disponible
            List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
            if (myOffer.size() >= MY_SLOTS.length) {
                player.sendMessage(ChatColor.RED + "Offre pleine (max " + MY_SLOTS.length + " stacks).");
                return;
            }
            myOffer.add(clicked.clone());

            // Retirer l'item directement depuis l'inventaire du joueur
            // (event.setCurrentItem(null) ne fonctionne pas sur les slots joueur quand l'event est cancelled)
            int playerInvSlot = event.getSlot(); // getSlot() donne le slot relatif à l'inventaire cliqué
            player.getInventory().setItem(playerInvSlot, null);

            // Déconfirme si besoin
            session.unconfirm();
            refreshBoth(session);
            return;
        }
    }

    /** Bloquer le drag aussi */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;
        // Autoriser uniquement si tous les slots dragués sont dans l'inventaire joueur (≥54)
        for (int slot : event.getRawSlots()) {
            if (slot < 54) { event.setCancelled(true); return; }
        }
    }

    /** Retirer un item de son offre en cliquant dessus dans le GUI */
    @EventHandler
    public void onInventoryClickMySlot(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;

        int slot = event.getRawSlot();
        if (!isMySlot(slot)) return;

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;
        if (session.hasConfirmed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier l'offre.");
            return;
        }

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Retirer de l'offre et rendre au joueur
        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
        int offerIndex = slotToOfferIndex(slot);
        if (offerIndex >= 0 && offerIndex < myOffer.size()) {
            ItemStack returned = myOffer.remove(offerIndex);
            player.getInventory().addItem(returned);
        }

        session.unconfirm();
        refreshBoth(session);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory closed = openInventories.get(player.getUniqueId());
        if (closed == null || !closed.equals(event.getInventory())) return;
        openInventories.remove(player.getUniqueId());

        // Si le joueur ferme sans confirmer/annuler, on annule le troc
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session != null) {
            Bukkit.getScheduler().runTask(plugin, () -> cancelTrade(player, session, false));
        }
    }

    // ── Logique confirmation / échange ────────────────────────────────────────

    private void handleConfirm(Player player, TradeManager.TradeSession session) {
        if (session.hasConfirmed(player.getUniqueId())) {
            session.unconfirm();
            player.sendMessage(ChatColor.YELLOW + "Tu as retiré ta confirmation.");
        } else {
            session.confirm(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "✔ Tu as confirmé l'échange. En attente de l'autre joueur...");
        }
        refreshBoth(session);

        if (session.isComplete()) {
            executeTrade(session);
        }
    }

    private void executeTrade(TradeManager.TradeSession session) {
        Player playerA = Bukkit.getPlayer(session.playerA);
        Player playerB = Bukkit.getPlayer(session.playerB);

        // Fermer les GUIs
        if (playerA != null) playerA.closeInventory();
        if (playerB != null) playerB.closeInventory();

        // Donner les items
        if (playerA != null) {
            for (ItemStack item : session.offerB) {
                if (item != null) playerA.getInventory().addItem(item.clone());
            }
            playerA.sendMessage(ChatColor.GREEN + "✔ Échange effectué !");
        }
        if (playerB != null) {
            for (ItemStack item : session.offerA) {
                if (item != null) playerB.getInventory().addItem(item.clone());
            }
            playerB.sendMessage(ChatColor.GREEN + "✔ Échange effectué !");
        }

        tradeManager.closeSession(session.playerA, session.playerB);
    }

    private void cancelTrade(Player initiator, TradeManager.TradeSession session, boolean notify) {
        // Rendre les items aux joueurs
        Player playerA = Bukkit.getPlayer(session.playerA);
        Player playerB = Bukkit.getPlayer(session.playerB);

        if (playerA != null) {
            for (ItemStack item : session.offerA) if (item != null) playerA.getInventory().addItem(item.clone());
            if (notify) playerA.sendMessage(ChatColor.RED + "✘ Le troc a été annulé. Tes items t'ont été rendus.");
            openInventories.remove(playerA.getUniqueId());
            if (!playerA.equals(initiator)) playerA.closeInventory();
        }
        if (playerB != null) {
            for (ItemStack item : session.offerB) if (item != null) playerB.getInventory().addItem(item.clone());
            if (notify) playerB.sendMessage(ChatColor.RED + "✘ Le troc a été annulé. Tes items t'ont été rendus.");
            openInventories.remove(playerB.getUniqueId());
            if (!playerB.equals(initiator)) playerB.closeInventory();
        }

        tradeManager.closeSession(session.playerA, session.playerB);
    }

    // ── Refresh des deux GUIs ─────────────────────────────────────────────────

    private void refreshBoth(TradeManager.TradeSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player a = Bukkit.getPlayer(session.playerA);
            Player b = Bukkit.getPlayer(session.playerB);
            if (a != null && openInventories.containsKey(a.getUniqueId())) openTradeGUI(a);
            if (b != null && openInventories.containsKey(b.getUniqueId())) openTradeGUI(b);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isMySlot(int slot) {
        for (int s : MY_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isTheirSlot(int slot) {
        for (int s : THEIR_SLOTS) if (s == slot) return true;
        for (int s : SEP_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isBorderSlot(int slot) {
        if (slot < 9 || slot >= 45) return true;
        return (slot % 9 == 0 || slot % 9 == 8);
    }

    private int slotToOfferIndex(int slot) {
        for (int i = 0; i < MY_SLOTS.length; i++) if (MY_SLOTS[i] == slot) return i;
        return -1;
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
