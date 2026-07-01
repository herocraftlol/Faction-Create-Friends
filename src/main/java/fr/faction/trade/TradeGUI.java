package fr.faction.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
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
 * ┌───────────────────────────────────────────────────────┐
 * │  [B][B][B][B][INFO][B][B][B][B]   ← ligne 0 (bordure)│
 * │  [B][M][M][M][SEP][T][T][T][B]   ← lignes 1-4        │
 * │  [B][M][M][M][SEP][T][T][T][B]                        │
 * │  [B][M][M][M][SEP][T][T][T][B]                        │
 * │  [B][M][M][M][SEP][T][T][T][B]                        │
 * │  [B][B][B][B][OK ][B][B][B][X]   ← ligne 5 (boutons) │
 * └───────────────────────────────────────────────────────┘
 * M = mes slots (éditables, drag possible)
 * T = leurs slots (lecture seule)
 * B = bordure (bloqué)
 *
 * Slots MY_SLOTS  : 10,11,12, 19,20,21, 28,29,30, 37,38,39
 * Slots THEIR_SLOTS: 14,15,16, 23,24,25, 32,33,34, 41,42,43
 * SEP  : 13,22,31,40
 * Bouton Confirmer : 49
 * Bouton Annuler   : 53
 * Info             : 4
 *
 * INTERACTIONS SUPPORTÉES :
 *  - Clic gauche / droit sur l'inventaire joueur (bas) → ajoute l'item dans MY_SLOTS
 *  - Shift-clic depuis l'inventaire joueur → ajoute l'item dans MY_SLOTS
 *  - Drag depuis l'inventaire joueur vers un MY_SLOT → place l'item
 *  - Clic sur un MY_SLOT occupé → rend l'item à l'inventaire joueur
 *  - Clic CONFIRM / CANCEL
 */
public class TradeGUI implements Listener {

    private static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Troc";

    private static final int[] MY_SLOTS    = {10,11,12, 19,20,21, 28,29,30, 37,38,39};
    private static final int[] THEIR_SLOTS = {14,15,16, 23,24,25, 32,33,34, 41,42,43};
    private static final int[] SEP_SLOTS   = {13,22,31,40};
    private static final int SLOT_CONFIRM  = 49;
    private static final int SLOT_CANCEL   = 53;
    private static final int SLOT_INFO     = 4;

    // Items "fantômes" de remplissage — on les reconnaît par leur nom
    private static final String BORDER_NAME   = ChatColor.BLACK + "§r";
    private static final String READONLY_TAG  = ChatColor.DARK_GRAY + "(lecture seule)";

    private final JavaPlugin plugin;
    private final TradeManager tradeManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public TradeGUI(JavaPlugin plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OUVERTURE / RENDU
    // ══════════════════════════════════════════════════════════════════════════

    public void openTradeGUI(Player player) {
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        UUID other = session.getOther(player.getUniqueId());
        Player otherPlayer = Bukkit.getPlayer(other);
        String otherName = otherPlayer != null ? otherPlayer.getName() : "???";

        String title = TITLE_PREFIX + " §8— §7" + otherName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // ── Bordure ──────────────────────────────────────────────────────────
        ItemStack border = makeItemRaw(Material.PURPLE_STAINED_GLASS_PANE, BORDER_NAME);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)  inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, border);

        // ── Séparateur ───────────────────────────────────────────────────────
        ItemStack sep = makeItemRaw(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "│");
        for (int s : SEP_SLOTS) inv.setItem(s, sep);

        // ── Info ─────────────────────────────────────────────────────────────
        boolean myConfirm    = session.hasConfirmed(player.getUniqueId());
        boolean theirConfirm = session.hasConfirmed(other);
        inv.setItem(SLOT_INFO, makeItem(Material.BOOK,
                ChatColor.LIGHT_PURPLE + "Troc avec §e" + otherName,
                ChatColor.GRAY + "◄ Tes items à gauche  |  Leur offre à droite ►",
                "",
                myConfirm    ? ChatColor.GREEN + "✔ Tu as confirmé"
                             : ChatColor.YELLOW + "⏳ Confirme quand tu es prêt",
                theirConfirm ? ChatColor.GREEN + "✔ " + otherName + " a confirmé"
                             : ChatColor.RED    + "✘ " + otherName + " n'a pas encore confirmé",
                "",
                ChatColor.GRAY + "Glisse des items dans la zone gauche.",
                ChatColor.GRAY + "Clique sur un item dans la zone gauche pour le reprendre."));

        // ── Mes slots (éditables — laissés VIDES pour permettre le drag) ─────
        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
        for (int i = 0; i < MY_SLOTS.length; i++) {
            if (i < myOffer.size() && myOffer.get(i) != null) {
                inv.setItem(MY_SLOTS[i], myOffer.get(i).clone());
            }
            // Slots vides → rien (null) pour permettre le vrai drag Minecraft
        }

        // ── Leurs slots (lecture seule) ───────────────────────────────────────
        List<ItemStack> theirOffer = session.getTheirOffer(player.getUniqueId());
        ItemStack readOnly = makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Offre de " + otherName,
                READONLY_TAG);
        for (int i = 0; i < THEIR_SLOTS.length; i++) {
            if (i < theirOffer.size() && theirOffer.get(i) != null)
                inv.setItem(THEIR_SLOTS[i], theirOffer.get(i).clone());
            else
                inv.setItem(THEIR_SLOTS[i], readOnly);
        }

        // ── Boutons ───────────────────────────────────────────────────────────
        inv.setItem(SLOT_CONFIRM, myConfirm
                ? makeItem(Material.RED_CONCRETE,
                        ChatColor.RED + "✘ Retirer ma confirmation",
                        ChatColor.GRAY + "Clique pour annuler ta validation.")
                : makeItem(Material.LIME_CONCRETE,
                        ChatColor.GREEN + "✔ Confirmer l'échange",
                        ChatColor.GRAY + "Les deux doivent confirmer pour échanger."));
        inv.setItem(SLOT_CANCEL, makeItem(Material.BARRIER,
                ChatColor.RED + "✘ Annuler le troc",
                ChatColor.GRAY + "Tes items te seront rendus."));

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GESTION DES CLICS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) { event.setCancelled(true); return; }

        int raw = event.getRawSlot();

        // ── Clic dans l'inventaire joueur (partie basse, raw ≥ 54) ───────────
        boolean fromPlayerInv = (raw >= 54)
                || (event.getClickedInventory() != null
                    && event.getClickedInventory().equals(player.getInventory()));

        if (fromPlayerInv) {
            // Shift-clic ou clic simple depuis l'inventaire joueur → ajout à l'offre
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || event.getClick().isLeftClick()
                    || event.getClick().isRightClick()) {

                if (session.hasConfirmed(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
                    return;
                }

                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) {
                    event.setCancelled(true);
                    return;
                }

                List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
                if (myOffer.size() >= MY_SLOTS.length) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Offre pleine ! (max " + MY_SLOTS.length + " stacks)");
                    return;
                }

                // Laisser l'action MOVE_TO_OTHER_INVENTORY se faire naturellement
                // est impossible car le GUI n'est pas un inventory normal.
                // On annule et on gère manuellement.
                event.setCancelled(true);

                ItemStack toAdd;
                if (event.getClick().isRightClick()) {
                    // Clic droit → on prend la moitié
                    int half = (int) Math.ceil(clicked.getAmount() / 2.0);
                    toAdd = clicked.clone();
                    toAdd.setAmount(half);
                    clicked.setAmount(clicked.getAmount() - half);
                    if (clicked.getAmount() <= 0)
                        player.getInventory().setItem(event.getSlot(), null);
                    else
                        player.getInventory().setItem(event.getSlot(), clicked);
                } else {
                    // Clic gauche ou shift → on prend tout le stack
                    toAdd = clicked.clone();
                    player.getInventory().setItem(event.getSlot(), null);
                }

                myOffer.add(toAdd);
                session.unconfirm();
                refreshBoth(session);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // ── Clic dans le GUI (partie haute, raw < 54) ─────────────────────────

        // Boutons fonctionnels
        if (raw == SLOT_CONFIRM) { event.setCancelled(true); handleConfirm(player, session); return; }
        if (raw == SLOT_CANCEL)  { event.setCancelled(true); cancelTrade(player, session, true); return; }

        // Mes slots → clic pour reprendre l'item
        if (isMySlot(raw)) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            if (session.hasConfirmed(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
                return;
            }
            // Retirer de l'offre et rendre au joueur
            List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
            int idx = slotToOfferIndex(raw);
            if (idx >= 0 && idx < myOffer.size()) {
                ItemStack returned = myOffer.remove(idx);
                player.getInventory().addItem(returned);
            }
            session.unconfirm();
            refreshBoth(session);
            return;
        }

        // Tout le reste (bordure, séparateur, leurs slots, info) → bloqué
        event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GESTION DU DRAG (glisser-déposer)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(TITLE_PREFIX)) return;

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) { event.setCancelled(true); return; }

        // Récupère les slots du GUI (< 54) touchés par le drag
        Set<Integer> guiSlots = new HashSet<>();
        for (int raw : event.getRawSlots()) {
            if (raw < 54) guiSlots.add(raw);
        }

        // Si aucun slot du GUI n'est touché → drag purement dans l'inventaire joueur → OK
        if (guiSlots.isEmpty()) return;

        // Vérifie que tous les slots GUI touchés sont bien des MY_SLOTS
        for (int gs : guiSlots) {
            if (!isMySlot(gs)) {
                // Un slot interdit est touché → on bloque tout
                event.setCancelled(true);
                return;
            }
        }

        // Tous les slots sont des MY_SLOTS valides
        if (session.hasConfirmed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
            return;
        }

        // On laisse Minecraft gérer le drag naturellement (les items vont dans les MY_SLOTS).
        // Mais on doit synchroniser l'offre session APRÈS que l'event ait été résolu.
        Bukkit.getScheduler().runTask(plugin, () -> syncOfferFromGUI(player, session));
    }

    /**
     * Lit les items actuellement dans les MY_SLOTS du GUI ouvert
     * et synchronise la liste d'offre de la session.
     * Appelé après un drag pour capturer le résultat.
     */
    private void syncOfferFromGUI(Player player, TradeManager.TradeSession session) {
        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null) return;

        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
        myOffer.clear();

        for (int s : MY_SLOTS) {
            ItemStack item = inv.getItem(s);
            if (item != null && item.getType() != Material.AIR) {
                myOffer.add(item.clone());
            }
        }

        session.unconfirm();
        // Rafraîchir seulement l'autre joueur pour ne pas interrompre le drag
        Bukkit.getScheduler().runTask(plugin, () -> {
            UUID other = session.getOther(player.getUniqueId());
            Player otherPlayer = Bukkit.getPlayer(other);
            if (otherPlayer != null && openInventories.containsKey(other)) {
                openTradeGUI(otherPlayer);
            }
            // Rafraîchir son propre GUI aussi (pour mettre à jour les compteurs)
            openTradeGUI(player);
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FERMETURE
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory closed = openInventories.get(player.getUniqueId());
        if (closed == null || !closed.equals(event.getInventory())) return;
        openInventories.remove(player.getUniqueId());

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session != null) {
            // Délai d'un tick pour distinguer un vrai "fermer" d'un refresh du GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Si le joueur n'a plus de GUI ouvert après le tick, c'est une vraie fermeture
                if (!openInventories.containsKey(player.getUniqueId())) {
                    cancelTrade(player, session, true);
                }
            }, 1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIQUE CONFIRMATION / ÉCHANGE
    // ══════════════════════════════════════════════════════════════════════════

    private void handleConfirm(Player player, TradeManager.TradeSession session) {
        if (session.hasConfirmed(player.getUniqueId())) {
            session.unconfirm();
            player.sendMessage(ChatColor.YELLOW + "Tu as retiré ta confirmation.");
        } else {
            session.confirm(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "✔ Confirmé ! En attente de l'autre joueur...");
        }
        refreshBoth(session);

        if (session.isComplete()) {
            executeTrade(session);
        }
    }

    private void executeTrade(TradeManager.TradeSession session) {
        Player playerA = Bukkit.getPlayer(session.playerA);
        Player playerB = Bukkit.getPlayer(session.playerB);

        // Marquer comme terminé AVANT de fermer pour éviter que onClose annule
        tradeManager.closeSession(session.playerA, session.playerB);
        openInventories.remove(session.playerA);
        openInventories.remove(session.playerB);

        if (playerA != null) playerA.closeInventory();
        if (playerB != null) playerB.closeInventory();

        if (playerA != null) {
            for (ItemStack item : session.offerB)
                if (item != null) playerA.getInventory().addItem(item.clone());
            playerA.sendMessage(ChatColor.GREEN + "✔ Échange réalisé avec succès !");
        }
        if (playerB != null) {
            for (ItemStack item : session.offerA)
                if (item != null) playerB.getInventory().addItem(item.clone());
            playerB.sendMessage(ChatColor.GREEN + "✔ Échange réalisé avec succès !");
        }
    }

    private void cancelTrade(Player initiator, TradeManager.TradeSession session, boolean notify) {
        tradeManager.closeSession(session.playerA, session.playerB);

        Player playerA = Bukkit.getPlayer(session.playerA);
        Player playerB = Bukkit.getPlayer(session.playerB);

        if (playerA != null) {
            for (ItemStack item : session.offerA) if (item != null) playerA.getInventory().addItem(item.clone());
            openInventories.remove(playerA.getUniqueId());
            if (!playerA.equals(initiator)) playerA.closeInventory();
            if (notify) playerA.sendMessage(ChatColor.RED + "✘ Troc annulé. Tes items t'ont été rendus.");
        }
        if (playerB != null) {
            for (ItemStack item : session.offerB) if (item != null) playerB.getInventory().addItem(item.clone());
            openInventories.remove(playerB.getUniqueId());
            if (!playerB.equals(initiator)) playerB.closeInventory();
            if (notify) playerB.sendMessage(ChatColor.RED + "✘ Troc annulé. Tes items t'ont été rendus.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REFRESH
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshBoth(TradeManager.TradeSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player a = Bukkit.getPlayer(session.playerA);
            Player b = Bukkit.getPlayer(session.playerB);
            if (a != null && openInventories.containsKey(a.getUniqueId())) openTradeGUI(a);
            if (b != null && openInventories.containsKey(b.getUniqueId())) openTradeGUI(b);
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private boolean isMySlot(int slot) {
        for (int s : MY_SLOTS) if (s == slot) return true;
        return false;
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

    /** Item sans lore, nom brut (pour les items de remplissage internes). */
    private ItemStack makeItemRaw(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }
}
