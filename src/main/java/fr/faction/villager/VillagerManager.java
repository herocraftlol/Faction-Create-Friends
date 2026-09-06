package fr.faction.villager;

import fr.faction.claim.ClaimManager;
import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.util.MobUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

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
 * (Constructeur / Guerrier), IA simplifiée (construction de zone, combat),
 * soin par la nourriture, et persistance dans villagers.yml.
 */
public class VillagerManager implements Listener {

    private static final long ATTACK_COOLDOWN_MS = 900L;

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private ClaimManager claimManager;

    private final Map<UUID, RecruitedVillager> villagers = new HashMap<>();
    private final Map<UUID, PendingZone> pendingZones = new HashMap<>();
    private final File dataFile;

    public VillagerManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.dataFile = new File(plugin.getDataFolder(), "villagers.yml");
        load();
    }

    public void setClaimManager(ClaimManager claimManager) { this.claimManager = claimManager; }

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
        pendingZones.values().removeIf(pz -> pz.villagerId.equals(rv.getEntityId()));
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
    // SÉLECTION DE ZONE DE CHANTIER (constructeur)
    // ════════════════════════════════════════════════════════════════════════

    private static class PendingZone {
        final UUID villagerId;
        Location corner1;
        PendingZone(UUID villagerId) { this.villagerId = villagerId; }
    }

    public void startZoneSelection(Player player, RecruitedVillager rv) {
        pendingZones.put(player.getUniqueId(), new PendingZone(rv.getEntityId()));
        player.sendMessage(prefix() + "§eClique-droit sur le §b1er coin§e du chantier (bloc au sol par ex.).");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractForZone(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PendingZone pz = pendingZones.get(player.getUniqueId());
        if (pz == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        event.setCancelled(true);

        RecruitedVillager rv = villagers.get(pz.villagerId);
        if (rv == null) { pendingZones.remove(player.getUniqueId()); return; }

        Location loc = event.getClickedBlock().getLocation();

        if (pz.corner1 == null) {
            pz.corner1 = loc;
            player.sendMessage(prefix() + "§aCoin A défini. §eClique-droit sur le §b2e coin§e.");
            return;
        }

        if (!Objects.equals(pz.corner1.getWorld(), loc.getWorld())) {
            player.sendMessage(prefix() + "§cLes deux coins doivent être dans le même monde. Sélection annulée.");
            pendingZones.remove(player.getUniqueId());
            return;
        }

        int volume = computeVolume(pz.corner1, loc);
        int maxVolume = plugin.getConfig().getInt("villager.max-zone-volume", 3375);
        if (volume > maxVolume) {
            player.sendMessage(prefix() + "§cZone trop grande (" + volume + " blocs, max " + maxVolume + "). Réessaie avec une zone plus petite.");
            pendingZones.remove(player.getUniqueId());
            return;
        }

        if (claimManager != null) {
            boolean okA = claimBelongsToFaction(pz.corner1, rv.getFactionName());
            boolean okB = claimBelongsToFaction(loc, rv.getFactionName());
            if (!okA || !okB) {
                player.sendMessage(prefix() + "§cLes deux coins doivent être dans un chunk claimé par ta faction.");
                pendingZones.remove(player.getUniqueId());
                return;
            }
        }

        rv.setZoneA(pz.corner1);
        rv.setZoneB(loc);
        pendingZones.remove(player.getUniqueId());
        save();
        player.sendMessage(prefix() + "§a✔ Chantier défini pour §e" + rv.getDisplayName() + " §a(" + volume + " blocs). "
                + "Donne-lui des blocs et il comblera les vides de la zone (construction ET réparation).");
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

    // ── Guerrier : combat les mobs hostiles à proximité (et donc défend les villageois voisins) ──
    private void warriorTick(RecruitedVillager rv, Villager v) {
        if (rv.getWeapon() == null) return; // pas d'arme = pas de combat

        double radius = plugin.getConfig().getDouble("villager.combat-radius", 14.0);

        LivingEntity target = resolveCurrentTarget(rv, v, radius);
        if (target == null) {
            target = findNearestHostile(v, radius);
            rv.setCurrentTarget(target != null ? target.getUniqueId() : null);
        }
        if (target == null) return;

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
    }

    private LivingEntity resolveCurrentTarget(RecruitedVillager rv, Villager v, double radius) {
        UUID id = rv.getCurrentTarget();
        if (id == null) return null;
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof LivingEntity le) || le.isDead() || !MobUtils.isHostileMob(le)) return null;
        if (le.getWorld() != v.getWorld() || le.getLocation().distance(v.getLocation()) > radius * 1.5) return null;
        return le;
    }

    private LivingEntity findNearestHostile(Villager v, double radius) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : v.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead()) continue;
            if (!MobUtils.isHostileMob(le)) continue;
            double d = le.getLocation().distanceSquared(v.getLocation());
            if (d < best) { best = d; nearest = le; }
        }
        return nearest;
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
