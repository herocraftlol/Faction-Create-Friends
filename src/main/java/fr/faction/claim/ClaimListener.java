package fr.faction.claim;

import fr.faction.managers.FactionManager;
import fr.faction.util.MobUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

/**
 * Empêche les interactions non autorisées dans les chunks claimés :
 * casser/placer des blocs, ouvrir des coffres, endommager des entités.
 */
public class ClaimListener implements Listener {

    private final ClaimManager claimManager;
    private final FactionManager factionManager;

    public ClaimListener(ClaimManager claimManager, FactionManager factionManager) {
        this.claimManager = claimManager;
        this.factionManager = factionManager;
    }

    // ── Casser un bloc ────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!guard(event.getPlayer(), event.getBlock().getChunk())) event.setCancelled(true);
    }

    // ── Placer un bloc ────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!guard(event.getPlayer(), event.getBlock().getChunk())) event.setCancelled(true);
    }

    // ── Interagir (coffre, levier, bouton, porte…) ────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!guard(event.getPlayer(), event.getClickedBlock().getChunk())) event.setCancelled(true);
    }

    // ── Explosion de bloc (TNT, creeper) ─────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Retire de la liste les blocs dans des chunks claimés
        event.blockList().removeIf(block ->
                claimManager.isClaimed(block.getChunk()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                claimManager.isClaimed(block.getChunk()));
    }

    // ── Attaque d'entité dans un claim ────────────────────────────────────────
    // Seuls les mobs non-hostiles (amicaux/passifs : villageois, loups, chats,
    // vaches, chevaux, golems de fer, etc.) sont protégés contre les joueurs
    // extérieurs à la faction propriétaire du claim. Les mobs hostiles (monstres)
    // restent librement combattables par tout le monde, même en territoire ennemi.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!MobUtils.isFriendlyMob(target)) return; // joueur ou mob hostile : non concerné ici

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        Chunk chunk = target.getLocation().getChunk();
        if (!guard(attacker, chunk)) event.setCancelled(true);
    }

    /**
     * Résout le joueur responsable des dégâts, qu'il s'agisse d'un coup direct
     * ou d'un projectile (flèche, boule de feu, trident…) tiré par un joueur.
     */
    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * @return true si le joueur peut agir dans ce chunk, false sinon (message envoyé)
     */
    private boolean guard(Player player, Chunk chunk) {
        if (!claimManager.isClaimed(chunk)) return true;

        UUID uuid = player.getUniqueId();
        // Op / admin bypass
        if (player.hasPermission("faction.admin")) return true;

        var fac = factionManager.getPlayerFaction(uuid);
        String playerFaction = fac == null ? "" : fac.getName();

        if (claimManager.canInteract(uuid, playerFaction, chunk)) return true;

        ClaimManager.ClaimData data = claimManager.getClaim(chunk);
        player.sendMessage(ChatColor.RED + "✘ Ce chunk est claimé par la faction §e"
                + data.getFactionName() + ChatColor.RED + ". Accès refusé.");
        return false;
    }
}
