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
    private int level = 1;
    private int xp = 0;
    private int killCount = 0;

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
    /** Si true : vagabonde et vit comme un villageois normal (commerce, déplacements...) tant
     *  qu'il n'a rien à faire, tout en continuant à se défendre s'il y a un ennemi. */
    private boolean freeRoam = false;
    /** Village (de sa faction) dont il est originaire/membre, null s'il n'en fait partie d'aucun. */
    private String villageName;

    // ── Navigateur / Cheminot ────────────────────────────────────────────────
    /** Contrat commercial en cours (persisté ; si le véhicule/trajet a été perdu au redémarrage, il est réinitialisé). */
    private java.util.UUID contractId;
    private transient java.util.List<org.bukkit.Location> transitWaypoints;
    private transient int waypointIndex;
    private transient java.util.UUID vehicleId;
    private transient boolean returningTrip;

    // ── Récolteur ────────────────────────────────────────────────────────────
    /** Outil en main : détermine ce qu'il récolte (pioche → minerais, hache → bois, pelle → terre/sable/…). */
    private ItemStack tool;
    /** Zone où il mine/coupe/creuse selon son outil. */
    private Location harvestZoneA;
    private Location harvestZoneB;
    /** Champ où il plante, fait pousser et récolte automatiquement (blé, carottes, pommes de terre, betteraves). */
    private Location farmZoneA;
    private Location farmZoneB;
    /** Coffre où il dépose tout ce qu'il récolte (les graines sont gardées sur lui pour replanter). */
    private Location outputChest;

    // ── Commun ───────────────────────────────────────────────────────────────
    /** Point de rassemblement : là où il retourne une fois "libre" (plus de chantier / plus de cible). */
    private Location rallyPoint;
    /** Nourriture donnée par les joueurs, consommée automatiquement pour soigner. */
    private ItemStack food;

    // ── Runtime (non persistant) ─────────────────────────────────────────────
    private transient UUID currentTarget;
    private transient long lastActionTick;
    private transient int buildScanCursor;
    private transient int patrolIndex;
    private transient boolean fleeing;

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
    public int getLevel()                     { return level; }
    public void setLevel(int l)               { this.level = l; }
    public int getXp()                        { return xp; }
    public void setXp(int x)                  { this.xp = x; }
    public int getKillCount()                 { return killCount; }
    public void setKillCount(int k)           { this.killCount = k; }

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
    public boolean isFreeRoam()               { return freeRoam; }
    public void setFreeRoam(boolean b)        { this.freeRoam = b; }
    public String getVillageName()            { return villageName; }
    public void setVillageName(String v)      { this.villageName = v; }

    public java.util.UUID getContractId()                 { return contractId; }
    public void setContractId(java.util.UUID id)           { this.contractId = id; }
    public java.util.List<org.bukkit.Location> getTransitWaypoints() { return transitWaypoints; }
    public void setTransitWaypoints(java.util.List<org.bukkit.Location> wps) { this.transitWaypoints = wps; }
    public int getWaypointIndex()                          { return waypointIndex; }
    public void setWaypointIndex(int i)                    { this.waypointIndex = i; }
    public java.util.UUID getVehicleId()                   { return vehicleId; }
    public void setVehicleId(java.util.UUID id)             { this.vehicleId = id; }
    public boolean isReturningTrip()                        { return returningTrip; }
    public void setReturningTrip(boolean b)                 { this.returningTrip = b; }

    public ItemStack getTool()                { return tool; }
    public void setTool(ItemStack i)          { this.tool = i; }
    public Location getHarvestZoneA()         { return harvestZoneA; }
    public void setHarvestZoneA(Location l)   { this.harvestZoneA = l; }
    public Location getHarvestZoneB()         { return harvestZoneB; }
    public void setHarvestZoneB(Location l)   { this.harvestZoneB = l; }
    public boolean hasHarvestZone() {
        return harvestZoneA != null && harvestZoneB != null
                && harvestZoneA.getWorld() != null && harvestZoneA.getWorld().equals(harvestZoneB.getWorld());
    }
    public Location getFarmZoneA()            { return farmZoneA; }
    public void setFarmZoneA(Location l)      { this.farmZoneA = l; }
    public Location getFarmZoneB()            { return farmZoneB; }
    public void setFarmZoneB(Location l)      { this.farmZoneB = l; }
    public boolean hasFarmZone() {
        return farmZoneA != null && farmZoneB != null
                && farmZoneA.getWorld() != null && farmZoneA.getWorld().equals(farmZoneB.getWorld());
    }
    public Location getOutputChest()          { return outputChest; }
    public void setOutputChest(Location l)    { this.outputChest = l; }

    public Location getRallyPoint()           { return rallyPoint; }
    public void setRallyPoint(Location l)     { this.rallyPoint = l; }
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
    public boolean isFleeing()                { return fleeing; }
    public void setFleeing(boolean f)         { this.fleeing = f; }
}
