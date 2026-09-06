package fr.faction.villager;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Représente un villageois recruté par une faction : identité, rôle,
 * ressources/équipement associés, et zone de chantier pour les constructeurs.
 *
 * Les champs marqués "runtime" ne sont pas persistés — ils sont reconstruits
 * à la volée par le VillagerManager pendant que le serveur tourne.
 */
public class RecruitedVillager {

    private final UUID entityId;
    private String factionName;
    private String customName;
    private VillagerRole role = VillagerRole.AUCUN;

    // ── Constructeur ─────────────────────────────────────────────────────────
    /** Réserve de blocs de construction (9 emplacements), partagée entre toutes ses tâches. */
    private ItemStack[] resources = new ItemStack[9];
    /** File de chantiers à réaliser dans l'ordre ; celui en tête est le chantier actif. */
    private final Deque<BuildTask> taskQueue = new ArrayDeque<>();
    /** Zone où il va miner lui-même le type de bloc qui lui manque pour sa tâche en cours. */
    private Location gatherZoneA;
    private Location gatherZoneB;

    // ── Guerrier ─────────────────────────────────────────────────────────────
    private ItemStack weapon;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;

    /** Arc (ou arbalète) et réserve de flèches pour le mode archerie. */
    private ItemStack bow;
    private ItemStack arrows;
    /** Si true, privilégie le tir à distance en gardant ses distances ; retombe en mêlée si à court de flèches ou au contact. */
    private boolean archeryMode = false;

    /** Poste central du guerrier : sert de centre au périmètre de défense et à la patrouille. */
    private Location postLocation;
    /** Rayon (en blocs) autour du poste dans lequel il engage le combat. */
    private double defenseRadius = 16.0;
    /** Points de ronde, parcourus en boucle quand il n'a ni cible ni joueur à suivre. */
    private final List<Location> patrolPoints = new ArrayList<>();
    /** Si false, il n'engage plus le combat (ni attaque, ni auto-défense) jusqu'à réactivation. */
    private boolean combatEnabled = true;
    /** Joueur qu'il doit suivre et défendre en priorité (null = ne suit personne). */
    private UUID followTarget;

    // ── Commun ───────────────────────────────────────────────────────────────
    /** Nourriture donnée par les joueurs, consommée automatiquement pour soigner. */
    private ItemStack food;

    // ── Runtime (non persistant) ─────────────────────────────────────────────
    private transient UUID currentTarget;
    private transient long lastActionTick;
    private transient int buildScanCursor;
    private transient int patrolIndex;

    public RecruitedVillager(UUID entityId, String factionName) {
        this.entityId = entityId;
        this.factionName = factionName;
    }

    public UUID getEntityId()                 { return entityId; }
    public String getFactionName()            { return factionName; }
    public void setFactionName(String f)      { this.factionName = f; }
    public String getCustomName()             { return customName; }
    public void setCustomName(String n)       { this.customName = n; }
    public String getDisplayName()            { return customName != null && !customName.isBlank() ? customName : "Villageois"; }
    public VillagerRole getRole()             { return role; }
    public void setRole(VillagerRole r)       { this.role = r; }

    public ItemStack[] getResources()         { return resources; }
    public void setResources(ItemStack[] r)   { this.resources = r; }

    public Deque<BuildTask> getTaskQueue()    { return taskQueue; }
    public BuildTask getCurrentTask()         { return taskQueue.peek(); }

    public Location getGatherZoneA()          { return gatherZoneA; }
    public void setGatherZoneA(Location l)    { this.gatherZoneA = l; }
    public Location getGatherZoneB()          { return gatherZoneB; }
    public void setGatherZoneB(Location l)    { this.gatherZoneB = l; }
    public boolean hasGatherZone() {
        return gatherZoneA != null && gatherZoneB != null
                && gatherZoneA.getWorld() != null && gatherZoneA.getWorld().equals(gatherZoneB.getWorld());
    }

    public ItemStack getWeapon()              { return weapon; }
    public void setWeapon(ItemStack i)        { this.weapon = i; }
    public ItemStack getHelmet()              { return helmet; }
    public void setHelmet(ItemStack i)        { this.helmet = i; }
    public ItemStack getChestplate()          { return chestplate; }
    public void setChestplate(ItemStack i)    { this.chestplate = i; }
    public ItemStack getLeggings()            { return leggings; }
    public void setLeggings(ItemStack i)      { this.leggings = i; }
    public ItemStack getBoots()               { return boots; }
    public void setBoots(ItemStack i)         { this.boots = i; }

    public ItemStack getBow()                 { return bow; }
    public void setBow(ItemStack i)           { this.bow = i; }
    public ItemStack getArrows()              { return arrows; }
    public void setArrows(ItemStack i)        { this.arrows = i; }
    public boolean isArcheryMode()            { return archeryMode; }
    public void setArcheryMode(boolean b)     { this.archeryMode = b; }

    public Location getPostLocation()         { return postLocation; }
    public void setPostLocation(Location l)   { this.postLocation = l; }
    public double getDefenseRadius()          { return defenseRadius; }
    public void setDefenseRadius(double r)    { this.defenseRadius = r; }
    public List<Location> getPatrolPoints()   { return patrolPoints; }
    public void setPatrolPoints(List<Location> pts) { this.patrolPoints.clear(); if (pts != null) this.patrolPoints.addAll(pts); }
    public boolean isCombatEnabled()          { return combatEnabled; }
    public void setCombatEnabled(boolean b)   { this.combatEnabled = b; }
    public UUID getFollowTarget()             { return followTarget; }
    public void setFollowTarget(UUID u)       { this.followTarget = u; }

    public ItemStack getFood()                { return food; }
    public void setFood(ItemStack i)          { this.food = i; }

    public UUID getCurrentTarget()            { return currentTarget; }
    public void setCurrentTarget(UUID u)      { this.currentTarget = u; }
    public long getLastActionTick()           { return lastActionTick; }
    public void setLastActionTick(long t)     { this.lastActionTick = t; }
    public int getBuildScanCursor()           { return buildScanCursor; }
    public void setBuildScanCursor(int c)     { this.buildScanCursor = c; }
    public int getPatrolIndex()               { return patrolIndex; }
    public void setPatrolIndex(int i)         { this.patrolIndex = i; }
}
