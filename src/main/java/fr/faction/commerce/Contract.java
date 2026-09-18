package fr.faction.commerce;

import fr.faction.village.PostType;
import org.bukkit.Material;

import java.util.UUID;

/**
 * Contrat commercial : une livraison à sens unique d'une ressource depuis le
 * port/gare d'un village vers celui d'un autre village (de sa propre faction,
 * ou d'une faction alliée avec qui elle commerce).
 */
public class Contract {

    public enum Status { WAITING, IN_TRANSIT, DELIVERED, CANCELLED }

    private final UUID id;
    private final String fromFaction;
    private final String fromVillage;
    private final PostType postType;
    private final String toFaction;
    private final String toVillage;
    private final Material resource;
    private final int quantity;

    private Status status = Status.WAITING;
    private UUID assignedVillagerId;

    public Contract(UUID id, String fromFaction, String fromVillage, PostType postType,
                     String toFaction, String toVillage, Material resource, int quantity) {
        this.id = id;
        this.fromFaction = fromFaction;
        this.fromVillage = fromVillage;
        this.postType = postType;
        this.toFaction = toFaction;
        this.toVillage = toVillage;
        this.resource = resource;
        this.quantity = quantity;
    }

    public UUID getId()                     { return id; }
    public String getFromFaction()          { return fromFaction; }
    public String getFromVillage()          { return fromVillage; }
    public PostType getPostType()           { return postType; }
    public String getToFaction()            { return toFaction; }
    public String getToVillage()            { return toVillage; }
    public Material getResource()           { return resource; }
    public int getQuantity()                { return quantity; }
    public Status getStatus()               { return status; }
    public void setStatus(Status s)         { this.status = s; }
    public UUID getAssignedVillagerId()     { return assignedVillagerId; }
    public void setAssignedVillagerId(UUID u) { this.assignedVillagerId = u; }
}
