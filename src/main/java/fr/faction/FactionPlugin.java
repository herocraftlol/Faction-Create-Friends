package fr.faction;

import fr.faction.alliance.AllianceManager;
import fr.faction.alliance.HomeManager;
import fr.faction.alliance.PlayerTeleportManager;
import fr.faction.alliance.PrivateChestManager;
import fr.faction.claim.ClaimListener;
import fr.faction.claim.ClaimManager;
import fr.faction.claim.ClaimPermissionGUI;
import fr.faction.commands.FactionCommand;
import fr.faction.economy.BankGUI;
import fr.faction.economy.EmeraldBankManager;
import fr.faction.gui.FactionGUI;
import fr.faction.gui.FactionRankingGUI;
import fr.faction.listeners.PlayerListener;
import fr.faction.managers.ActionBarManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.managers.PlaytimeTracker;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.PowerBridgeListener;
import fr.faction.shop.InvSeeGUI;
import fr.faction.shop.ShopGUI;
import fr.faction.shop.ShopManager;
import fr.faction.trade.TradeGUI;
import fr.faction.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionPlugin extends JavaPlugin {

    private FactionManager factionManager;
    private PlayerStatsManager statsManager;
    private ActionBarManager actionBarManager;
    private SharedInventoryManager sharedInventoryManager;
    private FactionTeleportManager teleportManager;
    private FactionGUI factionGUI;
    private FactionPowerManager powerManager;
    private fr.faction.power.FactionTabManager tabManager;
    private FactionRankingGUI rankingGUI;
    private PlaytimeTracker playtimeTracker;

    // v3.2
    private ClaimManager claimManager;
    private ClaimPermissionGUI claimPermissionGUI;
    private fr.faction.claim.ClaimVisualizer claimVisualizer;
    private fr.faction.map.FactionMapManager mapManager;
    private EmeraldBankManager bankManager;
    private BankGUI bankGUI;
    private TradeManager tradeManager;
    private TradeGUI tradeGUI;

    // v4.0 — shop & admin
    private ShopManager shopManager;
    private ShopGUI shopGUI;
    private fr.faction.shop.ShopCreateGUI shopCreateGUI;
    private InvSeeGUI invSeeGUI;

    // v5.0 — alliances, homes, coffres privés, tpa
    private AllianceManager allianceManager;
    private HomeManager homeManager;
    private PrivateChestManager privateChestManager;
    private PlayerTeleportManager playerTeleportManager;

    // v5.1 — guerre, GUI principal
    private fr.faction.war.WarManager warManager;
    private fr.faction.gui.MainMenuGUI mainMenuGUI;

    // v5.2 — tri de coffre
    private fr.faction.sort.SortMenuGUI sortMenuGUI;
    private fr.faction.web.WebLinkManager webLinkManager;
    private fr.faction.web.WebMapSync webMapSync;

    // v5.9 — villageois recrutés
    private fr.faction.villager.VillagerManager villagerManager;
    private fr.faction.villager.VillagerGUI villagerGUI;
    private fr.faction.village.VillageManager villageManager;
    private fr.faction.commerce.CommerceManager commerceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        factionManager         = new FactionManager(this);
        statsManager           = new PlayerStatsManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);
        powerManager           = new FactionPowerManager(this, factionManager, statsManager);
        // tabManager doit être créé avant powerManager.start() pour le rankUp
        tabManager             = new fr.faction.power.FactionTabManager(this, factionManager, powerManager);
        powerManager.setTabManager(tabManager);
        powerManager.start();

        claimManager       = new ClaimManager(this);
        claimPermissionGUI = new ClaimPermissionGUI(this, claimManager, factionManager);
        claimVisualizer    = new fr.faction.claim.ClaimVisualizer(this, claimManager, factionManager);
        mapManager         = new fr.faction.map.FactionMapManager(this, factionManager);
        bankManager        = new EmeraldBankManager(this);
        bankGUI            = new BankGUI(this, bankManager, factionManager);
        tradeManager       = new TradeManager();
        tradeGUI           = new TradeGUI(this, tradeManager);

        shopManager    = new ShopManager(this);
        shopGUI        = new ShopGUI(this, shopManager);
        shopCreateGUI  = new fr.faction.shop.ShopCreateGUI(this, shopManager);
        shopGUI.setCreateGUI(shopCreateGUI);
        invSeeGUI   = new InvSeeGUI(this);

        allianceManager       = new AllianceManager(this, factionManager);
        homeManager           = new HomeManager(this, factionManager);
        mapManager.setHomeManager(homeManager);
        // Injecter powerManager dans HomeManager (créé après)
        privateChestManager   = new PrivateChestManager(this, factionManager);
        playerTeleportManager = new PlayerTeleportManager(this);

        // Injection du bonus d'alliance dans le calcul de puissance
        powerManager.setAllianceManager(allianceManager);
        // Maintenant qu'on a powerManager, l'injecter dans homeManager
        homeManager.setPowerManager(powerManager);

        // GUIs (doivent être créés avant mainMenuGUI qui en dépend)
        factionGUI       = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);
        rankingGUI       = new FactionRankingGUI(this, factionManager, powerManager, bankManager);
        actionBarManager = new ActionBarManager(this, factionManager);


        // ── v5.1 — Guerre & GUI principal ───────────────────────────────────
        warManager  = new fr.faction.war.WarManager(this, factionManager, claimManager, sharedInventoryManager);
        mainMenuGUI = new fr.faction.gui.MainMenuGUI(this, factionManager, sharedInventoryManager,
                teleportManager, powerManager, allianceManager, homeManager, warManager, factionGUI, rankingGUI);

        // ── v5.2 — Tri de coffre ─────────────────────────────────────────────
        sortMenuGUI = new fr.faction.sort.SortMenuGUI(this, factionManager, sharedInventoryManager);
        sharedInventoryManager.setSortMenuGUI(sortMenuGUI);

        // ── v5.9 — Villageois recrutés ───────────────────────────────────────
        villagerManager = new fr.faction.villager.VillagerManager(this, factionManager);
        villagerManager.setClaimManager(claimManager);
        villagerManager.setWarManager(warManager);
        villagerManager.setPowerManager(powerManager);
        villagerGUI = new fr.faction.villager.VillagerGUI(this, factionManager, villagerManager);

        // ── v5.11 — Villages ─────────────────────────────────────────────────
        villageManager = new fr.faction.village.VillageManager(this, factionManager);
        villageManager.setClaimManager(claimManager);
        villageManager.setVillagerManager(villagerManager);
        villagerManager.setVillageManager(villageManager);
        villagerGUI.setVillageManager(villageManager);

        // ── v5.12 — Commerce inter-villes (ports/gares, contrats) ─────────────
        commerceManager = new fr.faction.commerce.CommerceManager(this, factionManager);
        commerceManager.setVillageManager(villageManager);
        commerceManager.setVillagerManager(villagerManager);
        villagerManager.setCommerceManager(commerceManager);

        villagerManager.start();

        // ── Liaison compte web (/lier) ────────────────────────────────────────────
        webLinkManager = new fr.faction.web.WebLinkManager(this);
        String siteUrl = getConfig().getString("site-url", "http://localhost:3000");
        getCommand("lier").setExecutor(new fr.faction.web.LierCommand(webLinkManager, siteUrl));

        // ── WebMap sync (remplace le plugin FactionWebMap séparé) ────────────────
        webMapSync = new fr.faction.web.WebMapSync(this, factionManager, powerManager);
        webMapSync.setClaimManager(claimManager);
        getServer().getPluginManager().registerEvents(webMapSync, this);

        FactionCommand cmd = new FactionCommand(
                this, factionManager, statsManager, sharedInventoryManager, teleportManager,
                factionGUI, rankingGUI, powerManager,
                claimManager, claimPermissionGUI, bankGUI, bankManager,
                tradeManager, tradeGUI,
                shopManager, shopGUI, invSeeGUI,
                allianceManager, homeManager, privateChestManager, playerTeleportManager);

        // Injection post-construction (warManager/mainMenuGUI créés après)
        cmd.setWarManager(warManager);
        cmd.setMainMenuGUI(mainMenuGUI);
        cmd.setSortMenuGUI(sortMenuGUI);
        cmd.setShopCreateGUI(shopCreateGUI);
        cmd.setClaimVisualizer(claimVisualizer);
        cmd.setTabManager(tabManager);
        cmd.setMapManager(mapManager);
        cmd.setVillagerManager(villagerManager);
        cmd.setVillagerGUI(villagerGUI);
        cmd.setVillageManager(villageManager);
        cmd.setCommerceManager(commerceManager);
        cmd.setWebMapSync(webMapSync);
        actionBarManager.setWarManager(warManager);

        getCommand("faction").setExecutor(cmd);
        getCommand("faction").setTabCompleter(cmd);
        getCommand("tpa").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p && a.length >= 1)
                playerTeleportManager.sendRequest(p, a[0]);
            return true;
        });
        getCommand("tpaccept").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p)
                playerTeleportManager.acceptRequest(p);
            return true;
        });
        getCommand("tpdeny").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p)
                playerTeleportManager.denyRequest(p);
            return true;
        });
        getCommand("sethome").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p) {
                String n = a.length >= 1 ? a[0] : "home";
                handleSetHome(p, n);
            }
            return true;
        });
        getCommand("home").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p) {
                String n = a.length >= 1 ? a[0] : "home";
                homeManager.teleportHome(p, n);
            }
            return true;
        });
        getCommand("home").setTabCompleter((sender, c, l, a) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return java.util.Collections.emptyList();
            if (a.length == 1) {
                String prefix = a[0].toLowerCase();
                return homeManager.getHomeNames(p.getUniqueId()).stream()
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });
        getCommand("delhome").setTabCompleter((sender, c, l, a) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return java.util.Collections.emptyList();
            if (a.length == 1) {
                String prefix = a[0].toLowerCase();
                return homeManager.getHomeNames(p.getUniqueId()).stream()
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });
        getCommand("delhome").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p && a.length >= 1) {
                boolean ok = homeManager.deleteHome(p.getUniqueId(), a[0]);
                p.sendMessage("§8[§a🏠 Home§8] §r" + (ok ? "§cHome §f" + a[0] + " §csupprimé." : "§cHome introuvable."));
            }
            return true;
        });
        getCommand("homes").setExecutor((sender, c, l, a) -> {
            if (sender instanceof org.bukkit.entity.Player p) listHomes(p);
            return true;
        });

        PlayerListener playerListener = new PlayerListener(factionManager, statsManager, powerManager, shopManager, shopGUI);
        playerListener.setWarManager(warManager);
        playerListener.setHomeManager(homeManager);
        playerListener.setTabManager(tabManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(
                new PowerBridgeListener(factionManager, powerManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager, factionManager), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(new fr.faction.listeners.FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(shopCreateGUI, this);
        getServer().getPluginManager().registerEvents(invSeeGUI, this);
        getServer().getPluginManager().registerEvents(allianceManager, this);
        getServer().getPluginManager().registerEvents(privateChestManager, this);
        getServer().getPluginManager().registerEvents(warManager, this);
        getServer().getPluginManager().registerEvents(mainMenuGUI, this);
        getServer().getPluginManager().registerEvents(sortMenuGUI, this);
        getServer().getPluginManager().registerEvents(villagerManager, this);
        getServer().getPluginManager().registerEvents(villagerGUI, this);
        getServer().getPluginManager().registerEvents(villageManager, this);
        getServer().getPluginManager().registerEvents(tradeManager, this);
        getServer().getPluginManager().registerEvents(commerceManager, this);

        actionBarManager.start();
        playtimeTracker = new PlaytimeTracker(this, statsManager);
        playtimeTracker.start();

        // Rafraîchir le tab de tous les joueurs toutes les 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tabManager.refreshAll();
            tabManager.pruneEmptyTeams();
        }, 20L * 10, 20L * 300);

        // ── Purge unique des effets bannis des anciennes versions ────────────────
        // Lance 2 secondes après le démarrage pour couvrir les joueurs déjà
        // connectés (si rechargement du plugin avec /reload).
        Bukkit.getScheduler().runTaskLater(this, () -> {
            java.util.List<org.bukkit.potion.PotionEffectType> legacy = java.util.List.of(
                    org.bukkit.potion.PotionEffectType.SPEED,
                    org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                    org.bukkit.potion.PotionEffectType.SLOW_FALLING
            );
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                for (org.bukkit.potion.PotionEffectType t : legacy) p.removePotionEffect(t);
            }
            getLogger().info("Purge des effets legacy effectuée.");
        }, 40L);

        getLogger().info("FactionPlugin v5.5.1 — purge effets legacy, fixes bank/troc/shop/home");
    }

    private void handleSetHome(org.bukkit.entity.Player player, String name) {
        HomeManager.SetHomeResult r = homeManager.setHome(player, name);
        int max = homeManager.getMaxHomes(player.getUniqueId());
        String pf = "§8[§a🏠 Home§8] §r";
        switch (r) {
            case SUCCESS -> player.sendMessage(pf + "§aHome §e" + name + " §adéfini !");
            case TOO_MANY_HOMES -> player.sendMessage(pf + "§cTu as atteint la limite de §e" + max + " §chome(s). "
                    + "§7(rejoins une faction ou allie-toi pour en débloquer plus)");
            case TOO_CLOSE_TO_OTHER_HOME -> player.sendMessage(pf
                    + "§cImpossible : un home d'un autre joueur est à moins de §e10 chunks§c. "
                    + "§7(sauf membres de ta faction ou faction alliée)");
            case NAME_TAKEN -> player.sendMessage(pf + "§cNom déjà utilisé.");
        }
    }

    private void listHomes(org.bukkit.entity.Player player) {
        String pf = "§8[§a🏠 Home§8] §r";
        var list = homeManager.getHomes(player.getUniqueId());
        int max  = homeManager.getMaxHomes(player.getUniqueId());
        player.sendMessage("§a══ Tes homes (" + list.size() + "/" + max + ") ══");
        if (list.isEmpty()) {
            player.sendMessage(pf + "§7Aucun home. Utilise §e/sethome <nom>§7.");
        } else {
            for (HomeManager.NamedHome h : list) {
                player.sendMessage("  §e" + h.name + " §7→ §f"
                        + h.location.getWorld().getName()
                        + " §7(" + (int)h.location.getX() + ", " + (int)h.location.getY()
                        + ", " + (int)h.location.getZ() + ")");
            }
        }
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null)       actionBarManager.stop();
        if (playtimeTracker != null)        playtimeTracker.stop();
        if (powerManager != null)           powerManager.stop();
        if (sharedInventoryManager != null) sharedInventoryManager.saveInventories();
        if (statsManager != null)           statsManager.saveAll();
        if (factionManager != null)         factionManager.saveFactions();
        if (claimManager != null)           claimManager.save();
        if (bankManager != null)            bankManager.save();
        if (shopManager != null)            shopManager.save();
        if (mapManager != null)             mapManager.save();
        if (homeManager != null)            homeManager.save();
        if (privateChestManager != null)    privateChestManager.save();
        if (warManager != null)             { warManager.save(); warManager.stop(); }
        if (villagerManager != null)        villagerManager.save();
        if (villageManager != null)         villageManager.save();
        if (tradeManager != null)           tradeManager.save();
        if (commerceManager != null)        commerceManager.save();
        if (webLinkManager != null)         webLinkManager.close();
        getLogger().info("FactionPlugin désactivé. Données sauvegardées.");
    }

    // Getters
    public FactionManager getFactionManager()              { return factionManager; }
    public PlayerStatsManager getStatsManager()            { return statsManager; }
    public ActionBarManager getActionBarManager()          { return actionBarManager; }
    public SharedInventoryManager getSharedInvManager()   { return sharedInventoryManager; }
    public FactionTeleportManager getTeleportManager()     { return teleportManager; }
    public FactionGUI getFactionGUI()                      { return factionGUI; }
    public FactionPowerManager getPowerManager()           { return powerManager; }
    public FactionRankingGUI getRankingGUI()               { return rankingGUI; }
    public ClaimManager getClaimManager()                  { return claimManager; }
    public fr.faction.claim.ClaimVisualizer getClaimVisualizer() { return claimVisualizer; }
    public EmeraldBankManager getBankManager()             { return bankManager; }
    public TradeManager getTradeManager()                  { return tradeManager; }
    public TradeGUI getTradeGUI()                          { return tradeGUI; }
    public ShopManager getShopManager()                    { return shopManager; }
    public ShopGUI getShopGUI()                            { return shopGUI; }
    public InvSeeGUI getInvSeeGUI()                        { return invSeeGUI; }
    public AllianceManager getAllianceManager()             { return allianceManager; }
    public HomeManager getHomeManager()                    { return homeManager; }
    public PrivateChestManager getPrivateChestManager()    { return privateChestManager; }
    public PlayerTeleportManager getPlayerTeleportManager(){ return playerTeleportManager; }
    public fr.faction.war.WarManager getWarManager()              { return warManager; }
    public fr.faction.gui.MainMenuGUI getMainMenuGUI()            { return mainMenuGUI; }
    public fr.faction.power.FactionTabManager getTabManager()     { return tabManager; }
    public fr.faction.sort.SortMenuGUI getSortMenuGUI()    { return sortMenuGUI; }
    public fr.faction.map.FactionMapManager getMapManager() { return mapManager; }
    public fr.faction.villager.VillagerManager getVillagerManager() { return villagerManager; }
    public fr.faction.village.VillageManager getVillageManager() { return villageManager; }
    public fr.faction.commerce.CommerceManager getCommerceManager() { return commerceManager; }
    public fr.faction.villager.VillagerGUI getVillagerGUI() { return villagerGUI; }
}
