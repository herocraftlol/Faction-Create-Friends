package fr.faction.managers;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Suit le temps de jeu de chaque joueur via une tâche périodique (toutes les secondes),
 * plutôt qu'un calcul unique à la déconnexion. Plus précis et robuste aux crashs serveur.
 * Porté du plugin FactionStats.
 */
public class PlaytimeTracker {

    private final JavaPlugin plugin;
    private final PlayerStatsManager statsManager;
    private BukkitTask task;

    public PlaytimeTracker(JavaPlugin plugin, PlayerStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                statsManager.getStats(player.getUniqueId()).addTicks(20L);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
