package fr.faction.commands;

import fr.faction.gui.FactionGUI;
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

    public FactionCommand(JavaPlugin plugin, FactionManager factionManager,
                          SharedInventoryManager sharedInvManager,
                          FactionTeleportManager teleportManager,
                          FactionGUI factionGUI) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        this.factionGUI = factionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            factionGUI.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":   handleCreate(player, args);   break;
            case "disband":  handleDisband(player);        break;
            case "invite":   handleInvite(player, args);   break;
            case "join":     handleJoin(player, args);     break;
            case "leave":    handleLeave(player);          break;
            case "setchef":  handleSetChef(player, args);  break;
            case "info":     handleInfo(player, args);     break;
            case "list":     handleList(player);           break;
            case "kick":     handleKick(player, args);     break;
            case "chest":
            case "coffrePartage":
            case "coffre":   sharedInvManager.openSharedInventory(player); break;
            case "tp":       handleTp(player, args);       break;
            case "menu":
            case "gui":      factionGUI.openMainMenu(player); break;
            default:         sendHelp(player);             break;
        }
        return true;
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction create <nom>"); return; }
        if (factionManager.isInFaction(player.getUniqueId())) { player.sendMessage(prefix() + msg("already-in-faction")); return; }

        String name = args[1];
        int min = plugin.getConfig().getInt("faction.min-name-length", 3);
        int max = plugin.getConfig().getInt("faction.max-name-length", 20);
        if (name.length() < min) { player.sendMessage(prefix() + msg("name-too-short").replace("%min%", String.valueOf(min))); return; }
        if (name.length() > max) { player.sendMessage(prefix() + msg("name-too-long").replace("%max%", String.valueOf(max)));  return; }
        if (!name.matches("[a-zA-Z0-9_\\-]+")) { player.sendMessage(prefix() + ChatColor.RED + "Nom invalide (lettres, chiffres, _ et - uniquement)."); return; }
        if (!factionManager.createFaction(name, player.getUniqueId())) { player.sendMessage(prefix() + msg("faction-already-exists")); return; }
        player.sendMessage(prefix() + msg("faction-created").replace("%name%", name));
    }

    // ─── DISBAND ────────────────────────────────────────────────────────────────

    private void handleDisband(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null)                    { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        String name = faction.getName();
        for (UUID uuid : new ArrayList<>(faction.getMembers())) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !m.equals(player))
                m.sendMessage(prefix() + ChatColor.RED + "La faction " + ChatColor.YELLOW + name + ChatColor.RED + " a ete dissoute.");
        }
        sharedInvManager.deleteFactionInventory(name);
        factionManager.disbandFaction(name);
        player.sendMessage(prefix() + msg("faction-disbanded").replace("%name%", name));
    }

    // ─── INVITE ─────────────────────────────────────────────────────────────────

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction invite <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null)                       { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null)                        { player.sendMessage(prefix() + msg("player-not-found")); return; }
        if (target.equals(player))                 { player.sendMessage(prefix() + msg("cannot-invite-self")); return; }
        if (factionManager.isInFaction(target.getUniqueId())) { player.sendMessage(prefix() + msg("player-already-in-faction").replace("%player%", target.getName())); return; }
        int maxMembers = plugin.getConfig().getInt("faction.max-members", 50);
        if (faction.getMemberCount() >= maxMembers) { player.sendMessage(prefix() + msg("faction-full")); return; }
        factionManager.addInvite(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + msg("invite-sent").replace("%player%", target.getName()));
        target.sendMessage(prefix() + msg("invite-received").replace("%player%", player.getName()).replace("%faction%", faction.getName()));
    }

    // ─── JOIN ───────────────────────────────────────────────────────────────────

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction join <nom>"); return; }
        if (factionManager.isInFaction(player.getUniqueId())) { player.sendMessage(prefix() + msg("already-in-faction")); return; }
        String factionName = args[1];
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null)                           { player.sendMessage(prefix() + msg("faction-not-found")); return; }
        if (!faction.hasInvite(player.getUniqueId())) { player.sendMessage(prefix() + msg("no-invite")); return; }
        int maxMembers = plugin.getConfig().getInt("faction.max-members", 50);
        if (faction.getMemberCount() >= maxMembers)    { player.sendMessage(prefix() + msg("faction-full")); return; }
        factionManager.addMember(factionName, player.getUniqueId());
        player.sendMessage(prefix() + msg("joined-faction").replace("%name%", faction.getName()));
        notifyMembers(faction, player, ChatColor.GREEN + player.getName() + " a rejoint la faction !");
    }

    // ─── LEAVE ──────────────────────────────────────────────────────────────────

    private void handleLeave(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (faction.isChef(player.getUniqueId()) && faction.getMemberCount() > 1) {
            player.sendMessage(prefix() + ChatColor.RED + "Transfere d abord le role de chef avec /faction setchef <joueur>.");
            return;
        }
        String name = faction.getName();
        notifyMembers(faction, player, ChatColor.YELLOW + player.getName() + " a quitte la faction.");
        factionManager.removeMember(name, player.getUniqueId());
        player.sendMessage(prefix() + msg("left-faction").replace("%name%", name));
    }

    // ─── SET CHEF ───────────────────────────────────────────────────────────────

    private void handleSetChef(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction setchef <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null)                       { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null)                        { player.sendMessage(prefix() + msg("player-not-found")); return; }
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + target.getName() + " n est pas dans ta faction."); return; }
        factionManager.setChef(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + msg("chef-set").replace("%player%", target.getName()));
        target.sendMessage(prefix() + ChatColor.GOLD + "Tu es maintenant chef de la faction " + faction.getName() + " !");
        notifyMembers(faction, player, ChatColor.GOLD + target.getName() + " est le nouveau chef !", target);
    }

    // ─── KICK ───────────────────────────────────────────────────────────────────

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(prefix() + ChatColor.RED + "Usage: /faction kick <joueur>"); return; }
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null)                       { player.sendMessage(prefix() + msg("not-in-faction")); return; }
        if (!faction.isChef(player.getUniqueId())) { player.sendMessage(prefix() + msg("not-chef")); return; }
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!faction.isMember(target.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + args[1] + " n est pas dans ta faction."); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage(prefix() + ChatColor.RED + "Tu ne peux pas te kick toi-meme."); return; }
        String tName = target.getName() != null ? target.getName() : args[1];
        factionManager.removeMember(faction.getName(), target.getUniqueId());
        player.sendMessage(prefix() + ChatColor.YELLOW + tName + " a ete exclu de la faction.");
        if (target.isOnline() && target.getPlayer() != null)
            target.getPlayer().sendMessage(prefix() + ChatColor.RED + "Tu as ete exclu de la faction " + faction.getName() + ".");
        notifyMembers(faction, player, ChatColor.RED + tName + " a ete exclu de la faction.");
    }

    // ─── TP ─────────────────────────────────────────────────────────────────────

    private void handleTp(Player player, String[] args) {
        if (args.length < 2) {
            teleportManager.teleportToNearest(player);
        } else {
            teleportManager.teleportToMember(player, args[1]);
        }
    }

    // ─── INFO ───────────────────────────────────────────────────────────────────

    private void handleInfo(Player player, String[] args) {
        Faction faction = args.length >= 2 ? factionManager.getFaction(args[1])
                : factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(prefix() + (args.length >= 2 ? msg("faction-not-found") : msg("not-in-faction")));
            return;
        }
        String chefName = getPlayerName(faction.getChef());
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + faction.getName() + ChatColor.GOLD + " ======");
        player.sendMessage(ChatColor.GRAY + "Chef : " + ChatColor.WHITE + chefName);
        player.sendMessage(ChatColor.GRAY + "Membres (" + faction.getMemberCount() + ") :");
        for (UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            String status = (m != null && m.isOnline()) ? ChatColor.GREEN + "● " : ChatColor.DARK_GRAY + "○ ";
            String tag = uuid.equals(faction.getChef()) ? ChatColor.GOLD + " [Chef]" : "";
            player.sendMessage("  " + status + ChatColor.WHITE + getPlayerName(uuid) + tag);
        }
    }

    // ─── LIST ───────────────────────────────────────────────────────────────────

    private void handleList(Player player) {
        Map<String, Faction> all = factionManager.getAllFactions();
        if (all.isEmpty()) { player.sendMessage(prefix() + ChatColor.GRAY + "Aucune faction existante."); return; }
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Factions (" + all.size() + ")" + ChatColor.GOLD + " ======");
        for (Faction f : all.values()) {
            long online = f.getMembers().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline()).count();
            player.sendMessage(ChatColor.YELLOW + f.getName() + ChatColor.GRAY + " - " + f.getMemberCount()
                    + " membre(s) " + ChatColor.GREEN + "(" + online + " en ligne)");
        }
    }

    // ─── HELP ───────────────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "====== " + ChatColor.YELLOW + "Aide Factions" + ChatColor.GOLD + " ======");
        player.sendMessage(ChatColor.YELLOW + "/faction" + ChatColor.GRAY + " ou " + ChatColor.YELLOW + "/f" + ChatColor.GRAY + " - Ouvre le menu graphique");
        player.sendMessage(ChatColor.YELLOW + "/faction create <nom>" + ChatColor.GRAY + " - Creer une faction");
        player.sendMessage(ChatColor.YELLOW + "/faction disband" + ChatColor.GRAY + " - Dissoudre ta faction");
        player.sendMessage(ChatColor.YELLOW + "/faction invite <joueur>" + ChatColor.GRAY + " - Inviter un joueur");
        player.sendMessage(ChatColor.YELLOW + "/faction join <nom>" + ChatColor.GRAY + " - Rejoindre une faction");
        player.sendMessage(ChatColor.YELLOW + "/faction leave" + ChatColor.GRAY + " - Quitter ta faction");
        player.sendMessage(ChatColor.YELLOW + "/faction kick <joueur>" + ChatColor.GRAY + " - Expulser un membre");
        player.sendMessage(ChatColor.YELLOW + "/faction setchef <joueur>" + ChatColor.GRAY + " - Nommer chef");
        player.sendMessage(ChatColor.YELLOW + "/faction tp [joueur]" + ChatColor.GRAY + " - Teleportation (le plus proche si sans arg)");
        player.sendMessage(ChatColor.YELLOW + "/faction coffre" + ChatColor.GRAY + " - Coffre partage de la faction");
        player.sendMessage(ChatColor.YELLOW + "/faction info [nom]" + ChatColor.GRAY + " - Infos d une faction");
        player.sendMessage(ChatColor.YELLOW + "/faction list" + ChatColor.GRAY + " - Lister les factions");
    }

    // ─── UTILS ──────────────────────────────────────────────────────────────────

    private void notifyMembers(Faction faction, Player exclude, String message, Player... extra) {
        Set<Player> excluded = new HashSet<>(Arrays.asList(extra));
        excluded.add(exclude);
        for (UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null && !excluded.contains(m)) m.sendMessage(prefix() + message);
        }
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&6Faction&8] &r"));
    }

    private String msg(String key) {
        String raw = plugin.getConfig().getString("messages." + key, "&cMessage manquant: " + key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return Arrays.asList("create", "disband", "invite", "join", "leave", "kick", "setchef",
                    "info", "list", "tp", "coffre", "menu");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info": case "join":
                    List<String> names = new ArrayList<>();
                    factionManager.getAllFactions().values().forEach(f -> names.add(f.getName()));
                    return names;
                case "invite": case "kick": case "setchef": case "tp":
                    List<String> players = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
                    return players;
            }
        }
        return Collections.emptyList();
    }
}
