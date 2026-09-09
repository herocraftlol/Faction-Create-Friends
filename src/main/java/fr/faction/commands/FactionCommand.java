package fr.faction.commands;

import fr.faction.alliance.AllianceManager;
import fr.faction.alliance.HomeManager;
import fr.faction.alliance.PlayerTeleportManager;
import fr.faction.alliance.PrivateChestManager;
import fr.faction.claim.ClaimManager;
import fr.faction.claim.ClaimPermissionGUI;
import fr.faction.economy.BankGUI;
import fr.faction.economy.EmeraldBankManager;
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
import fr.faction.shop.InvSeeGUI;
import fr.faction.shop.ShopGUI;
import fr.faction.shop.ShopListing;
import fr.faction.shop.ShopManager;
import fr.faction.trade.TradeGUI;
import fr.faction.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
 *   /faction claim               → Claimer le chunk actuel (coût en émeraudes, distance ≥6 chunks des claims adverses)
 *   /faction unclaim             → Retirer le claim du chunk actuel
 *   /faction claims              → Voir les claims de ta faction
 *   /faction claimallow <faction>→ Autoriser une faction à claimer à moins de 6 chunks de nos claims
 *   /faction claimdeny <faction> → Révoquer cette autorisation
 *   /faction claimallies         → Lister les factions autorisées à claimer près de nous
 *   /faction perms               → Gérer les permissions du chunk claimé (GUI)
 *   /faction banque         → Ouvrir la banque d'émeraudes (GUI)
 *   /faction troc <joueur>  → Proposer un troc à un joueur
 *   /faction accepter       → Accepter une invitation de troc
 */
public class FactionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> STATS_CATEGORIES = Arrays.asList(
            "mobs", "pvp", "advancements", "morts", "blocs", "temps", "dommages", "kd", "richesse"
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
    private fr.faction.claim.ClaimVisualizer claimVisualizer;
    private fr.faction.villager.VillagerManager villagerManager;
    private fr.faction.villager.VillagerGUI villagerGUI;
    private fr.faction.village.VillageManager villageManager;
    private fr.faction.commerce.CommerceManager commerceManager;
    private final BankGUI bankGUI;
    private final EmeraldBankManager bankManager;
    private final TradeManager tradeManager;
    private final TradeGUI tradeGUI;
    private final ShopManager shopManager;
    private final ShopGUI shopGUI;
    private final InvSeeGUI invSeeGUI;
    private final AllianceManager allianceManager;
    private final HomeManager homeManager;
    private final PrivateChestManager privateChestManager;
    private final PlayerTeleportManager playerTeleportManager;
    private fr.faction.war.WarManager warManager;
    private fr.faction.gui.MainMenuGUI mainMenuGUI;
    private fr.faction.power.FactionPowerManager powerManagerRef;
    private fr.faction.sort.SortMenuGUI sortMenuGUI;
    private fr.faction.power.FactionTabManager tabManager;
    private fr.faction.map.FactionMapManager mapManager;
    private fr.faction.shop.ShopCreateGUI shopCreateGUI;

    public FactionCommand(JavaPlugin plugin, FactionManager factionManager, PlayerStatsManager statsManager,
                          SharedInventoryManager sharedInvManager, FactionTeleportManager teleportManager,
                          FactionGUI factionGUI, FactionRankingGUI rankingGUI, FactionPowerManager powerManager,
                          ClaimManager claimManager, ClaimPermissionGUI claimPermissionGUI,
                          BankGUI bankGUI, EmeraldBankManager bankManager,
                          TradeManager tradeManager, TradeGUI tradeGUI,
                          ShopManager shopManager, ShopGUI shopGUI, InvSeeGUI invSeeGUI,
                          AllianceManager allianceManager, HomeManager homeManager,
                          PrivateChestManager privateChestManager, PlayerTeleportManager playerTeleportManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.statsManager = statsManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        this.factionGUI = factionGUI;
        this.rankingGUI = rankingGUI;
        this.powerManager = powerManager;
        this.powerManagerRef = powerManager;
        this.claimManager = claimManager;
        this.claimPermissionGUI = claimPermissionGUI;
        this.bankGUI = bankGUI;
        this.bankManager = bankManager;
        this.tradeManager = tradeManager;
        this.tradeGUI    = tradeGUI;
        this.shopManager = shopManager;
        this.shopGUI     = shopGUI;
        this.invSeeGUI   = invSeeGUI;
        this.allianceManager       = allianceManager;
        this.homeManager           = homeManager;
        this.privateChestManager   = privateChestManager;
        this.playerTeleportManager = playerTeleportManager;
        this.warManager = null; // injecté après via setWarManager
    }

    public void setWarManager(fr.faction.war.WarManager wm) { this.warManager = wm; }
    public void setMainMenuGUI(fr.faction.gui.MainMenuGUI gui) { this.mainMenuGUI = gui; }
    public void setSortMenuGUI(fr.faction.sort.SortMenuGUI gui) { this.sortMenuGUI = gui; }
    public void setShopCreateGUI(fr.faction.shop.ShopCreateGUI gui) { this.shopCreateGUI = gui; }
    public void setClaimVisualizer(fr.faction.claim.ClaimVisualizer cv) { this.claimVisualizer = cv; }
    public void setVillagerManager(fr.faction.villager.VillagerManager vm) { this.villagerManager = vm; }
    public void setVillagerGUI(fr.faction.villager.VillagerGUI vg) { this.villagerGUI = vg; }
    public void setVillageManager(fr.faction.village.VillageManager vm) { this.villageManager = vm; }
    public void setCommerceManager(fr.faction.commerce.CommerceManager cm) { this.commerceManager = cm; }
    public void setTabManager(fr.faction.power.FactionTabManager tm) { this.tabManager = tm; }
    public void setMapManager(fr.faction.map.FactionMapManager mm) { this.mapManager = mm; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            if (mainMenuGUI != null) mainMenuGUI.openHome(player);
            else factionGUI.openMainMenu(player);
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
            case "souschef", "subchief"       -> handleSousChef(player, args);
            case "rename", "renommer"        -> handleRename(player, args);
            case "info"                      -> handleInfo(player, args);
            case "list"                      -> handleList(player);
            case "kick"                      -> handleKick(player, args);
            case "coffre", "chest"           -> sharedInvManager.openSharedInventory(player);
            case "tp"                        -> handleTp(player, args);
            case "menu", "gui", "m"          -> { if (mainMenuGUI != null) mainMenuGUI.openHome(player); else factionGUI.openMainMenu(player); }

            // ── Classement & puissance ───────────────────────────────────────────
            case "classement", "ranking"     -> rankingGUI.openRankingGUI(player);
            case "rangs", "ranks"            -> rankingGUI.openRankInfoGUI(player);
            case "top"                       -> handleTop(player);
            case "topbanque", "topfactions",
                 "richessefactions"          -> handleTopFactionWealth(player);
            case "power", "puissance", "pw"  -> handlePower(player, args);
            case "stats"                     -> handleStats(player, args);
            case "classementjoueurs", "cj"   -> handlePlayerLeaderboard(player, args);

            // ── Territoire & économie ────────────────────────────────────────────
            case "claim"                     -> handleClaim(player);
            case "unclaim"                   -> handleUnclaim(player);
            case "claims"                    -> handleClaims(player);
            case "claimmap"                  -> handleClaimMap(player);
            case "map", "minimap"            -> handleFacMap(player, args);
            case "claimshow", "showclaim",
                 "claims-visual", "cv"       -> handleClaimShow(player);
            case "claimallow"                -> handleClaimAllow(player, args);
            case "claimdeny"                 -> handleClaimDeny(player, args);
            case "claimallies"               -> handleClaimAllies(player);
            case "perms", "permissions"      -> handlePerms(player);
            case "banque", "bank"            -> bankGUI.openMainBankMenu(player);
            case "troc", "trade"             -> handleTradeInvite(player, args);
            case "accepter", "accepttrade"   -> handleTradeAccept(player);

            // ── Shop Global v4 ────────────────────────────────────────────────
            case "shop", "marche", "ventes"  -> shopGUI.openShop(player);
            case "vendre", "sell"            -> handleShopSell(player, args);
            case "acheter", "buy"            -> handleShopBuy(player, args);
            case "recuperer", "recover", "reprendre" -> handleShopRecover(player, args);
            case "mesannonces"               -> shopGUI.openMyListings(player);

            // ── Admin ─────────────────────────────────────────────────────────
            case "invsee"                    -> handleInvSee(player, args);

            // ── Alliances v5 ─────────────────────────────────────────────────
            case "alliance"                  -> handleAlliance(player, args);

            // ── Spawn de faction v5 ──────────────────────────────────────────
            case "setspawn"                  -> handleSetFactionSpawn(player, args);
            case "spawn"                     -> handleFactionSpawn(player, args);

            // ── Homes v5 ─────────────────────────────────────────────────────
            case "sethome"                   -> handleSetHomeCmd(player, args);
            case "home"                      -> handleHomeCmd(player, args);
            case "delhome"                   -> handleDelHomeCmd(player, args);
            case "homes"                     -> handleHomesCmd(player);

            // ── TPA v5 ───────────────────────────────────────────────────────
            case "tpa"                       -> { if (args.length >= 2) playerTeleportManager.sendRequest(player, args[1]); else player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction tpa <joueur>"); }
            case "tpaccept", "tpac"          -> playerTeleportManager.acceptRequest(player);
            case "tpdeny", "tpd"             -> playerTeleportManager.denyRequest(player);

            // ── GUI principal v5.1 ───────────────────────────────────────────

            // ── Guerre v5.1 ──────────────────────────────────────────────────
            case "guerre", "war"             -> handleGuerre(player, args);

            // ── Tri de coffre v5.2 ───────────────────────────────────────────
            case "ranger", "trier", "organiser", "sort" -> handleRanger(player, args);

            // ── Villageois recrutés v5.9 ─────────────────────────────────────
            case "recruter"                  -> handleRecruter(player);
            case "villageois", "villagers"    -> handleVillageois(player, args);
            case "village"                    -> handleVillage(player, args);
            case "port"                       -> handlePost(player, args, fr.faction.village.PostType.PORT);
            case "gare"                       -> handlePost(player, args, fr.faction.village.PostType.GARE);
            case "contrat"                    -> handleContrat(player, args);

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
        if (tabManager != null) tabManager.refresh(player);
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
        if (!faction.canManage(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut inviter des joueurs."); return; }
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
        if (tabManager != null) tabManager.refresh(player);
        notifyMembers(faction, player, ChatColor.GREEN + player.getName() + " a rejoint la faction !");
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Usage : /faction rename <nouveau_nom>");
            return;
        }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut renommer la faction."); return; }

        String newName = args[1];

        // Validation du nom
        if (newName.length() < 3 || newName.length() > 20) {
            player.sendMessage(prefix() + ChatColor.RED + "Le nom doit faire entre 3 et 20 caractères.");
            return;
        }
        if (!newName.matches("[a-zA-Z0-9_\\-]+")) {
            player.sendMessage(prefix() + ChatColor.RED + "Le nom ne peut contenir que des lettres, chiffres, _ et -.");
            return;
        }
        if (factionManager.getFaction(newName) != null) {
            player.sendMessage(prefix() + ChatColor.RED + "Ce nom est déjà pris par une autre faction.");
            return;
        }

        String oldName = faction.getName();
        boolean success = factionManager.renameFaction(oldName, newName);
        if (!success) {
            player.sendMessage(prefix() + ChatColor.RED + "Renommage impossible (nom déjà pris ou faction introuvable).");
            return;
        }

        // Notifier tous les membres en ligne
        for (UUID uuid : faction.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(prefix() + ChatColor.YELLOW + "La faction §e" + oldName
                        + ChatColor.YELLOW + " a été renommée en §e" + newName + ChatColor.YELLOW + ".");
            }
        }
        player.sendMessage(prefix() + ChatColor.GREEN + "✔ Faction renommée : §e" + oldName
                + ChatColor.GREEN + " → §e" + newName + ChatColor.GREEN + ".");
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

    // ════════════════════════════════════════════════════════════════════════════
    // SOUS-CHEFS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * /faction souschef promouvoir <joueur>
     * /faction souschef retirer <joueur>
     * /faction souschef liste
     * /faction souschef limite <0-2>
     */
    private void handleSousChef(Player player, String[] args) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "══ Sous-chefs ══");
            player.sendMessage(ChatColor.YELLOW + "/faction souschef promouvoir <joueur> " + ChatColor.GRAY + "Nommer un sous-chef (chef uniquement)");
            player.sendMessage(ChatColor.YELLOW + "/faction souschef retirer <joueur>    " + ChatColor.GRAY + "Retirer un sous-chef (chef uniquement)");
            player.sendMessage(ChatColor.YELLOW + "/faction souschef liste               " + ChatColor.GRAY + "Voir les sous-chefs");
            player.sendMessage(ChatColor.YELLOW + "/faction souschef limite <0-2>        " + ChatColor.GRAY + "Régler le nombre max de sous-chefs (chef uniquement, max " + Faction.ABSOLUTE_MAX_SOUS_CHEFS + ")");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "promouvoir", "promote", "add" -> {
                if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut nommer un sous-chef."); return; }
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction souschef promouvoir <joueur>"); return; }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { player.sendMessage(prefix() + msg("player-not-found")); return; }
                Faction.SousChefResult result = factionManager.addSousChef(faction.getName(), target.getUniqueId());
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage(prefix() + ChatColor.GREEN + "✔ " + target.getName() + " est désormais sous-chef de la faction.");
                        target.sendMessage(prefix() + ChatColor.GOLD + "Tu es maintenant sous-chef de la faction " + faction.getName() + " !");
                        notifyMembers(faction, player, ChatColor.GOLD + target.getName() + " est désormais sous-chef !", target);
                    }
                    case NOT_MEMBER        -> player.sendMessage(prefix() + ChatColor.RED + target.getName() + " n'est pas dans ta faction.");
                    case IS_CHEF           -> player.sendMessage(prefix() + ChatColor.RED + "Le chef ne peut pas être son propre sous-chef.");
                    case ALREADY_SOUS_CHEF -> player.sendMessage(prefix() + ChatColor.RED + target.getName() + " est déjà sous-chef.");
                    case LIMIT_REACHED     -> player.sendMessage(prefix() + ChatColor.RED + "Limite de sous-chefs atteinte (" + faction.getMaxSousChefs() + "). Utilise §e/faction souschef limite§c pour l'augmenter (max " + Faction.ABSOLUTE_MAX_SOUS_CHEFS + ").");
                    default -> {}
                }
            }
            case "retirer", "demote", "remove" -> {
                if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut retirer un sous-chef."); return; }
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction souschef retirer <joueur>"); return; }
                @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                Faction.SousChefResult result = factionManager.removeSousChef(faction.getName(), target.getUniqueId());
                String tName = target.getName() != null ? target.getName() : args[2];
                if (result == Faction.SousChefResult.SUCCESS) {
                    player.sendMessage(prefix() + ChatColor.YELLOW + tName + " n'est plus sous-chef.");
                    if (target.isOnline() && target.getPlayer() != null)
                        target.getPlayer().sendMessage(prefix() + ChatColor.YELLOW + "Tu n'es plus sous-chef de la faction " + faction.getName() + ".");
                    notifyMembers(faction, player, ChatColor.YELLOW + tName + " n'est plus sous-chef.");
                } else {
                    player.sendMessage(prefix() + ChatColor.RED + tName + " n'est pas sous-chef.");
                }
            }
            case "liste", "list" -> {
                player.sendMessage(ChatColor.GOLD + "══ Sous-chefs de §e" + faction.getName() + ChatColor.GOLD + " ══");
                player.sendMessage(ChatColor.GRAY + "Limite : §f" + faction.getSousChefCount() + "§7/§f" + faction.getMaxSousChefs()
                        + ChatColor.GRAY + " (max possible : " + Faction.ABSOLUTE_MAX_SOUS_CHEFS + ")");
                if (faction.getSousChefs().isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "  Aucun sous-chef pour le moment.");
                } else {
                    for (UUID uuid : faction.getSousChefs()) {
                        Player m = Bukkit.getPlayer(uuid);
                        String status = (m != null && m.isOnline()) ? ChatColor.GREEN + "● " : ChatColor.DARK_GRAY + "○ ";
                        player.sendMessage("  " + status + ChatColor.WHITE + getPlayerName(uuid));
                    }
                }
            }
            case "limite", "limit" -> {
                if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut régler la limite de sous-chefs."); return; }
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction souschef limite <0-" + Faction.ABSOLUTE_MAX_SOUS_CHEFS + ">"); return; }
                int max;
                try { max = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    player.sendMessage(prefix() + ChatColor.RED + "Nombre invalide."); return;
                }
                if (max < 0 || max > Faction.ABSOLUTE_MAX_SOUS_CHEFS) {
                    player.sendMessage(prefix() + ChatColor.RED + "La limite doit être comprise entre 0 et " + Faction.ABSOLUTE_MAX_SOUS_CHEFS + ".");
                    return;
                }
                factionManager.setMaxSousChefs(faction.getName(), max);
                player.sendMessage(prefix() + ChatColor.GREEN + "✔ Limite de sous-chefs réglée à §e" + max + ChatColor.GREEN + ".");
            }
            default -> player.sendMessage(prefix() + ChatColor.RED + "Sous-commande inconnue. Utilise /faction souschef pour l'aide.");
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction kick <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.canManage(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut expulser un membre."); return; }
        @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + args[1] + " n'est pas dans ta faction."); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas te kick toi-même."); return; }
        if (faction.isChef(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas expulser le chef de la faction."); return; }
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
            String tag = uuid.equals(faction.getChef()) ? ChatColor.GOLD + " [Chef]"
                    : faction.isSousChef(uuid) ? ChatColor.AQUA + " [Sous-chef]" : "";
            player.sendMessage("  " + status + ChatColor.WHITE + getPlayerName(uuid) + tag);
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

    /** /faction topbanque — top des factions les plus riches (coffre de faction) */
    private void handleTopFactionWealth(Player player) {
        List<Map.Entry<String, Long>> lb = bankManager.getTopFactions(10);
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.GREEN + "\uD83D\uDC8E TOP Factions — Richesse" + ChatColor.GOLD + " ══════");
        if (lb.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Aucune faction n'a d'émeraudes en coffre pour le moment.");
        }
        for (int i = 0; i < lb.size(); i++) {
            Map.Entry<String, Long> e = lb.get(i);
            Faction faction = factionManager.getFaction(e.getKey());
            String displayName = faction != null ? faction.getName() : e.getKey();
            String medal = i == 0 ? ChatColor.GOLD + "①" : i == 1 ? ChatColor.WHITE + "②" : i == 2 ? ChatColor.GOLD + "③" : ChatColor.GRAY + "#" + (i + 1);
            player.sendMessage("  " + medal + " " + ChatColor.WHITE + displayName
                    + ChatColor.GRAY + " — " + ChatColor.GREEN + StatsMessageUtil.formatNumber(e.getValue()) + " ⬦ émeraudes");
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════════");
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

            case "richesse", "argent", "emeraudes" -> afficherClassementRichesse(player);

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
        player.sendMessage(StatsMessageUtil.colorize("  §a» §f/faction classementjoueurs richesse    §8─ §7Émeraudes en coffre personnel"));
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

    /** /faction classementjoueurs richesse — top des joueurs les plus riches (coffre personnel) */
    private void afficherClassementRichesse(Player player) {
        List<Map.Entry<UUID, Long>> classement = bankManager.getTopPlayers(10);

        player.sendMessage("");
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §a§l⬦ §r§aCLASSEMENT — RICHESSE (COFFRE PERSONNEL)"));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8Top " + Math.min(10, classement.size()) + " joueurs les plus riches"));
        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));

        if (classement.isEmpty()) {
            player.sendMessage(StatsMessageUtil.colorize("  §7Aucune donnée disponible pour le moment."));
        } else {
            for (int i = 0; i < classement.size(); i++) {
                Map.Entry<UUID, Long> entry = classement.get(i);
                int rang = i + 1;
                String nom = getPlayerName(entry.getKey());
                String ligne = "  " + StatsMessageUtil.getMedaille(rang) + "§f" + nom +
                        " §8— §a" + StatsMessageUtil.formatNumber(entry.getValue()) + " ⬦ émeraudes";
                player.sendMessage(StatsMessageUtil.colorize(ligne));
            }
        }

        player.sendMessage(StatsMessageUtil.colorize(StatsMessageUtil.separator()));
        player.sendMessage(StatsMessageUtil.colorize(
                "  §8Utilisez §7/faction banque §8pour voir ton propre solde."));
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLAIM — TERRITOIRE
    // ════════════════════════════════════════════════════════════════════════════

    private void handleClaim(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu dois être dans une faction pour claimer."); return; }
        if (!faction.canManage(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut claimer un chunk."); return; }

        Chunk chunk = player.getLocation().getChunk();
        if (claimManager.isClaimed(chunk)) {
            ClaimManager.ClaimData existing = claimManager.getClaim(chunk);
            player.sendMessage(prefix() + ChatColor.RED + "Ce chunk est déjà claimé par §e" + existing.getFactionName() + ChatColor.RED + ".");
            return;
        }

        // ── Vérification distance minimale inter-factions ─────────────────────
        String blocker = claimManager.checkProximityViolation(faction.getName(), chunk);
        if (blocker != null) {
            player.sendMessage(prefix() + ChatColor.RED + "Claim impossible ! Ce chunk est à moins de "
                    + ClaimManager.MIN_CLAIM_DISTANCE + " chunks des claims de §e" + blocker + ChatColor.RED + ".");
            player.sendMessage(prefix() + ChatColor.GRAY + "Demandez à §e" + blocker
                    + "§7 d'utiliser §e/faction claimallow " + faction.getName()
                    + "§7 pour vous autoriser à claimer près d'eux.");
            return;
        }

        int cost = claimManager.getNextClaimCost(faction.getName());
        long balance = bankManager.getFactionBalance(faction.getName());

        if (balance < cost) {
            player.sendMessage(prefix() + ChatColor.RED + "Fonds insuffisants ! Ce claim coûte §e" + cost
                    + " 💎§c et le coffre faction contient §e" + balance + " 💎§c.");
            player.sendMessage(prefix() + ChatColor.GRAY + "Utilisez §e/faction banque §7pour déposer des émeraudes dans le coffre faction.");
            return;
        }

        // Déduire le coût
        bankManager.withdrawFaction(faction.getName(), cost);
        claimManager.addClaim(faction.getName(), chunk);

        player.sendMessage(prefix() + ChatColor.GREEN + "✔ Chunk [" + chunk.getX() + ", " + chunk.getZ() + "] claimé pour §e"
                + faction.getName() + ChatColor.GREEN + " ! Coût : §a" + cost + " 💎");
        player.sendMessage(prefix() + ChatColor.GRAY + "Prochain claim : §e"
                + claimManager.getNextClaimCost(faction.getName()) + " 💎");
        notifyMembers(faction, player,
                ChatColor.GREEN + "Le chef a claimé un chunk ! (" + claimManager.getClaimCount(faction.getName()) + " claims total)");
    }

    /** /faction claimmap — carte ASCII des chunks claimés autour du joueur */
    // ════════════════════════════════════════════════════════════════════════════
    // CLAIM SHOW — visualisation particules
    // ════════════════════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════════════════════
    // MINI-MAP DE FACTION
    // ════════════════════════════════════════════════════════════════════════════

    private void handleFacMap(Player player, String[] args) {
        if (mapManager == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Système de carte non disponible.");
            return;
        }

        // /fac map partage → toggle le partage de position aux alliés
        if (args.length >= 2 && args[1].equalsIgnoreCase("partage")) {
            boolean now = mapManager.toggleSharing(player);
            if (now) {
                player.sendMessage(prefix() + ChatColor.YELLOW + "✔ Position partagée aux factions alliées.");
                player.sendMessage(ChatColor.GRAY + "  Les alliés peuvent voir un point jaune sur leur carte.");
            } else {
                player.sendMessage(prefix() + ChatColor.GRAY + "✘ Partage de position désactivé.");
            }
            return;
        }

        // /fac map → donner la carte
        org.bukkit.inventory.ItemStack mapItem = mapManager.getOrCreateMap(player);

        // Essayer de l'ajouter à l'inventaire
        Map<Integer, org.bukkit.inventory.ItemStack> leftover =
                player.getInventory().addItem(mapItem);

        if (leftover.isEmpty()) {
            player.sendMessage(prefix() + ChatColor.GREEN + "✦ Mini-Map de Faction reçue !");
            player.sendMessage(ChatColor.GRAY + "  Tiens-la en main pour voir les positions en temps réel.");
            player.sendMessage(ChatColor.GRAY + "  §e/fac map partage §7→ partager ta position aux alliés");
        } else {
            player.sendMessage(prefix() + ChatColor.RED + "Inventaire plein ! Libère une place pour recevoir la carte.");
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.5f);
    }

    private void handleClaimShow(Player player) {
        if (claimVisualizer == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Visualiseur non disponible.");
            return;
        }

        if (claimVisualizer.isActive(player.getUniqueId())) {
            claimVisualizer.cancel(player.getUniqueId());
            player.sendMessage(prefix() + ChatColor.GRAY + "Visualisation des claims désactivée.");
            return;
        }

        claimVisualizer.show(player);

        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage(prefix() + ChatColor.GREEN + "✦ " + ChatColor.BOLD + "Visualisation des claims"
                + ChatColor.RESET + ChatColor.GREEN + " activée " + ChatColor.GRAY + "(10 secondes)");
        player.sendMessage(ChatColor.GREEN + "  ██ " + ChatColor.WHITE + "Claims de ta faction");
        if (myFaction != null && !myFaction.getAllies().isEmpty())
            player.sendMessage(ChatColor.YELLOW + "  ██ " + ChatColor.WHITE + "Claims d'une faction alliée");
        player.sendMessage(ChatColor.RED + "  ██ " + ChatColor.WHITE + "Claims d'une faction ennemie");
        player.sendMessage(ChatColor.GRAY + "     (zones sans particules = non claimées)");
        player.sendMessage(ChatColor.GRAY + "  Rayon : 4 chunks — rejoue : §e/fac claimshow");
        player.sendMessage("");
    }

    private void handleClaimMap(Player player) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        String myFacName = myFaction != null ? myFaction.getName().toLowerCase() : null;

        Chunk center = player.getLocation().getChunk();
        int cx = center.getX();
        int cz = center.getZ();
        String world = center.getWorld().getName();

        // Taille de la grille : 21 colonnes × 11 lignes (largeur impaire pour centrer)
        int COLS = 21;
        int ROWS = 11;
        int halfCols = COLS / 2;
        int halfRows = ROWS / 2;

        Map<ClaimManager.ChunkKey, ClaimManager.ClaimData> allClaims = claimManager.getAllClaims();

        // Palette de couleurs par faction (cycle sur factions adverses rencontrées)
        ChatColor[] factionColors = {
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.AQUA,
            ChatColor.YELLOW, ChatColor.BLUE, ChatColor.GOLD
        };
        Map<String, ChatColor> colorMap = new HashMap<>();
        int colorIdx = 0;

        // Légende (factions vues sur la carte)
        Map<String, String> legend = new LinkedHashMap<>();

        // Construire la carte
        // Ligne de séparation et en-tête
        String sep = ChatColor.DARK_GRAY + "─────────────────────────────────────────";
        player.sendMessage("");
        player.sendMessage(sep);
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "  Carte des claims" +
                (myFaction != null ? ChatColor.WHITE + " — " + ChatColor.YELLOW + myFaction.getName() : "") +
                ChatColor.DARK_GRAY + "  [" + cx + ", " + cz + "]");
        player.sendMessage(sep);

        for (int row = 0; row < ROWS; row++) {
            int chunkZ = cz + (row - halfRows);
            StringBuilder line = new StringBuilder(ChatColor.DARK_GRAY + "  ");

            for (int col = 0; col < COLS; col++) {
                int chunkX = cx + (col - halfCols);

                // Chunk du joueur (position exacte)
                boolean isPlayerChunk = (col == halfCols && row == halfRows);

                ClaimManager.ChunkKey key = new ClaimManager.ChunkKey(world, chunkX, chunkZ);
                ClaimManager.ClaimData data = allClaims.get(key);

                if (isPlayerChunk) {
                    // Toujours afficher la position du joueur par-dessus
                    if (data == null) {
                        line.append(ChatColor.WHITE).append("✦");
                    } else if (myFacName != null && data.getFactionName().equalsIgnoreCase(myFacName)) {
                        line.append(ChatColor.GREEN).append("✦");
                    } else {
                        line.append(ChatColor.RED).append("✦");
                    }
                } else if (data == null) {
                    line.append(ChatColor.DARK_GRAY).append("·");
                } else {
                    String facLower = data.getFactionName().toLowerCase();
                    if (myFacName != null && facLower.equalsIgnoreCase(myFacName)) {
                        line.append(ChatColor.GREEN).append("█");
                        legend.put(myFacName, ChatColor.GREEN + "█ " + ChatColor.WHITE + myFaction.getName() + ChatColor.GRAY + " (vous)");
                    } else {
                        // Faction adverse
                        if (!colorMap.containsKey(facLower)) {
                            colorMap.put(facLower, factionColors[colorIdx % factionColors.length]);
                            colorIdx++;
                        }
                        ChatColor col2 = colorMap.get(facLower);
                        line.append(col2).append("▓");
                        // Récupérer le nom affiché proprement
                        Faction fac = factionManager.getFaction(facLower);
                        String displayName = fac != null ? fac.getName() : data.getFactionName();
                        legend.put(facLower, col2 + "▓ " + ChatColor.WHITE + displayName);
                    }
                }
            }
            player.sendMessage(line.toString());
        }

        // Légende
        player.sendMessage(sep);
        player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "  ✦" + ChatColor.GRAY + " Votre position   " +
                ChatColor.DARK_GRAY + "·" + ChatColor.GRAY + " Libre");
        for (String entry : legend.values()) {
            player.sendMessage("  " + entry);
        }
        player.sendMessage(sep);
        player.sendMessage(ChatColor.DARK_GRAY + "  Centre : chunk (" + cx + ", " + cz + ")  |  " +
                "Rayon : " + halfCols + " chunks");
        player.sendMessage("");
    }

    /** /faction claimallow <faction> — autoriser une faction à claimer près de nos claims */
    private void handleClaimAllow(Player player, String[] args) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut gérer les autorisations de claim."); return; }
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage : /faction claimallow <nom_faction>"); return; }

        String targetName = args[1];
        Faction target = factionManager.getFaction(targetName);
        if (target == null) { player.sendMessage(prefix() + ChatColor.RED + "Faction introuvable : §e" + targetName); return; }
        if (target.getName().equalsIgnoreCase(faction.getName())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas t'autoriser toi-même."); return; }

        claimManager.allowClaimProximity(faction.getName(), target.getName());
        player.sendMessage(prefix() + ChatColor.GREEN + "✔ §e" + target.getName() + ChatColor.GREEN
                + " peut désormais claimer à moins de " + ClaimManager.MIN_CLAIM_DISTANCE + " chunks de vos claims.");
    }

    /** /faction claimdeny <faction> — révoquer l'autorisation de proximité */
    private void handleClaimDeny(Player player, String[] args) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef peut gérer les autorisations de claim."); return; }
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage : /faction claimdeny <nom_faction>"); return; }

        String targetName = args[1];
        Faction target = factionManager.getFaction(targetName);
        if (target == null) { player.sendMessage(prefix() + ChatColor.RED + "Faction introuvable : §e" + targetName); return; }

        claimManager.revokeClaimProximity(faction.getName(), target.getName());
        player.sendMessage(prefix() + ChatColor.YELLOW + "✔ Autorisation de proximité révoquée pour §e" + target.getName() + ChatColor.YELLOW + ".");
    }

    /** /faction claimallies — lister les factions autorisées à claimer près de nous */
    private void handleClaimAllies(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }

        Set<String> allies = claimManager.getClaimAllies(faction.getName());
        player.sendMessage(ChatColor.GOLD + "══ Autorisations de claim de §e" + faction.getName() + ChatColor.GOLD + " ══");
        if (allies.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Aucune faction autorisée à claimer près de vous.");
        } else {
            player.sendMessage(ChatColor.GRAY + "  Factions pouvant claimer à moins de "
                    + ClaimManager.MIN_CLAIM_DISTANCE + " chunks de vos claims :");
            for (String ally : allies) {
                player.sendMessage(ChatColor.GREEN + "  ✔ " + ChatColor.WHITE + ally);
            }
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }

    private void handleUnclaim(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + ChatColor.RED + "Tu n'es pas dans une faction."); return; }
        if (!faction.canManage(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut retirer un claim."); return; }

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

    // ════════════════════════════════════════════════════════════════════════════
    // SHOP GLOBAL v4
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * /faction vendre <prix> <monnaie>
     * Monnaies acceptées : fer, or, diamant, emeraude (insensible à la casse)
     * Met en vente TOUT le stack tenu en main.
     */
    /**
     * /faction vendre → ouvre le GUI de création d'annonce.
     * L'ancienne syntaxe en ligne de commande est conservée comme raccourci optionnel.
     */
    private void handleShopSell(Player player, String[] args) {
        // Sans argument → ouvre le GUI de création
        if (args.length < 2) {
            if (shopCreateGUI != null) {
                shopCreateGUI.open(player);
            } else {
                player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction vendre [prix] [monnaie]");
                player.sendMessage(prefix() + ChatColor.GRAY + "Ou ouvre §e/fac shop §7et clique sur §e+ Créer une annonce§7.");
            }
            return;
        }

        // Raccourci texte : /fac vendre <prix> <monnaie> (item en main)
        if (args.length >= 3) {
            int price;
            try {
                price = Integer.parseInt(args[1]);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(prefix() + ChatColor.RED + "Le prix doit être un entier positif.");
                return;
            }
            ShopListing.Currency currency = parseCurrency(args[2]);
            if (currency == null) {
                player.sendMessage(prefix() + ChatColor.RED + "Monnaie invalide. Utilise : fer, or, diamant, emeraude");
                return;
            }
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(prefix() + ChatColor.RED + "Tu ne tiens rien dans ta main !");
                return;
            }
            int qty = hand.getAmount();
            org.bukkit.inventory.ItemStack forSale = hand.clone();
            player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.AIR));
            ShopListing listing = shopManager.createCurrencyListing(player, forSale, currency, price);
            player.sendMessage(prefix() + ChatColor.GREEN + "Annonce créée ! "
                    + ChatColor.YELLOW + qty + "× " + ShopManager.formatMat(forSale.getType())
                    + ChatColor.GREEN + " → " + ChatColor.GOLD + price + "× " + currency.getDisplayName()
                    + ChatColor.DARK_GRAY + " [ID: " + listing.getId() + "]");
            player.sendMessage(prefix() + ChatColor.GRAY + "Récupérable via : §e/faction recuperer " + listing.getId());
            return;
        }

        // /fac vendre avec 1 seul arg → ouvrir le GUI
        if (shopCreateGUI != null) shopCreateGUI.open(player);
    }

    /**
     * /faction acheter <ID>
     * Achète directement une annonce sans passer par le GUI.
     */
    private void handleShopBuy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction acheter <ID>");
            player.sendMessage(prefix() + ChatColor.GRAY + "L'ID est affiché dans le shop (§e/faction shop§7).");
            return;
        }
        String id = args[1].toUpperCase();
        ShopListing listing = shopManager.findById(id);
        if (listing == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Annonce '" + id + "' introuvable.");
            return;
        }
        if (listing.isSold()) {
            player.sendMessage(prefix() + ChatColor.RED + "Cet article a déjà été vendu !");
            return;
        }
        if (listing.getSellerUUID().equals(player.getUniqueId())) {
            player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas acheter ta propre annonce !");
            return;
        }

        // Afficher un résumé avant d'acheter
        player.sendMessage(prefix() + ChatColor.YELLOW + "Confirmation d'achat :");
        player.sendMessage(ChatColor.GRAY + "  Item   : §f" + listing.getItemLine());
        player.sendMessage(ChatColor.GRAY + "  " + (listing.getPriceMode() == ShopListing.PriceMode.BARTER ? "Troc  : §f" : "Prix  : §f") + listing.getPriceLine());
        player.sendMessage(ChatColor.GRAY + "  Vendeur: §f" + listing.getSellerName());

        ShopManager.BuyResult result = shopManager.buy(player, id);
        switch (result) {
            case SUCCESS ->
                player.sendMessage(prefix() + ChatColor.GREEN + "Échange effectué ! Item ajouté à ton inventaire.");
            case NOT_ENOUGH_PAYMENT ->
                player.sendMessage(prefix() + ChatColor.RED + "Tu n'as pas assez de "
                        + (listing.getPriceMode() == ShopListing.PriceMode.CURRENCY
                            ? listing.getCurrency().getDisplayName()
                            : ShopListing.displayName(listing.getPriceItem()))
                        + " pour cet échange.");
            case ALREADY_SOLD ->
                player.sendMessage(prefix() + ChatColor.RED + "Cet article vient d'être vendu !");
            case NOT_FOUND ->
                player.sendMessage(prefix() + ChatColor.RED + "Annonce introuvable.");
            case OWN_LISTING ->
                player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas acheter ta propre annonce !");
        }
    }

    /**
     * /faction recuperer <ID>
     * Récupère une annonce non vendue.
     */
    private void handleShopRecover(Player player, String[] args) {
        if (args.length < 2) {
            // Lister les annonces du joueur
            java.util.List<ShopListing> mine = shopManager.getSellerListings(player.getUniqueId());
            if (mine.isEmpty()) {
                player.sendMessage(prefix() + ChatColor.GRAY + "Tu n'as aucune annonce active dans le shop.");
                return;
            }
            player.sendMessage(prefix() + ChatColor.YELLOW + "Tes annonces actives :");
            for (ShopListing l : mine) {
                player.sendMessage(ChatColor.DARK_GRAY + "  [" + l.getId() + "] §f"
                        + l.getItemLine()
                        + " §7→ §e" + l.getPriceLine());
            }
            player.sendMessage(prefix() + ChatColor.GRAY + "Usage: §e/faction recuperer <ID>");
            return;
        }

        String id = args[1].toUpperCase();
        ShopManager.RecoverResult result = shopManager.recover(player, id);
        switch (result) {
            case SUCCESS      -> player.sendMessage(prefix() + ChatColor.GREEN + "Item '" + id + "' récupéré dans ton inventaire !");
            case NOT_FOUND    -> player.sendMessage(prefix() + ChatColor.RED + "Annonce '" + id + "' introuvable.");
            case NOT_OWNER    -> player.sendMessage(prefix() + ChatColor.RED + "Ce n'est pas ton annonce.");
            case ALREADY_SOLD -> player.sendMessage(prefix() + ChatColor.RED + "Cet item a déjà été vendu. Tu aurais dû recevoir le paiement.");
        }
    }

    private ShopListing.Currency parseCurrency(String input) {
        return switch (input.toLowerCase()) {
            case "fer", "iron", "lingot_de_fer"   -> ShopListing.Currency.IRON_INGOT;
            case "or", "gold", "lingot_d_or"      -> ShopListing.Currency.GOLD_INGOT;
            case "diamant", "diamond"              -> ShopListing.Currency.DIAMOND;
            case "emeraude", "emerald"             -> ShopListing.Currency.EMERALD;
            default -> null;
        };
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ALLIANCES v5
    // ════════════════════════════════════════════════════════════════════════════

    private void handleAlliance(Player player, String[] args) {
        if (args.length < 2) {
            allianceManager.handleAllianceList(player);
            return;
        }
        switch (args[1].toLowerCase()) {  // args[1] = sous-commande alliance
            case "inviter", "invite"    -> { if (args.length >= 3) allianceManager.handleAllianceInvite(player, args[2]); else player.sendMessage(prefix() + "§cUsage: /faction alliance inviter <faction>"); }
            case "accepter", "accept"   -> { if (args.length >= 3) allianceManager.handleAllianceAccept(player, args[2]); else player.sendMessage(prefix() + "§cUsage: /faction alliance accepter <faction>"); }
            case "refuser", "decline"   -> { if (args.length >= 3) allianceManager.handleAllianceDecline(player, args[2]); else player.sendMessage(prefix() + "§cUsage: /faction alliance refuser <faction>"); }
            case "rompre", "break"      -> { if (args.length >= 3) allianceManager.handleAllianceBreak(player, args[2]); else player.sendMessage(prefix() + "§cUsage: /faction alliance rompre <faction>"); }
            case "liste", "list"        -> allianceManager.handleAllianceList(player);
            case "gui", "menu"          -> allianceManager.openAllianceGUI(player);
            default                     -> {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "══ Alliance ══");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance inviter <faction>  " + ChatColor.GRAY + "Proposer une alliance");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance accepter <faction> " + ChatColor.GRAY + "Accepter une invitation");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance refuser <faction>  " + ChatColor.GRAY + "Refuser une invitation");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance rompre <faction>   " + ChatColor.GRAY + "Rompre une alliance");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance liste              " + ChatColor.GRAY + "Voir les alliés");
                player.sendMessage(ChatColor.YELLOW + "/faction alliance gui                " + ChatColor.GRAY + "Interface graphique");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SPAWN DE FACTION v5
    // ════════════════════════════════════════════════════════════════════════════

    private void handleSetFactionSpawn(Player player, String[] args) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.canManage(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut définir le spawn de la faction."); return; }

        fr.faction.ranking.FactionRank rank = powerManager.getFactionRank(faction.getName());
        int maxSpawns = rank.getMaxSpawns();

        // Déterminer le slot demandé (1 par défaut, 2 si arg "2")
        int slot = 1;
        if (args.length >= 2) {
            try { slot = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        slot = Math.max(1, Math.min(2, slot));

        if (slot == 2 && maxSpawns < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Le spawn n°2 est réservé au rang "
                    + ChatColor.AQUA + "◆ Diamant" + ChatColor.RED + " ou supérieur.");
            player.sendMessage(prefix() + ChatColor.GRAY + "Votre rang actuel : " + rank.getLabel()
                    + ChatColor.GRAY + " — puissance nécessaire : §e12 000");
            return;
        }

        factionManager.setFactionSpawn(faction.getName(), player.getLocation(), slot);

        player.sendMessage(prefix() + ChatColor.GREEN + "Spawn §e#" + slot + " §ade la faction §e"
                + faction.getName() + " §adéfini ici !");

        // Si rang Diamant+ et spawn 2, indiquer que les deux spawns sont actifs
        if (maxSpawns >= 2) {
            int defined = (faction.hasSpawn() ? 1 : 0) + (faction.hasSpawn2() ? 1 : 0);
            player.sendMessage(ChatColor.GRAY + "  Spawns définis : §f" + defined + "§7/§f2");
        }

        // Notifier les membres
        for (java.util.UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(player))
                m.sendMessage(prefix() + ChatColor.YELLOW + "Le spawn §e#" + slot
                        + " §ede la faction a été mis à jour par le chef !");
        }
    }

    private void handleFactionSpawn(Player player, String[] args) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }

        fr.faction.ranking.FactionRank rank = powerManager.getFactionRank(faction.getName());
        int maxSpawns = rank.getMaxSpawns();

        // Slot demandé (1 par défaut, 2 si arg "2")
        int slot = 1;
        if (args.length >= 2) {
            try { slot = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        slot = Math.max(1, Math.min(2, slot));

        if (slot == 2 && maxSpawns < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Le spawn n°2 n'est pas disponible "
                    + "(rang §c◆ Diamant §crequis).");
            return;
        }

        if (!faction.hasSpawnBySlot(slot)) {
            if (slot == 2)
                player.sendMessage(prefix() + ChatColor.RED + "Aucun spawn n°2 défini. "
                        + "Le chef peut utiliser §e/faction setspawn 2§c.");
            else
                player.sendMessage(prefix() + ChatColor.RED + "Aucun spawn de faction défini. "
                        + "Le chef peut utiliser §e/faction setspawn§c.");
            return;
        }

        org.bukkit.Location spawn = faction.getSpawnBySlot(slot);
        String label = maxSpawns >= 2 ? " §7(spawn §f#" + slot + "§7)" : "";
        player.sendMessage(prefix() + ChatColor.GREEN + "Téléportation au spawn de la faction…" + label);
        player.teleport(spawn);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HOMES v5
    // ════════════════════════════════════════════════════════════════════════════

    private void handleSetHomeCmd(Player player, String[] args) {
        String name = args.length >= 2 ? args[1] : "home";
        HomeManager.SetHomeResult r = homeManager.setHome(player, name);
        int max = homeManager.getMaxHomes(player.getUniqueId());
        String pf = "§8[§a🏠 Home§8] §r";
        switch (r) {
            case SUCCESS -> player.sendMessage(pf + "§aHome §e" + name + " §adéfini !");
            case TOO_MANY_HOMES -> {
                String progressHint = switch (max) {
                    case 1 -> "§7Rejoins une faction (§eBronze§7 → 2 homes)";
                    case 2 -> "§7Atteins le rang §eOr §7ou §eDiamant §7(→ 3 homes)";
                    case 3 -> "§7Atteins le rang §eÉmeraude §7(→ 4 homes)";
                    case 4 -> "§7Atteins le rang §dLégendaire §7(→ 5 homes)";
                    default -> "";
                };
                player.sendMessage(pf + "§cLimite de §e" + max + " §chome(s) atteinte. " + progressHint);
            }
            case TOO_CLOSE_TO_OTHER_HOME -> player.sendMessage(pf
                    + "§cImpossible : un home d'un autre joueur se trouve à moins de §e10 chunks§c. "
                    + "§7(exempt : membres de ta faction et factions alliées)");
            case NAME_TAKEN -> player.sendMessage(pf + "§cCe nom est déjà utilisé.");
        }
    }

    private void handleHomeCmd(Player player, String[] args) {
        String name = args.length >= 2 ? args[1] : "home";
        HomeManager.TpHomeResult r = homeManager.teleportHome(player, name);
        if (r == HomeManager.TpHomeResult.NOT_FOUND)
            player.sendMessage("§8[§a🏠 Home§8] §r§cHome §e" + name + " §cintrouvable. Utilise /faction homes.");
    }

    private void handleDelHomeCmd(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage("§8[§a🏠 Home§8] §r§cUsage: /faction delhome <nom>"); return; }
        boolean ok = homeManager.deleteHome(player.getUniqueId(), args[1]);
        player.sendMessage("§8[§a🏠 Home§8] §r" + (ok ? "§cHome §f" + args[1] + " §csupprimé." : "§cHome introuvable."));
    }

    private void handleHomesCmd(Player player) {
        var list = homeManager.getHomes(player.getUniqueId());
        int max  = homeManager.getMaxHomes(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "══ Tes homes (" + list.size() + "/" + max + ") ══");
        if (list.isEmpty()) {
            player.sendMessage("§7Aucun home. Utilise §e/faction sethome <nom>§7.");
        } else {
            for (HomeManager.NamedHome h : list) {
                player.sendMessage("  §e" + h.name + " §7→ §f"
                        + h.location.getWorld().getName()
                        + " §7(" + (int)h.location.getX() + ", " + (int)h.location.getY()
                        + ", " + (int)h.location.getZ() + ")");
            }
        }
        if (max < 5) {
            String hint = switch (max) {
                case 1 -> "Rejoins une faction (rang §eBronze§7 → 2 homes)";
                case 2 -> "Atteins le rang §eOr §7ou §bDiamant §7(→ 3 homes)";
                case 3 -> "Atteins le rang §aÉmeraude §7(→ 4 homes)";
                case 4 -> "Atteins le rang §dLégendaire §7(→ 5 homes)";
                default -> "";
            };
            if (!hint.isEmpty())
                player.sendMessage(ChatColor.GRAY + hint);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // INVSEE (Admin)
    // ════════════════════════════════════════════════════════════════════════════

    private void handleInvSee(Player player, String[] args) {
        if (!player.hasPermission("faction.admin")) {
            player.sendMessage(prefix() + ChatColor.RED + "Commande réservée aux admins (permission faction.admin).");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction invsee <joueur>");
            return;
        }
        invSeeGUI.openInvSee(player, args[1]);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GUERRE v5.1
    // ════════════════════════════════════════════════════════════════════════════

    private void handleGuerre(Player player, String[] args) {
        if (warManager == null) { player.sendMessage(prefix() + "§cSystème de guerre non disponible."); return; }

        if (args.length < 2) { warManager.showHelp(player); return; }

        switch (args[1].toLowerCase()) {

            case "declarer", "declare" -> {
                if (args.length < 3) {
                    player.sendMessage(prefix() + "§cUsage: /fac guerre declarer <faction> [claims:0-5] [pillage] [kills:5-50]");
                    return;
                }
                String targetFac = args[2];
                int claims = 0, kills = fr.faction.war.WarManager.DEFAULT_KILLS_TO_WIN;
                boolean pillage = false;
                for (int i = 3; i < args.length; i++) {
                    String opt = args[i].toLowerCase();
                    if (opt.startsWith("claims:")) {
                        try { claims = Integer.parseInt(opt.substring(7)); } catch (NumberFormatException ignored) {}
                    } else if (opt.startsWith("kills:")) {
                        try { kills = Integer.parseInt(opt.substring(6)); } catch (NumberFormatException ignored) {}
                    } else if (opt.equals("pillage") || opt.equals("raid")) {
                        pillage = true;
                    }
                }
                fr.faction.war.WarManager.DeclareResult result =
                        warManager.declare(player, targetFac, claims, pillage, kills, factionManager, powerManager);
                switch (result) {
                    case SUCCESS             -> {} // déjà notifié
                    case NOT_IN_FACTION      -> player.sendMessage(prefix() + msg("not-in-faction"));
                    case NOT_CHEF            -> player.sendMessage(prefix() + msg("not-chef"));
                    case TARGET_NOT_FOUND    -> player.sendMessage(prefix() + "§cFaction §f" + targetFac + " §cintrouvable.");
                    case ALREADY_AT_WAR      -> player.sendMessage(prefix() + "§cUne faction est déjà en guerre. Attendez la fin.");
                    case ON_COOLDOWN         -> {
                        long hrs = warManager.getCooldownRemaining(factionManager.getPlayerFaction(player.getUniqueId()).getName()) / 3600000;
                        player.sendMessage(prefix() + "§cCooldown encore §e" + hrs + "h §cavant de pouvoir déclarer la guerre.");
                    }
                    case TARGET_ON_COOLDOWN  -> player.sendMessage(prefix() + "§cCette faction est en cooldown (guerre récente).");
                    case IS_ALLY             -> player.sendMessage(prefix() + "§cTu ne peux pas déclarer la guerre à un allié !");
                    case POWER_RATIO_TOO_HIGH-> player.sendMessage(prefix() + "§cDifférence de puissance trop grande (ratio max 3:1). Guerre refusée.");
                    case TARGET_TOO_SMALL    -> player.sendMessage(prefix() + "§cLa faction cible doit avoir au moins 2 membres.");
                    case INVALID_CLAIMS      -> player.sendMessage(prefix() + "§cClaims à miser : entre 0 et " + fr.faction.war.WarManager.MAX_CLAIMS_AT_STAKE + ".");
                    case INVALID_KILLS       -> player.sendMessage(prefix() + "§cObjectif kills : entre " + fr.faction.war.WarManager.MIN_KILLS_TO_WIN + " et " + fr.faction.war.WarManager.MAX_KILLS_TO_WIN + ".");
                }
            }

            case "accepter", "accept" -> {
                fr.faction.war.WarManager.AcceptResult r = warManager.accept(player, factionManager);
                switch (r) {
                    case NOT_IN_FACTION   -> player.sendMessage(prefix() + msg("not-in-faction"));
                    case NOT_CHEF         -> player.sendMessage(prefix() + msg("not-chef"));
                    case NO_PENDING_WAR   -> player.sendMessage(prefix() + "§cAucune déclaration de guerre en attente pour ta faction.");
                    case SUCCESS          -> {} // broadcast déjà fait
                }
            }

            case "refuser", "decline", "refuse" -> {
                if (!warManager.decline(player, factionManager))
                    player.sendMessage(prefix() + "§cAucune déclaration de guerre à refuser (ou tu n'es pas chef).");
            }

            case "capituler", "surrend", "surrender" -> {
                if (!warManager.surrender(player, factionManager))
                    player.sendMessage(prefix() + "§cAucune guerre active, ou tu n'es pas chef.");
            }

            case "statut", "status", "score" -> warManager.showStatus(player, factionManager);

            case "liste", "list"             -> warManager.listWars(player);

            case "piller", "raid" -> {
                fr.faction.models.Faction myFac = factionManager.getPlayerFaction(player.getUniqueId());
                if (myFac == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
                fr.faction.war.WarSession raidSession = warManager.getRaidableWarFor(myFac.getName());
                if (raidSession == null) {
                    player.sendMessage(prefix() + "§cAucun pillage disponible pour ta faction.");
                    return;
                }
                String loser = raidSession.getOpponent(myFac.getName());
                warManager.openRaidChest(player, raidSession, loser, factionManager);
            }

            default -> warManager.showHelp(player);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TRI DE COFFRE v5.2
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * /fac ranger [coffre|perso|inventaire]
     * Sans argument → menu de tri du coffre partagé de la faction.
     * Avec "perso" ou "inventaire" → menu de tri de l'inventaire personnel.
     * Avec un mode direct → tri immédiat sans GUI.
     */
    private void handleRanger(Player player, String[] args) {
        if (sortMenuGUI == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Système de tri non disponible.");
            return;
        }

        // /fac ranger perso|inventaire → trier son propre inventaire
        if (args.length >= 2 && (args[1].equalsIgnoreCase("perso")
                || args[1].equalsIgnoreCase("inventaire")
                || args[1].equalsIgnoreCase("moi")
                || args[1].equalsIgnoreCase("sac"))) {
            sortMenuGUI.openForPersonalInventory(player);
            return;
        }

        // /fac ranger coffre → menu pour le coffre partagé (défaut)
        fr.faction.models.Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(prefix() + msg("not-in-faction"));
            return;
        }
        sortMenuGUI.openForSharedChest(player);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VILLAGEOIS RECRUTÉS v5.9
    // ════════════════════════════════════════════════════════════════════════════

    private void handleRecruter(Player player) {
        if (villagerManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villageois non disponible."); return; }

        fr.faction.villager.VillagerManager.RecruitResult result = villagerManager.recruit(player);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(prefix() + ChatColor.GREEN + "✔ Villageois recruté ! Gère-le avec §e/faction villageois§a.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            }
            case NOT_IN_FACTION -> player.sendMessage(prefix() + msg("not-in-faction"));
            case NO_PERMISSION  -> player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut recruter un villageois.");
            case NO_TARGET      -> player.sendMessage(prefix() + ChatColor.RED + "Vise un villageois (8 blocs max) et réessaie.");
            case ALREADY_RECRUITED -> player.sendMessage(prefix() + ChatColor.RED + "Ce villageois est déjà recruté.");
            case FACTION_FULL   -> player.sendMessage(prefix() + ChatColor.RED + "Ta faction a atteint sa limite de villageois recrutés.");
        }
    }

    private void handleVillageois(Player player, String[] args) {
        if (villagerManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villageois non disponible."); return; }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("ranger") || args[1].equalsIgnoreCase("formation"))) {
            fr.faction.villager.VillagerManager.FormationType type = fr.faction.villager.VillagerManager.FormationType.LIGNE;
            if (args.length >= 3 && (args[2].equalsIgnoreCase("cercle") || args[2].equalsIgnoreCase("circle"))) {
                type = fr.faction.villager.VillagerManager.FormationType.CERCLE;
            }
            villagerManager.formation(player, type);
            return;
        }
        if (villagerGUI == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villageois non disponible."); return; }
        villagerGUI.openList(player);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VILLAGES v5.11
    // ════════════════════════════════════════════════════════════════════════════

    private void handleVillage(Player player, String[] args) {
        if (villageManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villages non disponible."); return; }

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "══ Villages ══");
            player.sendMessage(ChatColor.YELLOW + "/faction village fonder <nom> " + ChatColor.GRAY + "Fonder un village (chef/sous-chef)");
            player.sendMessage(ChatColor.YELLOW + "/faction village liste        " + ChatColor.GRAY + "Voir les villages de ta faction");
            player.sendMessage(ChatColor.YELLOW + "/faction village dissoudre <nom> " + ChatColor.GRAY + "Dissoudre un village (chef/sous-chef)");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "fonder", "creer", "create" -> {
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction village fonder <nom>"); return; }
                String name = args[2];
                fr.faction.village.VillageManager.FoundResult result = villageManager.startFoundation(player, name);
                switch (result) {
                    case SUCCESS -> {} // le message d'instruction a déjà été envoyé par startFoundation
                    case NOT_IN_FACTION -> player.sendMessage(prefix() + msg("not-in-faction"));
                    case NO_PERMISSION -> player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut fonder un village.");
                    case NAME_TAKEN -> player.sendMessage(prefix() + ChatColor.RED + "Un village de ta faction porte déjà ce nom.");
                    case NAME_INVALID -> player.sendMessage(prefix() + ChatColor.RED + "Nom invalide (1 à 24 caractères).");
                }
            }
            case "liste", "list" -> {
                Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
                if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
                var villages = villageManager.getFactionVillages(faction.getName());
                if (villages.isEmpty()) { player.sendMessage(prefix() + ChatColor.GRAY + "Aucun village fondé. Utilise §e/faction village fonder <nom>§7."); return; }
                player.sendMessage(ChatColor.GOLD + "══ Villages de §e" + faction.getName() + ChatColor.GOLD + " ══");
                for (var village : villages) {
                    int pop = villageManager.getPopulation(village);
                    int level = villageManager.getLevel(village);
                    player.sendMessage(ChatColor.YELLOW + village.getName() + ChatColor.GRAY + " — "
                            + fr.faction.village.Village.levelLabel(level) + ChatColor.GRAY + ", " + pop + " habitant(s)");
                }
            }
            case "dissoudre", "disband", "supprimer" -> {
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction village dissoudre <nom>"); return; }
                Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
                if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
                var village = villageManager.getByName(faction.getName(), args[2]);
                if (village == null) { player.sendMessage(prefix() + ChatColor.RED + "Aucun village de ce nom."); return; }
                if (villageManager.disband(player, village)) {
                    player.sendMessage(prefix() + ChatColor.GREEN + "✔ Village §e" + village.getName() + " §adissous.");
                } else {
                    player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut dissoudre un village.");
                }
            }
            default -> player.sendMessage(prefix() + ChatColor.RED + "Sous-commande inconnue. Utilise /faction village pour l'aide.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PORT / GARE — v5.12
    // ════════════════════════════════════════════════════════════════════════════

    private void handlePost(Player player, String[] args, fr.faction.village.PostType type) {
        if (villageManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villages non disponible."); return; }
        String label = type == fr.faction.village.PostType.PORT ? "port" : "gare";
        if (args.length < 3 || !args[1].equalsIgnoreCase("definir")) {
            player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction " + (type == fr.faction.village.PostType.PORT ? "port" : "gare") + " definir <village>");
            return;
        }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        var village = villageManager.getByName(faction.getName(), args[2]);
        if (village == null) { player.sendMessage(prefix() + ChatColor.RED + "Aucun village de ce nom."); return; }
        if (!villageManager.startPostSelection(player, village, type)) {
            player.sendMessage(prefix() + ChatColor.RED + "Seul le chef ou un sous-chef peut définir un " + label + ".");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CONTRATS COMMERCIAUX — v5.12
    // ════════════════════════════════════════════════════════════════════════════

    private void handleContrat(Player player, String[] args) {
        if (commerceManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de commerce non disponible."); return; }

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "══ Contrats ══");
            player.sendMessage(ChatColor.YELLOW + "/faction contrat creer <village> <port|gare> <factionCible> <villageCible> <ressource> <quantite>");
            player.sendMessage(ChatColor.YELLOW + "/faction contrat liste                " + ChatColor.GRAY + "Voir les contrats de ta faction");
            player.sendMessage(ChatColor.YELLOW + "/faction contrat assigner <id>        " + ChatColor.GRAY + "Assigner (en visant un navigateur/cheminot)");
            player.sendMessage(ChatColor.YELLOW + "/faction contrat annuler <id>         " + ChatColor.GRAY + "Annuler un contrat en attente");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "creer", "create" -> {
                if (args.length < 8) {
                    player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction contrat creer <village> <port|gare> <factionCible> <villageCible> <ressource> <quantite>");
                    return;
                }
                fr.faction.village.PostType type;
                if (args[3].equalsIgnoreCase("port")) type = fr.faction.village.PostType.PORT;
                else if (args[3].equalsIgnoreCase("gare")) type = fr.faction.village.PostType.GARE;
                else { player.sendMessage(prefix() + ChatColor.RED + "Le type doit être 'port' ou 'gare'."); return; }

                org.bukkit.Material resource;
                try { resource = org.bukkit.Material.valueOf(args[6].toUpperCase()); }
                catch (IllegalArgumentException ex) { player.sendMessage(prefix() + ChatColor.RED + "Ressource invalide : " + args[6]); return; }

                int quantity;
                try { quantity = Integer.parseInt(args[7]); }
                catch (NumberFormatException ex) { player.sendMessage(prefix() + ChatColor.RED + "Quantité invalide."); return; }

                var result = commerceManager.createContract(player, args[2], type, args[4], args[5], resource, quantity);
                player.sendMessage(prefix() + (result.success() ? ChatColor.GREEN + "✔ " : ChatColor.RED) + result.message());
            }
            case "liste", "list" -> {
                Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
                if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
                var contracts = commerceManager.getFactionContracts(faction.getName());
                if (contracts.isEmpty()) { player.sendMessage(prefix() + ChatColor.GRAY + "Aucun contrat."); return; }
                player.sendMessage(ChatColor.GOLD + "══ Contrats de §e" + faction.getName() + ChatColor.GOLD + " ══");
                for (var c : contracts) {
                    player.sendMessage(ChatColor.YELLOW + c.getId().toString().substring(0, 8) + ChatColor.GRAY + " — "
                            + c.getFromVillage() + " (" + c.getFromFaction() + ") → " + c.getToVillage() + " (" + c.getToFaction() + ") : "
                            + c.getQuantity() + " " + c.getResource().name().toLowerCase() + ChatColor.GRAY + " [" + c.getStatus() + "]");
                }
            }
            case "assigner", "assign" -> {
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction contrat assigner <id>"); return; }
                if (villagerManager == null) { player.sendMessage(prefix() + ChatColor.RED + "Système de villageois non disponible."); return; }

                fr.faction.commerce.Contract contract = findContractByShortId(player, args[2]);
                if (contract == null) { player.sendMessage(prefix() + ChatColor.RED + "Contrat introuvable."); return; }

                var rt = player.rayTraceEntities(8);
                if (rt == null || !(rt.getHitEntity() instanceof org.bukkit.entity.Villager targetVillager)) {
                    player.sendMessage(prefix() + ChatColor.RED + "Vise le navigateur/cheminot à assigner (8 blocs max).");
                    return;
                }
                var rv = villagerManager.getByEntity(targetVillager.getUniqueId());
                if (rv == null) { player.sendMessage(prefix() + ChatColor.RED + "Ce n'est pas un villageois recruté."); return; }

                var result = commerceManager.assignContract(rv, contract);
                switch (result) {
                    case SUCCESS -> player.sendMessage(prefix() + ChatColor.GREEN + "✔ Contrat assigné : " + rv.getDisplayName() + " part en livraison !");
                    case WRONG_ROLE -> player.sendMessage(prefix() + ChatColor.RED + "Ce villageois n'a pas le bon rôle pour ce contrat (navigateur pour un port, cheminot pour une gare).");
                    case ALREADY_BUSY -> player.sendMessage(prefix() + ChatColor.RED + "Ce villageois a déjà un contrat en cours, ou ce contrat est déjà pris.");
                    case NOT_WAITING -> player.sendMessage(prefix() + ChatColor.RED + "Ce contrat n'est plus en attente.");
                    case WRONG_FACTION -> player.sendMessage(prefix() + ChatColor.RED + "Ce villageois n'appartient pas à la faction du contrat.");
                    case NO_WAREHOUSE -> player.sendMessage(prefix() + ChatColor.RED + "Aucun coffre au port/à la gare de départ.");
                    case NOT_ENOUGH_CARGO -> player.sendMessage(prefix() + ChatColor.RED + "Pas assez de marchandise dans l'entrepôt de départ.");
                    case NO_DEST_POST -> player.sendMessage(prefix() + ChatColor.RED + "Port/gare de départ ou d'arrivée introuvable.");
                }
            }
            case "annuler", "cancel" -> {
                if (args.length < 3) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction contrat annuler <id>"); return; }
                fr.faction.commerce.Contract contract = findContractByShortId(player, args[2]);
                if (contract == null) { player.sendMessage(prefix() + ChatColor.RED + "Contrat introuvable."); return; }
                if (commerceManager.cancelContract(player, contract)) {
                    player.sendMessage(prefix() + ChatColor.GREEN + "✔ Contrat annulé.");
                } else {
                    player.sendMessage(prefix() + ChatColor.RED + "Impossible (déjà en transit, ou tu n'as pas la permission).");
                }
            }
            default -> player.sendMessage(prefix() + ChatColor.RED + "Sous-commande inconnue. Utilise /faction contrat pour l'aide.");
        }
    }

    private fr.faction.commerce.Contract findContractByShortId(Player player, String shortId) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) return null;
        for (var c : commerceManager.getFactionContracts(faction.getName())) {
            if (c.getId().toString().startsWith(shortId.toLowerCase())) return c;
        }
        return null;
    }

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
        player.sendMessage(ChatColor.YELLOW + "/faction souschef            " + ChatColor.GRAY + "Gérer les sous-chefs (promouvoir/retirer/liste/limite)");
        player.sendMessage(ChatColor.YELLOW + "/faction rename <nouveau_nom>" + ChatColor.GRAY + "Renommer la faction (chef uniquement)");
        player.sendMessage(ChatColor.YELLOW + "/faction info [nom]          " + ChatColor.GRAY + "Info faction");
        player.sendMessage(ChatColor.YELLOW + "/faction list                " + ChatColor.GRAY + "Liste des factions");
        player.sendMessage(ChatColor.YELLOW + "/faction tp [joueur]         " + ChatColor.GRAY + "Téléportation");
        player.sendMessage(ChatColor.YELLOW + "/faction coffre              " + ChatColor.GRAY + "Coffre partagé");
        player.sendMessage(ChatColor.GRAY + "— Classement & Stats —");
        player.sendMessage(ChatColor.YELLOW + "/faction top                 " + ChatColor.GRAY + "Top 10 factions par puissance (texte)");
        player.sendMessage(ChatColor.YELLOW + "/faction topbanque           " + ChatColor.GRAY + "Top 10 factions par émeraudes en coffre (texte)");
        player.sendMessage(ChatColor.YELLOW + "/faction classement          " + ChatColor.GRAY + "Classement factions GUI");
        player.sendMessage(ChatColor.YELLOW + "/faction rangs               " + ChatColor.GRAY + "Guide des rangs GUI");
        player.sendMessage(ChatColor.YELLOW + "/faction power [joueur]      " + ChatColor.GRAY + "Puissance individuelle");
        player.sendMessage(ChatColor.YELLOW + "/faction stats [joueur]      " + ChatColor.GRAY + "Statistiques personnelles");
        player.sendMessage(ChatColor.YELLOW + "/faction classementjoueurs   " + ChatColor.GRAY + "Top 10 joueurs par stat (dont richesse)");
        player.sendMessage(ChatColor.GRAY + "— Territoire (Claims) —");
        player.sendMessage(ChatColor.GREEN  + "/faction claim               " + ChatColor.GRAY + "Claimer le chunk sous tes pieds (cout croissant en emeral.)");
        player.sendMessage(ChatColor.GREEN  + "/faction unclaim             " + ChatColor.GRAY + "Retirer le claim du chunk actuel");
        player.sendMessage(ChatColor.GREEN  + "/faction claims              " + ChatColor.GRAY + "Voir les claims de ta faction");
        player.sendMessage(ChatColor.GREEN  + "/faction claimmap            " + ChatColor.GRAY + "Carte visuelle des claims autour de ta position");
        player.sendMessage(ChatColor.GREEN  + "/faction claimshow           " + ChatColor.GRAY + "§aVisualiser les claims en §bparticules §a(10s) autour de toi");
        player.sendMessage(ChatColor.AQUA   + "/faction map                 " + ChatColor.GRAY + "§bMini-map de faction§7 (item carte 128×128)");
        player.sendMessage(ChatColor.AQUA   + "/faction map partage         " + ChatColor.GRAY + "Partager/masquer ta position aux factions alliées");
        player.sendMessage(ChatColor.GREEN  + "/faction claimallow <faction>" + ChatColor.GRAY + "Autoriser une faction à claimer à moins de " + ClaimManager.MIN_CLAIM_DISTANCE + " chunks de vos claims");
        player.sendMessage(ChatColor.GREEN  + "/faction claimdeny <faction> " + ChatColor.GRAY + "Révoquer l'autorisation de proximité de claim");
        player.sendMessage(ChatColor.GREEN  + "/faction claimallies         " + ChatColor.GRAY + "Lister les factions autorisées à claimer près de vous");
        player.sendMessage(ChatColor.GREEN  + "/faction perms               " + ChatColor.GRAY + "Gerer les acces a ce chunk claim (GUI)");
        player.sendMessage(ChatColor.GRAY + "— Economie —");
        player.sendMessage(ChatColor.AQUA   + "/faction banque              " + ChatColor.GRAY + "Banque emeraudes personnelle / faction (GUI)");
        player.sendMessage(ChatColor.GRAY + "— Troc —");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction troc <joueur>       " + ChatColor.GRAY + "Proposer un troc a un joueur (GUI)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction accepter            " + ChatColor.GRAY + "Accepter une invitation de troc");
        player.sendMessage(ChatColor.GRAY + "— Shop Global —");
        player.sendMessage(ChatColor.GOLD + "/faction shop                " + ChatColor.GRAY + "Ouvrir le shop global (GUI paginé + recherche)");
        player.sendMessage(ChatColor.GOLD + "/faction vendre <prix> <monnaie>" + ChatColor.GRAY + " Mettre l'item en main en vente");
        player.sendMessage(ChatColor.GOLD + "/faction acheter <ID>        " + ChatColor.GRAY + "Acheter une annonce par son ID");
        player.sendMessage(ChatColor.GOLD + "/faction recuperer [ID]      " + ChatColor.GRAY + "Récupérer une annonce non vendue");
        player.sendMessage(ChatColor.GRAY + "— Organisation du coffre —");
        player.sendMessage(ChatColor.YELLOW + "/faction ranger              " + ChatColor.GRAY + "Organiser le coffre partagé (GUI de tri)");
        player.sendMessage(ChatColor.YELLOW + "/faction ranger perso        " + ChatColor.GRAY + "Organiser son inventaire personnel");
        player.sendMessage(ChatColor.GRAY + "Modes : similaires, catégorie, A→Z, quantité ↑↓, rareté");
        player.sendMessage(ChatColor.GRAY + "— Alliances —");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction alliance            " + ChatColor.GRAY + "Gérer les alliances (sous-commandes + GUI)");
        player.sendMessage(ChatColor.GRAY + "— Spawn de faction —");
        player.sendMessage(ChatColor.AQUA + "/faction setspawn [1|2]      " + ChatColor.GRAY + "Définir un spawn (chef) — §b2 spawns dès le rang ◆ Diamant");
        player.sendMessage(ChatColor.AQUA + "/faction spawn [1|2]         " + ChatColor.GRAY + "Se téléporter à un spawn de faction");
        player.sendMessage(ChatColor.GRAY + "— Homes perso —");
        player.sendMessage(ChatColor.GREEN + "/faction sethome [nom]       " + ChatColor.GRAY + "Définir un home (1 sans faction, 2 avec, 3 si allié)");
        player.sendMessage(ChatColor.GREEN + "/faction home [nom]          " + ChatColor.GRAY + "Se téléporter à un home");
        player.sendMessage(ChatColor.GREEN + "/faction delhome <nom>       " + ChatColor.GRAY + "Supprimer un home");
        player.sendMessage(ChatColor.GREEN + "/faction homes               " + ChatColor.GRAY + "Lister ses homes");
        player.sendMessage(ChatColor.GRAY + "— Coffres privés —");
        player.sendMessage(ChatColor.YELLOW + "Sneak + §eclic§7 panneau sur coffre " + ChatColor.GRAY + "Verrouiller/déverrouiller");
        player.sendMessage(ChatColor.GRAY + "— Villageois recrutés —");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction recruter            " + ChatColor.GRAY + "Recruter le villageois visé (chef/sous-chef)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction villageois          " + ChatColor.GRAY + "Gérer tes villageois recrutés (GUI)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction villageois ranger   " + ChatColor.GRAY + "Aligner tes villageois proches devant toi");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction village fonder <nom>" + ChatColor.GRAY + "Fonder un village depuis tes claims");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction village liste       " + ChatColor.GRAY + "Voir les villages de ta faction");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction port definir <village>" + ChatColor.GRAY + "Définir le port d'un village (niveau Ville)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction gare definir <village>" + ChatColor.GRAY + "Définir la gare d'un village (niveau Ville)");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/faction contrat creer ...   " + ChatColor.GRAY + "Créer un contrat commercial (voir /faction contrat)");
        player.sendMessage(ChatColor.GRAY + "— TP joueur —");
        player.sendMessage(ChatColor.AQUA + "/faction tpa <joueur>        " + ChatColor.GRAY + "Demande de TP vers un joueur");
        player.sendMessage(ChatColor.AQUA + "/faction tpaccept            " + ChatColor.GRAY + "Accepter une demande de TP");
        player.sendMessage(ChatColor.AQUA + "/faction tpdeny              " + ChatColor.GRAY + "Refuser une demande de TP");
        if (player.hasPermission("faction.admin")) {
            player.sendMessage(ChatColor.GRAY + "— Admin —");
            player.sendMessage(ChatColor.RED + "/faction invsee <joueur>     " + ChatColor.GRAY + "Voir l'inventaire d'un joueur (admin)");
        }
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
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create","disband","invite","join","leave","kick","setchef","souschef","rename",
                    "info","list","tp","coffre","menu","gui",
                    "top","topbanque","classement","rangs","power","stats","classementjoueurs",
                    "claim","unclaim","claims","claimmap","claimshow","map","claimallow","claimdeny","claimallies","perms",
                    "banque","troc","accepter",
                    "shop","vendre","acheter","recuperer","mesannonces",
                    "invsee","alliance","setspawn","spawn",
                    "sethome","home","delhome","homes",
                    "tpa","tpaccept","tpdeny",
                    "guerre","ranger","trier","organiser",
                    "recruter","villageois","village","port","gare","contrat"
            );
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "info", "join", "alliance" -> factionManager.getAllFactions().values().stream()
                        .map(Faction::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "invite", "kick", "setchef", "tp", "power", "stats", "troc", "invsee", "tpa" ->
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                case "souschef", "subchief" -> Arrays.asList("promouvoir","retirer","liste","limite").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "villageois", "villagers" -> Arrays.asList("ranger","formation").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "village" -> Arrays.asList("fonder","liste","dissoudre").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "port", "gare" -> Arrays.asList("definir").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "contrat" -> Arrays.asList("creer","liste","assigner","annuler").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "classementjoueurs", "cj" -> STATS_CATEGORIES.stream()
                        .filter(c -> c.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "home", "delhome" -> homeManager.getHomeNames(player.getUniqueId()).stream()
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "setspawn", "spawn" -> {
                    // Proposer "1" toujours, "2" seulement si rang Diamant+
                    Faction spFac = factionManager.getPlayerFaction(player.getUniqueId());
                    List<String> slots = new ArrayList<>();
                    slots.add("1");
                    if (spFac != null) {
                        fr.faction.ranking.FactionRank spRank = powerManager.getFactionRank(spFac.getName());
                        if (spRank.getMaxSpawns() >= 2) slots.add("2");
                    }
                    yield slots.stream().filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
                }
                case "sethome" -> {
                    // Suggérer home, home2…home5 selon la limite du rang
                    int max = homeManager.getMaxHomes(player.getUniqueId());
                    List<String> allNames = new ArrayList<>();
                    for (int i = 1; i <= max; i++) allNames.add(i == 1 ? "home" : "home" + i);
                    allNames.removeAll(homeManager.getHomeNames(player.getUniqueId()));
                    yield allNames.stream()
                            .filter(n -> n.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "vendre" -> Collections.emptyList(); // prix en arg 2
                case "guerre" -> Arrays.asList("declarer","accepter","refuser","capituler","statut","liste","piller")
                        .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "map", "minimap" -> Arrays.asList("partage").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "ranger", "trier", "organiser", "sort" ->
                        Arrays.asList("coffre","perso","inventaire").stream()
                                .filter(s -> s.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                case "claimallow", "claimdeny" -> factionManager.getAllFactions().values().stream()
                        .map(Faction::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "acheter", "recuperer" -> Collections.emptyList(); // IDs dynamiques
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "vendre" -> Arrays.asList("fer","or","diamant","emeraude").stream()
                        .filter(c -> c.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                case "guerre" -> {
                    if (args[1].equalsIgnoreCase("declarer")) {
                        yield Arrays.asList("claims:0","claims:1","claims:2","claims:3","claims:5",
                                "pillage","kills:10","kills:20","kills:30","kills:50").stream()
                                .filter(s -> s.startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield Collections.emptyList();
                }
                case "alliance" -> switch (args[1].toLowerCase()) {
                    case "inviter","accepter","refuser","rompre" -> factionManager.getAllFactions().values().stream()
                            .map(Faction::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    default -> Collections.emptyList();
                };
                case "souschef", "subchief" -> switch (args[1].toLowerCase()) {
                    case "promouvoir","promote","add" -> Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    case "retirer","demote","remove" -> {
                        Faction scFac = factionManager.getPlayerFaction(player.getUniqueId());
                        if (scFac == null) yield Collections.emptyList();
                        yield scFac.getSousChefs().stream()
                                .map(this::getPlayerName)
                                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    case "limite","limit" -> Arrays.asList("0","1","2").stream()
                            .filter(s -> s.startsWith(args[2]))
                            .collect(Collectors.toList());
                    default -> Collections.emptyList();
                };
                case "villageois", "villagers" -> switch (args[1].toLowerCase()) {
                    case "ranger","formation" -> Arrays.asList("ligne","cercle").stream()
                            .filter(s -> s.startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    default -> Collections.emptyList();
                };
                case "village" -> switch (args[1].toLowerCase()) {
                    case "dissoudre","disband","supprimer" -> {
                        Faction vFac = factionManager.getPlayerFaction(player.getUniqueId());
                        if (vFac == null || villageManager == null) yield Collections.emptyList();
                        yield villageManager.getFactionVillages(vFac.getName()).stream()
                                .map(fr.faction.village.Village::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    default -> Collections.emptyList();
                };
                case "port", "gare" -> {
                    if (!args[1].equalsIgnoreCase("definir")) yield Collections.emptyList();
                    Faction pFac = factionManager.getPlayerFaction(player.getUniqueId());
                    if (pFac == null || villageManager == null) yield Collections.emptyList();
                    yield villageManager.getFactionVillages(pFac.getName()).stream()
                            .map(fr.faction.village.Village::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "contrat" -> {
                    if (!args[1].equalsIgnoreCase("creer")) yield Collections.emptyList();
                    Faction cFac = factionManager.getPlayerFaction(player.getUniqueId());
                    if (cFac == null || villageManager == null) yield Collections.emptyList();
                    yield villageManager.getFactionVillages(cFac.getName()).stream()
                            .map(fr.faction.village.Village::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                default -> Collections.emptyList();
            };
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("guerre") && args[1].equalsIgnoreCase("declarer")) {
            return Arrays.asList("claims:0","claims:1","claims:2","claims:3","claims:5",
                    "pillage","kills:10","kills:20","kills:30","kills:50").stream()
                    .filter(s -> s.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
