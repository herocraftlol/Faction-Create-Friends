package fr.faction.commands;

import fr.faction.gui.FactionGUI;
import fr.faction.gui.FactionRankingGUI;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FactionCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final SharedInventoryManager sharedInvManager;
    private final FactionTeleportManager teleportManager;
    private final FactionGUI factionGUI;
    private final FactionRankingGUI rankingGUI;

    public FactionCommand(JavaPlugin plugin, FactionManager factionManager,
                          SharedInventoryManager sharedInvManager,
                          FactionTeleportManager teleportManager,
                          FactionGUI factionGUI, FactionRankingGUI rankingGUI) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        this.factionGUI = factionGUI;
        this.rankingGUI = rankingGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }

        if (args.length == 0) { factionGUI.openMainMenu(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create"   -> handleCreate(player, args);
            case "disband"  -> handleDisband(player);
            case "invite"   -> handleInvite(player, args);
            case "join"     -> handleJoin(player, args);
            case "leave"    -> handleLeave(player);
            case "setchef"  -> handleSetChef(player, args);
            case "info"     -> handleInfo(player, args);
            case "list"     -> handleList(player);
            case "kick"     -> handleKick(player, args);
            case "coffre", "chest" -> sharedInvManager.openSharedInventory(player);
            case "tp"       -> handleTp(player, args);
            case "menu", "gui" -> factionGUI.openMainMenu(player);
            case "classement", "top", "ranking" -> rankingGUI.openRankingGUI(player);
            case "rangs", "ranks" -> rankingGUI.openRankInfoGUI(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction create <nom>"); return; }
        if (factionManager.isInFaction(player.getUniqueId())) { player.sendMessage(prefix() + msg("already-in-faction")); return; }
        String name = args[1];
        int min = plugin.getConfig().getInt("faction.min-name-length", 3);
        int max = plugin.getConfig().getInt("faction.max-name-length", 20);
        if (name.length() < min) { player.sendMessage(prefix() + msg("name-too-short").replace("%min%", "" + min)); return; }
        if (name.length() > max) { player.sendMessage(prefix() + msg("name-too-long").replace("%max%", "" + max)); return; }
        if (!name.matches("[a-zA-Z0-9_\\-]+")) { player.sendMessage(prefix() + ChatColor.RED + "Nom invalide."); return; }
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
            if (m != null && !m.equals(player)) m.sendMessage(prefix() + ChatColor.RED + "La faction " + name + " a ete dissoute.");
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
        if (target.equals(player)) { player.sendMessage(prefix() + msg("cannot-invite-self")); return; }
        if (factionManager.isInFaction(target.getUniqueId())) { player.sendMessage(prefix() + msg("player-already-in-faction").replace("%player%", target.getName())); return; }
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
            player.sendMessage(prefix() + ChatColor.RED + "Transfere d abord le role chef : /faction setchef <joueur>");
            return;
        }
        String name = faction.getName();
        notifyMembers(faction, player, ChatColor.YELLOW + player.getName() + " a quitte la faction.");
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
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + target.getName() + " n est pas dans ta faction."); return; }
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
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + args[1] + " n est pas dans ta faction."); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas te kick toi-meme."); return; }
        String tName = target.getName() != null ? target.getName() : args[1];
        factionManager.removeMember(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + ChatColor.YELLOW + tName + " expulse de la faction.");
        if (target.isOnline() && target.getPlayer() != null) target.getPlayer().sendMessage(prefix() + ChatColor.RED + "Tu as ete expulse de la faction " + faction.getName() + ".");
        notifyMembers(faction, player, ChatColor.RED + tName + " a ete expulse de la faction.");
    }

    private void handleTp(Player player, String[] args) {
        if (args.length < 2) teleportManager.teleportToNearest(player);
        else teleportManager.teleportToMember(player, args[1]);
    }

    private void handleInfo(Player player, String[] args) {
        Faction faction = args.length >= 2 ? factionManager.getFaction(args[1]) : factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + (args.length >= 2 ? msg("faction-not-found") : msg("not-in-faction"))); return; }
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + faction.getName() + ChatColor.GOLD + " ======");
        player.sendMessage(ChatColor.GRAY + "Chef : " + ChatColor.WHITE + getPlayerName(faction.getChef()));
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
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Factions (" + all.size() + ")" + ChatColor.GOLD + " ======");
        for (Faction f : all.values()) {
            long online = f.getMembers().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline()).count();
            player.sendMessage(ChatColor.YELLOW + f.getName() + ChatColor.GRAY + " - " + f.getMemberCount() + " membres " + ChatColor.GREEN + "(" + online + " en ligne)");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Aide Factions" + ChatColor.GOLD + " ======");
        player.sendMessage(ChatColor.YELLOW + "/faction " + ChatColor.GRAY + "- Menu graphique");
        player.sendMessage(ChatColor.YELLOW + "/faction create <nom>" + ChatColor.GRAY + " - Creer");
        player.sendMessage(ChatColor.YELLOW + "/faction invite/join/leave" + ChatColor.GRAY + " - Gestion membres");
        player.sendMessage(ChatColor.YELLOW + "/faction tp [joueur]" + ChatColor.GRAY + " - Teleportation");
        player.sendMessage(ChatColor.YELLOW + "/faction coffre" + ChatColor.GRAY + " - Coffre partage");
        player.sendMessage(ChatColor.YELLOW + "/faction classement" + ChatColor.GRAY + " - Classement factions");
        player.sendMessage(ChatColor.YELLOW + "/faction rangs" + ChatColor.GRAY + " - Guide des rangs");
        player.sendMessage(ChatColor.YELLOW + "/power" + ChatColor.GRAY + " - Votre puissance individuelle");
        player.sendMessage(ChatColor.YELLOW + "/power top" + ChatColor.GRAY + " - Top 10 factions");
    }

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

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    private String msg(String key) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + key, "&cMessage manquant: " + key));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return Arrays.asList("create","disband","invite","join","leave","kick","setchef","info","list","tp","coffre","classement","rangs","menu");
        if (args.length == 2) switch (args[0].toLowerCase()) {
            case "info","join" -> { List<String> l = new ArrayList<>(); factionManager.getAllFactions().values().forEach(f -> l.add(f.getName())); return l; }
            case "invite","kick","setchef","tp" -> { List<String> l = new ArrayList<>(); Bukkit.getOnlinePlayers().forEach(p -> l.add(p.getName())); return l; }
        }
        return Collections.emptyList();
    }
}
