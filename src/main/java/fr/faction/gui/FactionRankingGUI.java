package fr.faction.gui;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * GUI du classement inter-factions + détail d'une faction
 */
public class FactionRankingGUI implements Listener {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;

    private static final String TITLE_RANKING = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Classement des Factions";
    private static final String TITLE_DETAIL  = ChatColor.DARK_AQUA   + "" + ChatColor.BOLD + "Fiche de Faction";
    private static final String TITLE_RANK_INFO = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Rangs et Avantages";

    private final Map<UUID, String> openGUI = new HashMap<>();

    // Matériaux correspondant aux rangs pour l'icone de l'item
    private static final Material[] RANK_MATERIALS = {
            Material.STONE,           // Pierre
            Material.COPPER_INGOT,    // Bronze
            Material.IRON_INGOT,      // Argent
            Material.GOLD_INGOT,      // Or
            Material.DIAMOND,         // Diamant
            Material.EMERALD,         // Emeraude
            Material.NETHER_STAR      // Légendaire
    };

    public FactionRankingGUI(JavaPlugin plugin, FactionManager factionManager, FactionPowerManager powerManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLASSEMENT PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════════

    public void openRankingGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_RANKING);

        // Fond décoratif
        ItemStack bg = makeItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Titre central (slot 4)
        inv.setItem(4, makeItemGlowing(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Classement Factions",
                ChatColor.GRAY + "Classement par Puissance Globale",
                ChatColor.DARK_GRAY + "Clique sur une faction pour les details"));

        // Bouton info rangs (slot 8)
        inv.setItem(8, makeItem(Material.BOOK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Guide des Rangs",
                ChatColor.GRAY + "Voir tous les rangs et leurs avantages"));

        // Classement (slots 10-43, 3 rangées centrales)
        List<Map.Entry<String, Double>> leaderboard = powerManager.getLeaderboard();
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34};

        for (int i = 0; i < Math.min(leaderboard.size(), slots.length); i++) {
            Map.Entry<String, Double> entry = leaderboard.get(i);
            String factionName = entry.getKey();
            double power = entry.getValue();
            int position = i + 1;

            Faction faction = factionManager.getFaction(factionName);
            FactionRank rank = powerManager.getFactionRank(factionName);
            Material mat = RANK_MATERIALS[Math.min(rank.ordinal(), RANK_MATERIALS.length - 1)];

            // Compte les membres en ligne
            long onlineCount = faction != null ? faction.getMembers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .count() : 0;
            int totalMembers = faction != null ? faction.getMemberCount() : 0;

            String positionLabel = getPositionLabel(position);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rang : " + rank.getLabel());
            lore.add(ChatColor.GRAY + "Puissance : " + ChatColor.GOLD + formatPower(power) + " ⚡");
            lore.add(ChatColor.GRAY + "Membres : " + ChatColor.WHITE + onlineCount + ChatColor.GRAY + "/" + totalMembers + " en ligne");
            lore.add("");
            // Barre de progression
            lore.add(ChatColor.GRAY + "Progression :");
            lore.add(rank.progressBar(power));
            FactionRank next = rank.next();
            if (next != null) {
                double needed = next.puissanceMin - power;
                lore.add(ChatColor.DARK_GRAY + "Prochain rang : " + next.getLabel()
                        + ChatColor.DARK_GRAY + " (" + formatPower(needed) + " ⚡ manquants)");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique pour voir les details");

            ItemStack item = position <= 3
                    ? makeItemGlowing(mat, positionLabel + " " + factionName.toUpperCase(), lore)
                    : makeItem(mat, positionLabel + " " + factionName.toUpperCase(), lore);
            inv.setItem(slots[i], item);
        }

        // Votre faction (slot 49)
        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (myFaction != null) {
            FactionRank myRank = powerManager.getFactionRank(myFaction.getName());
            double myPower = powerManager.getFactionPower(myFaction.getName());
            int myPos = powerManager.getFactionPosition(myFaction.getName());

            inv.setItem(49, makeItemGlowing(Material.COMPASS,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Votre Faction : " + myFaction.getName(),
                    ChatColor.GRAY + "Rang : " + myRank.getLabel(),
                    ChatColor.GRAY + "Puissance : " + ChatColor.GOLD + formatPower(myPower) + " ⚡",
                    ChatColor.GRAY + "Position : " + ChatColor.WHITE + "#" + myPos,
                    "",
                    ChatColor.YELLOW + "Clique pour le detail"));
        }

        // Fermer (slot 45)
        inv.setItem(45, makeItem(Material.BARRIER, ChatColor.RED + "Fermer"));

        openGUI.put(player.getUniqueId(), "ranking");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // DETAIL D'UNE FACTION
    // ════════════════════════════════════════════════════════════════════════════

    public void openFactionDetail(Player player, String factionName) {
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null) return;

        FactionRank rank = powerManager.getFactionRank(factionName);
        double totalPower = powerManager.getFactionPower(factionName);
        int position = powerManager.getFactionPosition(factionName);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_DETAIL);

        // Fond
        ItemStack bg = makeItem(rank.couleur == ChatColor.GRAY ? Material.GRAY_STAINED_GLASS_PANE
                : Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Info principale (slot 4)
        List<String> mainLore = new ArrayList<>();
        mainLore.add(ChatColor.GRAY + "Position mondiale : " + ChatColor.GOLD + "#" + position);
        mainLore.add(ChatColor.GRAY + "Puissance globale : " + ChatColor.GOLD + formatPower(totalPower) + " ⚡");
        mainLore.add(ChatColor.GRAY + "Membres : " + ChatColor.WHITE + faction.getMemberCount());
        mainLore.add("");
        mainLore.add(ChatColor.GRAY + "Progression vers " + (rank.next() != null ? rank.next().getLabel() : ChatColor.GOLD + "MAX") + ChatColor.GRAY + " :");
        mainLore.add(rank.progressBar(totalPower));
        if (rank.next() != null) {
            mainLore.add(ChatColor.DARK_GRAY + "Il manque " + formatPower(rank.next().puissanceMin - totalPower) + " ⚡");
        }
        Material mainMat = RANK_MATERIALS[Math.min(rank.ordinal(), RANK_MATERIALS.length - 1)];
        inv.setItem(4, makeItemGlowing(mainMat, rank.getLabelBold() + ChatColor.WHITE + " " + factionName.toUpperCase(), mainLore));

        // Avantages débloqués (slot 2)
        List<String> advLore = new ArrayList<>();
        advLore.add(ChatColor.GREEN + "Avantages actifs :");
        for (String av : rank.avantages) {
            advLore.add(ChatColor.YELLOW + "✔ " + ChatColor.WHITE + av);
        }
        inv.setItem(2, makeItemGlowing(Material.BEACON,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Avantages du rang " + rank.nom,
                advLore));

        // Prochains avantages (slot 6)
        FactionRank next = rank.next();
        if (next != null) {
            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.GRAY + "Seuil : " + ChatColor.GOLD + formatPower(next.puissanceMin) + " ⚡");
            nextLore.add("");
            nextLore.add(ChatColor.AQUA + "Se debloqueront :");
            for (String av : next.avantages) {
                nextLore.add(ChatColor.DARK_GRAY + "○ " + ChatColor.GRAY + av);
            }
            inv.setItem(6, makeItem(Material.END_CRYSTAL,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Prochain rang : " + next.getLabel(),
                    nextLore));
        }

        // Liste des membres avec leur puissance individuelle (rangée 2 et 3)
        int[] memberSlots = {10,11,12,13,14,15,16,17,19,20,21,22,23,24,25,26};
        int si = 0;
        List<UUID> members = new ArrayList<>(faction.getMembers());
        // Trier par puissance décroissante
        members.sort((a, b) -> Double.compare(
                powerManager.getPlayerPower(b),
                powerManager.getPlayerPower(a)
        ));

        for (UUID uuid : members) {
            if (si >= memberSlots.length) break;
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            Player online = Bukkit.getPlayer(uuid);
            boolean isOnline = online != null && online.isOnline();
            double playerPower = powerManager.getPlayerPower(uuid);
            boolean isChef = faction.isChef(uuid);

            List<String> memberLore = new ArrayList<>();
            memberLore.add(isOnline ? ChatColor.GREEN + "● En ligne" : ChatColor.DARK_GRAY + "○ Hors ligne");
            if (isChef) memberLore.add(ChatColor.GOLD + "★ Chef");
            memberLore.add(ChatColor.GRAY + "Puissance : " + ChatColor.GOLD + formatPower(playerPower) + " ⚡");

            if (powerManager.getPlayerBreakdown(uuid) != null) {
                var bd = powerManager.getPlayerBreakdown(uuid);
                memberLore.add("");
                memberLore.add(ChatColor.DARK_GRAY + "PvP: " + ChatColor.RED + bd.pvp()
                        + ChatColor.DARK_GRAY + "  Survie: " + ChatColor.YELLOW + bd.survie());
                memberLore.add(ChatColor.DARK_GRAY + "Prog: " + ChatColor.GREEN + bd.progression()
                        + ChatColor.DARK_GRAY + "  Activité: " + ChatColor.AQUA + bd.activite());
            }

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                String dn = (op.getName() != null ? op.getName() : "Inconnu")
                        + (isChef ? ChatColor.GOLD + " ★" : "");
                meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + dn);
                meta.setLore(memberLore);
                skull.setItemMeta(meta);
            }
            inv.setItem(memberSlots[si++], skull);
        }

        // Retour (slot 45) et fermer (slot 53)
        inv.setItem(45, makeItem(Material.ARROW, ChatColor.GRAY + "Retour au classement"));
        inv.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Fermer"));

        openGUI.put(player.getUniqueId(), "detail:" + factionName.toLowerCase());
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GUIDE DES RANGS
    // ════════════════════════════════════════════════════════════════════════════

    public void openRankInfoGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_RANK_INFO);

        ItemStack bg = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        inv.setItem(4, makeItemGlowing(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Guide des Rangs de Faction",
                ChatColor.GRAY + "Montez en puissance pour debloquer",
                ChatColor.GRAY + "des avantages exclusifs !",
                "",
                ChatColor.DARK_GRAY + "La Puissance Globale = somme des",
                ChatColor.DARK_GRAY + "puissances individuelles + bonus membres"));

        FactionRank[] ranks = FactionRank.values();
        int[] rankSlots = {19, 21, 23, 25, 29, 31, 33};

        for (int i = 0; i < ranks.length && i < rankSlots.length; i++) {
            FactionRank rank = ranks[i];
            Material mat = RANK_MATERIALS[i];

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Seuil : " + ChatColor.GOLD
                    + (rank.puissanceMin == 0 ? "Rang de depart" : formatPower(rank.puissanceMin) + " ⚡ PG"));
            lore.add("");
            lore.add(ChatColor.GREEN + "Avantages :");
            for (String av : rank.avantages) {
                lore.add(ChatColor.YELLOW + "  › " + ChatColor.WHITE + av);
            }

            // Faction actuelle du joueur
            Faction myFac = factionManager.getPlayerFaction(player.getUniqueId());
            boolean isCurrent = myFac != null
                    && powerManager.getFactionRank(myFac.getName()) == rank;

            ItemStack item = isCurrent
                    ? makeItemGlowing(mat, rank.getLabelBold() + (isCurrent ? ChatColor.GREEN + " ◄ Votre rang" : ""), lore)
                    : makeItem(mat, rank.getLabelBold(), lore);
            inv.setItem(rankSlots[i], item);
        }

        // Info puissance individuelle (slot 49)
        inv.setItem(49, makeItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Comment gagner de la Puissance ?",
                ChatColor.GRAY + "Votre Puissance Individuelle (PI) :",
                ChatColor.RED   + "  PvP : " + ChatColor.WHITE + "Kills x10, Dommages/100",
                ChatColor.YELLOW + "  Survie : " + ChatColor.WHITE + "Blocs, Distance parcourue",
                ChatColor.GREEN + "  Progression : " + ChatColor.WHITE + "Advancements x15, Crafts",
                ChatColor.AQUA  + "  Activite : " + ChatColor.WHITE + "2pts/heure jouee",
                "",
                ChatColor.GOLD + "  + 50 ⚡ bonus par membre dans la faction"));

        inv.setItem(45, makeItem(Material.ARROW, ChatColor.GRAY + "Retour au classement"));
        inv.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Fermer"));

        openGUI.put(player.getUniqueId(), "rankinfo");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EVENT
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openGUI.containsKey(uuid)) return;

        String title = event.getView().getTitle();
        boolean ours = title.equals(TITLE_RANKING) || title.equals(TITLE_DETAIL) || title.equals(TITLE_RANK_INFO);
        if (!ours) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().contains("GLASS_PANE")) return;

        String itemName = clicked.hasItemMeta() && clicked.getItemMeta() != null
                && clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";

        String gui = openGUI.get(uuid);

        if (TITLE_RANKING.equals(title)) {
            if (itemName.equals("Fermer")) {
                openGUI.remove(uuid); player.closeInventory(); return;
            }
            if (itemName.contains("Guide des Rangs")) {
                player.closeInventory(); openRankInfoGUI(player); return;
            }
            if (itemName.contains("Votre Faction")) {
                Faction myFac = factionManager.getPlayerFaction(uuid);
                if (myFac != null) { player.closeInventory(); openFactionDetail(player, myFac.getName()); }
                return;
            }
            // Clic sur une faction du classement — le nom de la faction est après le label de position
            if (clicked.getType() != Material.BARRIER && clicked.getType() != Material.COMPASS
                    && clicked.getType() != Material.BOOK && clicked.getType() != Material.NETHER_STAR) {
                // Extraire le nom de faction depuis le titre de l'item
                String rawName = itemName.replaceAll("^[#\\d]+\\s+[^\\s]+\\s+", "").trim();
                // Chercher par correspondance dans les factions
                for (String fname : factionManager.getAllFactions().keySet()) {
                    if (fname.equalsIgnoreCase(rawName) || itemName.toUpperCase().contains(fname.toUpperCase())) {
                        player.closeInventory(); openFactionDetail(player, fname); return;
                    }
                }
            }
        }

        if (TITLE_DETAIL.equals(title)) {
            if (itemName.equals("Fermer")) {
                openGUI.remove(uuid); player.closeInventory();
            } else if (itemName.contains("Retour")) {
                player.closeInventory(); openRankingGUI(player);
            }
        }

        if (TITLE_RANK_INFO.equals(title)) {
            if (itemName.equals("Fermer")) {
                openGUI.remove(uuid); player.closeInventory();
            } else if (itemName.contains("Retour")) {
                player.closeInventory(); openRankingGUI(player);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════════

    private String getPositionLabel(int pos) {
        return switch (pos) {
            case 1 -> ChatColor.GOLD + "#1";
            case 2 -> ChatColor.WHITE + "#2";
            case 3 -> ChatColor.GOLD + "#3";
            default -> ChatColor.GRAY + "#" + pos;
        };
    }

    private String formatPower(double power) {
        if (power >= 1000) return String.format("%.1fk", power / 1000);
        return String.format("%.0f", power);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        return makeItem(mat, name, lore.toArray(new String[0]));
    }

    @SuppressWarnings("deprecation")
    private ItemStack makeItemGlowing(Material mat, String name, String... lore) {
        ItemStack item = makeItem(mat, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItemGlowing(Material mat, String name, List<String> lore) {
        return makeItemGlowing(mat, name, lore.toArray(new String[0]));
    }
}
