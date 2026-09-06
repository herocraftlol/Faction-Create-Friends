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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
     * Paper 1.21 : on utilise l'event moderne AsyncChatEvent (Adventure).
     * L'ancien AsyncPlayerChatEvent + setFormat() est toujours présent pour
     * compatibilité, mais Paper ignore souvent silencieusement setFormat()
     * depuis la gestion du chat signé/sécurisé — d'où le rang qui ne
     * s'affichait plus. On construit le rendu via un ChatRenderer à la place.
     * Le préfixe dans le tab-list reste géré via FactionTabManager (Scoreboard Teams).
     *
     * Format :
     *   [icone_rang][Faction] NomJoueur: message
     *   ex: §e§l[★] §e[TitanS] §fSteve§r: bonjour
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Intercepter la saisie recherche shop
        if (shopGUI.isAwaitingSearch(player.getUniqueId())) {
            event.setCancelled(true);
            final String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
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

        String prefixLegacy;
        String nameColorLegacy;
        if (faction == null) {
            // Sans faction : nom gris
            prefixLegacy = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "∅" + ChatColor.DARK_GRAY + "] ";
            nameColorLegacy = ChatColor.GRAY.toString();
        } else {
            // Construire le préfixe rang + faction
            String rankPrefix;
            if (rank == FactionRank.LEGENDAIRE) {
                rankPrefix = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[⚜] " + ChatColor.RESET;
            } else {
                rankPrefix = rank.getChatPrefix();
            }
            String factionTag = rank.couleur + "[" + faction.getName() + "]";

            prefixLegacy = warTag + rankPrefix + factionTag + " ";
            nameColorLegacy = rank.couleur.toString();
        }

        Component prefix = LEGACY.deserialize(prefixLegacy);
        Component styledName = LEGACY.deserialize(nameColorLegacy + player.getName());
        Component separator = LEGACY.deserialize(ChatColor.DARK_GRAY + ": " + ChatColor.WHITE);

        Component fullPrefix = prefix.append(styledName).append(separator);

        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                fullPrefix.append(message)));
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
