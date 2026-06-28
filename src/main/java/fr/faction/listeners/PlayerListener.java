package fr.faction.listeners;

import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;

    public PlayerListener(FactionManager factionManager, PlayerStatsManager statsManager) {
        this.factionManager = factionManager;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Crée/actualise le profil de stats (nom + dernière connexion)
        var stats = statsManager.getOrCreateStats(player.getUniqueId(), player.getName());
        stats.setLastJoin(System.currentTimeMillis());

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + "[Faction] " + ChatColor.YELLOW + player.getName()
                        + ChatColor.GREEN + " est en ligne.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Le temps de jeu est désormais suivi par une tâche périodique (cf. FactionPlugin#onEnable)
        statsManager.getStats(uuid).setLastJoin(System.currentTimeMillis());

        Faction faction = factionManager.getPlayerFaction(uuid);
        if (faction == null) return;
        for (UUID memberUuid : faction.getMembers()) {
            if (memberUuid.equals(uuid)) continue;
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GRAY + "[Faction] " + ChatColor.YELLOW + player.getName()
                        + ChatColor.GRAY + " s'est déconnecté.");
            }
        }
    }
}
