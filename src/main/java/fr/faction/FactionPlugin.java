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
import org.bukkit.plugin.java.JavaPlugin;

public class FactionPlugin extends JavaPlugin {

    private FactionManager factionManager;
    private PlayerStatsManager statsManager;
    private ActionBarManager actionBarManager;
    private SharedInventoryManager sharedInventoryManager;
    private FactionTeleportManager teleportManager;
    private FactionGUI factionGUI;
    private FactionPowerManager powerManager;
    private FactionRankingGUI rankingGUI;
    private PlaytimeTracker playtimeTracker;

    // v3.2
    private ClaimManager claimManager;
    private ClaimPermissionGUI claimPermissionGUI;
    private EmeraldBankManager bankManager;
    private BankGUI bankGUI;
    private TradeManager tradeManager;
    private TradeGUI tradeGUI;

    // v4.0 — shop & admin
    private ShopManager shopManager;
    private ShopGUI shopGUI;
    private InvSeeGUI invSeeGUI;

    // v5.0 — alliances, homes, coffres privés, tpa
    private AllianceManager allianceManager;
    private HomeManager homeManager;
    private PrivateChestManager privateChestManager;
    private PlayerTeleportManager playerTeleportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        factionManager         = new FactionManager(this);
        statsManager           = new PlayerStatsManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);
        powerManager           = new FactionPowerManager(this, factionManager, statsManager);
        powerManager.start();

        claimManager       = new ClaimManager(this);
        claimPermissionGUI = new ClaimPermissionGUI(this, claimManager, factionManager);
        bankManager        = new EmeraldBankManager(this);
        bankGUI            = new BankGUI(this, bankManager, factionManager);
        tradeManager       = new TradeManager();
        tradeGUI           = new TradeGUI(this, tradeManager);

        shopManager = new ShopManager(this);
        shopGUI     = new ShopGUI(this, shopManager);
        invSeeGUI   = new InvSeeGUI(this);

        allianceManager       = new AllianceManager(this, factionManager);
        homeManager           = new HomeManager(this, factionManager);
        privateChestManager   = new PrivateChestManager(this, factionManager);
        playerTeleportManager = new PlayerTeleportManager(this);

        // Injection du bonus d'alliance dans le calcul de puissance
        powerManager.setAllianceManager(allianceManager);

        factionGUI       = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);
        rankingGUI       = new FactionRankingGUI(this, factionManager, powerManager, bankManager);
        actionBarManager = new ActionBarManager(this, factionManager);

        FactionCommand cmd = new FactionCommand(
                this, factionManager, statsManager, sharedInventoryManager, teleportManager,
                factionGUI, rankingGUI, powerManager,
                claimManager, claimPermissionGUI, bankGUI, bankManager,
                tradeManager, tradeGUI,
                shopManager, shopGUI, invSeeGUI,
                allianceManager, homeManager, privateChestManager, playerTeleportManager);

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

        getServer().getPluginManager().registerEvents(
                new PlayerListener(factionManager, statsManager, powerManager, shopManager, shopGUI), this);
        getServer().getPluginManager().registerEvents(
                new PowerBridgeListener(factionManager, powerManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager, factionManager), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(invSeeGUI, this);
        getServer().getPluginManager().registerEvents(allianceManager, this);
        getServer().getPluginManager().registerEvents(privateChestManager, this);

        actionBarManager.start();
        playtimeTracker = new PlaytimeTracker(this, statsManager);
        playtimeTracker.start();

        getLogger().info("FactionPlugin v5.0 actif — Alliances, Homes, Coffres privés, TPA !");
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
        if (homeManager != null)            homeManager.save();
        if (privateChestManager != null)    privateChestManager.save();
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
}
