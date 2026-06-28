package fr.faction;

import fr.faction.commands.FactionCommand;
import fr.faction.commands.PowerCommand;
import fr.faction.gui.FactionGUI;
import fr.faction.gui.FactionRankingGUI;
import fr.faction.listeners.PlayerListener;
import fr.faction.managers.ActionBarManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.PowerBridgeListener;
import fr.factionstats.managers.StatsManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionPlugin extends JavaPlugin {

    private FactionManager factionManager;
    private ActionBarManager actionBarManager;
    private SharedInventoryManager sharedInventoryManager;
    private FactionTeleportManager teleportManager;
    private FactionGUI factionGUI;
    private FactionPowerManager powerManager;
    private FactionRankingGUI rankingGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ── Managers de base ─────────────────────────────────────────────────
        factionManager         = new FactionManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);

        // ── Integration FactionStats (optionnelle) ───────────────────────────
        StatsManager statsManager = null;
        Plugin statsPlugin = getServer().getPluginManager().getPlugin("FactionStats");
        if (statsPlugin != null && statsPlugin.isEnabled()) {
            statsManager = ((fr.factionstats.FactionStats) statsPlugin).getStatsManager();
            getLogger().info("[FactionPlugin] Integration FactionStats detectee !");
        } else {
            getLogger().warning("[FactionPlugin] FactionStats non detecte — puissance basee sur les membres uniquement.");
        }

        // ── Système de puissance et rangs ────────────────────────────────────
        powerManager = new FactionPowerManager(this, factionManager, statsManager);
        powerManager.start();

        // ── GUIs ─────────────────────────────────────────────────────────────
        factionGUI   = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);
        rankingGUI   = new FactionRankingGUI(this, factionManager, powerManager);
        actionBarManager = new ActionBarManager(this, factionManager);

        // ── Commandes ────────────────────────────────────────────────────────
        FactionCommand factionCommand = new FactionCommand(this, factionManager, sharedInventoryManager, teleportManager, factionGUI, rankingGUI);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        PowerCommand powerCommand = new PowerCommand(factionManager, powerManager);
        getCommand("power").setExecutor(powerCommand);
        getCommand("power").setTabCompleter(powerCommand);

        // ── Listeners ────────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new PlayerListener(factionManager), this);
        getServer().getPluginManager().registerEvents(new PowerBridgeListener(factionManager, powerManager), this);

        // ── ActionBar ────────────────────────────────────────────────────────
        actionBarManager.start();

        getLogger().info("FactionPlugin v2.0 active avec systeme de puissance et rangs !");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null)      actionBarManager.stop();
        if (powerManager != null)          powerManager.stop();
        if (sharedInventoryManager != null) sharedInventoryManager.saveInventories();
        if (factionManager != null)        factionManager.saveFactions();
        getLogger().info("FactionPlugin desactive. Donnees sauvegardees.");
    }

    public FactionManager getFactionManager()             { return factionManager; }
    public ActionBarManager getActionBarManager()         { return actionBarManager; }
    public SharedInventoryManager getSharedInvManager()  { return sharedInventoryManager; }
    public FactionTeleportManager getTeleportManager()    { return teleportManager; }
    public FactionGUI getFactionGUI()                     { return factionGUI; }
    public FactionPowerManager getPowerManager()          { return powerManager; }
    public FactionRankingGUI getRankingGUI()              { return rankingGUI; }
}
