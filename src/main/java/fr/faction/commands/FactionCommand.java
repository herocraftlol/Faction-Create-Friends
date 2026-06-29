package fr.faction.commands;

import fr.faction.claim.ClaimManager;
import fr.faction.claim.ClaimPermissionGUI;
import fr.faction.economy.BankGUI;
import fr.faction.gui.FactionGUI;
import fr.faction.gui.FactionRankingGUI;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.managers.StatsMessageUtil;
import fr.faction.models.Faction;
import fr.faction.models.PlayerStats;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.PlayerPowerCalculator;
import fr.faction.ranking.FactionRank;
import fr.faction.trade.TradeGUI;
import fr.faction.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande unique : /faction <sous-commande>
 *
 * Gestion de faction :
 *   create, disband, invite, join, leave, kick, setchef, info, list, tp, coffre, menu
 *
 * Classement & puissance :
 *   /faction top / classement / rangs / power / stats / classementjoueurs
 *
 * Économie & territoire :
 *   /faction claim          → Claimer le chunk actuel (coût en émeraudes)
 *   /faction unclaim        → Retirer le claim du chunk actuel
 *   /faction claims         → Voir les claims de ta faction
 *   /faction perms          → Gérer les permissions du chunk claimé (GUI)
 *   /faction banque         → Ouvrir la banque d'émeraudes (GUI)
 *   /faction troc <joueur>  → Proposer un troc à un joueur
 *   /faction accepter       → Accepter une invitation de troc
 */
public class FactionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> STATS_CATEGORIES = Arrays.asList(
            "mobs", "pvp", "advancements", "morts", "blocs", "temps", "dommages", "kd"
    );

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;
    private final SharedInventoryManager sharedInvManager;
    private final FactionTeleportManager teleportManager;
    private final FactionGUI factionGUI;
    private final FactionRankingGUI rankingGUI;
    private final FactionPowerManager powerManager;
    private final ClaimManager claimManager;
    private final ClaimPermissionGUI claimPermissionGUI;
    private final BankGUI bankGUI;
    private final TradeManager tradeManager;
    private final TradeGUI tradeGUI;

    public FactionCommand(JavaPlugin plugin, FactionManager factionManager, PlayerStatsManager statsManager,
                          SharedInventoryManager sharedInvManager, FactionTeleportManager teleportManager,
                          FactionGUI factionGUI, FactionRankingGUI rankingGUI, FactionPowerManager powerManager,
                          ClaimManager claimManager, ClaimPermissionGUI claimPermissionGUI,
                          BankGUI bankGUI, TradeManager tradeManager, TradeGUI tradeGUI) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.statsManager = statsManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        this.factionGUI = factionGUI;
        this.rankingGUI = rankingGUI;
        this.powerManager = powerManager;
        this.claimManager = claimManager;
        this.claimPermissionGUI = claimPermissionGUI;
        this.bankGUI = bankGUI;
        this.tradeManager = tradeManager;
        this.tradeGUI = tradeGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            factionGUI.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            // ── Gestion de faction ──────────────────────────────────────────────
            case "create"                    -> handleCreate(player, args);
            case "disband"                   -> handleDisband(player);
            case "invite"                    -> handleInvite(player, args);
            case "join"                      -> handleJoin(player, args);
            case "leave"                     -> handleLeave(player);
            case "setchef"                   -> handleSetChef(player, args);
            case "info"                      -> handleInfo(player, args);
            case "list"                      -> handleList(player);
            case "kick"                      -> handleKick(player, args);
            case "coffre", "chest"           -> sharedInvManager.openSharedInventory(player);
            case "tp"                        -> handleTp(player, args);
            case "menu", "gui", "m"          -> factionGUI.openMainMenu(player);

            // ── Classement & puissance ───────────────────────────────────────────
            case "classement", "ranking"     -> rankingGUI.openRankingGUI(player);
            case "rangs", "ranks"            -> rankingGUI.openRankInfoGUI(player);
            case "top"                       -> handleTop(player);
            case "power", "puissance", "pw"  -> handlePower(player, args);
            case "stats"                     -> handleStats(player, args);
            case "classementjoueurs", "cj"   -> handlePlayerLeaderboard(player, args);

            // ── Territoire & économie ────────────────────────────────────────────
            case "claim"                     -> handleClaim(player);
            case "unclaim"                   -> handleUnclaim(player);
            case "claims"                    -> handleClaims(player);
            case "perms", "permissions"      -> handlePerms(player);
            case "banque", "bank"            -> bankGUI.openMainBankMenu(player);
            case "troc", "trade"             -> handleTradeInvite(player, args);
            case "accepter", "accepttrade"   -> handleTradeAccept(player);

            default                          -> sendHelp(player);
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GESTION DE FACTION
    // ════════════════════════════════════════════════════════════════════════════

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction create <nom>"); return; }
        if (factionManager.isInFaction(player.getUniqueId())) { player.sendMessage(prefix() + msg("already-in-faction")); return; }
        String name = args[1];
        int min = plugin.getConfig().getInt("faction.min-name-length", 3);
        int max = plugin.getConfig().getInt("faction.max-name-length", 20);
        if (name.length() < min) { player.sendMessage(prefix() + ChatColor.RED + "Nom trop court (min " + min + " caractères)."); return; }
        if (name.length() > max) { player.sendMessage(prefix() + ChatColor.RED + "Nom trop long (max " + max + " caractères)."); return; }
        if (!name.matches("[a-zA-Z0-9_\\-]+")) { player.sendMessage(prefix() + ChatColor.RED + "Nom invalide (lettres, chiffres, _ et - uniquement)."); return; }
        if (!factionManager.createFaction(name, player.getUniqueId())) { player.sendMessage(prefix() + msg("faction-already-exists")); return; }
        player.sendMessage(prefix() + msg("faction-created").replace("%name%", name));
    }

    private void handleDisband(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        String name = faction.getName();
        for (UUID uuid : new ArrayList<>(faction.getMembers())) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(player)) m.sendMessage(prefix() + ChatColor.RED + "La faction " + name + " a été dissoute.");
        }
        sharedInvManager.deleteFactionInventory(name);
        factionManager.disbandFaction(name);
        player.sendMessage(prefix() + msg("faction-disbanded").replace("%name%", name));
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction invite <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(prefix() + msg("player-not-found")); return; }
        if (target.equals(player)) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas t'inviter toi-même."); return; }
        if (factionManager.isInFaction(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + target.getName() + " est déjà dans une faction."); return; }
        if (faction.getMemberCount() >= plugin.getConfig().getInt("faction.max-members", 50)) { player.sendMessage(prefix() + msg("faction-full")); return; }
        factionManager.addInvite(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + msg("invite-sent").replace("%player%", target.getName()));
        target.sendMessage(prefix() + msg("invite-received").replace("%player%", player.getName()).replace("%faction%", faction.getName()));
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction join <nom>"); return; }
        if (factionManager.isInFaction(player.getUniqueId())) { player.sendMessage(prefix() + msg("already-in-faction")); return; }
        Faction faction = factionManager.getFaction(args[1]);
        if (faction == null) { player.sendMessage(prefix() + msg("faction-not-found")); return; }
        if (!faction.hasInvite(player.getUniqueId())) { player.sendMessage(prefix() + msg("no-invite")); return; }
        if (faction.getMemberCount() >= plugin.getConfig().getInt("faction.max-members", 50)) { player.sendMessage(prefix() + msg("faction-full")); return; }
        factionManager.addMember(args[1], player.getUniqueId());
        player.sendMessage(prefix() + msg("joined-faction").replace("%name%", faction.getName()));
        notifyMembers(faction, player, ChatColor.GREEN + player.getName() + " a rejoint la faction !");
    }

    private void handleLeave(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (faction.isChef(player.getUniqueId()) && faction.getMemberCount() > 1) {
            player.sendMessage(prefix() + ChatColor.RED + "Transfère d'abord le rôle de chef : /faction setchef <joueur>");
            return;
        }
        String name = faction.getName();
        notifyMembers(faction, player, ChatColor.YELLOW + player.getName() + " a quitté la faction.");
        factionManager.removeMember(name, player.getUniqueId());
        player.sendMessage(prefix() + msg("left-faction").replace("%name%", name));
    }

    private void handleSetChef(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction setchef <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(prefix() + msg("player-not-found")); return; }
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + target.getName() + " n'est pas dans ta faction."); return; }
        factionManager.setChef(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + msg("chef-set").replace("%player%", target.getName()));
        target.sendMessage(prefix() + ChatColor.GOLD + "Tu es maintenant chef de la faction " + faction.getName() + " !");
        notifyMembers(faction, player, ChatColor.GOLD + target.getName() + " est le nouveau chef !", target);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction kick <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + args[1] + " n'est pas dans ta faction."); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas te kick toi-même."); return; }
        String tName = target.getName() != null ? target.getName() : args[1];
        factionManager.removeMember(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + ChatColor.YELLOW + tName + " expulsé de la faction.");
        if (target.isOnline() && target.getPlayer() != null) target.getPlayer().sendMessage(prefix() + ChatColor.RED + "Tu as été expulsé de la faction " + faction.getName() + ".");
        notifyMembers(faction, player, ChatColor.RED + tName + " a été expulsé de la faction.");
    }

    private void handleTp(Player player, String[] args) {
        if (args.length < 2) teleportManager.teleportToNearest(player);
        else teleportManager.teleportToMember(player, args[1]);
    }

    private void handleInfo(Player player, String[] args) {
        Faction faction = args.length >= 2
                ? factionManager.getFaction(args[1])
                : factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + (args.length >= 2 ? msg("faction-not-found") : msg("not-in-faction"))); return; }
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.YELLOW + faction.getName() + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.GRAY + "Chef : " + ChatColor.WHITE + getPlayerName(faction.getChef()));
        FactionRank rank = powerManager.getFactionRank(faction.getName());
        player.sendMessage(ChatColor.GRAY + "Rang  : " + rank.getLabel());
        player.sendMessage(ChatColor.GRAY + "Puissance : " + ChatColor.GOLD + formatPower(powerManager.getFactionPower(faction.getName())) + " ⚡");
        player.sendMessage(ChatColor.GRAY + "Membres (" + faction.getMemberCount() + ") :");
        for (UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            String status = (m != null && m.isOnline()) ? ChatColor.GREEN + "● " : ChatColor.DARK_GRAY + "○ ";
            player.sendMessage("  " + status + ChatColor.WHITE + getPlayerName(uuid) + (uuid.equals(faction.getChef()) ? ChatColor.GOLD + " [Chef]" : ""));
        }
    }

    private void handleList(Player player) {
        Map<String, Faction> all = factionManager.getAllFactions();
        if (all.isEmpty()) { player.sendMessage(prefix() + ChatColor.GRAY + "Aucune faction existante."); return; }
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.YELLOW + "Factions (" + all.size() + ")" + ChatColor.GOLD + " ══════");
        for (Faction f : all.values()) {
            long online = f.getMembers().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline()).count();
            FactionRank rank = powerManager.getFactionRank(f.getName());
            player.sendMessage(rank.couleur + f.getName() + ChatColor.GRAY + " — " + f.getMemberCount() + " membres " + ChatColor.GREEN + "(" + online + " en ligne)");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLASSEMENT & PUISSANCE
    // ════════════════════════════════════════════════════════════════════════════

    /** /faction top — leaderboard texte des factions */
    private void handleTop(Player player) {
        List<Map.Entry<String, Double>> lb = powerManager.getLeaderboard();
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.YELLOW + "⚡ TOP Factions" + ChatColor.GOLD + " ══════");
        if (lb.isEmpty()) { player.sendMessage(ChatColor.GRAY + "  Aucune faction enregistrée."); }
        for (int i = 0; i < Math.min(10, lb.size()); i++) {
            Map.Entry<String, Double> e = lb.get(i);
            FactionRank rank = powerManager.getFactionRank(e.getKey());
            String medal = i == 0 ? ChatColor.GOLD + "①" : i == 1 ? ChatColor.WHITE + "②" : i == 2 ? ChatColor.GOLD + "③" : ChatColor.GRAY + "#" + (i + 1);
            player.sendMessage("  " + medal + " " + rank.getLabel() + ChatColor.WHITE + " " + e.getKey()
                    + ChatColor.GRAY + " — " + ChatColor.GOLD + formatPower(e.getValue()) + " ⚡");
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage("");
    }

    /** /faction power [joueur] — puissance individuelle */
    private void handlePower(Player player, String[] args) {
        Player target = player;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage(prefix() + msg("player-not-found")); return; }
        }

        double pi = powerManager.getPlayerPower(target.getUniqueId());
        PlayerPowerCalculator.PowerBreakdown bd = powerManager.getPlayerBreakdown(target.getUniqueId());
        Faction faction = factionManager.getPlayerFaction(target.getUniqueId());

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.YELLOW + "⚡ Puissance de " + target.getName() + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.GRAY + "  Puissance individuelle : " + ChatColor.GOLD + ChatColor.BOLD + formatPower(pi) + " ⚡");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Breakdown :");
        player.sendMessage(ChatColor.RED   + "    ⚔ PvP         : " + ChatColor.WHITE + formatPower(bd.pvp()));
        player.sendMessage(ChatColor.YELLOW + "    ⛏ Survie      : " + ChatColor.WHITE + formatPower(bd.survie()));
        player.sendMessage(ChatColor.GREEN  + "    ★ Progression : " + ChatColor.WHITE + formatPower(bd.progression()));
        player.sendMessage(ChatColor.AQUA   + "    ⏱ Activité    : " + ChatColor.WHITE + formatPower(bd.activite()));

        if (faction != null) {
            FactionRank rank = powerManager.getFactionRank(faction.getName());
            double fp = powerManager.getFactionPower(faction.getName());
            int pos = powerManager.getFactionPosition(faction.getName());
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "  Faction : " + ChatColor.WHITE + ChatColor.BOLD + faction.getName());
            player.sendMessage(ChatColor.GRAY + "  Rang    : " + rank.getLabel());
            player.sendMessage(ChatColor.GRAY + "  PG      : " + ChatColor.GOLD + formatPower(fp) + " ⚡  " + ChatColor.GRAY + "Classement : " + ChatColor.WHITE + "#" + pos);
            player.sendMessage(ChatColor.GRAY + "  " + rank.progressBar(fp));
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════════");
        player.sendMessage("");
    }

    /** /faction stats [joueur] — statistiques personnelles détaillées (façon ex-plugin FactionStats) */
    private void handleStats(Player player, String[] args) {
        PlayerStats s;

        if (args.length >= 2) {
            String nomCible = args[1];
            s = statsManager.resolveStats(nomCible);
            if (s == null) {
                player.sendMessage(prefix() + ChatColor.RED + "Joueur introuvable ou jamais connecté : " + ChatColor.YELLOW + nomCible);
                return;
            }
        } else {
            s = statsManager.getOrCreateStats(player.getUniqueId(), player.getName());
        }

        String displayName = s.getPlayerName() != null ? s.getPlayerName() : player.getName();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        int rangMobs = statsManager.getRank(s.getUuid(), "mobs");
        int rangPvP  = statsManager.getRank(s.getUuid(), "pvp");
        int rangAdv  = statsManager.getRank(s.getUuid(), "advancements");

        player.sendMessage("");
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §6§l⚔ §r§eStatistiques de §6§l" + displayName + "§r§8  ✦ Faction Survie"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));

        player.sendMessage(StatsMessageUtil.colorize("  §8» §7Première connexion : §f" + sdf.format(new Date(s.getFirstJoin()))));
        player.sendMessage(StatsMessageUtil.colorize("  §8» §7Dernière connexion  : §f" + sdf.format(new Date(s.getLastJoin()))));
        player.sendMessage(StatsMessageUtil.colorize("  §8» §7Temps de jeu total  : §b" + s.getFormattedPlaytime()));
        player.sendMessage("");

        player.sendMessage(StatsMessageUtil.colorize(
                "  §c§l⚔ §r§cCOMBAT" + (rangPvP > 0 ? " §8(classement PvP: §6#" + rangPvP + "§8)" : "")));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separatorShort()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Mobs hostiles tués    : §c" + StatsMessageUtil.formatNumber(s.getMobsKilled())
                        + (rangMobs > 0 ? "  §8(#" + rangMobs + ")" : "")));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Joueurs/entités tués  : §c" + StatsMessageUtil.formatNumber(s.getKills())
                        + (rangPvP > 0 ? "  §8(#" + rangPvP + ")" : "")));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Morts                 : §c" + StatsMessageUtil.formatNumber(s.getDeaths())));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Ratio K/D             : §e" + s.getKDR()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Dégâts infligés       : §c" + String.format("%.0f", s.getDamageDealt()) + " ❤"));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Dégâts reçus          : §c" + String.format("%.0f", s.getDamageTaken()) + " ❤"));
        player.sendMessage("");

        player.sendMessage(StatsMessageUtil.colorize(
                "  §a§l★ §r§aAVANCEMENTS" + (rangAdv > 0 ? " §8(classement: §6#" + rangAdv + "§8)" : "")));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separatorShort()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Progrès accomplis     : §a" + StatsMessageUtil.formatNumber(s.getAdvancements())
                        + (rangAdv > 0 ? "  §8(#" + rangAdv + ")" : "")));
        player.sendMessage("");

        player.sendMessage(StatsMessageUtil.colorize("  §e§l⛏ §r§eSURVIE & CONSTRUCTION"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separatorShort()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Blocs cassés          : §e" + StatsMessageUtil.formatNumber(s.getBlocksBroken())));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Blocs placés          : §e" + StatsMessageUtil.formatNumber(s.getBlocksPlaced())));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Distance parcourue    : §e" + String.format("%.0f", s.getDistanceTravelled()) + " blocs"));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Items craftés         : §e" + StatsMessageUtil.formatNumber(s.getItemsCrafted())));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8› §7Items ramassés        : §e" + StatsMessageUtil.formatNumber(s.getItemsPickedUp())));

        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8Utilisez §7/faction classementjoueurs §8pour voir les classements."));
        player.sendMessage("");
    }

    /** /faction classementjoueurs <categorie> — Top 10 joueurs par statistique (ex-plugin FactionStats) */
    private void handlePlayerLeaderboard(Player player, String[] args) {
        if (args.length < 2) {
            afficherMenuClassementJoueurs(player);
            return;
        }

        String categorie = args[1].toLowerCase();

        switch (categorie) {
            case "mobs" -> afficherClassementJoueurs(player,
                    "Mobs Hostiles Tués", "⚔", "§c",
                    statsManager.getTopMobsKilled(10),
                    s -> StatsMessageUtil.formatNumber(s.getMobsKilled()) + " mobs");

            case "pvp", "joueurs" -> afficherClassementJoueurs(player,
                    "Joueurs/Entités Tués (PvP)", "☠", "§c",
                    statsManager.getTopKills(10),
                    s -> StatsMessageUtil.formatNumber(s.getKills()) + " kills");

            case "advancements", "progres" -> afficherClassementJoueurs(player,
                    "Progrès Accomplis", "★", "§a",
                    statsManager.getTopAdvancements(10),
                    s -> StatsMessageUtil.formatNumber(s.getAdvancements()) + " progrès");

            case "morts" -> afficherClassementJoueurs(player,
                    "Nombre de Morts", "💀", "§7",
                    statsManager.getTopDeaths(10),
                    s -> StatsMessageUtil.formatNumber(s.getDeaths()) + " morts");

            case "blocs" -> afficherClassementJoueurs(player,
                    "Blocs Cassés", "⛏", "§e",
                    statsManager.getTopBlocksBroken(10),
                    s -> StatsMessageUtil.formatNumber(s.getBlocksBroken()) + " blocs");

            case "temps" -> afficherClassementJoueurs(player,
                    "Temps de Jeu", "⏱", "§b",
                    statsManager.getTopPlaytime(10),
                    PlayerStats::getFormattedPlaytime);

            case "dommages" -> afficherClassementJoueurs(player,
                    "Dégâts Infligés", "❤", "§c",
                    statsManager.getTopDamageDealt(10),
                    s -> String.format("%.0f", s.getDamageDealt()) + " ❤");

            case "kd" -> afficherClassementJoueurs(player,
                    "Ratio K/D", "⚖", "§e",
                    statsManager.getTopKDR(10),
                    s -> "K/D: §e" + s.getKDR());

            default -> {
                player.sendMessage(prefix() + ChatColor.RED + "Catégorie inconnue : " + ChatColor.YELLOW + categorie);
                afficherMenuClassementJoueurs(player);
            }
        }
    }

    private void afficherMenuClassementJoueurs(Player player) {
        player.sendMessage("");
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize("  §6§l⚔ §r§6CLASSEMENT JOUEURS §8— §7Faction Survie"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize("  §7Choisissez une catégorie :"));
        player.sendMessage("");
        player.sendMessage(StatsMessageUtil.colorize("  §c» §f/faction classementjoueurs mobs        §8─ §7Mobs hostiles tués"));
        player.sendMessage(StatsMessageUtil.colorize("  §c» §f/faction classementjoueurs pvp         §8─ §7Joueurs tués (PvP)"));
        player.sendMessage(StatsMessageUtil.colorize("  §c» §f/faction classementjoueurs kd          §8─ §7Ratio Kills/Deaths"));
        player.sendMessage(StatsMessageUtil.colorize("  §a» §f/faction classementjoueurs advancements§8─ §7Progrès accomplis"));
        player.sendMessage(StatsMessageUtil.colorize("  §e» §f/faction classementjoueurs blocs       §8─ §7Blocs cassés"));
        player.sendMessage(StatsMessageUtil.colorize("  §7» §f/faction classementjoueurs morts       §8─ §7Nombre de morts"));
        player.sendMessage(StatsMessageUtil.colorize("  §b» §f/faction classementjoueurs temps       §8─ §7Temps de jeu"));
        player.sendMessage(StatsMessageUtil.colorize("  §c» §f/faction classementjoueurs dommages    §8─ §7Dégâts infligés"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage("");
    }

    @FunctionalInterface
    private interface StatExtractor {
        String extract(PlayerStats stats);
    }

    private void afficherClassementJoueurs(Player player, String titre, String icone, String couleur,
                                            List<PlayerStats> classement, StatExtractor extractor) {
        player.sendMessage("");
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  " + couleur + "§l" + icone + " §r" + couleur + "CLASSEMENT — " + titre.toUpperCase()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8Top " + Math.min(10, classement.size()) + " joueurs depuis le début du serveur"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));

        if (classement.isEmpty()) {
            player.sendMessage(StatsMessageUtil.colorize("  §7Aucune donnée disponible pour le moment."));
        } else {
            for (int i = 0; i < classement.size(); i++) {
                PlayerStats stats = classement.get(i);
                int rang = i + 1;
                String nom = stats.getPlayerName() != null ? stats.getPlayerName() : getPlayerName(stats.getUuid());
                String ligne = "  " + StatsMessageUtil.getMedaille(rang) + "§f" + nom +
                        " §8— " + couleur + extractor.extract(stats);
                player.sendMessage(StatsMessageUtil.colorize(ligne));
            }
        }

        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8Utilisez §7/faction stats <joueur> §8pour voir les détails."));
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLAIM — TERRITOIRE
    // ════════════════════════════════════════════════════════════════════════════

    private void handleClaim(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu dois être dans une faction pour claimer."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut claimer un chunk."); return; }

        Chunk chunk = player.getLocation().getChunk();
        if (claimManager.isClaimed(chunk)) {
            ClaimManager.ClaimData existing = claimManager.getClaim(chunk);
            player.sendMessage(prefix() + ChatColor.RED + "Ce chunk est déjà claimé par §e" + existing.getFactionName() + ChatColor.RED + ".");
            return;
        }

        int cost = claimManager.getNextClaimCost(faction.getName());
        long balance = ((fr.faction.FactionPlugin) plugin).getBankManager().getFactionBalance(faction.getName());

        if (balance < cost) {
            player.sendMessage(prefix() + ChatColor.RED + "Fonds insuffisants ! Ce claim coûte §e" + cost
                    + " 💎§c et le coffre faction contient §e" + balance + " 💎§c.");
            player.sendMessage(prefix() + ChatColor.GRAY + "Utilisez §e/faction banque §7pour déposer des émeraudes dans le coffre faction.");
            return;
        }

        // Déduire le coût
        ((fr.faction.FactionPlugin) plugin).getBankManager().withdrawFaction(faction.getName(), cost);
        claimManager.addClaim(faction.getName(), chunk);

        player.sendMessage(prefix() + ChatColor.GREEN + "✔ Chunk [" + chunk.getX() + ", " + chunk.getZ() + "] claimé pour §e"
                + faction.getName() + ChatColor.GREEN + " ! Coût : §a" + cost + " 💎");
        player.sendMessage(prefix() + ChatColor.GRAY + "Prochain claim : §e"
                + claimManager.getNextClaimCost(faction.getName()) + " 💎");
        notifyMembers(faction, player,
                ChatColor.GREEN + "Le chef a claimé un chunk ! (" + claimManager.getClaimCount(faction.getName()) + " claims total)");
    }

    private void handleUnclaim(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut retirer un claim."); return; }

        Chunk chunk = player.getLocation().getChunk();
        if (!claimManager.isClaimed(chunk)) { player.sendMessage(prefix() + ChatColor.RED + "Ce chunk n'est pas claimé."); return; }

        ClaimManager.ClaimData data = claimManager.getClaim(chunk);
        if (!data.getFactionName().equalsIgnoreCase(faction.getName())) {
            player.sendMessage(prefix() + ChatColor.RED + "Ce chunk appartient à §e" + data.getFactionName() + ChatColor.RED + ".");
            return;
        }

        claimManager.removeClaim(faction.getName(), chunk);
        player.sendMessage(prefix() + ChatColor.YELLOW + "Chunk [" + chunk.getX() + ", " + chunk.getZ() + "] libéré.");
    }

    private void handleClaims(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }

        int count = claimManager.getClaimCount(faction.getName());
        int nextCost = claimManager.getNextClaimCost(faction.getName());
        player.sendMessage(ChatColor.GOLD + "══════ Claims de §e" + faction.getName() + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.GRAY + "Total : §e" + count + " chunk(s) claimé(s)");
        player.sendMessage(ChatColor.GRAY + "Prochain claim : §a" + nextCost + " 💎 émeraudes");

        List<ClaimManager.ChunkKey> keys = claimManager.getFactionClaims(faction.getName());
        if (keys.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Aucun chunk claimé pour le moment.");
        } else {
            player.sendMessage(ChatColor.GRAY + "Liste (monde : x,z) :");
            int shown = 0;
            for (ClaimManager.ChunkKey k : keys) {
                if (shown++ >= 15) { player.sendMessage(ChatColor.DARK_GRAY + "  ... et " + (keys.size() - 15) + " de plus."); break; }
                player.sendMessage(ChatColor.DARK_GRAY + "  • §7" + k.world() + " §8: §f" + k.cx() + "§8, §f" + k.cz());
            }
        }
    }

    private void handlePerms(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut gérer les permissions."); return; }

        Chunk chunk = player.getLocation().getChunk();
        if (!claimManager.isClaimed(chunk)) { player.sendMessage(prefix() + ChatColor.RED + "Ce chunk n'est pas claimé."); return; }

        ClaimManager.ClaimData data = claimManager.getClaim(chunk);
        if (!data.getFactionName().equalsIgnoreCase(faction.getName())) {
            player.sendMessage(prefix() + ChatColor.RED + "Ce chunk appartient à §e" + data.getFactionName() + ChatColor.RED + ".");
            return;
        }

        claimPermissionGUI.openPermGUI(player, chunk);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TROC
    // ════════════════════════════════════════════════════════════════════════════

    private void handleTradeInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction troc <joueur>"); return; }
        if (tradeManager.inTrade(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu es déjà dans un troc actif."); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || target.equals(player)) { player.sendMessage(prefix() + ChatColor.RED + "Joueur introuvable."); return; }
        if (tradeManager.inTrade(target.getUniqueId())) {
            player.sendMessage(prefix() + ChatColor.RED + target.getName() + " est déjà dans un troc.");
            return;
        }

        tradeManager.sendInvite(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(prefix() + ChatColor.YELLOW + "Invitation de troc envoyée à §e" + target.getName() + ChatColor.YELLOW + ".");
        target.sendMessage(prefix() + ChatColor.AQUA + "§e" + player.getName() + ChatColor.AQUA
                + " te propose un troc ! Tape §e/faction accepter §bpour accepter.");
    }

    private void handleTradeAccept(Player player) {
        UUID inviter = tradeManager.getInviterFor(player.getUniqueId());
        if (inviter == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'as pas d'invitation de troc en attente."); return; }

        Player inviterPlayer = Bukkit.getPlayer(inviter);
        if (inviterPlayer == null) {
            tradeManager.removeInvite(inviter);
            player.sendMessage(prefix() + ChatColor.RED + "L'inviteur s'est déconnecté.");
            return;
        }
        if (tradeManager.inTrade(inviter) || tradeManager.inTrade(player.getUniqueId())) {
            player.sendMessage(prefix() + ChatColor.RED + "L'un de vous est déjà dans un troc.");
            return;
        }

        tradeManager.removeInvite(inviter);
        tradeManager.createSession(inviter, player.getUniqueId());

        player.sendMessage(prefix() + ChatColor.GREEN + "✔ Troc accepté avec §e" + inviterPlayer.getName() + ChatColor.GREEN + " !");
        inviterPlayer.sendMessage(prefix() + ChatColor.GREEN + "✔ §e" + player.getName() + ChatColor.GREEN + " a accepté le troc !");

        tradeGUI.openTradeGUI(inviterPlayer);
        tradeGUI.openTradeGUI(player);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // AIDE
    // ════════════════════════════════════════════════════════════════════════════

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.YELLOW + "Aide /faction" + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.GRAY + "— Gestion —");
        player.sendMessage(ChatColor.YELLOW + "/faction create <nom>        " + ChatColor.GRAY + "Créer une faction");
        player.sendMessage(ChatColor.YELLOW + "/faction disband             " + ChatColor.GRAY + "Dissoudre la faction");
        player.sendMessage(ChatColor.YELLOW + "/faction invite <joueur>     " + ChatColor.GRAY + "Inviter un joueur");
        player.sendMessage(ChatColor.YELLOW + "/faction join <nom>          " + ChatColor.GRAY + "Rejoindre une faction");
        player.sendMessage(ChatColor.YELLOW + "/faction leave               " + ChatColor.GRAY + "Quitter sa faction");
        player.sendMessage(ChatColor.YELLOW + "/faction kick <joueur>       " + ChatColor.GRAY + "Expulser un membre");
        player.sendMessage(ChatColor.YELLOW + "/faction setchef <joueur>    " + ChatColor.GRAY + "Transférer le chef");
        player.sendMessage(ChatColor.YELLOW + "/faction info [nom]          " + ChatColor.GRAY + "Info faction");
        player.sendMessage(ChatColor.YELLOW + "/faction list                " + ChatColor.GRAY + "Liste des factions");
        player.sendMessage(ChatColor.YELLOW + "/faction tp [joueur]         " + ChatColor.GRAY + "Téléportation");
        player.sendMessage(ChatColor.YELLOW + "/faction coffre              " + ChatColor.GRAY + "Coffre partagé");
        player.sendMessage(ChatColor.GRAY + "— Classement & Stats —");
        player.sendMessage(ChatColor.YELLOW + "/faction top                 " + ChatColor.GRAY + "Top 10 factions (texte)");
        player.sendMessage(ChatColor.YELLOW + "/faction classement          " + ChatColor.GRAY + "Classement factions GUI");
        player.sendMessage(ChatColor.YELLOW + "/faction rangs               " + ChatColor.GRAY + "Guide des rangs GUI");
        player.sendMessage(ChatColor.YELLOW + "/faction power [joueur]      " + ChatColor.GRAY + "Puissance individuelle");
        player.sendMessage(ChatColor.YELLOW + "/faction stats [joueur]      " + ChatColor.GRAY + "Statistiques personnelles");
        player.sendMessage(ChatColor.YELLOW + "/faction classementjoueurs   " + ChatColor.GRAY + "Top 10 joueurs par stat");
        player.sendMessage(ChatColor.GRAY + "— Territoire (Claims) —");
        player.sendMessage(ChatColor.GREEN  + "/faction claim               " + ChatColor.GRAY + "Claimer le chunk sous tes pieds (cout croissant en emeral.)");
        player.sendMessage(ChatColor.GREEN  + "/faction unclaim             " + ChatColor.GRAY + "Retirer le claim du chunk actuel");
        player.sendMessage(ChatColor.GREEN  + "/faction claims              " + ChatColor.GRAY + "Voir les claims de ta faction");
        player.sendMessage(ChatColor.GREEN  + "/faction perms               " + ChatColor.GRAY + "Gerer les acces a ce chunk claim (GUI)");
        player.sendMessage(ChatColor.GRAY + "— Economie —");
        player.sendMessage(ChatColor.AQUA   + "/faction banque              " + ChatColor.GRAY + "Banque emeraudes personnelle / faction (GUI)");
        player.sendMessage(ChatColor.GRAY + "— Troc —");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction troc <joueur>       " + ChatColor.GRAY + "Proposer un troc a un joueur (GUI)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction accepter            " + ChatColor.GRAY + "Accepter une invitation de troc");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════════

    private void notifyMembers(Faction faction, Player exclude, String message, Player... extra) {
        Set<Player> excl = new HashSet<>(Arrays.asList(extra));
        excl.add(exclude);
        for (UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !excl.contains(m)) m.sendMessage(prefix() + message);
        }
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    private String formatPower(double val) {
        if (val >= 1000) return String.format("%.1fk", val / 1000);
        return String.format("%.1f", val);
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    private String msg(String key) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages." + key, "&cMessage manquant: " + key));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TAB COMPLETE
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create","disband","invite","join","leave","kick","setchef",
                    "info","list","tp","coffre","menu",
                    "top","classement","rangs","power","stats","classementjoueurs",
                    "claim","unclaim","claims","perms","banque","troc","accepter"
            );
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "info", "join" -> factionManager.getAllFactions().values().stream()
                        .map(f -> f.getName())
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "invite", "kick", "setchef", "tp", "power", "stats", "troc" ->
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                case "classementjoueurs", "cj" -> STATS_CATEGORIES.stream()
                        .filter(c -> c.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }
}
