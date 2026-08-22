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

    // ── Spawn de faction ────────────────────────────────────────────────────────
    private Location factionSpawn;

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
    public void setChef(UUID chef)            { this.chef = chef; }
    public List<UUID> getMembers()            { return members; }
    public boolean isMember(UUID uuid)        { return members.contains(uuid); }
    public boolean isChef(UUID uuid)          { return chef.equals(uuid); }
    public void addMember(UUID uuid)          { if (!members.contains(uuid)) members.add(uuid); }
    public void removeMember(UUID uuid)       { members.remove(uuid); }
    public List<UUID> getPendingInvites()     { return pendingInvites; }
    public void addInvite(UUID uuid)          { if (!pendingInvites.contains(uuid)) pendingInvites.add(uuid); }
    public void removeInvite(UUID uuid)       { pendingInvites.remove(uuid); }
    public boolean hasInvite(UUID uuid)       { return pendingInvites.contains(uuid); }
    public int getMemberCount()               { return members.size(); }

    // ── Spawn ───────────────────────────────────────────────────────────────────
    public Location getFactionSpawn()                    { return factionSpawn; }
    public void setFactionSpawn(Location loc)            { this.factionSpawn = loc; }
    public boolean hasSpawn()                            { return factionSpawn != null; }

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
