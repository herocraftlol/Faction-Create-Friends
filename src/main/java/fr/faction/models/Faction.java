package fr.faction.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Faction {

    private String name;
    private UUID chef;
    private List<UUID> members;
    private List<UUID> pendingInvites;

    public Faction(String name, UUID chef) {
        this.name = name;
        this.chef = chef;
        this.members = new ArrayList<>();
        this.pendingInvites = new ArrayList<>();
        this.members.add(chef);
    }

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
}
