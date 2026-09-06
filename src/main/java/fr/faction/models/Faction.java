package fr.faction.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Faction {

    private String name;
    private UUID chef;
    private List<UUID> members;
    private List<UUID> pendingInvites;

    // ── Sous-chefs ─────────────────────────────────────────────────────────────
    /** Plafond absolu de sous-chefs, non modifiable même par le chef. */
    public static final int ABSOLUTE_MAX_SOUS_CHEFS = 2;
    /** Sous-chefs actuels de la faction. */
    private Set<UUID> sousChefs = new HashSet<>();
    /** Nombre max de sous-chefs autorisés, réglable par le chef (0 à ABSOLUTE_MAX_SOUS_CHEFS). */
    private int maxSousChefs = ABSOLUTE_MAX_SOUS_CHEFS;

    // ── Spawns de faction ───────────────────────────────────────────────────────
    /** Spawn principal (tous rangs) */
    private Location factionSpawn;
    /** Spawn secondaire — débloqué au rang Diamant */
    private Location factionSpawn2;

    // ── Alliances ───────────────────────────────────────────────────────────────
    /** Factions avec qui on est allié (en toLowerCase) */
    private Set<String> allies = new HashSet<>();
    /** Invitations d'alliance envoyées (en attente d'acceptation) */
    private Set<String> pendingAllianceInvites = new HashSet<>();

    public Faction(String name, UUID chef) {
        this.name = name;
        this.chef = chef;
        this.members = new ArrayList<>();
        this.pendingInvites = new ArrayList<>();
        this.members.add(chef);
    }

    // ── Base ────────────────────────────────────────────────────────────────────
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public UUID getChef()                     { return chef; }
    public void setChef(UUID chef)            { this.chef = chef; sousChefs.remove(chef); }
    public List<UUID> getMembers()            { return members; }
    public boolean isMember(UUID uuid)        { return members.contains(uuid); }
    public boolean isChef(UUID uuid)          { return chef.equals(uuid); }
    public void addMember(UUID uuid)          { if (!members.contains(uuid)) members.add(uuid); }
    public void removeMember(UUID uuid)       { members.remove(uuid); sousChefs.remove(uuid); }
    public List<UUID> getPendingInvites()     { return pendingInvites; }
    public void addInvite(UUID uuid)          { if (!pendingInvites.contains(uuid)) pendingInvites.add(uuid); }
    public void removeInvite(UUID uuid)       { pendingInvites.remove(uuid); }
    public boolean hasInvite(UUID uuid)       { return pendingInvites.contains(uuid); }
    public int getMemberCount()               { return members.size(); }

    // ── Sous-chefs ─────────────────────────────────────────────────────────────
    public Set<UUID> getSousChefs()            { return sousChefs; }
    public boolean isSousChef(UUID uuid)       { return sousChefs.contains(uuid); }
    /** Chef OU sous-chef : autorisé à gérer invites/kicks/alliances/guerres/spawns/claims. */
    public boolean canManage(UUID uuid)        { return isChef(uuid) || isSousChef(uuid); }
    public int getMaxSousChefs()               { return maxSousChefs; }
    /** Le chef peut régler la limite entre 0 et ABSOLUTE_MAX_SOUS_CHEFS (2). */
    public void setMaxSousChefs(int max)       { this.maxSousChefs = Math.max(0, Math.min(ABSOLUTE_MAX_SOUS_CHEFS, max)); }
    public int getSousChefCount()              { return sousChefs.size(); }

    public enum SousChefResult { SUCCESS, NOT_MEMBER, ALREADY_SOUS_CHEF, IS_CHEF, LIMIT_REACHED, NOT_SOUS_CHEF }

    public SousChefResult addSousChef(UUID uuid) {
        if (!members.contains(uuid))     return SousChefResult.NOT_MEMBER;
        if (isChef(uuid))                return SousChefResult.IS_CHEF;
        if (sousChefs.contains(uuid))    return SousChefResult.ALREADY_SOUS_CHEF;
        if (sousChefs.size() >= maxSousChefs) return SousChefResult.LIMIT_REACHED;
        sousChefs.add(uuid);
        return SousChefResult.SUCCESS;
    }

    public SousChefResult removeSousChef(UUID uuid) {
        if (!sousChefs.contains(uuid))   return SousChefResult.NOT_SOUS_CHEF;
        sousChefs.remove(uuid);
        return SousChefResult.SUCCESS;
    }

    // ── Spawn ───────────────────────────────────────────────────────────────────
    // Spawn 1
    public Location getFactionSpawn()                    { return factionSpawn; }
    public void     setFactionSpawn(Location loc)        { this.factionSpawn = loc; }
    public boolean  hasSpawn()                           { return factionSpawn != null; }

    // Spawn 2 (rang Diamant+)
    public Location getFactionSpawn2()                   { return factionSpawn2; }
    public void     setFactionSpawn2(Location loc)       { this.factionSpawn2 = loc; }
    public boolean  hasSpawn2()                          { return factionSpawn2 != null; }

    /**
     * Retourne le spawn par numéro de slot (1 ou 2).
     * Retourne null si le slot n'existe pas.
     */
    public Location getSpawnBySlot(int slot) {
        return slot == 2 ? factionSpawn2 : factionSpawn;
    }

    public boolean hasSpawnBySlot(int slot) {
        return slot == 2 ? factionSpawn2 != null : factionSpawn != null;
    }

    public void setSpawnBySlot(int slot, Location loc) {
        if (slot == 2) factionSpawn2 = loc;
        else           factionSpawn  = loc;
    }

    // ── Alliances ───────────────────────────────────────────────────────────────
    public Set<String> getAllies()                       { return allies; }
    public boolean isAlly(String factionName)            { return allies.contains(factionName.toLowerCase()); }
    public void addAlly(String factionName)              { allies.add(factionName.toLowerCase()); }
    public void removeAlly(String factionName)           { allies.remove(factionName.toLowerCase()); }

    public Set<String> getPendingAllianceInvites()       { return pendingAllianceInvites; }
    public boolean hasPendingAllianceFrom(String name)   { return pendingAllianceInvites.contains(name.toLowerCase()); }
    public void addPendingAlliance(String name)          { pendingAllianceInvites.add(name.toLowerCase()); }
    public void removePendingAlliance(String name)       { pendingAllianceInvites.remove(name.toLowerCase()); }
    public int getAllyCount()                             { return allies.size(); }
}
