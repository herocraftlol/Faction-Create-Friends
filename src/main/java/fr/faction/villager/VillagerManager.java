package fr.faction.villager;

import fr.faction.claim.ClaimManager;
import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
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

    private final Map<UUID, RecruitedVillager> villagers = new HashMap<>();
    private final Map<UUID, PendingSelection> pendingSelections = new HashMap<>();
    private final File dataFile;

    public VillagerManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "villagers.yml");
        load();
    }

    public void setClaimManager(ClaimManager claimManager) { this.claimManager = claimManager; }
    public void setWarManager(WarManager warManager)       { this.warManager = warManager; }

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
            EntityEquipment eq = v.getEquipment();
            if (eq != null) {
                for (ItemStack it : new ItemStack[]{eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots(), eq.getItemInMainHand()}) {
                    if (it != null && it.getType() != Material.AIR) v.getWorld().dropItemNaturally(v.getLocation(), it);
                }
                eq.clear();
            }
        }
        villagers.remove(rv.getEntityId());
        pendingSelections.values().removeIf(pz -> pz.villagerId.equals(rv.getEntityId()));
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

    private void applyVisuals(RecruitedVillager rv, Villager v) {
        String roleTag = switch (rv.getRole()) {
            case CONSTRUCTEUR -> "§b[Constructeur]";
            case GUERRIER -> "§c[Guerrier]";
            default -> "§7[Recrue]";
        };
        v.setCustomName(ChatColor.YELLOW + rv.getDisplayName() + " " + roleTag);
        v.setCustomNameVisible(true);
        v.setPersistent(true);
        v.setRemoveWhenFarAway(false);
        try {
            v.setProfession(switch (rv.getRole()) {
                case CONSTRUCTEUR -> Villager.Profession.MASON;
                case GUERRIER -> Villager.Profession.WEAPONSMITH;
                default -> Villager.Profession.NONE;
            });
        } catch (Exception ignored) {}

        EntityEquipment eq = v.getEquipment();
        if (eq != null) {
            if (rv.getRole() == VillagerRole.GUERRIER) {
                eq.setItemInMainHand(rv.getWeapon());
                eq.setHelmet(rv.getHelmet());
                eq.setChestplate(rv.getChestplate());
                eq.setLeggings(rv.getLeggings());
                eq.setBoots(rv.getBoots());
            } else {
                eq.setItemInMainHand(null);
                eq.setHelmet(null);
                eq.setChestplate(null);
                eq.setLeggings(null);
                eq.setBoots(null);
            }
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

    /** Aligne en formation, devant le joueur, tous ses villageois recrutés à moins de 40 blocs. */
    public void formation(Player player) {
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
        Vector dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        int n = present.size();
        for (int i = 0; i < n; i++) {
            double offset = (i - (n - 1) / 2.0) * 2.0;
            Location slot = base.clone().add(dir.clone().multiply(3)).add(right.clone().multiply(offset));
            present.get(i).getPathfinder().moveTo(slot, 0.6);
        }
        player.sendMessage(prefix() + "§a✔ " + n + " villageois se mettent en formation devant toi.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // SÉLECTION DE ZONE / PATROUILLE (clics dans le monde)
    // ════════════════════════════════════════════════════════════════════════

    private enum SelectionType { ZONE, PATROL }

    private static class PendingSelection {
        final UUID villagerId;
        final SelectionType type;
        final List<Location> points = new ArrayList<>();
        PendingSelection(UUID villagerId, SelectionType type) { this.villagerId = villagerId; this.type = type; }
    }

    public void startZoneSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.ZONE));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du chantier (bloc au sol par ex.).");
    }

    public void startPatrolSelection(Player player, RecruitedVillager rv) {
        pendingSelections.put(player.getUniqueId(), new PendingSelection(rv.getEntityId(), SelectionType.PATROL));
        player.sendMessage(prefix() + "§eClique-droit pour ajouter un point de ronde. §bShift+clic-droit§e pour terminer.");
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

        RecruitedVillager rv = villagers.get(sel.villagerId);
        if (rv == null) { pendingSelections.remove(player.getUniqueId()); return; }

        Location loc = event.getClickedBlock().getLocation();
        if (sel.type == SelectionType.ZONE) {
            handleZoneClick(player, rv, sel, loc);
        } else {
            handlePatrolClick(player, rv, sel, loc, player.isSneaking());
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

        if (claimManager != null) {
            boolean okA = claimBelongsToFaction(corner1, rv.getFactionName());
            boolean okB = claimBelongsToFaction(loc, rv.getFactionName());
            if (!okA || !okB) {
                player.sendMessage(prefix() + "§cLes deux coins doivent être dans un chunk claimé par ta faction.");
                pendingSelections.remove(player.getUniqueId());
                return;
            }
        }

        rv.setZoneA(corner1);
        rv.setZoneB(loc);
        pendingSelections.remove(player.getUniqueId());
        save();
        player.sendMessage(prefix() + "§a✔ Chantier défini pour §e" + rv.getDisplayName() + " §a(" + volume + " blocs). "
                + "Donne-lui des blocs et il comblera les vides de la zone (construction ET réparation).");
    }

    private void handlePatrolClick(Player player, RecruitedVillager rv, PendingSelection sel, Location loc, boolean finish) {
        if (claimManager != null && !claimBelongsToFaction(loc, rv.getFactionName())) {
            player.sendMessage(prefix() + "§cCe point doit être dans un chunk claimé par ta faction.");
            return;
        }
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

    private boolean claimBelongsToFaction(Location loc, String factionName) {
        if (claimManager == null) return true;
        var chunk = loc.getChunk();
        if (!claimManager.isClaimed(chunk)) return false;
        var data = claimManager.getClaim(chunk);
        return data != null && data.getFactionName().equalsIgnoreCase(factionName);
    }

    private int computeVolume(Location a, Location b) {
        int dx = Math.abs(a.getBlockX() - b.getBlockX()) + 1;
        int dy = Math.abs(a.getBlockY() - b.getBlockY()) + 1;
        int dz = Math.abs(a.getBlockZ() - b.getBlockZ()) + 1;
        return dx * dy * dz;
    }

    // ════════════════════════════════════════════════════════════════════════
    // BOUCLE D'IA
    // ════════════════════════════════════════════════════════════════════════

    private void tickAll() {
        for (RecruitedVillager rv : new ArrayList<>(villagers.values())) {
            Entity e = Bukkit.getEntity(rv.getEntityId());
            if (!(e instanceof Villager v) || v.isDead()) continue;
            feedTick(rv, v);
            switch (rv.getRole()) {
                case CONSTRUCTEUR -> builderTick(rv, v);
                case GUERRIER -> warriorTick(rv, v);
                default -> { }
            }
        }
    }

    private void feedTick(RecruitedVillager rv, Villager v) {
        ItemStack food = rv.getFood();
        if (food == null || food.getAmount() <= 0) return;

        double max = 20.0;
        try {
            var attr = v.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) max = attr.getValue();
        } catch (Throwable ignored) { /* nom d'attribut différent selon version serveur */ }

        if (v.getHealth() >= max) return;

        double healAmount = plugin.getConfig().getDouble("villager.heal-per-food", 4.0);
        v.setHealth(Math.min(max, v.getHealth() + healAmount));
        v.getWorld().playSound(v.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);

        int newAmount = food.getAmount() - 1;
        if (newAmount <= 0) rv.setFood(null);
        else food.setAmount(newAmount);
    }

    // ── Constructeur : comble les vides de sa zone avec ses ressources ────────
    private void builderTick(RecruitedVillager rv, Villager v) {
        if (!rv.hasZone()) return;

        Block target = findNextGap(rv);
        if (target == null) return; // zone entièrement construite

        int slot = firstUsableResourceSlot(rv);
        if (slot < 0) return; // plus de ressources

        Location targetLoc = target.getLocation().add(0.5, 0, 0.5);
        double dist = v.getLocation().distance(targetLoc);
        if (dist > 3.2) {
            v.getPathfinder().moveTo(targetLoc, 0.5);
            return;
        }

        ItemStack stack = rv.getResources()[slot];
        Material mat = stack.getType();
        target.setType(mat);
        v.getWorld().playSound(target.getLocation(), Sound.ENTITY_VILLAGER_WORK_MASON, 1f, 1f);

        int amount = stack.getAmount() - 1;
        if (amount <= 0) rv.getResources()[slot] = null;
        else stack.setAmount(amount);
    }

    private Block findNextGap(RecruitedVillager rv) {
        Location a = rv.getZoneA();
        Location b = rv.getZoneB();
        World world = a.getWorld();
        int minX = Math.min(a.getBlockX(), b.getBlockX()), maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY()), maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ()), maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    private int firstUsableResourceSlot(RecruitedVillager rv) {
        ItemStack[] res = rv.getResources();
        for (int i = 0; i < res.length; i++) {
            ItemStack it = res[i];
            if (it != null && it.getAmount() > 0 && it.getType().isBlock()) return i;
        }
        return -1;
    }

    // ── Guerrier : combat, périmètre, patrouille, suivi ───────────────────────
    private void warriorTick(RecruitedVillager rv, Villager v) {
        if (rv.getWeapon() == null) return; // pas d'arme = pas de combat

        LivingEntity target = rv.isCombatEnabled() ? resolveCurrentTarget(rv, v) : null;
        if (target == null && rv.isCombatEnabled()) {
            target = findThreat(rv, v);
            rv.setCurrentTarget(target != null ? target.getUniqueId() : null);
        }

        if (target != null) {
            Location anchor = engagementAnchor(rv);
            double leash = effectiveRadius(rv) * 1.6;
            if (anchor != null && target.getLocation().distance(anchor) > leash) {
                rv.setCurrentTarget(null); // la cible fuit trop loin du périmètre : abandon
                return;
            }
            double dist = v.getLocation().distance(target.getLocation());
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
                }
            }
            return;
        }

        // Pas de cible : suivre le joueur assigné, sinon patrouiller, sinon tenir le poste
        if (rv.getFollowTarget() != null) {
            Player followed = Bukkit.getPlayer(rv.getFollowTarget());
            if (followed != null && followed.isOnline() && followed.getWorld().equals(v.getWorld())) {
                double d = v.getLocation().distance(followed.getLocation());
                if (d > 3.5) v.getPathfinder().moveTo(followed.getLocation(), 0.6);
                return;
            }
        }

        if (!rv.getPatrolPoints().isEmpty()) {
            List<Location> points = rv.getPatrolPoints();
            int idx = rv.getPatrolIndex() % points.size();
            Location wp = points.get(idx);
            if (v.getLocation().distance(wp) <= 2.0) {
                rv.setPatrolIndex((idx + 1) % points.size());
            } else {
                v.getPathfinder().moveTo(wp, 0.4);
            }
            return;
        }

        if (rv.getPostLocation() != null && v.getLocation().distance(rv.getPostLocation()) > 4.0) {
            v.getPathfinder().moveTo(rv.getPostLocation(), 0.4);
        }
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
        ItemStack weapon = rv.getWeapon();
        if (weapon == null) return 1.0;
        String name = weapon.getType().name();
        double base = 2.0;
        if (name.contains("NETHERITE")) base = 8.0;
        else if (name.contains("DIAMOND")) base = 7.0;
        else if (name.contains("IRON")) base = 5.0;
        else if (name.contains("STONE")) base = 4.0;
        else if (name.contains("GOLD")) base = 4.0;
        else if (name.contains("WOOD")) base = 3.0;
        if (name.contains("AXE") && !name.contains("PICKAXE")) base += 1.0;
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

        String victimFaction = resolveFactionOf(victim);

        // Cas 1 : le villageois lui-même est attaqué → auto-défense
        RecruitedVillager selfRv = villagers.get(victim.getUniqueId());
        if (selfRv != null && selfRv.getRole() == VillagerRole.GUERRIER && selfRv.isCombatEnabled()
                && !isFriendly(selfRv.getFactionName(), attacker)) {
            selfRv.setCurrentTarget(attacker.getUniqueId());
        }

        // Cas 2 : un joueur de faction (ou un autre villageois recruté) proche est attaqué → les guerriers défendent
        if (victimFaction == null || isFriendly(victimFaction, attacker)) return;
        for (RecruitedVillager rv : getFactionVillagers(victimFaction)) {
            if (rv.getRole() != VillagerRole.GUERRIER || !rv.isCombatEnabled()) continue;
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
        RecruitedVillager rv = villagers.remove(event.getEntity().getUniqueId());
        if (rv != null) {
            save();
            notifyFaction(rv.getFactionName(), "§c☠ Le villageois §e" + rv.getDisplayName()
                    + " §c(" + rv.getRole().displayName() + ") a été tué !");
        }
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

            ItemStack[] res = rv.getResources();
            for (int i = 0; i < res.length; i++) if (res[i] != null) cfg.set(key + ".resources." + i, res[i]);

            if (rv.getWeapon() != null)     cfg.set(key + ".equip.weapon", rv.getWeapon());
            if (rv.getHelmet() != null)     cfg.set(key + ".equip.helmet", rv.getHelmet());
            if (rv.getChestplate() != null) cfg.set(key + ".equip.chestplate", rv.getChestplate());
            if (rv.getLeggings() != null)   cfg.set(key + ".equip.leggings", rv.getLeggings());
            if (rv.getBoots() != null)      cfg.set(key + ".equip.boots", rv.getBoots());
            if (rv.getFood() != null)       cfg.set(key + ".food", rv.getFood());

            if (rv.hasZone()) {
                cfg.set(key + ".zoneA", locToString(rv.getZoneA()));
                cfg.set(key + ".zoneB", locToString(rv.getZoneB()));
            }

            if (rv.getPostLocation() != null) cfg.set(key + ".post", locToString(rv.getPostLocation()));
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

            ItemStack[] res = new ItemStack[9];
            for (int i = 0; i < 9; i++) if (cfg.contains(path + ".resources." + i)) res[i] = cfg.getItemStack(path + ".resources." + i);
            rv.setResources(res);

            if (cfg.contains(path + ".equip.weapon"))     rv.setWeapon(cfg.getItemStack(path + ".equip.weapon"));
            if (cfg.contains(path + ".equip.helmet"))     rv.setHelmet(cfg.getItemStack(path + ".equip.helmet"));
            if (cfg.contains(path + ".equip.chestplate")) rv.setChestplate(cfg.getItemStack(path + ".equip.chestplate"));
            if (cfg.contains(path + ".equip.leggings"))   rv.setLeggings(cfg.getItemStack(path + ".equip.leggings"));
            if (cfg.contains(path + ".equip.boots"))      rv.setBoots(cfg.getItemStack(path + ".equip.boots"));
            if (cfg.contains(path + ".food"))             rv.setFood(cfg.getItemStack(path + ".food"));

            if (cfg.contains(path + ".zoneA") && cfg.contains(path + ".zoneB")) {
                Location a = stringToLoc(cfg.getString(path + ".zoneA"));
                Location b = stringToLoc(cfg.getString(path + ".zoneB"));
                if (a != null && b != null) { rv.setZoneA(a); rv.setZoneB(b); }
            }

            if (cfg.contains(path + ".post")) rv.setPostLocation(stringToLoc(cfg.getString(path + ".post")));
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
