package fr.factionstats.models;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int kills;
    private int deaths;
    private int blocksPlaced;
    private int blocksBroken;
    private long playTimeTicks;
    private int level;
    private int kdr;
    private int damage;
    private int distance;
    private int advancements;
    private int itemsCrafted;
    private int itemsPickedUp;

    public PlayerStats(UUID uuid, int kills, int deaths, int blocksPlaced, int blocksBroken, long playTimeTicks, int level) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
        this.blocksPlaced = blocksPlaced;
        this.blocksBroken = blocksBroken;
        this.playTimeTicks = playTimeTicks;
        this.level = level;
        this.kdr = deaths > 0 ? kills / deaths : kills;
        this.damage = 0;
        this.distance = 0;
        this.advancements = 0;
        this.itemsCrafted = 0;
        this.itemsPickedUp = 0;
    }

    public UUID getUuid() { return uuid; }
    public int getKills() { return kills; }
    public int getJoueursTues() { return kills; }
    public int getDeaths() { return deaths; }
    public int getMorts() { return deaths; }
    public int getBlocksPlaced() { return blocksPlaced; }
    public int getBlocsPlaces() { return blocksPlaced; }
    public int getBlocksBroken() { return blocksBroken; }
    public int getBlocsCasses() { return blocksBroken; }
    public long getPlayTimeSeconds() { return playTimeTicks / 20; }
    public long getTempsJoue() { return playTimeTicks; }
    public int getLevel() { return level; }
    public int getKdr() { return kdr; }
    public int getDommagesInfliges() { return damage; }
    public int getDistanceParcourue() { return distance; }
    public int getAdvancementsAccomplis() { return advancements; }
    public int getItemsCraftes() { return itemsCrafted; }
    public int getItemsRamasees() { return itemsPickedUp; }
}