package fr.faction.models;

import java.util.UUID;

/**
 * Stats individuelles d'un joueur, trackées directement par le plugin.
 */
public class PlayerStats {

    private final UUID uuid;
    private String playerName;

    // PvP
    private int kills = 0;
    private int deaths = 0;
    private double damageDealt = 0;
    private double damageTaken = 0;
    private long mobsKilled = 0;

    // Survie
    private long blocksBroken = 0;
    private long blocksPlaced = 0;
    private double distanceTravelled = 0;

    // Progression
    private int advancements = 0;
    private int itemsCrafted = 0;

    // Activité
    private long ticksPlayed = 0;
    private long itemsPickedUp = 0;

    // Métadonnées
    private long firstJoin = System.currentTimeMillis();
    private long lastJoin = System.currentTimeMillis();

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public PlayerStats(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName()                { return playerName; }
    public void setPlayerName(String playerName)  { this.playerName = playerName; }

    // PvP
    public int getKills()                     { return kills; }
    public void addKill()                     { kills++; }
    public int getDeaths()                    { return deaths; }
    public void addDeath()                    { deaths++; }
    public double getDamageDealt()            { return damageDealt; }
    public void addDamage(double dmg)         { damageDealt += dmg; }
    public double getDamageTaken()            { return damageTaken; }
    public void addDamageTaken(double dmg)    { damageTaken += dmg; }
    public long getMobsKilled()               { return mobsKilled; }
    public void addMobKill()                  { mobsKilled++; }

    // Survie
    public long getBlocksBroken()             { return blocksBroken; }
    public void addBlockBroken()              { blocksBroken++; }
    public long getBlocksPlaced()             { return blocksPlaced; }
    public void addBlockPlaced()              { blocksPlaced++; }
    public double getDistanceTravelled()      { return distanceTravelled; }
    public void addDistance(double d)         { distanceTravelled += d; }

    // Progression
    public int getAdvancements()              { return advancements; }
    public void addAdvancement()              { advancements++; }
    public int getItemsCrafted()              { return itemsCrafted; }
    public void addCraft(int amount)          { itemsCrafted += amount; }

    // Activité
    public long getTicksPlayed()              { return ticksPlayed; }
    public void addTicks(long t)              { ticksPlayed += t; }
    public long getItemsPickedUp()            { return itemsPickedUp; }
    public void addItemPickedUp()             { itemsPickedUp++; }

    // Métadonnées de connexion
    public long getFirstJoin()                { return firstJoin; }
    public void setFirstJoin(long t)          { this.firstJoin = t; }
    public long getLastJoin()                 { return lastJoin; }
    public void setLastJoin(long t)           { this.lastJoin = t; }

    // K/D ratio
    public double getKDR() {
        return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    // Heures jouées
    public double getHoursPlayed() {
        return Math.round(ticksPlayed / (20.0 * 3600.0) * 10.0) / 10.0;
    }

    /** Temps joué formaté (JJ:HH:MM:SS), porté de FactionStats */
    public String getFormattedPlaytime() {
        long secondes = ticksPlayed / 20;
        long minutes = secondes / 60;
        long heures = minutes / 60;
        long jours = heures / 24;

        secondes %= 60;
        minutes %= 60;
        heures %= 24;

        if (jours > 0) {
            return jours + "j " + heures + "h " + minutes + "m " + secondes + "s";
        } else if (heures > 0) {
            return heures + "h " + minutes + "m " + secondes + "s";
        } else if (minutes > 0) {
            return minutes + "m " + secondes + "s";
        } else {
            return secondes + "s";
        }
    }
}
