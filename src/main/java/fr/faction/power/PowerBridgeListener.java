package fr.faction.power;

import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.models.PlayerStats;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class PowerBridgeListener implements Listener {

    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;
    private final PlayerStatsManager statsManager;

    public PowerBridgeListener(FactionManager factionManager, FactionPowerManager powerManager, PlayerStatsManager statsManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.statsManager = statsManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        // Compter le kill
        statsManager.getStats(killer.getUniqueId()).addKill();
        // Compter la mort
        if (event.getEntity() instanceof Player dead) {
            statsManager.getStats(dead.getUniqueId()).addDeath();
        } else if (isHostileMob(event.getEntity())) {
            statsManager.getStats(killer.getUniqueId()).addMobKill();
        }
        Faction faction = factionManager.getPlayerFaction(killer.getUniqueId());
        if (faction != null) powerManager.invalidate(faction.getName());
    }

    /** Vérifie si une entité tuée est un mob hostile (ex-plugin FactionStats) */
    private boolean isHostileMob(LivingEntity entity) {
        return entity instanceof Monster
                || entity instanceof Ghast
                || entity instanceof Slime
                || entity instanceof Phantom
                || entity instanceof Shulker
                || entity instanceof ElderGuardian
                || entity instanceof Warden
                || entity instanceof EnderDragon
                || entity instanceof WitherSkeleton
                || entity instanceof Wither
                || entity instanceof PiglinBrute
                || entity instanceof Hoglin
                || entity instanceof Zoglin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            statsManager.getStats(attacker.getUniqueId()).addDamage(event.getFinalDamage());
        }
        if (event.getEntity() instanceof Player victim) {
            statsManager.getStats(victim.getUniqueId()).addDamageTaken(event.getFinalDamage());
        }
    }

    /** Dégâts reçus hors combat direct (chute, feu, noyade, etc.) — ex-plugin FactionStats */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageGeneric(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return; // déjà géré ci-dessus
        if (event.getEntity() instanceof Player victim) {
            statsManager.getStats(victim.getUniqueId()).addDamageTaken(event.getFinalDamage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        statsManager.getStats(event.getPlayer().getUniqueId()).addBlockBroken();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        statsManager.getStats(event.getPlayer().getUniqueId()).addBlockPlaced();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int amount = event.isShiftClick()
                ? event.getInventory().getResult() != null ? event.getInventory().getResult().getAmount() : 1
                : 1;
        statsManager.getStats(player.getUniqueId()).addCraft(amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        statsManager.getStats(event.getPlayer().getUniqueId()).addItemPickedUp();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dy = event.getTo().getY() - event.getFrom().getY();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.01) statsManager.getStats(event.getPlayer().getUniqueId()).addDistance(dist);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().getKey();
        if (key.startsWith("recipes/")) return;
        PlayerStats stats = statsManager.getStats(event.getPlayer().getUniqueId());
        stats.addAdvancement();
        Faction faction = factionManager.getPlayerFaction(event.getPlayer().getUniqueId());
        if (faction != null) powerManager.invalidate(faction.getName());
    }
}
