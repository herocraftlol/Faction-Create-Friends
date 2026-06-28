package fr.faction.managers;

import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FactionTeleportManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;

    // UUID joueur → temps de fin du cooldown (ms)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // UUID joueur en attente de confirmation de TP
    private final Map<UUID, UUID> pendingTeleports = new HashMap<>(); // joueur → cible

    private static final int WARMUP_SECONDS = 3;  // délai avant le TP
    private static final int COOLDOWN_SECONDS = 30; // cooldown entre deux TPs

    public FactionTeleportManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
    }

    // ─── TP vers un membre spécifique ────────────────────────────────────────────

    public void teleportToMember(Player player, String targetName) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction.");
            return;
        }

        // Chercher le membre cible
        Player target = null;
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && m.getName().equalsIgnoreCase(targetName)) {
                target = m;
                break;
            }
        }

        if (target == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Ce membre n'est pas en ligne ou n'est pas dans ta faction.");
            return;
        }

        teleportWithWarmup(player, target);
    }

    // ─── TP vers le membre le plus proche ────────────────────────────────────────

    public void teleportToNearest(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction.");
            return;
        }

        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m == null || !m.isOnline()) continue;
            if (!m.getWorld().equals(player.getWorld())) continue;
            double d = player.getLocation().distance(m.getLocation());
            if (d < nearestDist) {
                nearestDist = d;
                nearest = m;
            }
        }

        if (nearest == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Aucun membre en ligne dans ton monde.");
            return;
        }
        teleportWithWarmup(player, nearest);
    }

    // ─── Logique de TP avec warmup et cooldown ───────────────────────────────────

    private void teleportWithWarmup(Player player, Player target) {
        UUID uuid = player.getUniqueId();

        // Vérifier cooldown
        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage(prefix() + ChatColor.RED + "Cooldown : encore " + remaining + "s avant le prochain TP.");
                return;
            }
        }

        player.sendMessage(prefix() + ChatColor.YELLOW + "Téléportation vers "
                + ChatColor.WHITE + target.getName()
                + ChatColor.YELLOW + " dans " + WARMUP_SECONDS + "s… Ne bouge pas !");

        org.bukkit.Location startLoc = player.getLocation().clone();

        new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier que les deux joueurs sont encore en ligne
                if (!player.isOnline() || !target.isOnline()) {
                    player.sendMessage(prefix() + ChatColor.RED + "Téléportation annulée : joueur déconnecté.");
                    return;
                }
                // Vérifier que le joueur n'a pas bougé
                if (player.getLocation().distanceSquared(startLoc) > 1.0) {
                    player.sendMessage(prefix() + ChatColor.RED + "Téléportation annulée : tu as bougé !");
                    return;
                }
                player.teleport(target.getLocation());
                player.sendMessage(prefix() + ChatColor.GREEN + "Téléporté vers " + target.getName() + " !");
                target.sendMessage(prefix() + ChatColor.AQUA + player.getName() + " s'est téléporté vers toi.");

                // Appliquer cooldown
                cooldowns.put(uuid, System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000L));
            }
        }.runTaskLater(plugin, WARMUP_SECONDS * 20L);
    }

    // ─── Liste des membres en ligne ───────────────────────────────────────────────

    public List<Player> getOnlineMembersExcept(Player self) {
        Faction faction = factionManager.getPlayerFaction(self.getUniqueId());
        if (faction == null) return Collections.emptyList();
        List<Player> result = new ArrayList<>();
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(self.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && m.isOnline()) result.add(m);
        }
        return result;
    }

    private String prefix() {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Faction" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;
    }
}
