package fr.faction.listeners;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final FactionManager factionManager;

    public PlayerListener(FactionManager factionManager) {
        this.factionManager = factionManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        // Notifier les membres en ligne
        for (java.util.UUID uuid : faction.getMembers()) {
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
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        for (java.util.UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GRAY + "[Faction] " + ChatColor.YELLOW + player.getName()
                        + ChatColor.GRAY + " s'est déconnecté.");
            }
        }
    }
}
