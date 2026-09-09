package fr.faction.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les invitations et sessions de troc joueur-à-joueur (/faction troc, /faction accepter).
 * Purement en mémoire : une session de troc ne survit pas à un redémarrage du serveur
 * (comportement attendu pour une interaction ponctuelle entre deux joueurs en ligne).
 *
 * NOTE DE RECONSTRUCTION : ce fichier a été accidentellement écrasé par un autre système
 * (commerce inter-villes, sans rapport) plus tôt dans la session, puis reconstruit à partir
 * de son utilisation dans FactionCommand.java et TradeGUI.java (inTrade, sendInvite,
 * getInviterFor, removeInvite, createSession, getSession, closeSession, TradeSession avec
 * playerA/playerB/offerA/offerB/confirm/unconfirm/isComplete/getOther/getMyOffer). Le
 * comportement devrait être fidèle à l'original, mais à valider en jeu.
 */
public class TradeManager implements Listener {

    /** Session de troc active entre deux joueurs. */
    public static class TradeSession {
        public final UUID playerA;
        public final UUID playerB;
        public final List<ItemStack> offerA = new ArrayList<>();
        public final List<ItemStack> offerB = new ArrayList<>();
        private boolean confirmedA = false;
        private boolean confirmedB = false;

        public TradeSession(UUID playerA, UUID playerB) {
            this.playerA = playerA;
            this.playerB = playerB;
        }

        public UUID getOther(UUID player) {
            return player.equals(playerA) ? playerB : playerA;
        }

        public List<ItemStack> getMyOffer(UUID player) {
            return player.equals(playerA) ? offerA : offerB;
        }

        public boolean hasConfirmed(UUID player) {
            return player.equals(playerA) ? confirmedA : confirmedB;
        }

        public void confirm(UUID player) {
            if (player.equals(playerA)) confirmedA = true; else confirmedB = true;
        }

        /** Annule les DEUX confirmations : dès qu'une offre change, il faut reconfirmer. */
        public void unconfirm() {
            confirmedA = false;
            confirmedB = false;
        }

        public boolean isComplete() {
            return confirmedA && confirmedB;
        }
    }

    /** cible -> inviteur */
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    /** joueur -> session (les deux participants pointent vers la MÊME instance) */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public boolean inTrade(UUID player) {
        return sessions.containsKey(player);
    }

    public void sendInvite(UUID from, UUID to) {
        pendingInvites.put(to, from);
    }

    public UUID getInviterFor(UUID player) {
        return pendingInvites.get(player);
    }

    /** Retire toute invitation envoyée PAR ce joueur (quelle que soit la cible). */
    public void removeInvite(UUID inviter) {
        pendingInvites.entrySet().removeIf(e -> e.getValue().equals(inviter));
    }

    public void createSession(UUID playerA, UUID playerB) {
        TradeSession session = new TradeSession(playerA, playerB);
        sessions.put(playerA, session);
        sessions.put(playerB, session);
    }

    public TradeSession getSession(UUID player) {
        return sessions.get(player);
    }

    public void closeSession(UUID playerA, UUID playerB) {
        sessions.remove(playerA);
        sessions.remove(playerB);
    }

    /** Rien à persister : les invitations et sessions de troc sont éphémères, en mémoire. */
    public void save() {
    }

    /** Filet de sécurité : nettoie l'état si un joueur se déconnecte sans passer par la fermeture normale du GUI. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingInvites.entrySet().removeIf(e -> e.getKey().equals(uuid) || e.getValue().equals(uuid));
        TradeSession session = sessions.get(uuid);
        if (session != null) {
            sessions.remove(session.playerA);
            sessions.remove(session.playerB);
        }
    }
}
