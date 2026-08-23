package fr.faction.war;

import java.util.*;

/**
 * Représente une guerre entre deux super-factions (alliances).
 *
 * Une "super-faction" est une faction + ses alliés.
 * La guerre doit être acceptée par les DEUX chefs opposés pour démarrer.
 *
 * Enjeux négociables :
 *  - Nombre de claims à céder en cas de défaite (0-5)
 *  - Accès temporaire aux coffres partagés du perdant (durée limitée, items limités)
 *  - Bonus de puissance pour le vainqueur
 *
 * Conditions de fin :
 *  - Une faction capitule (/fac guerre capituler)
 *  - Score de kills atteint (configurable, défaut 20 kills PvP en zone de claim)
 *  - Durée max dépassée (72h par défaut) → match nul
 *
 * Anti-abus :
 *  - Cooldown de 48h entre deux guerres impliquant la même faction
 *  - Max 1 guerre active par faction
 *  - Seuls les members des factions concernées peuvent être tués pour le score
 *  - Les kills hors claim de l'ennemi ne comptent pas
 */
public class WarSession {

    public enum State {
        PENDING_ACCEPTANCE,  // déclarée, attente d'acceptation
        ACTIVE,              // en cours
        FINISHED_A_WINS,     // faction A gagnante
        FINISHED_B_WINS,     // faction B gagnante
        DRAW,                // match nul (temps dépassé)
        CANCELLED            // refusée ou annulée
    }

    // ── Identifiant ─────────────────────────────────────────────────────────────
    private final String id;

    // ── Protagonistes ────────────────────────────────────────────────────────────
    /** Faction A = déclarante */
    private final String factionA;
    /** Faction B = défenseur */
    private final String factionB;

    // ── Enjeux ──────────────────────────────────────────────────────────────────
    private int claimsAtStake;         // nombre de claims que le perdant doit céder
    private boolean chestRaidAllowed;  // le vainqueur peut-il piller le coffre partagé ?
    private int chestRaidLimit;        // nombre d'items pillables max si chestRaidAllowed
    private int powerBonusWinner;      // bonus de puissance pour le vainqueur

    // ── Progression ──────────────────────────────────────────────────────────────
    private int killsA = 0;
    private int killsB = 0;
    private int killsToWin;            // score de kills pour gagner
    private long startedAt;            // timestamp ms
    private long maxDurationMs;        // durée max avant match nul

    // ── État ────────────────────────────────────────────────────────────────────
    private State state;
    private boolean acceptedByB = false;

    // ── Pillage en cours ─────────────────────────────────────────────────────────
    private int itemsRaidedByA = 0;
    private int itemsRaidedByB = 0;

    public WarSession(String factionA, String factionB,
                       int claimsAtStake, boolean chestRaidAllowed, int chestRaidLimit,
                       int powerBonusWinner, int killsToWin, long maxDurationMs) {
        this.id               = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.factionA         = factionA.toLowerCase();
        this.factionB         = factionB.toLowerCase();
        this.claimsAtStake    = claimsAtStake;
        this.chestRaidAllowed = chestRaidAllowed;
        this.chestRaidLimit   = chestRaidLimit;
        this.powerBonusWinner = powerBonusWinner;
        this.killsToWin       = killsToWin;
        this.maxDurationMs    = maxDurationMs;
        this.state            = State.PENDING_ACCEPTANCE;
    }

    // Constructeur de désérialisation
    public WarSession(String id, String factionA, String factionB,
                       int claimsAtStake, boolean chestRaidAllowed, int chestRaidLimit,
                       int powerBonusWinner, int killsToWin, long maxDurationMs,
                       int killsA, int killsB, long startedAt, State state, boolean acceptedByB) {
        this.id               = id;
        this.factionA         = factionA;
        this.factionB         = factionB;
        this.claimsAtStake    = claimsAtStake;
        this.chestRaidAllowed = chestRaidAllowed;
        this.chestRaidLimit   = chestRaidLimit;
        this.powerBonusWinner = powerBonusWinner;
        this.killsToWin       = killsToWin;
        this.maxDurationMs    = maxDurationMs;
        this.killsA           = killsA;
        this.killsB           = killsB;
        this.startedAt        = startedAt;
        this.state            = state;
        this.acceptedByB      = acceptedByB;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────────
    public String getId()                    { return id; }
    public String getFactionA()              { return factionA; }
    public String getFactionB()              { return factionB; }
    public int getClaimsAtStake()            { return claimsAtStake; }
    public boolean isChestRaidAllowed()      { return chestRaidAllowed; }
    public int getChestRaidLimit()           { return chestRaidLimit; }
    public int getPowerBonusWinner()         { return powerBonusWinner; }
    public int getKillsA()                   { return killsA; }
    public int getKillsB()                   { return killsB; }
    public int getKillsToWin()              { return killsToWin; }
    public long getStartedAt()               { return startedAt; }
    public long getMaxDurationMs()           { return maxDurationMs; }
    public State getState()                  { return state; }
    public boolean isAcceptedByB()           { return acceptedByB; }
    public int getItemsRaidedByA()           { return itemsRaidedByA; }
    public int getItemsRaidedByB()           { return itemsRaidedByB; }

    public void setState(State s)            { this.state = s; }
    public void setAcceptedByB(boolean b)    { this.acceptedByB = b; }
    public void setStartedAt(long t)         { this.startedAt = t; }

    public void addKillA()                   { killsA++; }
    public void addKillB()                   { killsB++; }
    public void addRaidA(int items)          { itemsRaidedByA += items; }
    public void addRaidB(int items)          { itemsRaidedByB += items; }

    /** Renvoie true si la guerre implique cette faction (déclarante ou défenseur) */
    public boolean involves(String factionName) {
        String fn = factionName.toLowerCase();
        return factionA.equals(fn) || factionB.equals(fn);
    }

    /** Renvoie le nom de la faction opposée, ou null si pas concerné */
    public String getOpponent(String factionName) {
        String fn = factionName.toLowerCase();
        if (factionA.equals(fn)) return factionB;
        if (factionB.equals(fn)) return factionA;
        return null;
    }

    public boolean isActive()                { return state == State.ACTIVE; }
    public boolean isPending()               { return state == State.PENDING_ACCEPTANCE; }

    public boolean isTimedOut() {
        return isActive() && (System.currentTimeMillis() - startedAt) >= maxDurationMs;
    }

    /** Score de kills de la faction (par nom lowercase) */
    public int getKillsFor(String factionName) {
        if (factionA.equals(factionName.toLowerCase())) return killsA;
        if (factionB.equals(factionName.toLowerCase())) return killsB;
        return 0;
    }

    /** Retourne le temps restant en ms, ou 0 si expiré */
    public long getRemainingMs() {
        if (!isActive()) return 0;
        long elapsed = System.currentTimeMillis() - startedAt;
        return Math.max(0, maxDurationMs - elapsed);
    }

    /** Résumé compact pour le chat */
    public String getSummaryLine(String myFactionName) {
        String opp = getOpponent(myFactionName);
        int myKills  = getKillsFor(myFactionName.toLowerCase());
        int oppKills = getKillsFor(opp);
        return "§c" + myFactionName + " §7(" + myKills + " kills) §8vs §c" + opp + " §7(" + oppKills + " kills)"
                + " §8— §eObjectif : §f" + killsToWin + " kills"
                + " §8— §7Enjeu : §f" + claimsAtStake + " claim(s)";
    }
}
