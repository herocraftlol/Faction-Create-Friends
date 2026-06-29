package fr.faction;

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

    // Nouveaux systèmes
    private ClaimManager claimManager;
    private ClaimPermissionGUI claimPermissionGUI;
    private EmeraldBankManager bankManager;
    private BankGUI bankGUI;
    private TradeManager tradeManager;
    private TradeGUI tradeGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Managers de base
        factionManager         = new FactionManager(this);
        statsManager           = new PlayerStatsManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);

        // Système de puissance
        powerManager = new FactionPowerManager(this, factionManager, statsManager);
        powerManager.start();

        // ── Nouveaux systèmes ──────────────────────────────────────────────
        claimManager       = new ClaimManager(this);
        claimPermissionGUI = new ClaimPermissionGUI(this, claimManager, factionManager);
        bankManager        = new EmeraldBankManager(this);
        bankGUI            = new BankGUI(this, bankManager, factionManager);
        tradeManager       = new TradeManager();
        tradeGUI           = new TradeGUI(this, tradeManager);

        // GUIs
        factionGUI       = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);
        rankingGUI       = new FactionRankingGUI(this, factionManager, powerManager);
        actionBarManager = new ActionBarManager(this, factionManager);

        // Commandes
        FactionCommand factionCommand = new FactionCommand(
                this, factionManager, statsManager, sharedInventoryManager,
                teleportManager, factionGUI, rankingGUI, powerManager,
                claimManager, claimPermissionGUI, bankGUI, tradeManager, tradeGUI);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(factionManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new PowerBridgeListener(factionManager, powerManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(claimManager, factionManager), this);

        // ActionBar & timers
        actionBarManager.start();
        playtimeTracker = new PlaytimeTracker(this, statsManager);
        playtimeTracker.start();

        getLogger().info("FactionPlugin v3.2 actif — Claim, Banque, Troc ajoutés !");
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
        getLogger().info("FactionPlugin désactivé. Données sauvegardées.");
    }

    public FactionManager getFactionManager()             { return factionManager; }
    public PlayerStatsManager getStatsManager()           { return statsManager; }
    public ActionBarManager getActionBarManager()         { return actionBarManager; }
    public SharedInventoryManager getSharedInvManager()  { return sharedInventoryManager; }
    public FactionTeleportManager getTeleportManager()    { return teleportManager; }
    public FactionGUI getFactionGUI()                     { return factionGUI; }
    public FactionPowerManager getPowerManager()          { return powerManager; }
    public FactionRankingGUI getRankingGUI()              { return rankingGUI; }
    public ClaimManager getClaimManager()                 { return claimManager; }
    public EmeraldBankManager getBankManager()            { return bankManager; }
    public TradeManager getTradeManager()                 { return tradeManager; }
    public TradeGUI getTradeGUI()                         { return tradeGUI; }
}
