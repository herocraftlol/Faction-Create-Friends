package fr.faction.villager;

import fr.faction.claim.ClaimManager;
import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import fr.faction.util.MobUtils;
import fr.faction.war.WarManager;
import fr.faction.war.WarSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Gère les villageois recrutés par les factions : conversion, rôles
 * (Constructeur / Guerrier), IA simplifiée (construction de zone, combat,
 * patrouille, suivi, formation), soin par la nourriture, et persistance
 * dans villagers.yml.
 */
public class VillagerManager implements Listener {

    private static final long ATTACK_COOLDOWN_MS = 900L;

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private ClaimManager claimManager;
    private WarManager warManager;
    private FactionPowerManager powerManager;

    private final Map<UUID, RecruitedVillager> villagers = new HashMap<>();
    private final Map<UUID, PendingSelection> pendingSelections = new HashMap<>();
    private final Map<UUID, UUID> lastDamagedBy = new HashMap<>(); // victime -> villageois recruté qui l'a frappée en dernier
    private final Map<UUID, List<Location>> activeScaffolds = new HashMap<>(); // échafaudages temporaires posés par chaque constructeur
    private final File dataFile;

    public VillagerManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "villagers.yml");
        load();
    }

    public void setClaimManager(ClaimManager claimManager) { this.claimManager = claimManager; }
    public void setWarManager(WarManager warManager)       { this.warManager = warManager; }
    public void setPowerManager(FactionPowerManager pm)    { this.powerManager = pm; }

    public void start() {
        long interval = plugin.getConfig().getLong("villager.tick-interval", 20L);
        new BukkitRunnable() {
            @Override public void run() { tickAll(); }
        }.runTaskTimer(plugin, 60L, interval);
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECRUTEMENT
    // ════════════════════════════════════════════════════════════════════════

    public enum RecruitResult { SUCCESS, NOT_IN_FACTION, NO_PERMISSION, NO_TARGET, ALREADY_RECRUITED, FACTION_FULL }

    public RecruitResult recruit(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return RecruitResult.NOT_IN_FACTION;
        if (!faction.canManage(player.getUniqueId())) return RecruitResult.NO_PERMISSION;

        RayTraceResult rt = player.rayTraceEntities(8);
        if (rt == null || !(rt.getHitEntity() instanceof Villager villager)) return RecruitResult.NO_TARGET;
        if (villagers.containsKey(villager.getUniqueId())) return RecruitResult.ALREADY_RECRUITED;

        int max = plugin.getConfig().getInt("villager.max-per-faction", 5);
        if (countForFaction(faction.getName()) >= max) return RecruitResult.FACTION_FULL;

        RecruitedVillager rv = new RecruitedVillager(villager.getUniqueId(), faction.getName());
        rv.setPostLocation(villager.getLocation());
        villagers.put(villager.getUniqueId(), rv);
        applyVisuals(rv, villager);
        save();
        return RecruitResult.SUCCESS;
    }

    /** Rend un villageois recruté à la vie sauvage (retire ses effets/équipement/rôle). */
    public void release(RecruitedVillager rv) {
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) {
            v.setCustomName(null);
            v.setCustomNameVisible(false);
            try { v.setProfession(Villager.Profession.NONE); } catch (Exception ignored) {}
            dropAllItems(rv, v.getLocation());
            EntityEquipment eq = v.getEquipment();
            if (eq != null) eq.clear();
        }
        villagers.remove(rv.getEntityId());
        pendingSelections.values().removeIf(pz -> pz.villagerId.equals(rv.getEntityId()));
        clearScaffolds(rv);
        save();
    }

    public void setRole(RecruitedVillager rv, VillagerRole role) {
        rv.setRole(role);
        rv.setCurrentTarget(null);
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) applyVisuals(rv, v);
        save();
    }

    public void rename(RecruitedVillager rv, String name) {
        rv.setCustomName(name);
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) applyVisuals(rv, v);
        save();
    }

    /** Doit être appelé après toute modification de l'inventaire/équipement stocké pour re-synchroniser l'entité vivante. */
    public void syncLiveEntity(RecruitedVillager rv) {
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) applyVisuals(rv, v);
    }

    // ════════════════════════════════════════════════════════════════════════
    // NIVEAUX & EXPÉRIENCE (5 niveaux, de plus en plus durs à atteindre)
    // ════════════════════════════════════════════════════════════════════════

    private static final int MAX_LEVEL = 5;
    /** XP total cumulé requis pour être au niveau (index + 1). */
    private static final int[] XP_THRESHOLDS = {0, 50, 150, 350, 700};
    /** Blocs posés/récoltés par passage d'IA selon le niveau (index = niveau - 1). */
    private static final int[] BLOCKS_PER_ACTION = {1, 1, 2, 2, 3};

    private void addXp(RecruitedVillager rv, int amount) {
        if (amount <= 0 || rv.getLevel() >= MAX_LEVEL) return;
        rv.setXp(rv.getXp() + amount);
        int newLevel = computeLevelForXp(rv.getXp());
        if (newLevel > rv.getLevel()) {
            rv.setLevel(newLevel);
            onLevelUp(rv);
        } else {
            save();
        }
    }

    private int computeLevelForXp(int xp) {
        int lvl = 1;
        for (int i = 1; i < XP_THRESHOLDS.length; i++) if (xp >= XP_THRESHOLDS[i]) lvl = i + 1;
        return Math.min(lvl, MAX_LEVEL);
    }

    private void onLevelUp(RecruitedVillager rv) {
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) {
            applyMaxHealthForLevel(rv, v);
            v.setHealth(getMaxHealth(v)); // la montée de niveau soigne entièrement
            v.getWorld().playSound(v.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            applyVisuals(rv, v); // rafraîchit le "Nv.X" dans le nom
        }
        save();
        notifyFaction(rv.getFactionName(), "§7[§e" + rv.getDisplayName() + "§7] §fJe suis maintenant §a"
                + (rv.getLevel() >= MAX_LEVEL ? "niveau max (" + MAX_LEVEL + ")" : "niveau " + rv.getLevel()) + " §f!");
    }

    /**
     * Retire (au mieux) les comportements vanille de déplacement autonome (déambulation
     * aléatoire, panique) : nos recrues ne bougent que quand NOUS le décidons. Best-effort :
     * l'API Paper de retrait de goals est parfois peu fiable selon la version du serveur.
     */
    private void disableVanillaWander(Villager v) {
        try {
            var goals = Bukkit.getMobGoals();
            goals.removeGoal(v, com.destroystokyo.paper.entity.ai.VanillaGoal.WATER_AVOIDING_RANDOM_STROLL);
            goals.removeGoal(v, com.destroystokyo.paper.entity.ai.VanillaGoal.PANIC);
            goals.removeGoal(v, com.destroystokyo.paper.entity.ai.VanillaGoal.MOVE_BACK_TO_VILLAGE);
            goals.removeGoal(v, com.destroystokyo.paper.entity.ai.VanillaGoal.MOVE_THROUGH_VILLAGE);
        } catch (Throwable ignored) { /* selon la version du serveur, certains goals peuvent ne pas exister */ }
    }

    private void applyMaxHealthForLevel(RecruitedVillager rv, Villager v) {
        try {
            var attr = v.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                double bonus = (rv.getLevel() - 1) * plugin.getConfig().getDouble("villager.health-per-level", 4.0);
                attr.setBaseValue(20.0 + bonus);
            }
        } catch (Throwable ignored) { /* nom d'attribut différent selon version serveur */ }
    }

    private int blocksPerAction(RecruitedVillager rv) {
        int idx = Math.max(1, Math.min(rv.getLevel(), MAX_LEVEL)) - 1;
        return BLOCKS_PER_ACTION[idx];
    }

    
    private void applyVisuals(RecruitedVillager rv, Villager v) {
        String roleTag = switch (rv.getRole()) {
            case CONSTRUCTEUR -> "§b[Constructeur]";
            case GUERRIER -> "§c[Guerrier]";
            case RECOLTEUR -> "§a[Récolteur]";
            default -> "§7[Recrue]";
        };
        String factionTag = "";
        if (powerManager != null) {
            FactionRank rank = powerManager.getFactionRank(rv.getFactionName());
            factionTag = " " + rank.couleur + rank.icone + " " + rv.getFactionName();
        }
        v.setCustomName(ChatColor.YELLOW + rv.getDisplayName() + " §7Nv." + rv.getLevel() + " " + roleTag + factionTag);
        v.setCustomNameVisible(true);
        v.setPersistent(true);
        v.setRemoveWhenFarAway(false);
        try {
            v.setProfession(switch (rv.getRole()) {
                case CONSTRUCTEUR -> Villager.Profession.MASON;
                case GUERRIER -> Villager.Profession.WEAPONSMITH;
                case RECOLTEUR -> Villager.Profession.FARMER;
                default -> Villager.Profession.NONE;
            });
        } catch (Exception ignored) {}

        // Élargit la portée de suivi vanilla pour ne pas gêner le rattrapage sur de longues distances.
        try {
            var followAttr = v.getAttribute(Attribute.FOLLOW_RANGE);
            if (followAttr != null) followAttr.setBaseValue(64.0);
        } catch (Throwable ignored) { /* nom d'attribut différent selon version serveur */ }

        applyMaxHealthForLevel(rv, v);
        disableVanillaWander(v);
        EntityEquipment eq = v.getEquipment();
        if (eq != null) {
            if (rv.getRole() == VillagerRole.GUERRIER) {
                boolean preferBow = rv.isArcheryMode() && rv.getBow() != null && countArrows(rv) > 0;
                eq.setItemInMainHand(preferBow ? rv.getBow() : rv.getWeapon());
                eq.setHelmet(rv.getHelmet());
                eq.setChestplate(rv.getChestplate());
                eq.setLeggings(rv.getLeggings());
                eq.setBoots(rv.getBoots());
            } else if (rv.getRole() == VillagerRole.RECOLTEUR) {
                eq.setItemInMainHand(rv.getTool());
                eq.setHelmet(null);
                eq.setChestplate(null);
                eq.setLeggings(null);
                eq.setBoots(null);
            } else if (rv.getRole() == VillagerRole.CONSTRUCTEUR) {
                eq.setItemInMainHand(currentBuildMaterial(rv));
                eq.setHelmet(null);
                eq.setChestplate(null);
                eq.setLeggings(null);
                eq.setBoots(null);
            } else {
                eq.setItemInMainHand(null);
                eq.setHelmet(null);
                eq.setChestplate(null);
                eq.setLeggings(null);
                eq.setBoots(null);
            }
            // On gère nous-mêmes le drop complet à la mort (voir onDeath) : la mécanique
            // vanille de "chance de drop d'équipement" est désactivée pour éviter les doublons.
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACCESSEURS
    // ════════════════════════════════════════════════════════════════════════

    public RecruitedVillager getByEntity(UUID entityId) { return villagers.get(entityId); }

    public List<RecruitedVillager> getFactionVillagers(String factionName) {
        List<RecruitedVillager> list = new ArrayList<>();
        for (RecruitedVillager rv : villagers.values())
            if (rv.getFactionName().equalsIgnoreCase(factionName)) list.add(rv);
        return list;
    }

    public int countForFaction(String factionName) { return getFactionVillagers(factionName).size(); }

    public void markDirty() { save(); }

    // ════════════════════════════════════════════════════════════════════════
    // RÉGLAGES GUERRIER : poste, périmètre, combat, suivi, formation
    // ════════════════════════════════════════════════════════════════════════

    public void setPost(RecruitedVillager rv, Location loc) {
        rv.setPostLocation(loc);
        save();
    }

    public void setDefenseRadius(RecruitedVillager rv, double radius) {
        rv.setDefenseRadius(Math.max(4.0, Math.min(48.0, radius)));
        save();
    }

    public void setCombatEnabled(RecruitedVillager rv, boolean enabled) {
        rv.setCombatEnabled(enabled);
        if (!enabled) rv.setCurrentTarget(null);
        save();
    }

    public void toggleFollow(RecruitedVillager rv, Player player) {
        if (player.getUniqueId().equals(rv.getFollowTarget())) rv.setFollowTarget(null);
        else rv.setFollowTarget(player.getUniqueId());
        save();
    }

    public void setArcheryMode(RecruitedVillager rv, boolean enabled) {
        rv.setArcheryMode(enabled);
        Entity e = Bukkit.getEntity(rv.getEntityId());
        if (e instanceof Villager v) applyVisuals(rv, v);
        save();
    }

    public enum FormationType { LIGNE, CERCLE }

    /** Aligne en formation tous ses villageois recrutés à moins de 40 blocs (ligne devant le joueur, ou cercle autour de lui). */
    public void formation(Player player, FormationType type) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + "§cTu n'es pas dans une faction."); return; }

        List<Villager> present = new ArrayList<>();
        for (RecruitedVillager rv : getFactionVillagers(faction.getName())) {
            Entity e = Bukkit.getEntity(rv.getEntityId());
            if (e instanceof Villager v && !v.isDead() && v.getWorld().equals(player.getWorld())
                    && v.getLocation().distance(player.getLocation()) <= 40) {
                present.add(v);
            }
        }
        if (present.isEmpty()) { player.sendMessage(prefix() + "§cAucun villageois à moins de 40 blocs."); return; }

        Location base = player.getLocation();
        int n = present.size();

        if (type == FormationType.CERCLE) {
            double radius = Math.max(3.0, 1.2 + n * 0.5);
            for (int i = 0; i < n; i++) {
                double angle = (2 * Math.PI / n) * i;
                Location slot = base.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                present.get(i).getPathfinder().moveTo(slot, 0.6);
            }
            player.sendMessage(prefix() + "§a✔ " + n + " villageois se mettent en cercle autour de toi.");
            return;
        }

        Vector dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        for (int i = 0; i < n; i++) {
            double offset = (i - (n - 1) / 2.0) * 2.0;
            Location slot = base.clone().add(dir.clone().multiply(3)).add(right.clone().multiply(offset));
            present.get(i).getPathfinder().moveTo(slot, 0.6);
        }
        player.sendMessage(prefix() + "§a✔ " + n + " villageois se mettent en ligne devant toi.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // GESTION DES CHANTIERS (constructeur)
    // ════════════════════════════════════════════════════════════════════════

    public void cancelCurrentTask(RecruitedVillager rv) {
        rv.getTaskQueue().poll();
        clearScaffolds(rv);
        save();
    }

    public void clearTaskQueue(RecruitedVillager rv) {
        rv.getTaskQueue().clear();
        clearScaffolds(rv);
        save();
    }

    public void clearGatherZone(RecruitedVillager rv) {
        rv.setGatherZoneA(null);
        rv.setGatherZoneB(null);
        save();
    }

    public void clearRallyPoint(RecruitedVillager rv) {
        rv.setRallyPoint(null);
        save();
    }

    public void clearHarvestZone(RecruitedVillager rv) {
        rv.setHarvestZoneA(null);
        rv.setHarvestZoneB(null);
        save();
    }

    public void clearFarmZone(RecruitedVillager rv) {
        rv.setFarmZoneA(null);
        rv.setFarmZoneB(null);
        save();
    }

    public void clearOutputChest(RecruitedVillager rv) {
        rv.setOutputChest(null);
        save();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SÉLECTION DE ZONE / PATROUILLE (clics dans le monde)
    // ════════════════════════════════════════════════════════════════════════

    private enum SelectionType { TASK_ZONE, GATHER_ZONE, PATROL, GROUP_POST, GROUP_TASK_ZONE, RALLY, GROUP_RALLY,
            HARVEST_ZONE, FARM_ZONE, OUTPUT_CHEST }

    private static class PendingSelection {
        UUID villagerId;          // null pour les sélections de groupe
        final SelectionType type;
        final List<Location> points = new ArrayList<>();
        Material blockType;       // pour TASK_ZONE / GROUP_TASK_ZONE
        UUID assignedBy;          // pour TASK_ZONE / GROUP_TASK_ZONE
        String assignedByName;    // pour TASK_ZONE / GROUP_TASK_ZONE
        List<UUID> groupIds;      // pour GROUP_POST / GROUP_TASK_ZONE
        PendingSelection(UUID villagerId, SelectionType type) { this.villagerId = villagerId; this.type = type; }
    }

    public void startTaskZoneSelection(Player player, RecruitedVillager rv, Material blockType) {
        PendingSelection sel = new PendingSelection(rv.getEntityId(), SelectionType.TASK_ZONE);
        sel.blockType = blockType;
        sel.assignedBy = player.getUniqueId();
        sel.assignedByName = player.getName();
        pendingSelections.put(player.getUniqueId(), sel);
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du nouveau chantier §7(bloc : "
                + prettyMaterial(blockType) + ")§e.");
    }

    public void startGatherZoneSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.GATHER_ZONE));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e de la zone de récolte.");
    }

    public void startPatrolSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.PATROL));
        player.sendMessage(prefix() + "§eClique-droit pour ajouter un point de ronde. §bShift+clic-droit§e pour terminer.");
    }

    public void startHarvestZoneSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.HARVEST_ZONE));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e de la zone de récolte (mine/bois/terre selon son outil).");
    }

    public void startFarmZoneSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.FARM_ZONE));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du champ.");
    }

    public void startOutputChestSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.OUTPUT_CHEST));
        player.sendMessage(prefix() + "§eClique-droit sur le coffre où il doit tout déposer.");
    }

    /** Assigne le même poste (position de défense) à plusieurs guerriers en un seul clic. */
    public void startGroupPostSelection(Player player, List<UUID> guerrierIds) {
        PendingSelection sel = new PendingSelection(null, SelectionType.GROUP_POST);
        sel.groupIds = guerrierIds;
        pendingSelections.put(player.getUniqueId(), sel);
        player.sendMessage(prefix() + "§eClique-droit à l'endroit du poste commun §7(" + guerrierIds.size() + " guerrier(s))§e.");
    }

    /** Point de rassemblement individuel : là où ce villageois retourne une fois "libre". */
    public void startRallyPointSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.RALLY));
        player.sendMessage(prefix() + "§eClique-droit à l'endroit du point de rassemblement.");
    }

    /** Assigne le même point de rassemblement à plusieurs villageois (guerriers ET/OU constructeurs) à la fois. */
    public void startGroupRallyPointSelection(Player player, List<UUID> villagerIds) {
        PendingSelection sel = new PendingSelection(null, SelectionType.GROUP_RALLY);
        sel.groupIds = villagerIds;
        pendingSelections.put(player.getUniqueId(), sel);
        player.sendMessage(prefix() + "§eClique-droit à l'endroit du rassemblement commun §7("
                + villagerIds.size() + " villageois)§e.");
    }

    /** Assigne le même chantier (zone + type de bloc) à plusieurs constructeurs à la fois. */
    public void startGroupTaskZoneSelection(Player player, List<UUID> builderIds, Material blockType) {
        PendingSelection sel = new PendingSelection(null, SelectionType.GROUP_TASK_ZONE);
        sel.groupIds = builderIds;
        sel.blockType = blockType;
        sel.assignedBy = player.getUniqueId();
        sel.assignedByName = player.getName();
        pendingSelections.put(player.getUniqueId(), sel);
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du chantier commun §7("
                + builderIds.size() + " constructeur(s), bloc : " + prettyMaterial(blockType) + ")§e.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractForSelection(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PendingSelection sel = pendingSelections.get(player.getUniqueId());
        if (sel == null) return;
        // PlayerInteractEvent se déclenche 2 fois par clic (main + off-hand) : on ignore l'off-hand.
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        event.setCancelled(true);

        Location loc = event.getClickedBlock().getLocation();

        if (sel.type == SelectionType.GROUP_POST) {
            handleGroupPostClick(player, sel, loc);
            return;
        }
        if (sel.type == SelectionType.GROUP_RALLY) {
            handleGroupRallyClick(player, sel, loc);
            return;
        }
        if (sel.type == SelectionType.RALLY) {
            pendingSelections.remove(player.getUniqueId());
            RecruitedVillager rv = villagers.get(sel.villagerId);
            if (rv == null) return;
            rv.setRallyPoint(loc.clone());
            save();
            player.sendMessage(prefix() + "§a✔ Point de rassemblement défini pour §e" + rv.getDisplayName() + "§a.");
            return;
        }
        if (sel.type == SelectionType.OUTPUT_CHEST) {
            pendingSelections.remove(player.getUniqueId());
            RecruitedVillager rv = villagers.get(sel.villagerId);
            if (rv == null) return;
            if (!(loc.getBlock().getState() instanceof org.bukkit.block.Container)) {
                player.sendMessage(prefix() + "§cCe bloc n'est pas un coffre (ou un conteneur similaire).");
                return;
            }
            rv.setOutputChest(loc.clone());
            save();
            player.sendMessage(prefix() + "§a✔ Coffre de dépôt défini pour §e" + rv.getDisplayName() + "§a.");
            return;
        }

        RecruitedVillager rv = sel.villagerId != null ? villagers.get(sel.villagerId) : null;
        if (sel.type != SelectionType.GROUP_TASK_ZONE && rv == null) {
            pendingSelections.remove(player.getUniqueId());
            return;
        }

        if (sel.type == SelectionType.PATROL) {
            handlePatrolClick(player, rv, sel, loc, player.isSneaking());
        } else {
            handleZoneClick(player, rv, sel, loc);
        }
    }

    private void handleZoneClick(Player player, RecruitedVillager rv, PendingSelection sel, Location loc) {
        if (sel.points.isEmpty()) {
            sel.points.add(loc);
            player.sendMessage(prefix() + "§aCoin A défini. §eClique-droit sur le §b2e coin§e.");
            return;
        }

        Location corner1 = sel.points.get(0);
        if (!Objects.equals(corner1.getWorld(), loc.getWorld())) {
            player.sendMessage(prefix() + "§cLes deux coins doivent être dans le même monde. Sélection annulée.");
            pendingSelections.remove(player.getUniqueId());
            return;
        }

        int volume = computeVolume(corner1, loc);
        int maxVolume = plugin.getConfig().getInt("villager.max-zone-volume", 3375);
        if (volume > maxVolume) {
            player.sendMessage(prefix() + "§cZone trop grande (" + volume + " blocs, max " + maxVolume + "). Réessaie avec une zone plus petite.");
            pendingSelections.remove(player.getUniqueId());
            return;
        }

        pendingSelections.remove(player.getUniqueId());

        if (sel.type == SelectionType.TASK_ZONE) {
            BuildTask task = new BuildTask(corner1, loc, sel.blockType, sel.assignedBy, sel.assignedByName);
            rv.getTaskQueue().add(task);
            save();
            player.sendMessage(prefix() + "§a✔ Chantier ajouté à la file de §e" + rv.getDisplayName()
                    + " §a(" + volume + " blocs, " + prettyMaterial(sel.blockType) + "). File : §e"
                    + rv.getTaskQueue().size() + " chantier(s)§a.");
        } else if (sel.type == SelectionType.GROUP_TASK_ZONE) {
            int count = 0;
            for (UUID id : sel.groupIds) {
                RecruitedVillager builder = villagers.get(id);
                if (builder != null && builder.getRole() == VillagerRole.CONSTRUCTEUR) {
                    builder.getTaskQueue().add(new BuildTask(corner1.clone(), loc.clone(), sel.blockType, sel.assignedBy, sel.assignedByName));
                    count++;
                }
            }
            save();
            player.sendMessage(prefix() + "§a✔ Chantier commun ajouté à §e" + count + " constructeur(s) §a("
                    + volume + " blocs, " + prettyMaterial(sel.blockType) + "). Ils vont s'y mettre en même temps.");
        } else if (sel.type == SelectionType.GATHER_ZONE) {
            rv.setGatherZoneA(corner1);
            rv.setGatherZoneB(loc);
            save();
            player.sendMessage(prefix() + "§a✔ Zone de récolte définie pour §e" + rv.getDisplayName()
                    + " §a(" + volume + " blocs). S'il n'a plus le bloc voulu, il ira le miner ici.");
        } else if (sel.type == SelectionType.HARVEST_ZONE) {
            rv.setHarvestZoneA(corner1);
            rv.setHarvestZoneB(loc);
            save();
            player.sendMessage(prefix() + "§a✔ Zone de récolte définie pour §e" + rv.getDisplayName()
                    + " §a(" + volume + " blocs). Donne-lui l'outil adapté (pioche/hache/pelle) pour qu'il s'y mette.");
        } else { // FARM_ZONE
            rv.setFarmZoneA(corner1);
            rv.setFarmZoneB(loc);
            save();
            player.sendMessage(prefix() + "§a✔ Champ défini pour §e" + rv.getDisplayName()
                    + " §a(" + volume + " blocs). Donne-lui des graines : il plante, récolte et replante tout seul.");
        }
    }

    private void handleGroupPostClick(Player player, PendingSelection sel, Location loc) {
        pendingSelections.remove(player.getUniqueId());
        int count = 0;
        for (UUID id : sel.groupIds) {
            RecruitedVillager guard = villagers.get(id);
            if (guard != null && guard.getRole() == VillagerRole.GUERRIER) {
                guard.setPostLocation(loc.clone());
                count++;
            }
        }
        save();
        player.sendMessage(prefix() + "§a✔ Poste commun défini pour §e" + count + " guerrier(s)§a.");
    }

    private void handleGroupRallyClick(Player player, PendingSelection sel, Location loc) {
        pendingSelections.remove(player.getUniqueId());
        int count = 0;
        for (UUID id : sel.groupIds) {
            RecruitedVillager rv = villagers.get(id);
            if (rv != null) { rv.setRallyPoint(loc.clone()); count++; }
        }
        save();
        player.sendMessage(prefix() + "§a✔ Point de rassemblement commun défini pour §e" + count + " villageois§a.");
    }

    private void handlePatrolClick(Player player, RecruitedVillager rv, PendingSelection sel, Location loc, boolean finish) {
        sel.points.add(loc);
        int max = plugin.getConfig().getInt("villager.max-patrol-points", 10);
        if (finish || sel.points.size() >= max) {
            rv.setPatrolPoints(sel.points);
            rv.setPatrolIndex(0);
            pendingSelections.remove(player.getUniqueId());
            save();
            player.sendMessage(prefix() + "§a✔ Patrouille définie pour §e" + rv.getDisplayName()
                    + " §a(" + sel.points.size() + " point(s)). Il fera la ronde quand il n'a rien d'autre à faire.");
        } else {
            player.sendMessage(prefix() + "§aPoint " + sel.points.size() + " ajouté. §eClique-droit pour en ajouter un autre, "
                    + "§bshift+clic-droit§e pour terminer.");
        }
    }

    private int computeVolume(Location a, Location b) {
        int dx = Math.abs(a.getBlockX() - b.getBlockX()) + 1;
        int dy = Math.abs(a.getBlockY() - b.getBlockY()) + 1;
        int dz = Math.abs(a.getBlockZ() - b.getBlockZ()) + 1;
        return dx * dy * dz;
    }

    private String prettyMaterial(Material m) {
        return m.name().toLowerCase().replace('_', ' ');
    }

    // ════════════════════════════════════════════════════════════════════════
    // BOUCLE D'IA
    // ════════════════════════════════════════════════════════════════════════

    private void tickAll() {
        for (RecruitedVillager rv : new ArrayList<>(villagers.values())) {
            Entity e = Bukkit.getEntity(rv.getEntityId());
            if (!(e instanceof Villager v) || v.isDead()) continue;
            feedTick(rv, v);
            sleepPolling(rv, v);
            switch (rv.getRole()) {
                case CONSTRUCTEUR -> builderTick(rv, v);
                case GUERRIER -> warriorTick(rv, v);
                case RECOLTEUR -> recolteurTick(rv, v);
                default -> { }
            }
        }
    }

    private void feedTick(RecruitedVillager rv, Villager v) {
        ItemStack food = rv.getFood();
        double max = getMaxHealth(v);
        if (v.getHealth() >= max) return;

        if (food != null && food.getAmount() > 0) {
            double healAmount = plugin.getConfig().getDouble("villager.heal-per-food", 4.0);
            v.setHealth(Math.min(max, v.getHealth() + healAmount));
            v.getWorld().playSound(v.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

            int newAmount = food.getAmount() - 1;
            if (newAmount <= 0) rv.setFood(null);
            else food.setAmount(newAmount);
            return;
        }

        // Pas de nourriture donnée : à haut niveau, le constructeur se débrouille seul.
        int selfFeedLevel = plugin.getConfig().getInt("villager.self-feed-level", 5);
        if (rv.getRole() == VillagerRole.CONSTRUCTEUR && rv.getLevel() >= selfFeedLevel) {
            double selfHeal = plugin.getConfig().getDouble("villager.self-feed-heal", 1.0);
            v.setHealth(Math.min(max, v.getHealth() + selfHeal));
        }
    }

    private double getMaxHealth(Villager v) {
        double max = 20.0;
        try {
            var attr = v.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) max = attr.getValue();
        } catch (Throwable ignored) { /* nom d'attribut différent selon version serveur */ }
        return max;
    }

    // ════════════════════════════════════════════════════════════════════════
    // SOIN EN DORMANT DANS UN LIT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Détection de sommeil par polling pour Paper 1.21.4+, où l'événement
     * EntitySleepEvent n'est plus jamais lancé (supprimé côté Bukkit depuis le
     * passage des mobs à l'IA Brain). On détecte la transition awake → sleeping
     * dans la boucle d'IA principale (tickAll) et on déclenche startSleepHealing
     * une seule fois par "aller se coucher".
     */
    private void sleepPolling(RecruitedVillager rv, Villager v) {
        boolean sleeping = v.isSleeping();
        if (sleeping && !rv.isWasSleeping()) {
            startSleepHealing(rv);
        }
        rv.setWasSleeping(sleeping);
    }

    /** Petit soin périodique pendant quelques dizaines de secondes après qu'il se soit couché. */
    private void startSleepHealing(RecruitedVillager rv) {
        double healAmount = plugin.getConfig().getDouble("villager.heal-per-sleep-tick", 2.0);
        int maxTicks = plugin.getConfig().getInt("villager.sleep-heal-ticks", 8);
        long period = plugin.getConfig().getLong("villager.sleep-heal-period", 60L);

        new BukkitRunnable() {
            int done = 0;
            @Override public void run() {
                Entity e = Bukkit.getEntity(rv.getEntityId());
                if (!(e instanceof Villager vill) || vill.isDead()) { cancel(); return; }
                double max = getMaxHealth(vill);
                if (vill.getHealth() < max) {
                    vill.setHealth(Math.min(max, vill.getHealth() + healAmount));
                    vill.getWorld().playSound(vill.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.6f);
                }
                if (++done >= maxTicks) cancel();
            }
        }.runTaskTimer(plugin, period, period);
    }

    // ── Constructeur : traite le chantier en tête de file, récolte si besoin ──
    private void builderTick(RecruitedVillager rv, Villager v) {
        updateBuilderHand(rv, v);
        int actions = blocksPerAction(rv);
        for (int i = 0; i < actions; i++) {
            BuildTask task = rv.getCurrentTask();
            if (task == null) {
                // Plus de chantier en file : direction le point de rassemblement s'il est défini.
                clearScaffolds(rv);
                if (rv.getRallyPoint() != null) approach(v, rv.getRallyPoint(), 2.0, 0.5, "villager.task-teleport-distance", 64.0);
                return;
            }
            if (!builderAction(rv, v, task)) return; // en attente de ressources : on retente au tick suivant
        }
    }

    /** Met à jour ce qu'il tient en main pour refléter le matériau qu'il utilise en ce moment. */
    private void updateBuilderHand(RecruitedVillager rv, Villager v) {
        EntityEquipment eq = v.getEquipment();
        if (eq == null) return;
        ItemStack desired = currentBuildMaterial(rv);
        ItemStack current = eq.getItemInMainHand();
        boolean currentEmpty = current == null || current.getType() == Material.AIR;
        boolean desiredEmpty = desired == null;
        if (desiredEmpty != currentEmpty || (!desiredEmpty && !desired.isSimilar(current))) {
            eq.setItemInMainHand(desired);
        }
    }

    private ItemStack currentBuildMaterial(RecruitedVillager rv) {
        BuildTask task = rv.getCurrentTask();
        Material mat = task != null ? task.getBlockType() : null;
        if (mat == null) {
            for (ItemStack it : rv.getResources()) {
                if (it != null && it.getType() != Material.AIR) { mat = it.getType(); break; }
            }
        }
        return mat != null ? new ItemStack(mat, 1) : null;
    }

    /** Une "action" = un aller-miner OU une pose de bloc, exécutée immédiatement (téléportation + échafaudage si besoin). */
    private boolean builderAction(RecruitedVillager rv, Villager v, BuildTask task) {
        Material needed = task.getBlockType();
        int have = countResource(rv, needed);

        // Récupère un GROS lot d'un coup (au lieu d'un aller-retour à chaque bloc) : assez pour finir le chantier.
        int remainingGaps = countRemainingGaps(task);
        int gatherThreshold = Math.min(Math.max(remainingGaps, 1), 9 * 64);

        if (rv.hasGatherZone() && have < gatherThreshold) {
            Block ore = findGatherableBlock(rv, v, needed);
            if (ore != null) {
                Location oreLoc = ore.getLocation().add(0.5, 0, 0.5);
                goInstantlyTo(rv, v, oreLoc);
                ore.setType(Material.AIR);
                addResource(rv, needed, 1);
                v.getWorld().playSound(ore.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1f, 1f);
                addXp(rv, plugin.getConfig().getInt("villager.xp-per-gather", 1));
                return true;
            }
            // La zone de récolte n'a plus ce type de bloc : on continue avec le stock actuel.
        }

        if (have <= 0) return false; // plus de ressources et rien à récolter

        Block gap = findNextGap(task);
        if (gap == null) {
            completeTask(rv, v, task);
            return false;
        }

        Location targetLoc = gap.getLocation().add(0.5, 0, 0.5);
        goInstantlyTo(rv, v, targetLoc);

        gap.setType(needed);
        v.getWorld().playSound(gap.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1f, 1f);
        removeResource(rv, needed, 1);
        addXp(rv, plugin.getConfig().getInt("villager.xp-per-block", 2));
        return true;
    }

    /**
     * Se rend immédiatement là où il doit travailler, peu importe la distance : il n'y a pas
     * d'attente. Si l'endroit est en hauteur, un échafaudage en bambou est posé sous lui pour
     * qu'il ne tombe pas (retiré à la fin du chantier, sans drop).
     */
    private void goInstantlyTo(RecruitedVillager rv, Villager v, Location target) {
        buildScaffoldTo(rv, target);
        Location dest = target.clone();
        dest.setYaw(v.getLocation().getYaw());
        dest.setPitch(0f);
        v.teleport(dest);
    }

    /** Pose une colonne d'échafaudage en bambou du sol jusqu'à la cible si elle est trop haute pour l'atteindre autrement. */
    private void buildScaffoldTo(RecruitedVillager rv, Location target) {
        World world = target.getWorld();
        int x = target.getBlockX(), z = target.getBlockZ();
        int groundY = world.getHighestBlockYAt(x, z);
        int targetY = target.getBlockY();
        if (targetY - groundY <= 2) return; // assez bas, pas besoin d'échafaudage

        List<Location> scaffolds = activeScaffolds.computeIfAbsent(rv.getEntityId(), k -> new ArrayList<>());
        for (int y = groundY + 1; y < targetY; y++) {
            Block b = world.getBlockAt(x, y, z);
            Material type = b.getType();
            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
                b.setType(Material.SCAFFOLDING);
                scaffolds.add(b.getLocation());
            }
        }
    }

    /** Retire (sans les faire tomber au sol) les échafaudages posés pour le chantier en cours. */
    private void clearScaffolds(RecruitedVillager rv) {
        List<Location> scaffolds = activeScaffolds.remove(rv.getEntityId());
        if (scaffolds == null) return;
        for (Location loc : scaffolds) {
            Block b = loc.getBlock();
            if (b.getType() == Material.SCAFFOLDING) b.setType(Material.AIR); // setType() ne génère pas de drop
        }
    }

    private void completeTask(RecruitedVillager rv, Villager v, BuildTask task) {
        rv.getTaskQueue().poll(); // retire le chantier terminé, le suivant devient actif
        clearScaffolds(rv);
        addXp(rv, plugin.getConfig().getInt("villager.xp-per-task", 20));
        save();

        String line = "§7[§e" + rv.getDisplayName() + "§7] §fChantier terminé ! (" + prettyMaterial(task.getBlockType()) + ")";
        Player assigner = task.getAssignedBy() != null ? Bukkit.getPlayer(task.getAssignedBy()) : null;
        if (assigner != null && assigner.isOnline()) {
            assigner.sendMessage(line);
            assigner.playSound(assigner.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        }
    }

    /** Un échafaudage compte comme un "vide" à combler : il sera remplacé par le vrai bloc, jamais laissé tel quel. */
    private Block findNextGap(BuildTask task) {
        Location a = task.getZoneA();
        Location b = task.getZoneB();
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isGapMaterial(block.getType())) return block;
                }
            }
        }
        return null;
    }

    private int countRemainingGaps(BuildTask task) {
        Location a = task.getZoneA();
        Location b = task.getZoneB();
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isGapMaterial(world.getBlockAt(x, y, z).getType())) count++;
                }
            }
        }
        return count;
    }

    private boolean isGapMaterial(Material type) {
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR || type == Material.SCAFFOLDING;
    }

    /** Cherche le bloc du type voulu le plus proche du villageois, dans sa zone de récolte. */
    private Block findGatherableBlock(RecruitedVillager rv, Villager v, Material type) {
        Location a = rv.getGatherZoneA();
        Location b = rv.getGatherZoneB();
        if (a == null || b == null) return null;
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        Location vLoc = v.getLocation();
        Block nearest = null;
        double best = Double.MAX_VALUE;
        int maxScan = plugin.getConfig().getInt("villager.max-gather-scan", 4096);
        int scanned = 0;

        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (++scanned > maxScan) return nearest;
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != type) continue;
                    double d = block.getLocation().distanceSquared(vLoc);
                    if (d < best) { best = d; nearest = block; }
                }
            }
        }
        return nearest;
    }

    private int countResource(RecruitedVillager rv, Material type) {
        int total = 0;
        for (ItemStack it : rv.getResources()) if (it != null && it.getType() == type) total += it.getAmount();
        return total;
    }

    private void addResource(RecruitedVillager rv, Material type, int amount) {
        ItemStack[] res = rv.getResources();
        for (ItemStack it : res) {
            if (amount <= 0) return;
            if (it != null && it.getType() == type && it.getAmount() < it.getMaxStackSize()) {
                int add = Math.min(it.getMaxStackSize() - it.getAmount(), amount);
                it.setAmount(it.getAmount() + add);
                amount -= add;
            }
        }
        for (int i = 0; i < res.length && amount > 0; i++) {
            if (res[i] == null) {
                int stack = Math.min(amount, type.getMaxStackSize());
                res[i] = new ItemStack(type, stack);
                amount -= stack;
            }
        }
        // Si amount > 0 ici, la réserve (9 emplacements) est pleine : le surplus est perdu.
    }

    private void removeResource(RecruitedVillager rv, Material type, int amount) {
        ItemStack[] res = rv.getResources();
        for (int i = 0; i < res.length && amount > 0; i++) {
            ItemStack it = res[i];
            if (it != null && it.getType() == type) {
                int take = Math.min(it.getAmount(), amount);
                it.setAmount(it.getAmount() - take);
                amount -= take;
                if (it.getAmount() <= 0) res[i] = null;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RÉCOLTEUR : mine/coupe/creuse selon l'outil, cultive et récolte automatiquement
    // ════════════════════════════════════════════════════════════════════════

    private enum ToolCategory { PICKAXE, AXE, SHOVEL, NONE }

    private ToolCategory toolCategory(ItemStack tool) {
        if (tool == null) return ToolCategory.NONE;
        String name = tool.getType().name();
        if (name.endsWith("_PICKAXE")) return ToolCategory.PICKAXE;
        if (name.endsWith("_AXE") && !name.contains("PICK")) return ToolCategory.AXE;
        if (name.endsWith("_SHOVEL")) return ToolCategory.SHOVEL;
        return ToolCategory.NONE;
    }

    private boolean isHarvestableBy(ToolCategory cat, Material type) {
        String name = type.name();
        return switch (cat) {
            case PICKAXE -> name.endsWith("_ORE") || type == Material.ANCIENT_DEBRIS;
            case AXE -> name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
            case SHOVEL -> type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.SAND
                    || type == Material.RED_SAND || type == Material.GRAVEL || type == Material.CLAY
                    || type == Material.SOUL_SAND || type == Material.SOUL_SOIL
                    || type == Material.MYCELIUM || type == Material.PODZOL || type == Material.SNOW_BLOCK;
            case NONE -> false;
        };
    }

    private void recolteurTick(RecruitedVillager rv, Villager v) {
        ToolCategory cat = toolCategory(rv.getTool());
        if (cat != ToolCategory.NONE && rv.hasHarvestZone()) {
            Block target = findHarvestableBlock(rv, v, cat);
            if (target != null) {
                Location loc = target.getLocation().add(0.5, 0, 0.5);
                v.teleport(faceKeepingTeleport(v, loc));
                for (ItemStack drop : target.getDrops(rv.getTool())) depositOrKeep(rv, drop);
                target.setType(Material.AIR);
                v.getWorld().playSound(target.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1f, 1f);
                wearTool(rv);
                addXp(rv, plugin.getConfig().getInt("villager.xp-per-harvest", 2));
                return;
            }
        }
        if (rv.hasFarmZone() && farmTick(rv, v)) return;

        // Rien à faire pour l'instant : direction le point de rassemblement s'il est défini.
        if (rv.getRallyPoint() != null) approach(v, rv.getRallyPoint(), 2.0, 0.5, "villager.task-teleport-distance", 64.0);
    }

    private Location faceKeepingTeleport(Villager v, Location dest) {
        Location target = dest.clone();
        target.setYaw(v.getLocation().getYaw());
        target.setPitch(0f);
        return target;
    }

    /** Cherche le bloc récoltable le plus proche (selon l'outil) dans la zone de récolte. */
    private Block findHarvestableBlock(RecruitedVillager rv, Villager v, ToolCategory cat) {
        Location a = rv.getHarvestZoneA();
        Location b = rv.getHarvestZoneB();
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        Location vLoc = v.getLocation();
        Block nearest = null;
        double best = Double.MAX_VALUE;
        int maxScan = plugin.getConfig().getInt("villager.max-gather-scan", 4096);
        int scanned = 0;

        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (++scanned > maxScan) return nearest;
                    Block block = world.getBlockAt(x, y, z);
                    if (!isHarvestableBy(cat, block.getType())) continue;
                    double d = block.getLocation().distanceSquared(vLoc);
                    if (d < best) { best = d; nearest = block; }
                }
            }
        }
        return nearest;
    }

    private static final Material[] PLANTABLE_SEEDS = {
            Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO, Material.BEETROOT_SEEDS
    };

    private Material seedToCrop(Material seed) {
        return switch (seed) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            default -> null;
        };
    }

    private boolean isPlantableSeed(Material m) {
        for (Material s : PLANTABLE_SEEDS) if (s == m) return true;
        return false;
    }

    /** Un passage = récolter une culture mûre (et la replanter aussitôt), sinon ensemencer une case de champ libre. */
    /** Renvoie true s'il a fait quelque chose (récolté ou planté) ce passage. */
    private boolean farmTick(RecruitedVillager rv, Villager v) {
        Location a = rv.getFarmZoneA();
        Location b = rv.getFarmZoneB();
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        // 1) Récolter une culture mûre et la replanter aussitôt (même position)
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!(block.getBlockData() instanceof org.bukkit.block.data.Ageable age)) continue;
                    if (age.getAge() < age.getMaximumAge()) continue;
                    if (seedToCrop(block.getType()) == null && !isCropBlock(block.getType())) continue;

                    v.teleport(faceKeepingTeleport(v, block.getLocation().add(0.5, 0, 0.5)));
                    for (ItemStack drop : block.getDrops()) depositOrKeep(rv, drop);
                    age.setAge(0);
                    block.setBlockData(age);
                    v.getWorld().playSound(block.getLocation(), Sound.ENTITY_VILLAGER_WORK_FARMER, 1f, 1f);
                    addXp(rv, plugin.getConfig().getInt("villager.xp-per-harvest", 2));
                    return true;
                }
            }
        }

        // 2) Sinon, ensemencer une case de champ labourée mais vide, avec les graines qu'il a en réserve
        Material seed = firstAvailableSeed(rv);
        if (seed == null) return false;
        Material crop = seedToCrop(seed);
        if (crop == null) return false;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.FARMLAND) continue;
                    Block above = block.getRelative(org.bukkit.block.BlockFace.UP);
                    if (above.getType() != Material.AIR) continue;

                    v.teleport(faceKeepingTeleport(v, above.getLocation().add(0.5, 0, 0.5)));
                    above.setType(crop);
                    removeResource(rv, seed, 1);
                    v.getWorld().playSound(above.getLocation(), Sound.ENTITY_VILLAGER_WORK_FARMER, 1f, 1f);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCropBlock(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS;
    }

    private Material firstAvailableSeed(RecruitedVillager rv) {
        for (ItemStack it : rv.getResources()) {
            if (it != null && it.getAmount() > 0 && isPlantableSeed(it.getType())) return it.getType();
        }
        return null;
    }

    /** Les graines sont gardées sur lui (pour replanter) ; tout le reste part directement dans le coffre défini. */
    private void depositOrKeep(RecruitedVillager rv, ItemStack drop) {
        if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) return;

        if (isPlantableSeed(drop.getType())) {
            int remaining = addLootItem(rv, drop);
            if (remaining > 0 && rv.getOutputChest() != null) {
                ItemStack overflow = drop.clone();
                overflow.setAmount(remaining);
                depositIntoChest(rv, overflow);
            }
            return;
        }

        if (rv.getOutputChest() != null && depositIntoChest(rv, drop)) return;
        addLootItem(rv, drop); // pas de coffre (ou plein) : il garde le surplus sur lui en attendant
    }

    /** Renvoie true si tout a pu être déposé dans le coffre. */
    private boolean depositIntoChest(RecruitedVillager rv, ItemStack item) {
        Location chestLoc = rv.getOutputChest();
        if (chestLoc == null || chestLoc.getWorld() == null) return false;
        var state = chestLoc.getBlock().getState();
        if (!(state instanceof org.bukkit.block.Container container)) return false;
        var leftover = container.getInventory().addItem(item.clone());
        return leftover.isEmpty();
    }

    /** Use l'outil comme une arme (usure normale) ; à haut niveau (self-repair-level), il s'en refabrique un neuf. */
    private void wearTool(RecruitedVillager rv) {
        ItemStack tool = rv.getTool();
        if (tool == null) return;
        short maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) return;
        if (!(tool.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) return;

        int newDamage = dmg.getDamage() + 1;
        if (newDamage >= maxDurability) {
            int repairLevel = plugin.getConfig().getInt("villager.self-repair-level", 4);
            if (rv.getLevel() >= repairLevel) {
                dmg.setDamage(0);
                tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
                notifyFaction(rv.getFactionName(), "§7[§e" + rv.getDisplayName() + "§7] §fJe me suis fabriqué un nouvel outil pour remplacer celui qui s'est usé.");
            } else {
                rv.setTool(null);
                notifyFaction(rv.getFactionName(), "§7[§e" + rv.getDisplayName() + "§7] §fMon outil s'est cassé, il m'en faut un nouveau !");
            }
            syncLiveEntity(rv);
            save();
        } else {
            dmg.setDamage(newDamage);
            tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
        }
    }

    // ── Guerrier : combat, périmètre, patrouille, suivi ───────────────────────
    private void warriorTick(RecruitedVillager rv, Villager v) {
        if (handleFleeing(rv, v)) return; // priorité absolue : fuir/se cacher si en danger

        boolean canMelee = rv.getWeapon() != null;
        boolean canArcher = rv.isArcheryMode() && rv.getBow() != null && countArrows(rv) > 0;
        boolean canFight = canMelee || canArcher;

        LivingEntity target = null;
        if (canFight && rv.isCombatEnabled()) {
            target = resolveCurrentTarget(rv, v);
            if (target == null) {
                target = findThreat(rv, v);
                rv.setCurrentTarget(target != null ? target.getUniqueId() : null);
            }
        }

        if (target != null) {
            Location anchor = engagementAnchor(rv);
            double leash = effectiveRadius(rv) * 1.6;
            if (anchor != null && sameWorld(target.getLocation(), anchor) && target.getLocation().distance(anchor) > leash) {
                rv.setCurrentTarget(null); // la cible fuit trop loin du périmètre : abandon
                return;
            }
            if (!v.getWorld().equals(target.getWorld())) { rv.setCurrentTarget(null); return; }

            double dist = v.getLocation().distance(target.getLocation());
            boolean useArcher = canArcher && countArrows(rv) > 0 && dist > 2.2;

            if (useArcher) {
                ensureMainHand(v, rv.getBow());
                double minDist = plugin.getConfig().getDouble("villager.archer-min-distance", 6.0);
                double maxDist = plugin.getConfig().getDouble("villager.archer-max-distance", 18.0);
                v.lookAt(target);
                if (dist < minDist) {
                    // Trop près : recule pour garder ses distances, en tirant quand même si le cooldown le permet.
                    Vector away = v.getLocation().toVector().subtract(target.getLocation().toVector());
                    if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 0);
                    away.normalize().multiply(4);
                    v.getPathfinder().moveTo(v.getLocation().add(away), 0.5);
                    tryShoot(rv, v, target);
                } else if (dist <= maxDist) {
                    tryShoot(rv, v, target);
                } else {
                    v.getPathfinder().moveTo(target, 0.6);
                }
                return;
            }

            // Mode corps-à-corps : épée si équipée, sinon mains nues.
            ensureMainHand(v, rv.getWeapon());
            if (dist > 2.2) {
                v.getPathfinder().moveTo(target, 0.6);
                v.lookAt(target);
            } else {
                long now = System.currentTimeMillis();
                if (now - rv.getLastActionTick() >= ATTACK_COOLDOWN_MS) {
                    target.damage(computeDamage(rv), v);
                    v.swingMainHand();
                    v.getWorld().playSound(v.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
                    rv.setLastActionTick(now);
                    wearAndMaybeReplace(rv, false);
                }
            }
            return;
        }

        // Pas de cible : suivre le joueur assigné, sinon patrouiller, sinon rejoindre le point de
        // rassemblement s'il est "libre", sinon rejoindre/tenir le poste
        if (rv.getFollowTarget() != null) {
            Player followed = Bukkit.getPlayer(rv.getFollowTarget());
            if (followed != null && followed.isOnline()) {
                approach(v, followed.getLocation(), 3.5, 0.6, "villager.follow-teleport-distance", 50.0);
                return;
            }
        }

        if (!rv.getPatrolPoints().isEmpty()) {
            List<Location> points = rv.getPatrolPoints();
            int idx = rv.getPatrolIndex() % points.size();
            Location wp = points.get(idx);
            if (sameWorld(v.getLocation(), wp) && v.getLocation().distance(wp) <= 2.0) {
                rv.setPatrolIndex((idx + 1) % points.size());
            } else {
                approach(v, wp, 2.0, 0.4, "villager.task-teleport-distance", 64.0);
            }
            return;
        }

        if (rv.getRallyPoint() != null) {
            approach(v, rv.getRallyPoint(), 2.0, 0.5, "villager.task-teleport-distance", 64.0);
            return;
        }

        if (rv.getPostLocation() != null) {
            approach(v, rv.getPostLocation(), 4.0, 0.4, "villager.post-teleport-distance", 64.0);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FUITE / DISCRÉTION : trop peu de vie ou trop d'ennemis à la fois → se replier
    // ════════════════════════════════════════════════════════════════════════

    /** Renvoie true si le guerrier fuit ce tick (et a donc déjà agi : ne rien faire d'autre). */
    private boolean handleFleeing(RecruitedVillager rv, Villager v) {
        double healthPct = v.getHealth() / getMaxHealth(v);
        double fleeThreshold = plugin.getConfig().getDouble("villager.flee-health-percent", 0.25);
        double resumeThreshold = plugin.getConfig().getDouble("villager.flee-resume-percent", 0.6);
        int enemyThreshold = plugin.getConfig().getInt("villager.flee-enemy-count", 3);

        if (rv.isFleeing()) {
            boolean stillInDanger = healthPct < resumeThreshold
                    || countNearbyThreats(rv, v, 12.0) > 0;
            if (!stillInDanger) { rv.setFleeing(false); return false; }
            fleeMovement(rv, v);
            return true;
        }

        boolean lowHealth = healthPct <= fleeThreshold;
        boolean outnumbered = countNearbyThreats(rv, v, Math.max(effectiveRadius(rv), 12.0)) >= enemyThreshold;
        if (lowHealth || outnumbered) {
            rv.setFleeing(true);
            rv.setCurrentTarget(null);
            fleeMovement(rv, v);
            return true;
        }
        return false;
    }

    private void fleeMovement(RecruitedVillager rv, Villager v) {
        LivingEntity nearest = findNearestThreatUnrestricted(rv, v, 16.0);
        Location dest;
        if (nearest != null) {
            Vector away = v.getLocation().toVector().subtract(nearest.getLocation().toVector());
            if (away.lengthSquared() < 1.0E-4) away = new Vector(1, 0, 0);
            away.normalize().multiply(8);
            dest = v.getLocation().add(away);
        } else if (rv.getPostLocation() != null) {
            dest = rv.getPostLocation();
        } else {
            return; // rien de mieux à faire que rester immobile et se planquer sur place
        }
        v.getPathfinder().moveTo(dest, 0.9);
    }

    private int countNearbyThreats(RecruitedVillager rv, Villager v, double radius) {
        int count = 0;
        for (Entity e : v.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            boolean isThreat = (le instanceof Player p) ? isEnemyPlayer(rv, p) : MobUtils.isHostileMob(le);
            if (isThreat) count++;
        }
        return count;
    }

    /** Comme findThreat, mais sans restriction de périmètre : sert uniquement à savoir où fuir. */
    private LivingEntity findNearestThreatUnrestricted(RecruitedVillager rv, Villager v, double radius) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : v.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            boolean isThreat = (le instanceof Player p) ? isEnemyPlayer(rv, p) : MobUtils.isHostileMob(le);
            if (!isThreat) continue;
            double d = le.getLocation().distanceSquared(v.getLocation());
            if (d < best) { best = d; nearest = le; }
        }
        return nearest;
    }

    private void ensureMainHand(Villager v, ItemStack item) {
        EntityEquipment eq = v.getEquipment();
        if (eq != null) eq.setItemInMainHand(item);
    }

    private void tryShoot(RecruitedVillager rv, Villager v, LivingEntity target) {
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("villager.archer-cooldown-ms", 1500L);
        if (now - rv.getLastActionTick() < cooldown) return;

        Location eye = v.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        Arrow arrow = v.getWorld().spawnArrow(eye, dir, 2.2f, 8.0f);
        arrow.setShooter(v);
        arrow.setDamage(plugin.getConfig().getDouble("villager.archer-damage", 3.0)
                + (rv.getLevel() - 1) * plugin.getConfig().getDouble("villager.damage-per-level", 0.75));
        v.swingMainHand();
        v.getWorld().playSound(v.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
        rv.setLastActionTick(now);
        consumeArrow(rv);
        wearAndMaybeReplace(rv, true);
    }

    /**
     * Use une arme (ou un arc) d'un point de durabilité. Si elle casse : au niveau de
     * "réparation auto" (config), le guerrier s'en fabrique une nouvelle sur-le-champ ;
     * sinon elle est perdue et il faudra lui en redonner une.
     */
    private void wearAndMaybeReplace(RecruitedVillager rv, boolean isBow) {
        ItemStack item = isBow ? rv.getBow() : rv.getWeapon();
        if (item == null) return;
        short maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) return; // pas d'usure pour ce type d'objet
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) return;

        int newDamage = dmg.getDamage() + 1;
        if (newDamage >= maxDurability) {
            int repairLevel = plugin.getConfig().getInt("villager.self-repair-level", 4);
            if (rv.getLevel() >= repairLevel) {
                dmg.setDamage(0);
                item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
                notifyFaction(rv.getFactionName(), "§7[§e" + rv.getDisplayName() + "§7] §fJe me suis fabriqué "
                        + (isBow ? "un nouvel arc" : "une nouvelle arme") + " pour remplacer celle qui s'est usée.");
            } else {
                if (isBow) rv.setBow(null); else rv.setWeapon(null);
                notifyFaction(rv.getFactionName(), "§7[§e" + rv.getDisplayName() + "§7] §f"
                        + (isBow ? "Mon arc s'est cassé" : "Mon arme s'est cassée") + ", il m'en faut un(e) nouveau/nouvelle !");
            }
            syncLiveEntity(rv);
            save();
        } else {
            dmg.setDamage(newDamage);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
        }
    }

    private int countArrows(RecruitedVillager rv) {
        ItemStack a = rv.getArrows();
        return a != null ? a.getAmount() : 0;
    }

    private void consumeArrow(RecruitedVillager rv) {
        ItemStack a = rv.getArrows();
        if (a == null) return;
        int amount = a.getAmount() - 1;
        if (amount <= 0) rv.setArrows(null);
        else a.setAmount(amount);
    }

    /**
     * Déplace un villageois vers une destination, où qu'elle soit sur la carte :
     * pathfinding normal si elle est raisonnablement proche, téléportation de
     * rattrapage sinon (ou si elle est dans un autre monde) pour ne jamais le
     * laisser bloqué loin de sa tâche.
     */
    private void approach(Villager v, Location dest, double arriveDistance, double speed,
                           String teleportConfigKey, double teleportDefault) {
        if (!v.getWorld().equals(dest.getWorld())) {
            safeTeleportNear(v, dest);
            return;
        }
        double dist = v.getLocation().distance(dest);
        double teleportDist = plugin.getConfig().getDouble(teleportConfigKey, teleportDefault);
        if (dist > teleportDist) {
            safeTeleportNear(v, dest);
        } else if (dist > arriveDistance) {
            v.getPathfinder().moveTo(dest, speed);
        }
    }

    /** Téléportation de secours proche d'une destination lointaine, en évitant de l'encastrer sous terre. */
    private void safeTeleportNear(Villager v, Location dest) {
        Location target = dest.clone();
        target.setYaw(v.getLocation().getYaw());
        target.setPitch(0f);
        v.teleport(target);
    }

    private boolean sameWorld(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    /** Centre utilisé pour le calcul du périmètre : le joueur suivi si présent, sinon le poste. */
    private Location engagementAnchor(RecruitedVillager rv) {
        if (rv.getFollowTarget() != null) {
            Player p = Bukkit.getPlayer(rv.getFollowTarget());
            if (p != null && p.isOnline()) return p.getLocation();
        }
        return rv.getPostLocation();
    }

    private double effectiveRadius(RecruitedVillager rv) {
        return rv.getDefenseRadius();
    }

    /** Cible déjà engagée (attaque en cours ou auto-défense) : on ne revalide pas son statut d'ennemi. */
    private LivingEntity resolveCurrentTarget(RecruitedVillager rv, Villager v) {
        UUID id = rv.getCurrentTarget();
        if (id == null) return null;
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof LivingEntity le) || le.isDead()) return null;
        if (!le.getWorld().equals(v.getWorld())) return null;
        return le;
    }

    /** Cherche un mob hostile ou un joueur d'une faction ennemie (en guerre) dans le périmètre. */
    private LivingEntity findThreat(RecruitedVillager rv, Villager v) {
        Location anchor = engagementAnchor(rv);
        double radius = effectiveRadius(rv);
        double scanRadius = Math.max(radius, 6.0);

        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : v.getNearbyEntities(scanRadius, scanRadius / 2.0, scanRadius)) {
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            boolean isThreat = (le instanceof Player p) ? isEnemyPlayer(rv, p) : MobUtils.isHostileMob(le);
            if (!isThreat) continue;
            if (anchor != null && le.getLocation().distance(anchor) > radius) continue;
            double d = le.getLocation().distanceSquared(v.getLocation());
            if (d < best) { best = d; nearest = le; }
        }
        return nearest;
    }

    /** Une faction B est "ennemie" de A si elle est actuellement en guerre active contre A (ni alliée, ni elle-même). */
    private boolean isEnemyFaction(String myFaction, String otherFaction) {
        if (myFaction.equalsIgnoreCase(otherFaction)) return false;
        Faction mine = factionManager.getFaction(myFaction);
        if (mine != null && mine.isAlly(otherFaction)) return false;
        if (warManager != null) {
            WarSession session = warManager.getActiveWarOf(myFaction);
            if (session != null) {
                String opponent = session.getOpponent(myFaction);
                return opponent != null && opponent.equalsIgnoreCase(otherFaction);
            }
        }
        return false;
    }

    private boolean isEnemyPlayer(RecruitedVillager rv, Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return false;
        Faction theirFaction = factionManager.getPlayerFaction(p.getUniqueId());
        if (theirFaction == null) return false;
        return isEnemyFaction(rv.getFactionName(), theirFaction.getName());
    }

    private double computeDamage(RecruitedVillager rv) {
        double base;
        ItemStack weapon = rv.getWeapon();
        if (weapon == null) {
            base = 1.0; // à mains nues
        } else {
            String name = weapon.getType().name();
            base = 2.0;
            if (name.contains("NETHERITE")) base = 8.0;
            else if (name.contains("DIAMOND")) base = 7.0;
            else if (name.contains("IRON")) base = 5.0;
            else if (name.contains("STONE")) base = 4.0;
            else if (name.contains("GOLD")) base = 4.0;
            else if (name.contains("WOOD")) base = 3.0;
            if (name.contains("AXE") && !name.contains("PICKAXE")) base += 1.0;
        }
        base += (rv.getLevel() - 1) * plugin.getConfig().getDouble("villager.damage-per-level", 0.75);
        return base;
    }

    // ════════════════════════════════════════════════════════════════════════
    // AUTO-DÉFENSE : riposte si le villageois, un membre de la faction,
    // ou un autre villageois recruté à proximité se fait attaquer.
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageForDefense(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        LivingEntity attacker = resolveAttackerEntity(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;

        // XP de combat pour le guerrier qui vient de porter un coup (mêlée ou flèche)
        RecruitedVillager attackerRv = villagers.get(attacker.getUniqueId());
        if (attackerRv != null && attackerRv.getRole() == VillagerRole.GUERRIER) {
            addXp(attackerRv, plugin.getConfig().getInt("villager.xp-per-hit", 3));
            lastDamagedBy.put(victim.getUniqueId(), attacker.getUniqueId());
        }

        String victimFaction = resolveFactionOf(victim);

        // Cas 1 : le villageois lui-même est attaqué → auto-défense (sauf s'il est déjà en train de fuir)
        RecruitedVillager selfRv = villagers.get(victim.getUniqueId());
        if (selfRv != null && selfRv.getRole() == VillagerRole.GUERRIER && selfRv.isCombatEnabled()
                && !selfRv.isFleeing() && !isFriendly(selfRv.getFactionName(), attacker)) {
            selfRv.setCurrentTarget(attacker.getUniqueId());
        }

        // Cas 2 : un joueur de faction (ou un autre villageois recruté) proche est attaqué → les guerriers défendent
        if (victimFaction == null || isFriendly(victimFaction, attacker)) return;
        for (RecruitedVillager rv : getFactionVillagers(victimFaction)) {
            if (rv.getRole() != VillagerRole.GUERRIER || !rv.isCombatEnabled() || rv.isFleeing()) continue;
            if (rv.getEntityId().equals(victim.getUniqueId())) continue; // déjà géré au-dessus
            Entity e = Bukkit.getEntity(rv.getEntityId());
            if (!(e instanceof Villager guard) || guard.isDead()) continue;
            if (!guard.getWorld().equals(victim.getWorld())) continue;
            double radius = Math.max(effectiveRadius(rv), 10.0);
            if (guard.getLocation().distance(victim.getLocation()) <= radius) {
                rv.setCurrentTarget(attacker.getUniqueId());
            }
        }
    }

    private boolean isFriendly(String factionName, LivingEntity attacker) {
        if (!(attacker instanceof Player p)) return false;
        Faction f = factionManager.getPlayerFaction(p.getUniqueId());
        return f != null && f.getName().equalsIgnoreCase(factionName);
    }

    private LivingEntity resolveAttackerEntity(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            return (source instanceof LivingEntity le) ? le : null;
        }
        if (damager instanceof LivingEntity le) return le; // mob attaquant directement (ex : zombie)
        return null;
    }

    private String resolveFactionOf(LivingEntity victim) {
        if (victim instanceof Player p) {
            Faction f = factionManager.getPlayerFaction(p.getUniqueId());
            return f != null ? f.getName() : null;
        }
        RecruitedVillager rv = villagers.get(victim.getUniqueId());
        return rv != null ? rv.getFactionName() : null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // NETTOYAGE À LA MORT
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        UUID deadId = event.getEntity().getUniqueId();

        // XP + kill-count pour le guerrier qui a porté le coup fatal, et récupération du butin
        UUID killerId = lastDamagedBy.remove(deadId);
        if (killerId != null) {
            RecruitedVillager killerRv = villagers.get(killerId);
            if (killerRv != null && killerRv.getRole() == VillagerRole.GUERRIER) {
                addXp(killerRv, plugin.getConfig().getInt("villager.xp-per-kill", 15));
                killerRv.setKillCount(killerRv.getKillCount() + 1);
                collectLoot(killerRv, event.getDrops());
            }
        }

        RecruitedVillager rv = villagers.remove(deadId);
        if (rv != null) {
            dropAllItems(rv, event.getEntity().getLocation());
            clearScaffolds(rv);
            save();
            notifyFaction(rv.getFactionName(), "§c☠ Le villageois §e" + rv.getDisplayName()
                    + " §c(" + rv.getRole().displayName() + ") a été tué !");
        }
    }

    /** Fait tomber au sol tout ce que le villageois avait sur lui (équipement + réserve) à sa mort. */
    private void dropAllItems(RecruitedVillager rv, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        dropIfPresent(world, loc, rv.getWeapon());
        dropIfPresent(world, loc, rv.getHelmet());
        dropIfPresent(world, loc, rv.getChestplate());
        dropIfPresent(world, loc, rv.getLeggings());
        dropIfPresent(world, loc, rv.getBoots());
        dropIfPresent(world, loc, rv.getBow());
        dropIfPresent(world, loc, rv.getArrows());
        dropIfPresent(world, loc, rv.getTool());
        dropIfPresent(world, loc, rv.getFood());
        for (ItemStack it : rv.getResources()) dropIfPresent(world, loc, it);
    }

    private void dropIfPresent(World world, Location loc, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
            world.dropItemNaturally(loc, item);
        }
    }

    /** Le guerrier ramasse le butin de sa victime dans sa réserve (9 emplacements) ; le surplus reste au sol. */
    private void collectLoot(RecruitedVillager rv, List<ItemStack> drops) {
        boolean changed = false;
        var it = drops.iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
            int remaining = addLootItem(rv, item);
            if (remaining <= 0) { it.remove(); changed = true; }
            else if (remaining < item.getAmount()) { item.setAmount(remaining); changed = true; }
        }
        if (changed) save();
    }

    /** Ajoute un item dans la réserve du villageois. Renvoie la quantité qui n'a pas pu être absorbée (0 = tout pris). */
    private int addLootItem(RecruitedVillager rv, ItemStack item) {
        ItemStack[] res = rv.getResources();
        int amount = item.getAmount();
        for (ItemStack slot : res) {
            if (amount <= 0) break;
            if (slot != null && slot.isSimilar(item)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                int add = Math.min(space, amount);
                if (add > 0) { slot.setAmount(slot.getAmount() + add); amount -= add; }
            }
        }
        for (int i = 0; i < res.length && amount > 0; i++) {
            if (res[i] == null) {
                int stack = Math.min(amount, item.getMaxStackSize());
                ItemStack clone = item.clone();
                clone.setAmount(stack);
                res[i] = clone;
                amount -= stack;
            }
        }
        return amount;
    }

    private void notifyFaction(String factionName, String message) {
        Faction f = factionManager.getFaction(factionName);
        if (f == null) return;
        for (UUID uuid : f.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(prefix() + message);
        }
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERSISTANCE
    // ════════════════════════════════════════════════════════════════════════

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (RecruitedVillager rv : villagers.values()) {
            String key = "villagers." + rv.getEntityId();
            cfg.set(key + ".faction", rv.getFactionName());
            if (rv.getCustomName() != null) cfg.set(key + ".name", rv.getCustomName());
            cfg.set(key + ".role", rv.getRole().name());
            cfg.set(key + ".level", rv.getLevel());
            cfg.set(key + ".xp", rv.getXp());
            cfg.set(key + ".kills", rv.getKillCount());

            ItemStack[] res = rv.getResources();
            for (int i = 0; i < res.length; i++) if (res[i] != null) cfg.set(key + ".resources." + i, res[i]);

            if (rv.getWeapon() != null)     cfg.set(key + ".equip.weapon", rv.getWeapon());
            if (rv.getHelmet() != null)     cfg.set(key + ".equip.helmet", rv.getHelmet());
            if (rv.getChestplate() != null) cfg.set(key + ".equip.chestplate", rv.getChestplate());
            if (rv.getLeggings() != null)   cfg.set(key + ".equip.leggings", rv.getLeggings());
            if (rv.getBoots() != null)      cfg.set(key + ".equip.boots", rv.getBoots());
            if (rv.getBow() != null)        cfg.set(key + ".equip.bow", rv.getBow());
            if (rv.getArrows() != null)     cfg.set(key + ".equip.arrows", rv.getArrows());
            cfg.set(key + ".archeryMode", rv.isArcheryMode());
            if (rv.getFood() != null)       cfg.set(key + ".food", rv.getFood());

            if (!rv.getTaskQueue().isEmpty()) {
                int idx = 0;
                for (BuildTask task : rv.getTaskQueue()) {
                    String tKey = key + ".tasks." + idx;
                    cfg.set(tKey + ".zoneA", locToString(task.getZoneA()));
                    cfg.set(tKey + ".zoneB", locToString(task.getZoneB()));
                    cfg.set(tKey + ".blockType", task.getBlockType().name());
                    if (task.getAssignedBy() != null) cfg.set(tKey + ".assignedBy", task.getAssignedBy().toString());
                    if (task.getAssignedByName() != null) cfg.set(tKey + ".assignedByName", task.getAssignedByName());
                    idx++;
                }
            }
            if (rv.hasGatherZone()) {
                cfg.set(key + ".gatherA", locToString(rv.getGatherZoneA()));
                cfg.set(key + ".gatherB", locToString(rv.getGatherZoneB()));
            }

            if (rv.getTool() != null) cfg.set(key + ".tool", rv.getTool());
            if (rv.hasHarvestZone()) {
                cfg.set(key + ".harvestA", locToString(rv.getHarvestZoneA()));
                cfg.set(key + ".harvestB", locToString(rv.getHarvestZoneB()));
            }
            if (rv.hasFarmZone()) {
                cfg.set(key + ".farmA", locToString(rv.getFarmZoneA()));
                cfg.set(key + ".farmB", locToString(rv.getFarmZoneB()));
            }
            if (rv.getOutputChest() != null) cfg.set(key + ".outputChest", locToString(rv.getOutputChest()));

            if (rv.getPostLocation() != null) cfg.set(key + ".post", locToString(rv.getPostLocation()));
            if (rv.getRallyPoint() != null)   cfg.set(key + ".rally", locToString(rv.getRallyPoint()));
            cfg.set(key + ".defenseRadius", rv.getDefenseRadius());
            cfg.set(key + ".combatEnabled", rv.isCombatEnabled());
            if (!rv.getPatrolPoints().isEmpty()) {
                List<String> pts = new ArrayList<>();
                for (Location l : rv.getPatrolPoints()) pts.add(locToString(l));
                cfg.set(key + ".patrol", pts);
            }
        }
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Erreur sauvegarde villageois : " + e.getMessage());
        }
    }

    public void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (!cfg.contains("villagers")) return;
        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("villagers")).getKeys(false)) {
            String path = "villagers." + key;
            UUID id;
            try { id = UUID.fromString(key); } catch (Exception e) { continue; }

            String factionName = cfg.getString(path + ".faction", "");
            RecruitedVillager rv = new RecruitedVillager(id, factionName);
            rv.setCustomName(cfg.getString(path + ".name", null));
            rv.setRole(VillagerRole.fromString(cfg.getString(path + ".role", "AUCUN")));
            rv.setLevel(Math.max(1, Math.min(MAX_LEVEL, cfg.getInt(path + ".level", 1))));
            rv.setXp(cfg.getInt(path + ".xp", 0));
            rv.setKillCount(cfg.getInt(path + ".kills", 0));

            ItemStack[] res = new ItemStack[9];
            for (int i = 0; i < 9; i++) if (cfg.contains(path + ".resources." + i)) res[i] = cfg.getItemStack(path + ".resources." + i);
            rv.setResources(res);

            if (cfg.contains(path + ".equip.weapon"))     rv.setWeapon(cfg.getItemStack(path + ".equip.weapon"));
            if (cfg.contains(path + ".equip.helmet"))     rv.setHelmet(cfg.getItemStack(path + ".equip.helmet"));
            if (cfg.contains(path + ".equip.chestplate")) rv.setChestplate(cfg.getItemStack(path + ".equip.chestplate"));
            if (cfg.contains(path + ".equip.leggings"))   rv.setLeggings(cfg.getItemStack(path + ".equip.leggings"));
            if (cfg.contains(path + ".equip.boots"))      rv.setBoots(cfg.getItemStack(path + ".equip.boots"));
            if (cfg.contains(path + ".equip.bow"))        rv.setBow(cfg.getItemStack(path + ".equip.bow"));
            if (cfg.contains(path + ".equip.arrows"))     rv.setArrows(cfg.getItemStack(path + ".equip.arrows"));
            rv.setArcheryMode(cfg.getBoolean(path + ".archeryMode", false));
            if (cfg.contains(path + ".food"))             rv.setFood(cfg.getItemStack(path + ".food"));

            if (cfg.contains(path + ".tasks")) {
                var tasksSection = cfg.getConfigurationSection(path + ".tasks");
                if (tasksSection != null) {
                    List<String> indices = new ArrayList<>(tasksSection.getKeys(false));
                    indices.sort((s1, s2) -> {
                        try { return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2)); }
                        catch (NumberFormatException e) { return s1.compareTo(s2); }
                    });
                    for (String idxKey : indices) {
                        String tKey = path + ".tasks." + idxKey;
                        Location ta = stringToLoc(cfg.getString(tKey + ".zoneA"));
                        Location tb = stringToLoc(cfg.getString(tKey + ".zoneB"));
                        String matName = cfg.getString(tKey + ".blockType");
                        if (ta == null || tb == null || matName == null) continue;
                        Material mat;
                        try { mat = Material.valueOf(matName); } catch (IllegalArgumentException e) { continue; }
                        UUID assignedBy = null;
                        String assignedByStr = cfg.getString(tKey + ".assignedBy");
                        if (assignedByStr != null) { try { assignedBy = UUID.fromString(assignedByStr); } catch (Exception ignored) {} }
                        String assignedByName = cfg.getString(tKey + ".assignedByName");
                        rv.getTaskQueue().add(new BuildTask(ta, tb, mat, assignedBy, assignedByName));
                    }
                }
            }
            if (cfg.contains(path + ".gatherA") && cfg.contains(path + ".gatherB")) {
                Location ga = stringToLoc(cfg.getString(path + ".gatherA"));
                Location gb = stringToLoc(cfg.getString(path + ".gatherB"));
                if (ga != null && gb != null) { rv.setGatherZoneA(ga); rv.setGatherZoneB(gb); }
            }

            if (cfg.contains(path + ".tool")) rv.setTool(cfg.getItemStack(path + ".tool"));
            if (cfg.contains(path + ".harvestA") && cfg.contains(path + ".harvestB")) {
                Location ha = stringToLoc(cfg.getString(path + ".harvestA"));
                Location hb = stringToLoc(cfg.getString(path + ".harvestB"));
                if (ha != null && hb != null) { rv.setHarvestZoneA(ha); rv.setHarvestZoneB(hb); }
            }
            if (cfg.contains(path + ".farmA") && cfg.contains(path + ".farmB")) {
                Location fa = stringToLoc(cfg.getString(path + ".farmA"));
                Location fb = stringToLoc(cfg.getString(path + ".farmB"));
                if (fa != null && fb != null) { rv.setFarmZoneA(fa); rv.setFarmZoneB(fb); }
            }
            if (cfg.contains(path + ".outputChest")) rv.setOutputChest(stringToLoc(cfg.getString(path + ".outputChest")));

            if (cfg.contains(path + ".post")) rv.setPostLocation(stringToLoc(cfg.getString(path + ".post")));
            if (cfg.contains(path + ".rally")) rv.setRallyPoint(stringToLoc(cfg.getString(path + ".rally")));
            rv.setDefenseRadius(cfg.getDouble(path + ".defenseRadius", 16.0));
            rv.setCombatEnabled(cfg.getBoolean(path + ".combatEnabled", true));
            if (cfg.contains(path + ".patrol")) {
                List<Location> pts = new ArrayList<>();
                for (String s : cfg.getStringList(path + ".patrol")) {
                    Location l = stringToLoc(s);
                    if (l != null) pts.add(l);
                }
                rv.setPatrolPoints(pts);
            }

            villagers.put(id, rv);
        }
        plugin.getLogger().info(villagers.size() + " villageois recruté(s) chargé(s).");
    }

    private String locToString(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    private Location stringToLoc(String s) {
        if (s == null) return null;
        String[] p = s.split(";");
        if (p.length != 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }
}
