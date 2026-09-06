package fr.faction.web;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /lier — génère un code à 6 chiffres pour lier son compte Minecraft
 * à un compte web sur le site HeroCraft.
 *
 * Usage :
 *   /lier          → affiche le code + instructions
 *   /lier statut   → indique si le compte est déjà lié
 */
public class LierCommand implements CommandExecutor {

    private final WebLinkManager linkManager;
    private final String siteUrl;

    public LierCommand(WebLinkManager linkManager, String siteUrl) {
        this.linkManager = linkManager;
        this.siteUrl     = siteUrl.replaceAll("/$", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande est réservée aux joueurs.");
            return true;
        }

        if (!linkManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "❌ Le système de liaison n'est pas configuré.");
            player.sendMessage(ChatColor.GRAY + "Vérifie la section §emysql:§7 dans "
                    + "§eplugins/FactionPlugin/config.yml§7.");
            return true;
        }

        // /lier statut
        if (args.length >= 1 && args[0].equalsIgnoreCase("statut")) {
            String webPseudo = linkManager.getLinkedWebPseudo(player.getUniqueId());
            if (webPseudo != null) {
                player.sendMessage("");
                player.sendMessage(ChatColor.GREEN + "✔ Compte Minecraft lié au compte site : "
                        + ChatColor.YELLOW + webPseudo);
                player.sendMessage(ChatColor.GRAY + "  Carte : §b" + siteUrl + "/faction.html");
                player.sendMessage("");
            } else {
                player.sendMessage(ChatColor.YELLOW + "⚠ Pas encore lié. Tape §e/lier §epour obtenir un code.");
            }
            return true;
        }

        // /lier → générer le code (async pour ne pas bloquer le thread principal)
        player.sendMessage(ChatColor.GRAY + "Génération du code…");

        Bukkit.getScheduler().runTaskAsynchronously(
                Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                () -> {
                    final String code = linkManager.generateCode(player.getUniqueId(), player.getName());

                    Bukkit.getScheduler().runTask(
                            Bukkit.getPluginManager().getPlugin("FactionPlugin"),
                            () -> {
                                if (!player.isOnline()) return;

                                if (code == null) {
                                    player.sendMessage(ChatColor.RED + "❌ Impossible de générer un code.");
                                    player.sendMessage(ChatColor.GRAY + "MySQL inaccessible — contacte un admin.");
                                    return;
                                }

                                String sep = ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
                                player.sendMessage(sep);
                                player.sendMessage(ChatColor.GOLD + "⬡ " + ChatColor.YELLOW
                                        + ChatColor.BOLD + "Liaison compte HeroCraft");
                                player.sendMessage("");
                                player.sendMessage(ChatColor.GRAY + "  Ton code : "
                                        + ChatColor.YELLOW + ChatColor.BOLD + "  " + code + "  ");
                                player.sendMessage("");
                                player.sendMessage(ChatColor.WHITE + "  1. " + ChatColor.GRAY
                                        + "Va sur §b" + siteUrl + "/faction.html");
                                player.sendMessage(ChatColor.WHITE + "  2. " + ChatColor.GRAY
                                        + "Connecte-toi (ou crée un compte)");
                                player.sendMessage(ChatColor.WHITE + "  3. " + ChatColor.GRAY
                                        + "Entre le code §e" + code + " §7dans le champ de liaison");
                                player.sendMessage(ChatColor.WHITE + "  4. " + ChatColor.GRAY
                                        + "Clique §b🔗 Lier le compte");
                                player.sendMessage("");
                                player.sendMessage(ChatColor.DARK_GRAY + "⏱ Expire dans 10 minutes. "
                                        + "§8/lier statut §8pour vérifier.");
                                player.sendMessage(sep);

                                player.playSound(player.getLocation(),
                                        Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
                            }
                    );
                }
        );

        return true;
    }
}
