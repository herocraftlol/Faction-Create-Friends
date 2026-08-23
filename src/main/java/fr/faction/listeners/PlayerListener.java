package fr.faction.listeners;

import fr.faction.alliance.HomeManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import fr.faction.shop.ExchangeGUI;
import fr.faction.shop.ShopGUI;
import fr.faction.shop.ShopManager;
import fr.faction.war.WarManager;
import fr.faction.war.WarSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;
    private final FactionPowerManager powerManager;
    private final ShopManager shopManager;
    private final ShopGUI shopGUI;
    private final ExchangeGUI exchangeGUI;
    private WarManager warManager;
    private HomeManager homeManager;

    public PlayerListener(FactionManager factionManager, PlayerStatsManager statsManager,
                          FactionPowerManager powerManager,
                          ShopManager shopManager, ShopGUI shopGUI, ExchangeGUI exchangeGUI) {
        this.factionManager = factionManager;
        this.statsManager   = statsManager;
        this.powerManager   = powerManager;
        this.shopManager    = shopManager;
        this.shopGUI        = shopGUI;
        this.exchangeGUI    = exchangeGUI;
    }

    public void setWarManager(WarManager wm)    { this.warManager = wm; }
    public void setHomeManager(HomeManager hm)  { this.homeManager = hm; }

    // ── Chat ─────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Intercepter la saisie de recherche shop
        if (shopGUI.isAwaitingSearch(player.getUniqueId())) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                    () -> shopGUI.handleSearchInput(player, msg));
            return;
        }

        // Intercepter la saisie de création d'ordre d'échange
        if (exchangeGUI.isAwaitingDeposit(player.getUniqueId())) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                    () -> exchangeGUI.handleDepositInput(player, msg));
            return;
        }

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            event.setFormat(ChatColor.GRAY + "[Sans faction] "
                    + ChatColor.WHITE + "%s" + ChatColor.RESET + ": %s");
            return;
        }

        FactionRank rank = powerManager.getFactionRank(faction.getName());

        // Indicateur de guerre dans le tag si en guerre
        String warTag = "";
        if (warManager != null && warManager.isAtWar(faction.getName())) {
            warTag = ChatColor.RED + "⚔ ";
        }

        String factionTag;
        if (rank.ordinal() >= FactionRank.OR.ordinal()) {
            factionTag = warTag + rank.couleur + "" + ChatColor.BOLD
                    + "[" + faction.getName() + "]" + ChatColor.RESET;
        } else {
            factionTag = warTag + ChatColor.GRAY + "[" + faction.getName() + "]" + ChatColor.RESET;
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

        // Paiements shop en attente (délai pour que le joueur soit bien chargé)
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                () -> shopManager.deliverPendingPayments(player), 60L);

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        // Notifier les membres de faction
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + "[Faction] " + ChatColor.YELLOW + player.getName()
                        + ChatColor.GREEN + " est en ligne.");
            }
        }

        // Rappel si guerre active
        if (warManager != null) {
            WarSession session = warManager.getActiveWarOf(faction.getName());
            if (session != null) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("FactionPlugin"), () -> {
                    if (!player.isOnline()) return;
                    String opp = session.getOpponent(faction.getName());
                    int myKills  = session.getKillsFor(faction.getName().toLowerCase());
                    int oppKills = session.getKillsFor(opp.toLowerCase());
                    player.sendMessage("§8[§c⚔ Guerre§8] §c⚔ Guerre en cours contre §f" + opp
                            + " §c— Score : §f" + myKills + "§c/§f" + oppKills
                            + "§c (objectif §f" + session.getKillsToWin() + "§c kills)");
                }, 80L);
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

    // ── Respawn → spawn de faction si défini ─────────────────────────────────────

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;
        if (!faction.hasSpawn()) return;

        Location spawn = faction.getFactionSpawn();
        // Vérifier que le monde existe encore
        if (spawn.getWorld() == null) return;

        event.setRespawnLocation(spawn);
        // Message différé car le joueur n'est pas encore téléporté
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"), () -> {
            if (player.isOnline())
                player.sendMessage(ChatColor.GREEN + "[Faction] Réapparition au spawn de "
                        + ChatColor.YELLOW + faction.getName() + ChatColor.GREEN + ".");
        }, 5L);
    }
}
