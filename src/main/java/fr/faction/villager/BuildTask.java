package fr.faction.villager;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

/**
 * Une tâche de chantier : une zone à combler avec un type de bloc précis,
 * assignée par un joueur donné (qui sera prévenu à la fin).
 */
public class BuildTask {

    private final Location zoneA;
    private final Location zoneB;
    private final Material blockType;
    private final UUID assignedBy;
    private final String assignedByName;

    public BuildTask(Location zoneA, Location zoneB, Material blockType, UUID assignedBy, String assignedByName) {
        this.zoneA = zoneA;
        this.zoneB = zoneB;
        this.blockType = blockType;
        this.assignedBy = assignedBy;
        this.assignedByName = assignedByName;
    }

    public Location getZoneA()        { return zoneA; }
    public Location getZoneB()        { return zoneB; }
    public Material getBlockType()    { return blockType; }
    public UUID getAssignedBy()       { return assignedBy; }
    public String getAssignedByName() { return assignedByName; }
}
