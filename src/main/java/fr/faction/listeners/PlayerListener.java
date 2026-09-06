package fr.faction.listeners;

import fr.faction.alliance.HomeManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.FactionTabManager;
import fr.faction.ranking.FactionRank;
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

    private final FactionManager      factionManager;
    private final PlayerStatsManager  statsManager;
    private final FactionPowerManager powerManager;
    private final ShopManager         shopManager;
    private final ShopGUI             shopGUI;
    private WarManager     warManager;
    private HomeManager    homeManager;
    private FactionTabManager tabManager;

    public PlayerListener(FactionManager factionManager, PlayerStatsManager statsManager,
                          FactionPowerManager powerManager,
                          ShopManager shopManager, ShopGUI shopGUI) {
        this.factionManager = factionManager;
        this.statsManager   = statsManager;
        this.powerManager   = powerManager;
        this.shopManager    = shopManager;
        this.shopGUI        = shopGUI;
    }

    public void setWarManager(WarManager wm)      { this.warManager  = wm; }
    public void setHomeManager(HomeManager hm)    { this.homeManager = hm; }
    public void setTabManager(FactionTabManager t){ this.tabManager   = t; }

    // ── Chat ─────────────────────────────────────────────────────────────────────
    /**
     * Paper 1.21 utilise AsyncPlayerChatEvent (encore supporté en mode legacy).
     * Le vrai texte affiché dans le chat est contrôlé par setFormat().
     * Le préfixe dans le tab-list est géré via FactionTabManager (Scoreboard Teams).
     *
     * Format :
     *   [icone_rang][Faction] NomJoueur: message
     *   ex: §e§l[★] §e[TitanS] §fSteve§r: bonjour
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Intercepter la saisie recherche shop
        if (shopGUI.isAwaitingSearch(player.getUniqueId())) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                    () -> shopGUI.handleSearchInput(player, msg));
            return;
        }

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        FactionRank rank = faction != null
                ? powerManager.getFactionRank(faction.getName())
                : null;

        // Icône de guerre si en cours
        String warTag = "";
        if (warManager != null && faction != null && warManager.isAtWar(faction.getName())) {
            warTag = ChatColor.RED + "⚔ ";
        }

        String format;
        if (faction == null) {
            // Sans faction : nom gris
            format = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "∅" + ChatColor.DARK_GRAY + "] "
                    + ChatColor.GRAY + "%s" + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + "%s";
        } else {
            // Construire le préfixe rang + faction
            String rankPrefix;
            if (rank == FactionRank.LEGENDAIRE) {
                rankPrefix = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[⚜] " + ChatColor.RESET;
            } else {
                rankPrefix = rank.getChatPrefix();
            }
            String factionTag = rank.couleur + "[" + faction.getName() + "]";

            format = warTag + rankPrefix + factionTag + " "
                    + rank.couleur + "%s"
                    + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + "%s";
        }

        event.setFormat(format);
    }

    // ── Join ─────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        statsManager.getOrCreateStats(player.getUniqueId(), player.getName())
                    .setLastJoin(System.currentTimeMillis());

        // Appliquer le scoreboard de faction (tab + préfixe)
        if (tabManager != null) {
            // Légèrement différé pour que le joueur soit bien enregistré
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                    () -> { if (player.isOnline()) tabManager.refresh(player); },
                    5L);
        }

        // Paiements shop en attente
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                () -> shopManager.deliverPendingPayments(player), 60L);

        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        // Notifier la faction
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) m.sendMessage(ChatColor.GREEN + "[Faction] "
                    + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + " est en ligne.");
        }

        // Rappel de guerre
        if (warManager != null) {
            WarSession war = warManager.getActiveWarOf(faction.getName());
            if (war != null) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("FactionPlugin"), () -> {
                    if (!player.isOnline()) return;
                    String opp    = war.getOpponent(faction.getName());
                    int myKills   = war.getKillsFor(faction.getName().toLowerCase());
                    int oppKills  = war.getKillsFor(opp.toLowerCase());
                    player.sendMessage("§8[§c⚔ Guerre§8] §c⚔ Guerre contre §f" + opp
                            + " — §f" + myKills + "§c/§f" + oppKills
                            + "§c (objectif §f" + war.getKillsToWin() + "§c kills)");
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

        if (tabManager != null) tabManager.remove(player);

        Faction faction = factionManager.getPlayerFaction(uuid);
        if (faction == null) return;
        for (UUID memberUuid : faction.getMembers()) {
            if (memberUuid.equals(uuid)) continue;
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null) m.sendMessage(ChatColor.GRAY + "[Faction] "
                    + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " s'est déconnecté.");
        }
    }

    // ── Respawn → spawn de faction ────────────────────────────────────────────────

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return;

        // Choisir le spawn à utiliser
        boolean has1 = faction.hasSpawn();
        boolean has2 = faction.hasSpawn2();
        if (!has1 && !has2) return;

        // Si les deux spawns sont définis, choisir aléatoirement
        Location spawn;
        int slot;
        if (has1 && has2) {
            slot = (Math.random() < 0.5) ? 1 : 2;
            spawn = faction.getSpawnBySlot(slot);
        } else if (has2) {
            slot = 2; spawn = faction.getFactionSpawn2();
        } else {
            slot = 1; spawn = faction.getFactionSpawn();
        }

        if (spawn.getWorld() == null) return;
        event.setRespawnLocation(spawn);

        final int finalSlot = slot;
        final boolean twoSpawns = has1 && has2;
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"), () -> {
            if (!player.isOnline()) return;
            String msg = ChatColor.GREEN + "[Faction] Réapparition au spawn de "
                    + ChatColor.YELLOW + faction.getName()
                    + ChatColor.GRAY + (twoSpawns ? " §7(spawn §f#" + finalSlot + "§7)" : "");
            player.sendMessage(msg);
        }, 5L);
    }
}
