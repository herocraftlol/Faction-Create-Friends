package fr.faction.listeners;

import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import fr.faction.shop.ShopGUI;
import fr.faction.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;
    private final FactionPowerManager powerManager;
    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public PlayerListener(FactionManager factionManager, PlayerStatsManager statsManager,
                          FactionPowerManager powerManager,
                          ShopManager shopManager, ShopGUI shopGUI) {
        this.factionManager = factionManager;
        this.statsManager   = statsManager;
        this.powerManager   = powerManager;
        this.shopManager    = shopManager;
        this.shopGUI        = shopGUI;
    }

    // ── Chat ─────────────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Intercepter la saisie de recherche shop (sync nécessaire)
        if (shopGUI.isAwaitingSearch(player.getUniqueId())) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                    () -> shopGUI.handleSearchInput(player, msg));
            return;
        }

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            event.setFormat(ChatColor.GRAY + "[Sans faction] "
                    + ChatColor.WHITE + "%s" + ChatColor.RESET + ": %s");
            return;
        }

        FactionRank rank = powerManager.getFactionRank(faction.getName());
        String factionTag;
        if (rank.ordinal() >= FactionRank.OR.ordinal()) {
            factionTag = rank.couleur + "" + ChatColor.BOLD
                    + "[" + faction.getName() + "]" + ChatColor.RESET;
        } else {
            factionTag = ChatColor.GRAY + "[" + faction.getName() + "]" + ChatColor.RESET;
        }
        String prefix = "";
        if (rank == FactionRank.LEGENDAIRE) {
            prefix = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[LEGENDAIRE] " + ChatColor.RESET;
        }
        event.setFormat(prefix + factionTag + " "
                + ChatColor.WHITE + "%s" + ChatColor.RESET + ": %s");
    }

    // ── Join ─────────────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var stats = statsManager.getOrCreateStats(player.getUniqueId(), player.getName());
        stats.setLastJoin(System.currentTimeMillis());

        // Paiements shop en attente
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                () -> shopManager.deliverPendingPayments(player), 40L);

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

    // ── Quit ─────────────────────────────────────────────────────────────────────
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
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
