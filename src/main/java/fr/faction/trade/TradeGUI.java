package fr.faction.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
import org.bukkit.plugin.java.JavaPlugin;

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
 *  - Drag (glisser-déposer) depuis l'inventaire joueur vers un ou plusieurs MY_SLOT → place l'item
 *  - Clic sur un MY_SLOT occupé → rend l'item à l'inventaire joueur
 *  - Clic CONFIRM / CANCEL
 *
 * IMPORTANT (fix v3.2.1) : l'inventaire Bukkit n'est créé et ouvert (player.openInventory)
 * qu'UNE SEULE FOIS par session/joueur. Toutes les mises à jour suivantes (après un clic,
 * un drag, ou une action de l'autre joueur) modifient le contenu du MÊME objet Inventory
 * déjà affiché (via inv.setItem), sans jamais rappeler player.openInventory().
 * Rouvrir l'inventaire à chaque interaction cassait le glisser-déposer côté client
 * (le client annule/renvoie l'item quand la fenêtre est fermée puis rouverte immédiatement
 * après un drag) — c'est la cause du bug « le troc ne veut pas faire glisser les items ».
 *
 * NOTE DE RECONSTRUCTION : ce fichier a été accidentellement supprimé puis restauré dans
 * la même session. L'essentiel a pu être récupéré tel quel depuis l'historique de la
 * conversation ; la section gestion des clics/drag (onInventoryClick, début de
 * onInventoryDrag, fin de renderDynamicContent) a dû être reconstruite à l'identique de
 * l'esprit documenté ci-dessus plutôt que restaurée telle quelle — à valider en jeu.
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
    // OUVERTURE (une seule fois par session)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ouvre le GUI de troc pour ce joueur. Si un GUI de troc est déjà ouvert pour
     * lui (même session), ne fait que rafraîchir son contenu SANS le rouvrir.
     */
    public void openTradeGUI(Player player) {
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        Inventory existing = openInventories.get(player.getUniqueId());
        if (existing != null) {
            // Déjà ouvert : on met juste à jour le contenu, pas de reopen.
            renderDynamicContent(player, session, existing);
            return;
        }

        UUID other = session.getOther(player.getUniqueId());
        Player otherPlayer = Bukkit.getPlayer(other);
        String otherName = otherPlayer != null ? otherPlayer.getName() : "???";

        String title = TITLE_PREFIX + " §8— §7" + otherName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        drawStaticFrame(inv);
        renderDynamicContent(player, session, inv);

        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    /**
     * Rafraîchit le contenu d'un GUI déjà ouvert (sans jamais le rouvrir).
     * Ne fait rien si le joueur n'a pas de GUI de troc ouvert.
     */
    private void refresh(Player player) {
        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null) return;
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;
        renderDynamicContent(player, session, inv);
    }

    private void refreshBoth(TradeManager.TradeSession session) {
        Player a = Bukkit.getPlayer(session.playerA);
        Player b = Bukkit.getPlayer(session.playerB);
        if (a != null) refresh(a);
        if (b != null) refresh(b);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENDU
    // ══════════════════════════════════════════════════════════════════════════

    /** Éléments fixes qui ne changent jamais après l'ouverture : bordure + séparateur. */
    private void drawStaticFrame(Inventory inv) {
        ItemStack border = makeItemRaw(Material.PURPLE_STAINED_GLASS_PANE, BORDER_NAME);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)  inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, border);

        ItemStack sep = makeItemRaw(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "│");
        for (int s : SEP_SLOTS) inv.setItem(s, sep);
    }

    /**
     * Éléments qui changent au fil du troc : info, mes slots, leurs slots, boutons.
     * Met à jour le contenu du MÊME Inventory (déjà ouvert côté client) sans le remplacer.
     */
    private void renderDynamicContent(Player player, TradeManager.TradeSession session, Inventory inv) {
        UUID other = session.getOther(player.getUniqueId());
        Player otherPlayer = Bukkit.getPlayer(other);
        String otherName = otherPlayer != null ? otherPlayer.getName() : "???";

        boolean myConfirm    = session.hasConfirmed(player.getUniqueId());
        boolean theirConfirm = session.hasConfirmed(other);

        // ── Info ─────────────────────────────────────────────────────────────
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

        // ── Mes slots (éditables) — on efface explicitement les slots vides ────
        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());
        for (int i = 0; i < MY_SLOTS.length; i++) {
            ItemStack item = (i < myOffer.size()) ? myOffer.get(i) : null;
            inv.setItem(MY_SLOTS[i], (item != null && item.getType() != Material.AIR) ? item.clone() : null);
        }

        // ── Leurs slots (lecture seule) ──────────────────────────────────────
        List<ItemStack> theirOffer = session.getMyOffer(other);
        for (int i = 0; i < THEIR_SLOTS.length; i++) {
            ItemStack item = (i < theirOffer.size()) ? theirOffer.get(i) : null;
            inv.setItem(THEIR_SLOTS[i], (item != null && item.getType() != Material.AIR) ? item.clone() : null);
        }

        // ── Boutons ──────────────────────────────────────────────────────────
        inv.setItem(SLOT_CONFIRM, makeItem(
                myConfirm ? Material.LIME_CONCRETE : Material.LIME_STAINED_GLASS_PANE,
                myConfirm ? ChatColor.GREEN + "✔ Confirmé §7(clic pour annuler)" : ChatColor.GREEN + "" + ChatColor.BOLD + "Confirmer l'échange",
                ChatColor.GRAY + "Les deux joueurs doivent confirmer."));
        inv.setItem(SLOT_CANCEL, makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Annuler le troc",
                ChatColor.GRAY + "Rend tous les objets déposés."));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CLICS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!topInv.equals(openInventories.get(player.getUniqueId()))) return;

        Inventory clickedInv = event.getClickedInventory();
        boolean clickedTop = clickedInv != null && clickedInv.equals(topInv);

        if (!clickedTop) {
            // Clic (simple ou shift) dans l'inventaire du joueur : ajoute l'item cliqué à son offre.
            ItemStack toAdd = event.getCurrentItem();
            if (toAdd == null || toAdd.getType() == Material.AIR) return;
            event.setCancelled(true);

            if (session.hasConfirmed(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
                return;
            }

            int freeSlot = -1;
            for (int s : MY_SLOTS) {
                ItemStack existing = topInv.getItem(s);
                if (existing == null || existing.getType() == Material.AIR) { freeSlot = s; break; }
            }
            if (freeSlot == -1) {
                player.sendMessage(ChatColor.RED + "Offre pleine ! (max " + MY_SLOTS.length + " stacks)");
                return;
            }

            topInv.setItem(freeSlot, toAdd.clone());
            clickedInv.setItem(event.getSlot(), null);
            syncOfferFromGUI(player, session);
            return;
        }

        // Clic dans notre GUI (haut) : tout est bloqué par défaut, sauf cas gérés ci-dessous.
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == SLOT_CONFIRM) { handleConfirm(player, session); return; }
        if (slot == SLOT_CANCEL) { cancelTrade(player, session, true); return; }

        if (isMySlot(slot)) {
            if (session.hasConfirmed(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
                return;
            }
            ItemStack current = topInv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) return;

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(current.clone());
            if (!leftover.isEmpty()) leftover.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));

            topInv.setItem(slot, null);
            syncOfferFromGUI(player, session);
        }
        // THEIR_SLOTS / bordure / séparateur : déjà annulé plus haut, lecture seule.
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DRAG (glisser-déposer)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        Inventory top = event.getView().getTopInventory();
        if (!top.equals(openInventories.get(player.getUniqueId()))) return;

        int topSize = top.getSize();
        List<Integer> guiSlots = new ArrayList<>();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) guiSlots.add(raw);
        }
        if (guiSlots.isEmpty()) return; // drag entièrement dans l'inventaire du joueur : rien à faire ici

        for (int gs : guiSlots) {
            if (!isMySlot(gs)) {
                event.setCancelled(true);
                return;
            }
        }

        if (session.hasConfirmed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Annule ta confirmation avant de modifier ton offre.");
            return;
        }

        List<ItemStack> myOffer = session.getMyOffer(player.getUniqueId());

        // Combien de NOUVEAUX stacks ce drag va-t-il créer dans des slots actuellement vides ?
        Inventory topInv = event.getView().getTopInventory();
        int newStacksCount = 0;
        for (int gs : guiSlots) {
            ItemStack current = topInv.getItem(gs);
            if (current == null || current.getType() == Material.AIR) newStacksCount++;
        }
        if (myOffer.size() + newStacksCount > MY_SLOTS.length) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Offre pleine ! (max " + MY_SLOTS.length + " stacks)");
            return;
        }

        // On laisse Minecraft placer les items dans les MY_SLOTS (on ne cancel pas).
        // On relit l'inventaire un tick plus tard, une fois le drag réellement appliqué
        // par le serveur, pour synchroniser l'offre de session avec exactitude
        // (gère aussi les stacks partiellement fusionnés dans un slot déjà occupé).
        Bukkit.getScheduler().runTask(plugin, () -> syncOfferFromGUI(player, session));
    }

    /**
     * Lit les items actuellement dans les MY_SLOTS du GUI ouvert
     * et synchronise la liste d'offre de la session, puis rafraîchit
     * les DEUX joueurs sans jamais rouvrir leur inventaire.
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
        refreshBoth(session);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FERMETURE
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory closed = openInventories.get(player.getUniqueId());
        if (closed == null || !closed.equals(event.getInventory())) return;

        // Comme le GUI n'est plus jamais fermé/rouvert par notre propre code (fix v3.2.1),
        // toute InventoryCloseEvent reçue ici correspond à une vraie fermeture par le joueur.
        openInventories.remove(player.getUniqueId());

        TradeManager.TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session != null) {
            cancelTrade(player, session, true);
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
            for (ItemStack item : session.offerB) giveOrDrop(playerA, item);
            playerA.sendMessage(ChatColor.GREEN + "✔ Échange réalisé avec succès !");
        }
        if (playerB != null) {
            for (ItemStack item : session.offerA) giveOrDrop(playerB, item);
            playerB.sendMessage(ChatColor.GREEN + "✔ Échange réalisé avec succès !");
        }
    }

    private void cancelTrade(Player initiator, TradeManager.TradeSession session, boolean notify) {
        tradeManager.closeSession(session.playerA, session.playerB);

        Player playerA = Bukkit.getPlayer(session.playerA);
        Player playerB = Bukkit.getPlayer(session.playerB);

        if (playerA != null) {
            for (ItemStack item : session.offerA) giveOrDrop(playerA, item);
            openInventories.remove(playerA.getUniqueId());
            if (!playerA.equals(initiator)) playerA.closeInventory();
            if (notify) playerA.sendMessage(ChatColor.RED + "✘ Troc annulé. Tes items t'ont été rendus.");
        }
        if (playerB != null) {
            for (ItemStack item : session.offerB) giveOrDrop(playerB, item);
            openInventories.remove(playerB.getUniqueId());
            if (!playerB.equals(initiator)) playerB.closeInventory();
            if (notify) playerB.sendMessage(ChatColor.RED + "✘ Troc annulé. Tes items t'ont été rendus.");
        }
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

    // ── Drop helper ──────────────────────────────────────────────────────────────
    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            leftover.values().forEach(is -> player.getWorld().dropItemNaturally(player.getLocation(), is));
            player.sendMessage(ChatColor.YELLOW + "⚠ Inventaire plein — item(s) dropé(s) à tes pieds !");
        }
    }

}
