package fr.faction.gui;

import fr.faction.alliance.AllianceManager;
import fr.faction.alliance.HomeManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import fr.faction.war.WarManager;
import org.bukkit.*;
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
 * GUI principal de /fac — affiche TOUTES les fonctionnalités du plugin
 * organisées en catégories avec des pages par section.
 *
 * Navigation :
 *   Page ACCUEIL   → infos faction + raccourcis rapides
 *   Page GESTION   → create/disband/invite/kick/setchef/rename
 *   Page TERRITOIRE → claim/unclaim/spawn/home/tpa
 *   Page ÉCONOMIE  → shop/banque/troc/coffre
 *   Page ALLIANCES → alliance GUI + guerre
 *   Page STATS     → stats/classement/power/rangs
 *   Page AIDE      → liste de TOUTES les commandes textuelles
 */
public class MainMenuGUI implements Listener {

    // Titres des pages
    private static final String T_HOME     = "§8§l[ §6§lFaction §8§l] §7Accueil";
    private static final String T_GESTION  = "§8§l[ §6§lFaction §8§l] §7Gestion";
    private static final String T_TERRIT   = "§8§l[ §6§lFaction §8§l] §7Territoire & TP";
    private static final String T_ECONO    = "§8§l[ §6§lFaction §8§l] §7Économie";
    private static final String T_ALLIANCE = "§8§l[ §6§lFaction §8§l] §7Alliances & Guerre";
    private static final String T_STATS    = "§8§l[ §6§lFaction §8§l] §7Stats & Classements";
    private static final String T_AIDE     = "§8§l[ §6§lFaction §8§l] §7Aide — Commandes";

    private static final String PREFIX_PAGES = "§8§l[§6§lFaction§8§l] §r";

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final SharedInventoryManager sharedInvManager;
    private final FactionTeleportManager teleportManager;
    private final FactionPowerManager powerManager;
    private final AllianceManager allianceManager;
    private final HomeManager homeManager;
    private final WarManager warManager;
    private final FactionGUI factionGUI;
    private final FactionRankingGUI rankingGUI;

    // Page ouverte par joueur
    private final Map<UUID, String> openPage = new HashMap<>();

    public MainMenuGUI(JavaPlugin plugin, FactionManager factionManager,
                        SharedInventoryManager sharedInvManager,
                        FactionTeleportManager teleportManager,
                        FactionPowerManager powerManager,
                        AllianceManager allianceManager,
                        HomeManager homeManager,
                        WarManager warManager,
                        FactionGUI factionGUI,
                        FactionRankingGUI rankingGUI) {
        this.plugin          = plugin;
        this.factionManager  = factionManager;
        this.sharedInvManager = sharedInvManager;
        this.teleportManager = teleportManager;
        this.powerManager    = powerManager;
        this.allianceManager = allianceManager;
        this.homeManager     = homeManager;
        this.warManager      = warManager;
        this.factionGUI      = factionGUI;
        this.rankingGUI      = rankingGUI;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE ACCUEIL
    // ════════════════════════════════════════════════════════════════════════════

    public void openHome(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_HOME);
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());

        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        if (faction == null) {
            // Pas de faction — proposer création / liste
            inv.setItem(19, make(Material.WRITABLE_BOOK, "§a§lCréer une faction",
                    "§7Fonde ta propre faction.", "", "§e/fac create <nom>"));
            inv.setItem(22, make(Material.PAPER, "§b§lRejoindre une faction",
                    "§7Tu dois avoir été invité.", "", "§e/fac join <nom>"));
            inv.setItem(25, make(Material.BOOK, "§f§lListe des factions",
                    "§7Voir toutes les factions.", "", "§e/fac list"));
        } else {
            FactionRank rank  = powerManager.getFactionRank(faction.getName());
            double power      = powerManager.getFactionPower(faction.getName());
            long online       = faction.getMembers().stream().filter(u -> Bukkit.getPlayer(u) != null).count();
            double allyBonus  = allianceManager.getAlliancePowerBonus(faction.getName());
            boolean atWar     = warManager.isAtWar(faction.getName());

            // Slot 4 — Info faction centrale
            List<String> infoLore = new ArrayList<>(Arrays.asList(
                    "§7Chef : §f" + getPlayerName(faction.getChef()),
                    "§7Membres : §f" + faction.getMemberCount() + "§7, en ligne : §a" + online,
                    "§7Alliés : §d" + faction.getAllyCount(),
                    "§7Rang : " + rank.getLabel(),
                    "§7Puissance : §e" + (int) power + " §8(+" + (int) allyBonus + " alliances)",
                    rank.progressBar(power),
                    "",
                    faction.isChef(player.getUniqueId()) ? "§6★ Tu es le Chef" : "§7Rôle : Membre"
            ));
            if (atWar) infoLore.add("§c⚔ En GUERRE ! §e/fac guerre statut");
            inv.setItem(4, glowing(make(Material.GOLDEN_HELMET, rank.couleur + "§l" + faction.getName(), infoLore.toArray(new String[0]))));

            // Ligne 2 — Actions rapides
            inv.setItem(19, make(Material.PLAYER_HEAD,    "§b§lMembres",         "§7Voir & gérer les membres.", "", "§e§Clic → ouvrir"));
            inv.setItem(20, make(Material.CHEST,           "§e§lCoffre Partagé", "§7Inventaire commun de la faction.", "", "§e§Clic → ouvrir"));
            inv.setItem(21, make(Material.ENDER_PEARL,     "§d§lTéléportation",  "§7TP vers un membre.", "", "§e§Clic → ouvrir"));
            inv.setItem(22, make(Material.LIME_BANNER,     "§d§lAlliances",      "§7Gérer les alliances.", "§7Alliés : §d" + faction.getAllyCount(), "", "§e§Clic → ouvrir"));
            inv.setItem(23, make(atWar ? Material.RED_BANNER : Material.ORANGE_BANNER,
                    atWar ? "§c§l⚔ Guerre EN COURS" : "§6§lGuerre",
                    atWar ? "§7Voir le statut de la guerre." : "§7Déclarer la guerre à une faction.",
                    "", "§e§Clic → ouvrir"));
            inv.setItem(24, make(Material.COMPASS,         "§f§lListe",          "§7Toutes les factions.", "", "§e/fac list"));
            inv.setItem(25, make(Material.BEACON,          "§b§lShop Global",    "§7Acheter/vendre des items.", "", "§e/fac shop"));
        }

        // Ligne 3 — Navigation catégories
        addNavBar(inv, "home");

        // Slot 49 — Fermer
        inv.setItem(49, make(Material.BARRIER, "§cFermer", "§7Ferme ce menu."));

        openPage.put(player.getUniqueId(), "home");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE GESTION
    // ════════════════════════════════════════════════════════════════════════════

    private void openGestion(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_GESTION);
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef  = faction != null && faction.isChef(player.getUniqueId());
        boolean canMng  = faction != null && faction.canManage(player.getUniqueId());
        boolean hasFac  = faction != null;

        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        // Sans faction
        if (!hasFac) {
            inv.setItem(19, make(Material.WRITABLE_BOOK, "§a§l/fac create <nom>",
                    "§7Créer une faction.", "§7Tu en deviens automatiquement le Chef."));
            inv.setItem(22, make(Material.PAPER, "§b§l/fac join <nom>",
                    "§7Rejoindre une faction.", "§7Tu dois avoir été invité par le Chef."));
            inv.setItem(25, make(Material.COMPASS, "§f§l/fac list",
                    "§7Voir toutes les factions existantes."));
        } else {
            // Commandes universelles (tous membres)
            inv.setItem(10, cmdItem(Material.RED_BED,        "§c§l/fac leave",         "§7Quitter ta faction.", !isChef));
            inv.setItem(11, cmdItem(Material.BOOK,           "§f§l/fac info",           "§7Voir les infos de ta faction.", true));
            inv.setItem(12, cmdItem(Material.PAPER,          "§f§l/fac info <nom>",     "§7Infos d'une autre faction.", true));
            inv.setItem(13, cmdItem(Material.COMPASS,        "§f§l/fac list",           "§7Lister toutes les factions.", true));
            inv.setItem(14, cmdItem(Material.BOOK,           "§f§l/fac top",            "§7Top 10 par puissance.", true));

            // Commandes chef + sous-chef
            inv.setItem(19, cmdItem(Material.NAME_TAG,       "§a§l/fac invite <joueur>","§7Inviter un joueur dans la faction.", canMng));
            inv.setItem(20, cmdItem(Material.IRON_BOOTS,     "§e§l/fac kick <joueur>",  "§7Expulser un membre (sauf le chef).", canMng));
            // Commandes chef uniquement
            inv.setItem(21, cmdItem(Material.GOLDEN_HELMET,  "§6§l/fac setchef <joueur>","§7Transférer le rôle de Chef.", isChef));
            inv.setItem(22, cmdItem(Material.NAME_TAG,       "§b§l/fac rename <nom>",   "§7Renommer la faction.", isChef));
            inv.setItem(23, cmdItem(Material.ENDER_PEARL,    "§d§l/fac setspawn",       "§7Définir le spawn de la faction.", canMng));
            inv.setItem(25, cmdItem(Material.TNT,            "§c§l/fac disband",        "§c§lDissoudre la faction §c(irréversible).", isChef));

            if (!isChef) {
                inv.setItem(40, make(Material.ORANGE_STAINED_GLASS_PANE, "§7Note", "§7Les commandes §8grisées §7sont réservées au Chef (ou au sous-chef selon la commande)."));
            }
        }

        addNavBar(inv, "gestion");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", "§7Retour à l'accueil."));
        openPage.put(player.getUniqueId(), "gestion");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE TERRITOIRE & TP
    // ════════════════════════════════════════════════════════════════════════════

    private void openTerritoire(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_TERRIT);
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef  = faction != null && faction.isChef(player.getUniqueId());
        boolean canMng  = faction != null && faction.canManage(player.getUniqueId());
        int maxHomes    = homeManager.getMaxHomes(player.getUniqueId());
        int curHomes    = homeManager.getHomes(player.getUniqueId()).size();

        fillBorder(inv, Material.GREEN_STAINED_GLASS_PANE);

        // Claims
        inv.setItem(10, cmdItem(Material.GRASS_BLOCK,   "§a§l/fac claim",        "§7Claimer le chunk sous tes pieds.", canMng));
        inv.setItem(11, cmdItem(Material.DIRT,          "§c§l/fac unclaim",       "§7Retirer le claim du chunk.", canMng));
        inv.setItem(12, cmdItem(Material.MAP,           "§b§l/fac claims",        "§7Voir les claims de ta faction.", faction != null));
        inv.setItem(13, cmdItem(Material.FILLED_MAP,    "§b§l/fac claimmap",      "§7Carte visuelle des claims.", faction != null));
        inv.setItem(14, cmdItem(Material.IRON_DOOR,     "§e§l/fac perms",         "§7Gérer les permissions du chunk.", isChef));

        // Spawn faction
        inv.setItem(19, cmdItem(Material.RESPAWN_ANCHOR,"§d§l/fac spawn [1|2]",   "§7Aller au spawn de ta faction.",
                faction != null && faction.hasSpawn(),
                "§7Spawn 1 : " + (faction != null && faction.hasSpawn()  ? "§a✔ Défini" : "§c✘ Non défini"),
                "§7Spawn 2 : " + (faction != null && faction.hasSpawn2() ? "§a✔ Défini" : "§c✘ Non défini (rang ◆ Diamant)")));
        inv.setItem(20, cmdItem(Material.LODESTONE,     "§d§l/fac setspawn [1|2]","§7Définir un spawn de faction.",
                canMng,
                "§7/fac setspawn   → spawn principal",
                "§7/fac setspawn 2 → spawn secondaire §c(rang ◆ Diamant+)"));

        // Homes
        String homeLore = "§7Homes : §e" + curHomes + "§7/§e" + maxHomes
                + "\n§7(1 sans faction, 2 avec, 3 si allié)";
        inv.setItem(22, make(Material.RED_BED, "§a§l/fac sethome [nom]", homeLore.split("\n")));
        inv.setItem(23, make(Material.ENDER_EYE, "§a§l/fac home [nom]",    "§7Se TP à un home.", "§7Warmup 5s, cooldown 30s."));
        inv.setItem(24, make(Material.BARRIER,   "§c§l/fac delhome <nom>", "§7Supprimer un home."));

        // Coffres privés
        inv.setItem(28, make(Material.CHEST, "§6§lCoffres Privés",
                "§7Sneak + §eclic droit §7avec un panneau",
                "§7sur un coffre pour le verrouiller.",
                "§7Seul toi peux l'ouvrir."));

        // TPA
        inv.setItem(31, make(Material.ENDER_PEARL, "§b§l/fac tpa <joueur>",  "§7Demander à se TP vers un joueur.", "§7Warmup 3s. Cooldown 60s."));
        inv.setItem(32, make(Material.NETHER_STAR, "§a§l/fac tpaccept",       "§7Accepter une demande de TP."));
        inv.setItem(33, make(Material.BARRIER,     "§c§l/fac tpdeny",         "§7Refuser une demande de TP."));

        // TP membres
        inv.setItem(34, cmdItem(Material.COMPASS,      "§d§l/fac tp [membre]", "§7Se TP vers un membre de la faction.", faction != null));

        addNavBar(inv, "territoire");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", ""));
        openPage.put(player.getUniqueId(), "territoire");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE ÉCONOMIE
    // ════════════════════════════════════════════════════════════════════════════

    private void openEconomie(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_ECONO);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        // Shop
        inv.setItem(10, make(Material.BEACON,       "§e§l/fac shop",              "§7Shop global paginé.", "§7Achète et vends des items.", "", "§eClic → ouvrir le shop"));
        inv.setItem(11, make(Material.GOLD_INGOT,   "§6§l/fac vendre <prix> <monnaie>","§7Vend l'item tenu en main.",
                "§7Monnaies : §fler, or, diamant, emeraude", "§7Prix = par item (x quantité en main)"));
        inv.setItem(12, make(Material.IRON_INGOT,   "§f§l/fac acheter <ID>",      "§7Acheter une annonce par son ID."));
        inv.setItem(13, make(Material.DIAMOND,      "§b§l/fac recuperer [ID]",    "§7Récupérer une annonce non vendue.", "§7Sans ID : liste tes annonces actives."));
        inv.setItem(14, make(Material.EMERALD,      "§a§l/fac mesannonces",       "§7Voir toutes tes annonces actives (GUI)."));

        // Banque
        inv.setItem(19, make(Material.EMERALD_BLOCK,"§a§l/fac banque",            "§7Banque d'émeraudes de faction.", "§7Dépôt / retrait / coffre faction.", "", "§eClic → ouvrir la banque"));
        inv.setItem(20, make(Material.GOLD_BLOCK,   "§6§l/fac topbanque",         "§7Top 10 factions les plus riches."));

        // Coffre partagé
        inv.setItem(22, make(Material.CHEST,        "§e§l/fac coffre",            "§7Coffre partagé de ta faction.", "§7Accessible à tous les membres.", "", "§eClic → ouvrir le coffre"));

        // Organiser le coffre
        inv.setItem(23, make(Material.HOPPER,       "§6§l/fac ranger",            "§7Organiser le coffre partagé.",
                "§8• §bSimilaires §8• §aCatégorie §8• §eA→Z",
                "§8• §6Quantité ↑↓ §8• §dRareté",
                "", "§eClic → ouvrir le menu de tri"));

        // Troc
        inv.setItem(25, make(TRADING_SIGN_ITEM(), "§d§l/fac troc <joueur>","§7Proposer un échange direct à un joueur.", "§7Interface graphique de troc."));
        inv.setItem(26, make(Material.NETHER_STAR,  "§d§l/fac accepter",          "§7Accepter une invitation de troc."));

        addNavBar(inv, "economie");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", ""));
        openPage.put(player.getUniqueId(), "economie");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE ALLIANCES & GUERRE
    // ════════════════════════════════════════════════════════════════════════════

    private void openAlliances(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_ALLIANCE);
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef  = faction != null && faction.isChef(player.getUniqueId());
        boolean canMng  = faction != null && faction.canManage(player.getUniqueId());
        boolean atWar   = faction != null && warManager.isAtWar(faction.getName());
        double allyBonus = faction != null ? allianceManager.getAlliancePowerBonus(faction.getName()) : 0;

        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        // Alliances
        inv.setItem(10, make(Material.LIME_BANNER,   "§d§lGUI Alliances",           "§7Interface graphique des alliances.", "§7Voir, accepter, rompre.", "", "§eClic → ouvrir"));
        inv.setItem(11, cmdItem(Material.PAPER,      "§d§l/fac alliance inviter <faction>", "§7Proposer une alliance.", canMng));
        inv.setItem(12, cmdItem(Material.LIME_DYE,   "§a§l/fac alliance accepter <faction>","§7Accepter une invitation.", canMng));
        inv.setItem(13, cmdItem(Material.RED_DYE,    "§c§l/fac alliance refuser <faction>", "§7Refuser une invitation.", canMng));
        inv.setItem(14, cmdItem(Material.SHEARS,     "§c§l/fac alliance rompre <faction>",  "§7Rompre une alliance.", canMng));
        inv.setItem(15, make(Material.NETHER_STAR,   "§d§l/fac alliance liste",      "§7Voir les alliés.",
                "§7Bonus actuel : §6+" + (int) allyBonus + " power"));

        // Bonus info
        inv.setItem(22, make(Material.BOOK, "§7Avantages des alliances",
                "§71 allié  → §6+500 power §7+ homes 2→3",
                "§72 alliés → §6+1200 power",
                "§73 alliés → §6+2500 power",
                "§74+ alliés → §6+500 par allié sup.",
                "§7Homes proches autorisés entre alliés",
                "§7Homes claims proches autorisés"));

        // Guerre
        inv.setItem(28, make(atWar ? Material.RED_BANNER : Material.ORANGE_BANNER,
                atWar ? "§c§l⚔ GUERRE EN COURS" : "§6§l/fac guerre declarer <faction>",
                atWar ? "§7Tape /fac guerre statut" : "§7Déclarer la guerre à une faction.",
                "§7Options : [claims:0-5] [pillage] [kills:5-50]",
                "§8Conditions : chef ou sous-chef, ratio power ≤3:1,",
                "§8cooldown 48h, pas d'allié, cible ≥2 membres"));
        inv.setItem(29, cmdItem(Material.GREEN_DYE,  "§a§l/fac guerre accepter",    "§7Accepter une déclaration de guerre.", canMng));
        inv.setItem(30, cmdItem(Material.RED_DYE,    "§c§l/fac guerre refuser",     "§7Refuser une déclaration de guerre.", canMng));
        inv.setItem(31, make(Material.FILLED_MAP,    "§e§l/fac guerre statut",      "§7Score, temps restant, enjeux.",
                atWar ? "§aClic → voir le statut" : "§7(aucune guerre active)"));
        inv.setItem(32, make(Material.COMPASS,       "§b§l/fac guerre liste",       "§7Toutes les guerres actives du serveur."));
        inv.setItem(33, cmdItem(WHITE_FLAG_ITEM(), "§7§l/fac guerre capituler","§cAbandonner la guerre (perds automatiquement, chef uniquement).", isChef));
        inv.setItem(34, cmdItem(Material.CHEST,      "§6§l/fac guerre piller",      "§7Piller le coffre du vaincu (si négocié).", canMng && atWar));

        addNavBar(inv, "alliances");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", ""));
        openPage.put(player.getUniqueId(), "alliances");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE STATS
    // ════════════════════════════════════════════════════════════════════════════

    private void openStats(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_STATS);
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        inv.setItem(10, make(Material.NETHER_STAR,    "§e§l/fac classement",      "§7Classement des factions (GUI).", "", "§eClic → ouvrir"));
        inv.setItem(11, make(Material.BOOK,           "§e§l/fac rangs",           "§7Guide des rangs et leurs avantages (GUI).", "", "§eClic → ouvrir"));
        inv.setItem(12, make(Material.GOLD_INGOT,     "§6§l/fac top",             "§7Top 10 factions par puissance (texte)."));
        inv.setItem(13, make(Material.EMERALD,        "§a§l/fac topbanque",       "§7Top 10 par émeraudes en coffre."));
        inv.setItem(14, make(Material.IRON_SWORD,     "§f§l/fac power [joueur]",  "§7Puissance individuelle d'un joueur."));
        inv.setItem(15, make(Material.WRITTEN_BOOK,   "§b§l/fac stats [joueur]",  "§7Statistiques personnelles détaillées."));
        inv.setItem(19, make(Material.DIAMOND,        "§b§l/fac classementjoueurs","§7Top 10 joueurs (PvP, minage, etc.)."));
        inv.setItem(21, make(Material.COMPASS,        "§7Catégories de stats",
                "§emobs §8— §epvp §8— §eadvancements",
                "§emorts §8— §eblocs §8— §etemps",
                "§edommages §8— §ekd §8— §erichesse"));

        // Rangs visuels
        FactionRank[] ranks = FactionRank.values();
        for (int i = 0; i < Math.min(ranks.length, 7); i++) {
            FactionRank r = ranks[i];
            inv.setItem(27 + i, make(Material.PAPER, r.getLabel(),
                    "§7Puissance min : §e" + (int) r.puissanceMin,
                    "§7Avantages :",
                    Arrays.stream(r.avantages).map(a -> "§8• §7" + a).reduce("", (a, b) -> a + "\n" + b).trim()));
        }

        addNavBar(inv, "stats");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", ""));
        openPage.put(player.getUniqueId(), "stats");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PAGE AIDE
    // ════════════════════════════════════════════════════════════════════════════

    private void openAide(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, T_AIDE);
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        // Chaque item = catégorie de commandes
        inv.setItem(10, make(Material.WRITABLE_BOOK, "§a§lGestion de faction",
                "§e/fac create <nom>       §7Créer une faction",
                "§e/fac disband            §7Dissoudre",
                "§e/fac invite <joueur>    §7Inviter",
                "§e/fac join <nom>         §7Rejoindre",
                "§e/fac leave              §7Quitter",
                "§e/fac kick <joueur>      §7Expulser",
                "§e/fac setchef <joueur>   §7Changer de chef",
                "§e/fac rename <nom>       §7Renommer",
                "§e/fac info [nom]         §7Infos faction",
                "§e/fac list               §7Liste des factions"));

        inv.setItem(12, make(Material.GRASS_BLOCK, "§a§lTerritoire — Claims",
                "§e/fac claim              §7Claimer le chunk",
                "§e/fac unclaim            §7Unclaim",
                "§e/fac claims             §7Voir mes claims",
                "§e/fac claimmap           §7Carte des claims",
                "§e/fac perms              §7Permissions du chunk",
                "§e/fac claimallow <fac>   §7Autoriser proximité",
                "§e/fac claimdeny <fac>    §7Révoquer autorisation",
                "§e/fac claimallies        §7Lister autorisations"));

        inv.setItem(14, make(Material.RED_BED, "§a§lHomes & Spawn",
                "§e/sethome [nom]          §7Définir un home",
                "§e/home [nom]             §7Aller à un home",
                "§e/delhome <nom>          §7Supprimer un home",
                "§e/homes                  §7Lister ses homes",
                "§e/fac setspawn           §7Spawn faction (chef)",
                "§e/fac spawn              §7Aller au spawn faction",
                "§7Homes selon le rang : 1→2→3→4→5",
                "§8Distance min : 10 chunks (sauf faction/alliés)"));

        inv.setItem(16, make(Material.ENDER_PEARL, "§b§lTéléportation",
                "§e/fac tp [membre]        §7TP vers un membre",
                "§e/fac tpa <joueur>       §7Demande de TP",
                "§e/tpaccept               §7Accepter TP",
                "§e/tpdeny                 §7Refuser TP",
                "§8Raccourcis : /tpa /tpaccept /tpdeny"));

        inv.setItem(28, make(Material.BEACON, "§e§lShop Global",
                "§e/fac shop               §7Ouvrir le shop (GUI)",
                "§e/fac vendre <prix> <monnaie>",
                "§8  Monnaies : fer or diamant emeraude",
                "§e/fac acheter <ID>        §7Acheter par ID",
                "§e/fac recuperer [ID]      §7Récupérer annonce",
                "§e/fac mesannonces         §7Mes annonces (GUI)"));

        inv.setItem(30, make(Material.EMERALD, "§a§lÉconomie",
                "§e/fac banque             §7Banque d'émeraudes",
                "§e/fac coffre             §7Coffre partagé",
                "§e/fac troc <joueur>      §7Proposer un troc",
                "§e/fac accepter           §7Accepter un troc",
                "§e/fac topbanque          §7Classement richesse"));

        inv.setItem(32, make(Material.LIME_BANNER, "§d§lAlliances",
                "§e/fac alliance inviter <faction>",
                "§e/fac alliance accepter <faction>",
                "§e/fac alliance refuser <faction>",
                "§e/fac alliance rompre <faction>",
                "§e/fac alliance liste",
                "§e/fac alliance gui",
                "§8Bonus : +500 à +2500 power par allié"));

        inv.setItem(34, make(Material.RED_BANNER, "§c§lGuerre",
                "§e/fac guerre declarer <faction>",
                "§8  [claims:0-5] [pillage] [kills:5-50]",
                "§e/fac guerre accepter",
                "§e/fac guerre refuser",
                "§e/fac guerre capituler",
                "§e/fac guerre statut",
                "§e/fac guerre piller",
                "§e/fac guerre liste",
                "§8Kills en zone claimée seulement",
                "§8Cooldown 48h • ratio power max 3:1"));

        inv.setItem(40, make(Material.NETHER_STAR, "§e§lStats & Classements",
                "§e/fac stats [joueur]     §7Statistiques",
                "§e/fac power [joueur]     §7Puissance",
                "§e/fac classement         §7Classement (GUI)",
                "§e/fac rangs              §7Rangs (GUI)",
                "§e/fac top                §7Top 10 power",
                "§e/fac classementjoueurs  §7Top joueurs"));

        inv.setItem(42, make(Material.COMMAND_BLOCK, "§c§lAdmin",
                "§e/fac invsee <joueur>    §7Voir l'inventaire",
                "§8Requiert permission : faction.admin",
                "§7Coffres privés : admin bypass auto"));

        addNavBar(inv, "aide");
        inv.setItem(49, make(Material.ARROW, "§7◀ Retour", ""));
        openPage.put(player.getUniqueId(), "aide");
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Barre de navigation commune (slots 45-53).
     * Les 7 catégories + fermer.
     */
    private void addNavBar(Inventory inv, String currentPage) {
        String[] pages = {"home","gestion","territoire","economie","alliances","stats","aide"};
        Material[] mats = {
                HOUSE_BANNER_ITEM(),
                Material.WRITABLE_BOOK,
                Material.GRASS_BLOCK,
                Material.GOLD_INGOT,
                Material.LIME_BANNER,
                Material.NETHER_STAR,
                Material.KNOWLEDGE_BOOK
        };
        String[] labels = {
                "§f§lAccueil",
                "§a§lGestion",
                "§a§lTerritoire & TP",
                "§e§lÉconomie",
                "§d§lAlliances & Guerre",
                "§b§lStats",
                "§7§lAide"
        };

        for (int i = 0; i < pages.length; i++) {
            boolean isCurrent = pages[i].equals(currentPage);
            ItemStack item = isCurrent
                    ? glowing(make(mats[i], labels[i], "§7← Page actuelle"))
                    : make(mats[i], labels[i], "§7Clic pour naviguer");
            inv.setItem(45 + i, item);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EVENTS
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        // ── Sous-GUI : choix spawn ────────────────────────────────────────────────
        if (title.equals("§d§lChoisir un spawn à définir") || title.equals("§d§lChoisir un spawn")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            boolean isSet = title.contains("définir");
            int slot = e.getRawSlot();

            if (slot == 22) { navigateTo(player, "territoire"); return; }

            Faction f = factionManager.getPlayerFaction(player.getUniqueId());
            if (f == null) return;

            if (slot == 11) { // Spawn 1
                player.closeInventory();
                if (isSet) cmd(player, "/faction setspawn 1");
                else if (f.hasSpawn()) cmd(player, "/faction spawn 1");
                else send(player, "§cSpawn #1 non défini. Le chef peut faire §e/fac setspawn§c.");
            } else if (slot == 15) { // Spawn 2
                player.closeInventory();
                if (isSet) cmd(player, "/faction setspawn 2");
                else if (f.hasSpawn2()) cmd(player, "/faction spawn 2");
                else send(player, "§cSpawn #2 non défini. Le chef peut faire §e/fac setspawn 2§c.");
            }
            return;
        }

        // ── Sous-GUI : confirmation dissolution ──────────────────────────────────
        if (title.startsWith("§c§lDissoudre ")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            int slot = e.getRawSlot();
            if (slot == 11) { // confirmer
                player.closeInventory();
                Faction f = factionManager.getPlayerFaction(player.getUniqueId());
                if (f != null && f.isChef(player.getUniqueId())) {
                    cmd(player, "/faction disband");
                }
            } else if (slot == 15) {
                openGestion(player);
            }
            return;
        }

        // ── Sous-GUI : confirmation capitulation ──────────────────────────────────
        if (title.equals("§c§l⚔ Capituler ?")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            int slot = e.getRawSlot();
            if (slot == 11) { player.closeInventory(); cmd(player, "/faction guerre capituler"); }
            else if (slot == 15) { navigateTo(player, "alliances"); }
            return;
        }

        // ── Sous-GUI : définir un home ────────────────────────────────────────────
        if (title.equals("§a§l🏠 Définir un home")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            int slot = e.getRawSlot();
            if (slot == 26) { navigateTo(player, "territoire"); return; }
            int max = homeManager.getMaxHomes(player.getUniqueId());
            int[] slots = homeSlots(max);
            for (int i = 0; i < slots.length; i++) {
                if (slot == slots[i]) {
                    final String name = homeNameFor(i);
                    player.closeInventory();
                    fr.faction.alliance.HomeManager.SetHomeResult r = homeManager.setHome(player, name);
                    switch (r) {
                        case SUCCESS -> player.sendMessage("§8[§a🏠 Home§8] §r§aHome §e" + name + " §adéfini !");
                        case TOO_MANY_HOMES -> player.sendMessage("§8[§a🏠 Home§8] §r§cLimite atteinte.");
                        case TOO_CLOSE_TO_OTHER_HOME -> player.sendMessage("§8[§a🏠 Home§8] §r§cTrop proche d'un autre home (min 10 chunks).");
                        case NAME_TAKEN -> player.sendMessage("§8[§a🏠 Home§8] §r§cNom déjà utilisé.");
                    }
                    return;
                }
            }
            return;
        }

        // ── Sous-GUI : liste des homes (TP) ──────────────────────────────────────
        if (title.equals("§a§l🏠 Mes homes")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            int slot = e.getRawSlot();
            if (slot == 26) { navigateTo(player, "territoire"); return; }
            java.util.List<fr.faction.alliance.HomeManager.NamedHome> homes = homeManager.getHomes(player.getUniqueId());
            int[] slots = homeSlots(homes.size());
            for (int i = 0; i < slots.length && i < homes.size(); i++) {
                if (slot == slots[i]) {
                    final String name = homes.get(i).name;
                    player.closeInventory();
                    homeManager.teleportHome(player, name);
                    return;
                }
            }
            return;
        }

        // ── Sous-GUI : supprimer un home ─────────────────────────────────────────
        if (title.equals("§c§l🏠 Supprimer un home")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            int slot = e.getRawSlot();
            if (slot == 26) { navigateTo(player, "territoire"); return; }
            java.util.List<fr.faction.alliance.HomeManager.NamedHome> homes = homeManager.getHomes(player.getUniqueId());
            int[] slots = homeSlots(homes.size());
            for (int i = 0; i < slots.length && i < homes.size(); i++) {
                if (slot == slots[i]) {
                    final String name = homes.get(i).name;
                    player.closeInventory();
                    homeManager.deleteHome(player.getUniqueId(), name);
                    player.sendMessage("§8[§a🏠 Home§8] §r§cHome §f" + name + " §csupprimé.");
                    return;
                }
            }
            return;
        }

        // ── Pages principales ─────────────────────────────────────────────────────
        boolean isOurGUI = title.equals(T_HOME) || title.equals(T_GESTION) || title.equals(T_TERRIT)
                || title.equals(T_ECONO) || title.equals(T_ALLIANCE) || title.equals(T_STATS)
                || title.equals(T_AIDE);
        if (!isOurGUI) return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        int slot = e.getRawSlot();
        String page = openPage.getOrDefault(player.getUniqueId(), "home");

        // Barre de navigation (slots 45-51)
        if (slot >= 45 && slot <= 51) {
            String[] pages = {"home","gestion","territoire","economie","alliances","stats","aide"};
            String target = pages[slot - 45];
            navigateTo(player, target);
            return;
        }

        // Fermer / retour
        if (e.getCurrentItem().getType() == Material.BARRIER && slot == 49 && page.equals("home")) {
            player.closeInventory(); return;
        }
        if (e.getCurrentItem().getType() == Material.ARROW && slot == 49) {
            openHome(player); return;
        }

        // Actions spécifiques par page
        switch (page) {
            case "home"       -> handleHomeClick(player, slot);
            case "gestion"    -> handleGestionClick(player, slot);
            case "territoire" -> handleTerritoireClick(player, slot);
            case "economie"   -> handleEconomieClick(player, slot);
            case "alliances"  -> handleAlliancesClick(player, slot);
            case "stats"      -> handleStatsClick(player, slot);
        }
    }

    private void navigateTo(Player player, String page) {
        switch (page) {
            case "home"       -> openHome(player);
            case "gestion"    -> openGestion(player);
            case "territoire" -> openTerritoire(player);
            case "economie"   -> openEconomie(player);
            case "alliances"  -> openAlliances(player);
            case "stats"      -> openStats(player);
            case "aide"       -> openAide(player);
        }
    }

    private void handleHomeClick(Player player, int slot) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        switch (slot) {
            case 19 -> { if (faction != null) navigateTo(player, "gestion"); else cmd(player, "/faction list"); }
            case 20 -> { if (faction != null) { player.closeInventory(); sharedInvManager.openSharedInventory(player); } }
            case 21 -> navigateTo(player, "territoire");
            case 22 -> navigateTo(player, "alliances");
            case 23 -> navigateTo(player, "alliances");
            case 24 -> { player.closeInventory(); cmd(player, "/faction list"); }
            case 25 -> { player.closeInventory(); cmd(player, "/faction shop"); }
            case 49 -> player.closeInventory();
        }
    }

    private void handleGestionClick(Player player, int slot) {
        Faction f = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef = f != null && f.isChef(player.getUniqueId());
        boolean canMng = f != null && f.canManage(player.getUniqueId());
        switch (slot) {
            // Membres (tous)
            case 10 -> { // leave
                if (f != null && !isChef) { player.closeInventory(); cmd(player, "/faction leave"); }
                else if (isChef) send(player, "§cTu es chef. Transfère d'abord le rôle avec §e/fac setchef <joueur>§c.");
                else send(player, "§cTu n'es pas dans une faction.");
            }
            case 11 -> { player.closeInventory(); cmd(player, "/faction info"); }             // info ma faction
            case 12 -> { player.closeInventory(); cmd(player, "/faction list"); }             // info autre → list
            case 13 -> { player.closeInventory(); cmd(player, "/faction list"); }             // list
            case 14 -> { player.closeInventory(); cmd(player, "/faction top"); }              // top
            // Chef + sous-chef
            case 19 -> { // invite
                if (canMng) {
                    player.closeInventory();
                    send(player, "§eTape §b/fac invite <joueur> §epour inviter un joueur.");
                } else send(player, "§cRéservé au Chef ou à un sous-chef.");
            }
            case 20 -> { // kick
                if (canMng) {
                    player.closeInventory();
                    send(player, "§eTape §c/fac kick <joueur> §epour expulser un membre (sauf le chef).");
                } else send(player, "§cRéservé au Chef ou à un sous-chef.");
            }
            // Chef uniquement
            case 21 -> { // setchef
                if (isChef) {
                    player.closeInventory();
                    send(player, "§eTape §6/fac setchef <joueur> §epour transférer le rôle de Chef.");
                } else send(player, "§cRéservé au Chef.");
            }
            case 22 -> { // rename
                if (isChef) {
                    player.closeInventory();
                    send(player, "§eTape §b/fac rename <nouveau_nom> §epour renommer la faction.");
                } else send(player, "§cRéservé au Chef.");
            }
            case 23 -> { // setspawn
                if (canMng) { player.closeInventory(); cmd(player, "/faction setspawn"); }
                else send(player, "§cRéservé au Chef ou à un sous-chef.");
            }
            case 25 -> { // disband
                if (isChef) {
                    player.closeInventory();
                    // Ouvrir un sous-GUI de confirmation pour éviter les erreurs
                    openDisbandConfirm(player);
                } else send(player, "§cRéservé au Chef.");
            }
        }
    }

    /** Retourne le rang de la faction, ou null si faction null */
    private fr.faction.ranking.FactionRank rankForFaction(Faction f) {
        if (f == null || warManager == null) return null;
        // On passe par le FactionPowerManager via réflexion légère — il est dans le plugin
        try {
            org.bukkit.plugin.Plugin fp = org.bukkit.Bukkit.getPluginManager().getPlugin("FactionPlugin");
            if (fp == null) return null;
            var pm = fp.getClass().getMethod("getPowerManager").invoke(fp);
            var r  = pm.getClass().getMethod("getFactionRank", String.class).invoke(pm, f.getName());
            return (fr.faction.ranking.FactionRank) r;
        } catch (Exception e) { return null; }
    }

    /**
     * Sous-GUI de choix du spawn (1 ou 2).
     * @param isSet true = "setspawn", false = "spawn" (téléportation)
     */
    private void openSpawnChoiceMenu(Player player, Faction f, boolean isSet) {
        String title = isSet ? "§d§lChoisir un spawn à définir" : "§d§lChoisir un spawn";
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, title);
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Spawn 1
        boolean s1Defined = f.hasSpawn();
        inv.setItem(11, make(Material.RESPAWN_ANCHOR,
                "§a§l#1 Spawn Principal",
                s1Defined ? "§a✔ Défini" : "§7✘ Pas encore défini",
                f.hasSpawn() && !isSet ? "§7" + loc3(f.getFactionSpawn()) : "",
                "",
                isSet ? "§eClic → définir le spawn #1 ici" : (s1Defined ? "§eClic → se téléporter" : "§cNon défini")));

        // Spawn 2
        boolean s2Defined = f.hasSpawn2();
        inv.setItem(15, make(Material.LODESTONE,
                "§b§l#2 Spawn Secondaire",
                "§8Rang ◆ Diamant requis",
                s2Defined ? "§a✔ Défini" : "§7✘ Pas encore défini",
                f.hasSpawn2() && !isSet ? "§7" + loc3(f.getFactionSpawn2()) : "",
                "",
                isSet ? "§eClic → définir le spawn #2 ici" : (s2Defined ? "§eClic → se téléporter" : "§cNon défini")));

        inv.setItem(22, make(Material.ARROW, "§7◀ Annuler", ""));
        player.openInventory(inv);
    }

    private String loc3(org.bukkit.Location loc) {
        if (loc == null) return "";
        return loc.getWorld().getName() + " (" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + ")";
    }

    private void openDisbandConfirm(Player player) {
        Faction f = factionManager.getPlayerFaction(player.getUniqueId());
        if (f == null) return;
        Inventory conf = Bukkit.createInventory(null, 27, "§c§lDissoudre " + f.getName() + " ?");
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) conf.setItem(i, glass);
        conf.setItem(11, make(Material.LIME_STAINED_GLASS_PANE, "§a§l✔ Confirmer",
                "§7La faction §e" + f.getName() + " §7sera définitivement supprimée.",
                "§c⚠ Action irréversible."));
        conf.setItem(13, make(Material.BARRIER, "§c§l⚠ DISSOUDRE LA FACTION",
                "§7Faction : §e" + f.getName(),
                "§7Membres : §f" + f.getMemberCount(),
                "§7Claims : §f" + (f.getMemberCount()),
                "", "§cCette action est irréversible."));
        conf.setItem(15, make(Material.RED_STAINED_GLASS_PANE, "§c§l✗ Annuler", "§7Retour au menu de gestion."));
        player.openInventory(conf);
    }

    private void handleTerritoireClick(Player player, int slot) {
        Faction f = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef = f != null && f.isChef(player.getUniqueId());
        boolean canMng = f != null && f.canManage(player.getUniqueId());
        switch (slot) {
            case 10 -> { // claim
                if (canMng) { player.closeInventory(); cmd(player, "/faction claim"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut claimer des chunks.");
            }
            case 11 -> { // unclaim
                if (canMng) { player.closeInventory(); cmd(player, "/faction unclaim"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut supprimer des claims.");
            }
            case 12 -> { player.closeInventory(); cmd(player, "/faction claims"); }           // liste claims
            case 13 -> { player.closeInventory(); cmd(player, "/faction claimmap"); }         // carte
            case 14 -> { // perms
                if (isChef) { player.closeInventory(); cmd(player, "/faction perms"); }
                else send(player, "§cSeul le Chef peut gérer les permissions de chunk.");
            }
            case 19 -> { // spawn faction — ouvrir un sous-menu si deux spawns
                if (f == null) { send(player, "§cTu n'es pas dans une faction."); return; }
                if (!f.hasSpawn() && !f.hasSpawn2()) {
                    send(player, "§cAucun spawn défini. Le chef ou un sous-chef peut faire §e/fac setspawn§c.");
                    return;
                }
                if (f.hasSpawn() && f.hasSpawn2()) {
                    // Deux spawns → ouvrir mini-menu de choix
                    player.closeInventory();
                    openSpawnChoiceMenu(player, f, false);
                } else {
                    player.closeInventory();
                    cmd(player, f.hasSpawn() ? "/faction spawn 1" : "/faction spawn 2");
                }
            }
            case 20 -> { // setspawn
                if (!canMng) { send(player, "§cSeul le Chef ou un sous-chef peut définir le spawn."); return; }
                fr.faction.ranking.FactionRank rank = rankForFaction(f);
                if (rank != null && rank.getMaxSpawns() >= 2) {
                    player.closeInventory();
                    openSpawnChoiceMenu(player, f, true);
                } else {
                    player.closeInventory();
                    cmd(player, "/faction setspawn 1");
                }
            }
            case 22 -> { // sethome — ouvrir un sous-GUI nommé
                player.closeInventory();
                openSetHomeMenu(player);
            }
            case 23 -> { // home — lister et TP
                player.closeInventory();
                openHomeListMenu(player);
            }
            case 24 -> { // delhome
                player.closeInventory();
                openDelHomeMenu(player);
            }
            case 28 -> send(player, "§e🔒 Coffres privés : §7Sneak + §eclic droit §7avec un §epanneau §7en main sur n'importe quel coffre.");
            case 31 -> { // tpa
                player.closeInventory();
                send(player, "§eTape §b/fac tpa <joueur> §epour demander à te téléporter vers un joueur.");
            }
            case 32 -> { player.closeInventory(); cmd(player, "/faction tpaccept"); }
            case 33 -> { player.closeInventory(); cmd(player, "/faction tpdeny"); }
            case 34 -> { // tp membre
                if (f != null) { player.closeInventory(); cmd(player, "/faction tp"); }
                else send(player, "§cTu n'es pas dans une faction.");
            }
        }
    }

    // ── Sous-GUI homes ──────────────────────────────────────────────────────────

    /** Distribue jusqu'à 5 homes dans un inventaire 9×N. */
    private int[] homeSlots(int count) {
        // Inventaire 27 slots (3 rangées) : on centre les items
        return switch (count) {
            case 1 -> new int[]{13};
            case 2 -> new int[]{11, 15};
            case 3 -> new int[]{10, 13, 16};
            case 4 -> new int[]{10, 12, 14, 16};
            default -> new int[]{9, 11, 13, 15, 17}; // 5 homes
        };
    }

    private String homeNameFor(int index) {
        return index == 0 ? "home" : "home" + (index + 1);
    }

    private void openSetHomeMenu(Player player) {
        int max = homeManager.getMaxHomes(player.getUniqueId());
        java.util.List<fr.faction.alliance.HomeManager.NamedHome> existing = homeManager.getHomes(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "§a§l🏠 Définir un home");
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        int[] slots = homeSlots(max);
        for (int i = 0; i < max; i++) {
            final String name = homeNameFor(i);
            fr.faction.alliance.HomeManager.NamedHome existing_h = existing.stream()
                    .filter(h -> h.name.equalsIgnoreCase(name)).findFirst().orElse(null);
            if (existing_h != null) {
                String coords = "§8x" + (int)existing_h.location.getX()
                        + " y" + (int)existing_h.location.getY()
                        + " z" + (int)existing_h.location.getZ();
                inv.setItem(slots[i], make(Material.LIME_BED, "§a§l" + name + " §8(défini)",
                        coords, "", "§eClic → redéfinir à ta position actuelle"));
            } else {
                inv.setItem(slots[i], make(Material.GRAY_BED, "§7" + name + " §8(libre)",
                        "§eClic → définir à ta position actuelle"));
            }
        }
        // Rangée inférieure : indication du rang si pas encore au max
        if (max < 5) {
            String[] ranks = {"—", "Bronze (→2)", "Or/Diamant (→3)", "Émeraude (→4)", "Légendaire (→5)"};
            inv.setItem(22, make(Material.NETHER_STAR, "§7" + max + "/" + 5 + " homes débloqués",
                    "§7Prochain déblocage : §e" + (max < ranks.length ? ranks[max] : "MAX")));
        }
        inv.setItem(26, make(Material.ARROW, "§7◀ Retour", ""));
        player.openInventory(inv);
    }

    private void openHomeListMenu(Player player) {
        java.util.List<fr.faction.alliance.HomeManager.NamedHome> homes = homeManager.getHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            send(player, "§cAucun home défini. Ouvre §eTerritoire & TP §cet clique sur §e🏠 Définir.");
            navigateTo(player, "territoire");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, "§a§l🏠 Mes homes");
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        int[] slots = homeSlots(homes.size());
        for (int i = 0; i < homes.size() && i < slots.length; i++) {
            fr.faction.alliance.HomeManager.NamedHome h = homes.get(i);
            inv.setItem(slots[i], make(Material.ENDER_PEARL, "§a§l» " + h.name,
                    "§7Monde : §f" + h.location.getWorld().getName(),
                    "§7Position : §f" + (int)h.location.getX() + "§7, §f" + (int)h.location.getY() + "§7, §f" + (int)h.location.getZ(),
                    "", "§eClic → se téléporter"));
        }
        inv.setItem(26, make(Material.ARROW, "§7◀ Retour", ""));
        player.openInventory(inv);
    }

    private void openDelHomeMenu(Player player) {
        java.util.List<fr.faction.alliance.HomeManager.NamedHome> homes = homeManager.getHomes(player.getUniqueId());
        if (homes.isEmpty()) { send(player, "§cAucun home à supprimer."); navigateTo(player, "territoire"); return; }
        Inventory inv = Bukkit.createInventory(null, 27, "§c§l🏠 Supprimer un home");
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        int[] slots = homeSlots(homes.size());
        for (int i = 0; i < homes.size() && i < slots.length; i++) {
            fr.faction.alliance.HomeManager.NamedHome h = homes.get(i);
            inv.setItem(slots[i], make(Material.RED_BED, "§c§l✗ " + h.name,
                    "§7" + (int)h.location.getX() + ", " + (int)h.location.getY() + ", " + (int)h.location.getZ(),
                    "", "§cClic → supprimer"));
        }
        inv.setItem(26, make(Material.ARROW, "§7◀ Retour", ""));
        player.openInventory(inv);
    }

    private void handleEconomieClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { player.closeInventory(); cmd(player, "/faction shop"); }
            case 11 -> { // vendre — info
                player.closeInventory();
                send(player, "§eTiens un item en main et tape : §b/fac vendre <prix> <fer|or|diamant|emeraude>");
            }
            case 12 -> { // acheter — ouvrir le shop directement
                player.closeInventory(); cmd(player, "/faction shop");
            }
            case 13 -> { player.closeInventory(); cmd(player, "/faction recuperer"); }        // recuperer liste
            case 14 -> { player.closeInventory(); cmd(player, "/faction mesannonces"); }      // mes annonces GUI
            case 19 -> { player.closeInventory(); cmd(player, "/faction banque"); }
            case 20 -> { player.closeInventory(); cmd(player, "/faction topbanque"); }        // top banque
            case 22 -> { player.closeInventory(); sharedInvManager.openSharedInventory(player); }
            case 23 -> { player.closeInventory(); cmd(player, "/faction ranger"); }
            case 25 -> {
                player.closeInventory();
                send(player, "§eTape §b/fac troc <joueur> §epour proposer un troc à un joueur en ligne.");
            }
            case 26 -> { player.closeInventory(); cmd(player, "/faction accepter"); }         // accepter troc
        }
    }

    private void handleAlliancesClick(Player player, int slot) {
        Faction f = factionManager.getPlayerFaction(player.getUniqueId());
        boolean isChef = f != null && f.isChef(player.getUniqueId());
        boolean canMng = f != null && f.canManage(player.getUniqueId());
        switch (slot) {
            case 10 -> { player.closeInventory(); cmd(player, "/faction alliance gui"); }     // GUI alliances
            case 11 -> { // inviter
                if (canMng) { player.closeInventory(); send(player, "§eTape §b/fac alliance inviter <faction>"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut inviter des alliés.");
            }
            case 12 -> { // accepter alliance
                if (canMng) { player.closeInventory(); cmd(player, "/faction alliance liste"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut accepter des alliances.");
            }
            case 13 -> { // refuser — afficher liste des invitations
                player.closeInventory(); cmd(player, "/faction alliance liste");
            }
            case 14 -> { // rompre
                if (canMng) { player.closeInventory(); send(player, "§eTape §c/fac alliance rompre <faction>"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut rompre une alliance.");
            }
            case 15 -> { player.closeInventory(); cmd(player, "/faction alliance liste"); }   // liste

            // Guerre
            case 28 -> { // déclarer ou voir statut
                player.closeInventory();
                if (warManager != null && f != null && warManager.isAtWar(f.getName())) {
                    cmd(player, "/faction guerre statut");
                } else if (canMng) {
                    send(player, "§eTape §c/fac guerre declarer <faction> [claims:0-5] [pillage] [kills:5-50]");
                } else {
                    send(player, "§cSeul le Chef ou un sous-chef peut déclarer la guerre.");
                }
            }
            case 29 -> { // accepter guerre
                if (canMng) { player.closeInventory(); cmd(player, "/faction guerre accepter"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut accepter une guerre.");
            }
            case 30 -> { // refuser guerre
                if (canMng) { player.closeInventory(); cmd(player, "/faction guerre refuser"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut refuser une guerre.");
            }
            case 31 -> { player.closeInventory(); cmd(player, "/faction guerre statut"); }    // statut
            case 32 -> { player.closeInventory(); cmd(player, "/faction guerre liste"); }     // liste guerres
            case 33 -> { // capituler
                if (isChef) { player.closeInventory(); openSurrenderConfirm(player); }
                else send(player, "§cSeul le Chef peut capituler.");
            }
            case 34 -> { // piller
                if (canMng) { player.closeInventory(); cmd(player, "/faction guerre piller"); }
                else send(player, "§cSeul le Chef ou un sous-chef peut lancer le pillage.");
            }
        }
    }

    private void openSurrenderConfirm(Player player) {
        Inventory conf = Bukkit.createInventory(null, 27, "§c§l⚔ Capituler ?");
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) conf.setItem(i, glass);
        conf.setItem(11, make(Material.LIME_STAINED_GLASS_PANE, "§a§l✔ Confirmer la capitulation",
                "§7Ta faction perdra la guerre.", "§c⚠ Le vainqueur gagnera les enjeux négociés."));
        conf.setItem(13, make(Material.WHITE_BANNER, "§f§l⚑ Capitulation",
                "§7En capitulant, tu mets fin immédiatement à la guerre.", "§7Le vainqueur obtient les enjeux négociés."));
        conf.setItem(15, make(Material.RED_STAINED_GLASS_PANE, "§c§l✗ Continuer à se battre", "§7Retour."));
        player.openInventory(conf);
    }

    private void handleStatsClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { player.closeInventory(); cmd(player, "/faction classement"); }
            case 11 -> { player.closeInventory(); cmd(player, "/faction rangs"); }
            case 12 -> { player.closeInventory(); cmd(player, "/faction top"); }
            case 13 -> { player.closeInventory(); cmd(player, "/faction topbanque"); }
            case 14 -> { player.closeInventory(); cmd(player, "/faction power"); }
            case 15 -> { player.closeInventory(); cmd(player, "/faction stats"); }
            case 19 -> { player.closeInventory(); cmd(player, "/faction classementjoueurs pvp"); }
            case 21 -> { player.closeInventory(); cmd(player, "/faction classementjoueurs mobs"); }
            // Boutons de rangs visuels (slots 27-33) — afficher les infos du rang
            case 27, 28, 29, 30, 31, 32, 33 -> {
                player.closeInventory(); cmd(player, "/faction rangs");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // BUILDERS & UTILS
    // ════════════════════════════════════════════════════════════════════════════

    private ItemStack make(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) { if (s != null) for (String line : s.split("\n")) l.add(line); }
            meta.setLore(l);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        is.setItemMeta(meta);
        return is;
    }

    /** Item grisé si disabled */
    private ItemStack cmdItem(Material mat, String name, String desc, boolean enabled, String... extraLore) {
        if (enabled) {
            if (extraLore != null && extraLore.length > 0) {
                String[] lore = new String[extraLore.length + 2];
                lore[0] = desc;
                System.arraycopy(extraLore, 0, lore, 1, extraLore.length);
                lore[lore.length - 1] = "§7Clic pour info";
                return make(mat, name, lore);
            }
            return make(mat, name, desc, "", "§7Clic pour info");
        }
        return make(Material.GRAY_STAINED_GLASS_PANE, "§8" + ChatColor.stripColor(name),
                "§8Réservé au §7Chef §8ou non disponible.");
    }

    private ItemStack glowing(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        is.setItemMeta(meta);
        return is;
    }

    private void fillBorder(Inventory inv, Material mat) {
        ItemStack border = make(mat, " ");
        for (int i = 0; i < 9; i++)     inv.setItem(i, border);
        for (int i = 45; i < 54; i++)   inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 54; i += 9) inv.setItem(i, border);
    }

    private String getPlayerName(java.util.UUID uuid) {
        org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "Inconnu";
    }

    private void cmd(Player player, String command) {
        player.performCommand(command.substring(1));
    }

    private void send(Player player, String msg) {
        player.sendMessage(PREFIX_PAGES + msg);
    }

    private static Material TRADING_SIGN_ITEM() { return Material.PAPER; }
    private static Material WHITE_FLAG_ITEM()    { return Material.WHITE_BANNER; }
    private static Material HOUSE_BANNER_ITEM()  { return Material.WHITE_BANNER; }
}
