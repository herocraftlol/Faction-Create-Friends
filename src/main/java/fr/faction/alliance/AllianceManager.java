package fr.faction.alliance;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Gère les alliances entre factions.
 *
 * Avantages des alliances (proportionnels au nombre d'alliés) :
 *  1 allié → +500 power bonus pour les deux factions
 *  2 alliés → +1 200 power bonus
 *  3 alliés → +2 500 power bonus
 *  4+ alliés → +500 supplémentaires par allié
 *
 * Le bonus est injecté dans FactionPowerManager via getAlliancePowerBonus().
 */
public class AllianceManager implements Listener {

    private static final String TITLE_ALLIES    = "§8§l[§d§lAlliances§8§l] §f";
    private static final String TITLE_INVITES   = "§8§l[§d§lInvitations§8§l] §f";

    private static final double[] ALLY_BONUS = { 0, 500, 1200, 2500 };

    private final JavaPlugin plugin;
    private final FactionManager factionManager;

    public AllianceManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
    }

    // ─── BONUS POWER ────────────────────────────────────────────────────────────

    public double getAlliancePowerBonus(String factionName) {
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) return 0;
        int count = faction.getAllyCount();
        if (count == 0) return 0;
        if (count < ALLY_BONUS.length) return ALLY_BONUS[count];
        return ALLY_BONUS[ALLY_BONUS.length - 1] + (count - (ALLY_BONUS.length - 1)) * 500.0;
    }

    // ─── COMMANDES ──────────────────────────────────────────────────────────────

    /** /faction alliance inviter <faction> */
    public void handleAllianceInvite(Player player, String targetName) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }
        if (!myFaction.isChef(player.getUniqueId())) { send(player, "§cSeul le chef peut inviter des alliés."); return; }

        Faction target = factionManager.getFaction(targetName);
        if (target == null) { send(player, "§cFaction §f" + targetName + " §cintrouvable."); return; }
        if (myFaction.getName().equalsIgnoreCase(targetName)) { send(player, "§cTu ne peux pas t'allier à toi-même."); return; }
        if (myFaction.isAlly(targetName)) { send(player, "§cVous êtes déjà alliés avec §f" + target.getName() + "§c."); return; }
        if (target.hasPendingAllianceFrom(myFaction.getName())) { send(player, "§cUne invitation a déjà été envoyée à §f" + target.getName() + "§c."); return; }

        factionManager.sendAllianceInvite(myFaction.getName(), targetName);
        send(player, "§aInvitation d'alliance envoyée à §e" + target.getName() + "§a !");

        // Notifier le chef de la faction cible
        Player targetChef = Bukkit.getPlayer(target.getChef());
        if (targetChef != null) {
            targetChef.sendMessage(prefix() + "§e" + myFaction.getName()
                    + " §avous propose une alliance ! Tape §b/faction alliance accepter " + myFaction.getName()
                    + " §aou §c/faction alliance refuser " + myFaction.getName() + "§a.");
        }
    }

    /** /faction alliance accepter <faction> */
    public void handleAllianceAccept(Player player, String inviterName) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }
        if (!myFaction.isChef(player.getUniqueId())) { send(player, "§cSeul le chef peut accepter une alliance."); return; }

        if (!myFaction.hasPendingAllianceFrom(inviterName)) {
            send(player, "§cAucune invitation d'alliance de §f" + inviterName + "§c."); return;
        }

        boolean ok = factionManager.acceptAlliance(myFaction.getName(), inviterName);
        if (!ok) { send(player, "§cErreur lors de la création de l'alliance."); return; }

        Faction inviter = factionManager.getFaction(inviterName);
        send(player, "§aAlliance conclue avec §e" + (inviter != null ? inviter.getName() : inviterName) + " §a! 🤝");

        // Notifier l'autre faction
        if (inviter != null) {
            notifyFaction(inviter, "§aAlliance acceptée par §e" + myFaction.getName() + " §a! 🤝");
            double bonus = getAlliancePowerBonus(myFaction.getName());
            if (bonus > 0) {
                send(player, "§7Bonus de puissance alliés : §6+" + (int) bonus);
                notifyFaction(inviter, "§7Bonus de puissance alliés : §6+" + (int) getAlliancePowerBonus(inviterName));
            }
        }
    }

    /** /faction alliance refuser <faction> */
    public void handleAllianceDecline(Player player, String inviterName) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }
        if (!myFaction.isChef(player.getUniqueId())) { send(player, "§cSeul le chef peut refuser une alliance."); return; }
        if (!myFaction.hasPendingAllianceFrom(inviterName)) {
            send(player, "§cAucune invitation d'alliance de §f" + inviterName + "§c."); return;
        }
        myFaction.removePendingAlliance(inviterName);
        factionManager.saveFactions();
        send(player, "§cInvitation d'alliance de §f" + inviterName + " §crefusée.");
        Faction inviter = factionManager.getFaction(inviterName);
        if (inviter != null) notifyFaction(inviter, "§c" + myFaction.getName() + " a refusé votre invitation d'alliance.");
    }

    /** /faction alliance rompre <faction> */
    public void handleAllianceBreak(Player player, String targetName) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }
        if (!myFaction.isChef(player.getUniqueId())) { send(player, "§cSeul le chef peut rompre une alliance."); return; }
        if (!myFaction.isAlly(targetName)) { send(player, "§cVous n'êtes pas alliés avec §f" + targetName + "§c."); return; }

        Faction target = factionManager.getFaction(targetName);
        factionManager.breakAlliance(myFaction.getName(), targetName);
        send(player, "§cAlliance rompue avec §f" + targetName + "§c.");
        if (target != null) notifyFaction(target, "§c" + myFaction.getName() + " a rompu l'alliance avec vous !");
    }

    /** /faction alliance liste — affiche les alliés en chat */
    public void handleAllianceList(Player player) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "══ §dAlliances de §f" + myFaction.getName() + ChatColor.LIGHT_PURPLE + " ══");
        if (myFaction.getAllies().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Aucune alliance active.");
        } else {
            for (String ally : myFaction.getAllies()) {
                Faction allyF = factionManager.getFaction(ally);
                if (allyF == null) continue;
                long online = allyF.getMembers().stream()
                        .filter(u -> Bukkit.getPlayer(u) != null).count();
                player.sendMessage(ChatColor.LIGHT_PURPLE + "  🤝 §f" + allyF.getName()
                        + ChatColor.GRAY + " — " + allyF.getMemberCount() + " membre(s)"
                        + ChatColor.GREEN + " (" + online + " en ligne)"
                        + ChatColor.GOLD + " [+" + (int) getAlliancePowerBonus(myFaction.getName()) + " pw/total]");
            }
        }
        if (!myFaction.getPendingAllianceInvites().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "  Invitations en attente :");
            for (String pa : myFaction.getPendingAllianceInvites()) {
                player.sendMessage(ChatColor.YELLOW + "  ⏳ " + pa
                        + " §7— /faction alliance accepter " + pa + " §7ou refuser");
            }
        }
        double bonus = getAlliancePowerBonus(myFaction.getName());
        player.sendMessage(ChatColor.GOLD + "  Bonus de puissance total : §6+" + (int) bonus);
    }

    /** /faction alliance gui — ouvre le GUI des alliances */
    public void openAllianceGUI(Player player) {
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) { send(player, "§cTu n'es pas dans une faction."); return; }

        String title = TITLE_ALLIES + myFaction.getName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Slots 0-44 : alliés actuels
        int slot = 0;
        for (String allyKey : myFaction.getAllies()) {
            if (slot >= 45) break;
            Faction ally = factionManager.getFaction(allyKey);
            if (ally == null) continue;
            ItemStack item = new ItemStack(Material.LIME_BANNER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "🤝 " + ally.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Membres : " + ally.getMemberCount());
                long on = ally.getMembers().stream().filter(u -> Bukkit.getPlayer(u) != null).count();
                lore.add(ChatColor.GREEN + "En ligne : " + on);
                lore.add("");
                if (myFaction.isChef(player.getUniqueId()))
                    lore.add(ChatColor.RED + "[Clic droit] Rompre l'alliance");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Invitations reçues (slots 45-48)
        int pendSlot = 45;
        for (String pa : myFaction.getPendingAllianceInvites()) {
            if (pendSlot >= 50) break;
            Faction inviter = factionManager.getFaction(pa);
            if (inviter == null) continue;
            ItemStack item = new ItemStack(Material.YELLOW_BANNER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "⏳ Invitation de " + inviter.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "[Clic gauche] Accepter");
                lore.add(ChatColor.RED + "[Clic droit] Refuser");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(pendSlot++, item);
        }

        // Info bonus (slot 49)
        double bonus = getAlliancePowerBonus(myFaction.getName());
        ItemStack info = makeItem(Material.NETHER_STAR,
                ChatColor.GOLD + "Bonus de puissance : §6+" + (int) bonus,
                ChatColor.GRAY + "1 allié → +500 power",
                ChatColor.GRAY + "2 alliés → +1200 power",
                ChatColor.GRAY + "3 alliés → +2500 power",
                ChatColor.GRAY + "4+ → +500 par allié sup.");
        inv.setItem(49, info);

        // Fermer (slot 53)
        inv.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Fermer", ""));
        player.openInventory(inv);
    }

    // ─── GUI EVENT ──────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE_ALLIES)) return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction == null) return;

        ItemStack clicked = e.getCurrentItem();
        String displayName = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";

        if (clicked.getType() == Material.LIME_BANNER) {
            // Allié actuel
            String allyName = ChatColor.stripColor(displayName).replace("🤝 ", "").trim();
            if (e.isRightClick() && myFaction.isChef(player.getUniqueId())) {
                handleAllianceBreak(player, allyName);
                player.closeInventory();
            }
        } else if (clicked.getType() == Material.YELLOW_BANNER) {
            // Invitation en attente
            String inviterName = ChatColor.stripColor(displayName).replace("⏳ Invitation de ", "").trim();
            if (e.isLeftClick())  { handleAllianceAccept(player, inviterName); player.closeInventory(); }
            if (e.isRightClick()) { handleAllianceDecline(player, inviterName); player.closeInventory(); }
        } else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    // ─── UTILS ──────────────────────────────────────────────────────────────────

    private void notifyFaction(Faction faction, String message) {
        for (UUID uuid : faction.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(prefix() + message);
        }
    }

    private ItemStack makeItem(Material mat, String name, String... loreLines) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String l : loreLines) if (!l.isEmpty()) lore.add(l);
        if (!lore.isEmpty()) meta.setLore(lore);
        is.setItemMeta(meta);
        return is;
    }

    private void send(Player p, String msg) { p.sendMessage(prefix() + msg); }
    private String prefix() { return "§8[§dAlliance§8] §r"; }
}
