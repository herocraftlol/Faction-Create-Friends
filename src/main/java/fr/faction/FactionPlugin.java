package fr.faction;

import fr.faction.commands.FactionCommand;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Managers de base
        factionManager         = new FactionManager(this);
        statsManager           = new PlayerStatsManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);

        // Système de puissance (intégré, sans dépendance externe)
        powerManager = new FactionPowerManager(this, factionManager, statsManager);
        powerManager.start();

        // GUIs
        factionGUI       = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);
        rankingGUI       = new FactionRankingGUI(this, factionManager, powerManager);
        actionBarManager = new ActionBarManager(this, factionManager);

        // Commandes — uniquement /faction
        FactionCommand factionCommand = new FactionCommand(this, factionManager, statsManager,
                sharedInventoryManager, teleportManager, factionGUI, rankingGUI, powerManager);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(factionManager, statsManager), this);
        getServer().getPluginManager().registerEvents(new PowerBridgeListener(factionManager, powerManager, statsManager), this);

        // ActionBar
        actionBarManager.start();

        // Suivi du temps de jeu (tâche périodique, ~1x/seconde)
        playtimeTracker = new PlaytimeTracker(this, statsManager);
        playtimeTracker.start();

        getLogger().info("FactionPlugin v3.1 actif — commande unique /faction (stats & classements joueurs intégrés) !");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null)       actionBarManager.stop();
        if (playtimeTracker != null)        playtimeTracker.stop();
        if (powerManager != null)           powerManager.stop();
        if (sharedInventoryManager != null) sharedInventoryManager.saveInventories();
        if (statsManager != null)           statsManager.saveAll();
        if (factionManager != null)         factionManager.saveFactions();
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
}
