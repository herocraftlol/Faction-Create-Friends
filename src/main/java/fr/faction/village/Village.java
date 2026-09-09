package fr.faction.village;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Village fondé par une faction sur son territoire (à partir de claims déjà posés).
 * Sert de regroupement pour les villageois recrutés "originaires" de cette zone,
 * et de base de repli pour eux lorsqu'ils n'ont plus de tâche.
 */
public class Village {

    private final UUID id;
    private String name;
    private final String factionName;

    /** Zone de fondation (utilisée pour savoir si un villageois recruté y "naît"). */
    private final Location zoneA;
    private final Location zoneB;
    /** Centre du village : point de repli des habitants sans tâche. */
    private final Location center;

    /** Port (commerce maritime) et gare (commerce ferroviaire) — un seul de chaque par village. */
    private Location port;
    private Location station;

    public Village(UUID id, String name, String factionName, Location zoneA, Location zoneB, Location center) {
        this.id = id;
        this.name = name;
        this.factionName = factionName;
        this.zoneA = zoneA;
        this.zoneB = zoneB;
        this.center = center;
    }

    public UUID getId()                { return id; }
    public String getName()            { return name; }
    public void setName(String name)   { this.name = name; }
    public String getFactionName()     { return factionName; }
    public Location getZoneA()         { return zoneA; }
    public Location getZoneB()         { return zoneB; }
    public Location getCenter()        { return center; }

    public Location getPort()          { return port; }
    public void setPort(Location l)    { this.port = l; }
    public boolean hasPort()           { return port != null; }
    public Location getStation()       { return station; }
    public void setStation(Location l) { this.station = l; }
    public boolean hasStation()        { return station != null; }

    /** Débloqué uniquement au palier "Ville" (niveau 5+) : commerce inter-villes. */
    public boolean canTrade(int level) { return isCityAtLevel(level); }

    /** Un point (typiquement le lieu où un villageois vient d'être recruté) est-il "né" dans ce village ? */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(zoneA.getWorld())) return false;
        int minX = Math.min(zoneA.getBlockX(), zoneB.getBlockX()), maxX = Math.max(zoneA.getBlockX(), zoneB.getBlockX());
        int minY = Math.min(zoneA.getBlockY(), zoneB.getBlockY()), maxY = Math.max(zoneA.getBlockY(), zoneB.getBlockY());
        int minZ = Math.min(zoneA.getBlockZ(), zoneB.getBlockZ()), maxZ = Math.max(zoneA.getBlockZ(), zoneB.getBlockZ());
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY - 8 && y <= maxY + 32 && z >= minZ && z <= maxZ;
    }

    /**
     * Niveau du village selon sa population (dérivé, jamais stocké — toujours cohérent).
     * 1-4 : village, palier "Ville" à partir du niveau 5 (débloque les avantages de
     * commerce inter-villes — système de commerce à venir, non encore implémenté).
     */
    public static int levelForPopulation(int population) {
        if (population >= 20) return 6;
        if (population >= 15) return 5;
        if (population >= 10) return 4;
        if (population >= 6)  return 3;
        if (population >= 3)  return 2;
        return 1;
    }

    public static boolean isCityAtLevel(int level) {
        return level >= 5;
    }

    public static String levelLabel(int level) {
        return isCityAtLevel(level) ? "Ville (niveau " + level + ")" : "Village (niveau " + level + ")";
    }
}
