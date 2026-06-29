package fr.faction.trade;

import java.util.*;

/**
 * Gère le cycle de vie des sessions de troc.
 * Chaque session implique deux joueurs qui proposent des items
 * et confirment mutuellement avant l'échange.
 */
public class TradeManager {

    public enum TradeState { PENDING, CONFIRMED_A, CONFIRMED_B, BOTH_CONFIRMED }

    public static class TradeSession {
        public final UUID playerA;   // initiateur
        public final UUID playerB;   // invité

        // Items proposés par chaque joueur (max 21 slots)
        public final List<org.bukkit.inventory.ItemStack> offerA = new ArrayList<>();
        public final List<org.bukkit.inventory.ItemStack> offerB = new ArrayList<>();

        public TradeState state = TradeState.PENDING;

        public TradeSession(UUID a, UUID b) {
            this.playerA = a;
            this.playerB = b;
        }

        public boolean hasConfirmed(UUID uuid) {
            if (uuid.equals(playerA)) return state == TradeState.CONFIRMED_A || state == TradeState.BOTH_CONFIRMED;
            if (uuid.equals(playerB)) return state == TradeState.CONFIRMED_B || state == TradeState.BOTH_CONFIRMED;
            return false;
        }

        public void confirm(UUID uuid) {
            if (uuid.equals(playerA)) {
                state = (state == TradeState.CONFIRMED_B) ? TradeState.BOTH_CONFIRMED : TradeState.CONFIRMED_A;
            } else if (uuid.equals(playerB)) {
                state = (state == TradeState.CONFIRMED_A) ? TradeState.BOTH_CONFIRMED : TradeState.CONFIRMED_B;
            }
        }

        public void unconfirm() {
            state = TradeState.PENDING;
        }

        public boolean isComplete() {
            return state == TradeState.BOTH_CONFIRMED;
        }

        public UUID getOther(UUID uuid) {
            return uuid.equals(playerA) ? playerB : playerA;
        }

        public List<org.bukkit.inventory.ItemStack> getMyOffer(UUID uuid) {
            return uuid.equals(playerA) ? offerA : offerB;
        }

        public List<org.bukkit.inventory.ItemStack> getTheirOffer(UUID uuid) {
            return uuid.equals(playerA) ? offerB : offerA;
        }
    }

    // ── Stockage des sessions actives ─────────────────────────────────────────

    /** UUID d'un des deux joueurs → session */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    /** Invitations en attente : inviteur → invité */
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    // ── Invitations ───────────────────────────────────────────────────────────

    public void sendInvite(UUID from, UUID to) {
        pendingInvites.put(from, to);
    }

    public boolean hasPendingInvite(UUID inviter, UUID target) {
        return target.equals(pendingInvites.get(inviter));
    }

    public UUID getInviterFor(UUID target) {
        for (Map.Entry<UUID, UUID> e : pendingInvites.entrySet())
            if (e.getValue().equals(target)) return e.getKey();
        return null;
    }

    public void removeInvite(UUID inviter) {
        pendingInvites.remove(inviter);
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    public TradeSession createSession(UUID a, UUID b) {
        TradeSession session = new TradeSession(a, b);
        sessions.put(a, session);
        sessions.put(b, session);
        return session;
    }

    public TradeSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean inTrade(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public void closeSession(UUID a, UUID b) {
        sessions.remove(a);
        sessions.remove(b);
    }
}
