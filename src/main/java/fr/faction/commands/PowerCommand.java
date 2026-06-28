package fr.faction.commands;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.PlayerPowerCalculator;
import fr.faction.ranking.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /power [joueur|faction]
 *   /power            → ma puissance individuelle + faction
 *   /power <joueur>   → puissance d'un joueur
 *   /power faction [nom] → puissance et rang d'une faction
 *   /power top        → classement texte des factions
 */
public class PowerCommand implements CommandExecutor, TabCompleter {

    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;

    public PowerCommand(FactionManager factionManager, FactionPowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Specifiez un joueur : /power <joueur>");
                return true;
            }
            showPlayerPower(player, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "top" -> showTop(sender);
            case "faction", "f" -> {
                String name = args.length >= 2 ? args[1] : (sender instanceof Player p
                        ? Optional.ofNullable(factionManager.getPlayerFaction(p.getUniqueId()))
                        .map(Faction::getName).orElse(null) : null);
                if (name == null) { sender.sendMessage(ChatColor.RED + "Pas de faction. Usage: /power faction <nom>"); return true; }
                showFactionPower(sender, name);
            }
            default -> {
                // Chercher un joueur
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null && sender instanceof Player self) {
                    showPlayerPower(self, target);
                } else {
                    sender.sendMessage(ChatColor.RED + "Joueur introuvable ou usage: /power [top|faction <nom>|<joueur>]");
                }
            }
        }
        return true;
    }

    private void showPlayerPower(CommandSender sender, Player target) {
        double pi = powerManager.getPlayerPower(target.getUniqueId());
        PlayerPowerCalculator.PowerBreakdown bd = powerManager.getPlayerBreakdown(target.getUniqueId());
        Faction faction = factionManager.getPlayerFaction(target.getUniqueId());

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══════════ " + ChatColor.YELLOW + "⚡ Puissance de " + target.getName() + ChatColor.GOLD + " ══════════");
        sender.sendMessage(ChatColor.GRAY + "  Puissance individuelle : " + ChatColor.GOLD + ChatColor.BOLD + format(pi) + " ⚡");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Breakdown :");
        sender.sendMessage(ChatColor.RED   + "    ⚔ PvP          : " + ChatColor.WHITE + format(bd.pvp()));
        sender.sendMessage(ChatColor.YELLOW + "    ⛏ Survie       : " + ChatColor.WHITE + format(bd.survie()));
        sender.sendMessage(ChatColor.GREEN  + "    ★ Progression  : " + ChatColor.WHITE + format(bd.progression()));
        sender.sendMessage(ChatColor.AQUA   + "    ⏱ Activite     : " + ChatColor.WHITE + format(bd.activite()));

        if (faction != null) {
            FactionRank rank = powerManager.getFactionRank(faction.getName());
            double fp = powerManager.getFactionPower(faction.getName());
            int pos = powerManager.getFactionPosition(faction.getName());
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "  Faction : " + ChatColor.WHITE + ChatColor.BOLD + faction.getName());
            sender.sendMessage(ChatColor.GRAY + "  Rang faction : " + rank.getLabel());
            sender.sendMessage(ChatColor.GRAY + "  Puissance globale : " + ChatColor.GOLD + format(fp) + " ⚡");
            sender.sendMessage(ChatColor.GRAY + "  Classement : " + ChatColor.WHITE + "#" + pos);
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "  " + rank.progressBar(fp));
        }
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════════════");
        sender.sendMessage("");
    }

    private void showFactionPower(CommandSender sender, String factionName) {
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) { sender.sendMessage(ChatColor.RED + "Faction introuvable."); return; }

        FactionRank rank = powerManager.getFactionRank(factionName);
        double power = powerManager.getFactionPower(factionName);
        int pos = powerManager.getFactionPosition(factionName);
        FactionRank next = rank.next();

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══════════ " + rank.getLabelBold() + ChatColor.GOLD + " " + factionName.toUpperCase() + ChatColor.GOLD + " ══════════");
        sender.sendMessage(ChatColor.GRAY + "  Puissance globale : " + ChatColor.GOLD + ChatColor.BOLD + format(power) + " ⚡");
        sender.sendMessage(ChatColor.GRAY + "  Classement : " + ChatColor.WHITE + "#" + pos
                + ChatColor.DARK_GRAY + " (" + factionManager.getAllFactions().size() + " factions)");
        sender.sendMessage(ChatColor.GRAY + "  Membres : " + ChatColor.WHITE + faction.getMemberCount());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  Progression : " + rank.progressBar(power));
        if (next != null) {
            sender.sendMessage(ChatColor.DARK_GRAY + "  Vers " + next.getLabel()
                    + ChatColor.DARK_GRAY + " : " + format(next.puissanceMin - power) + " ⚡ manquants");
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "  Avantages actifs :");
        for (String av : rank.avantages) {
            sender.sendMessage(ChatColor.YELLOW + "    › " + ChatColor.WHITE + av);
        }
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════════════");
        sender.sendMessage("");
    }

    private void showTop(CommandSender sender) {
        List<Map.Entry<String, Double>> lb = powerManager.getLeaderboard();
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "══════════ " + ChatColor.YELLOW + "⚡ TOP Factions" + ChatColor.GOLD + " ══════════");
        if (lb.isEmpty()) { sender.sendMessage(ChatColor.GRAY + "  Aucune faction enregistree."); }
        for (int i = 0; i < Math.min(10, lb.size()); i++) {
            Map.Entry<String, Double> e = lb.get(i);
            FactionRank rank = powerManager.getFactionRank(e.getKey());
            String medal = i == 0 ? ChatColor.GOLD + "①" : i == 1 ? ChatColor.WHITE + "②" : i == 2 ? ChatColor.GOLD + "③" : ChatColor.GRAY + "#" + (i + 1);
            sender.sendMessage("  " + medal + " " + rank.getLabel() + ChatColor.WHITE + " " + e.getKey()
                    + ChatColor.GRAY + " — " + ChatColor.GOLD + format(e.getValue()) + " ⚡");
        }
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════════════");
        sender.sendMessage("");
    }

    private String format(double val) {
        if (val >= 1000) return String.format("%.1fk", val / 1000);
        return String.format("%.1f", val);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return Arrays.asList("top", "faction").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("faction")) {
            return factionManager.getAllFactions().keySet().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
