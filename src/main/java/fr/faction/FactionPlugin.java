package fr.faction;

import fr.faction.commands.FactionCommand;
import fr.faction.gui.FactionGUI;
import fr.faction.listeners.PlayerListener;
import fr.faction.managers.ActionBarManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.FactionTeleportManager;
import fr.faction.managers.SharedInventoryManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionPlugin extends JavaPlugin {

    private FactionManager factionManager;
    private ActionBarManager actionBarManager;
    private SharedInventoryManager sharedInventoryManager;
    private FactionTeleportManager teleportManager;
    private FactionGUI factionGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        factionManager         = new FactionManager(this);
        sharedInventoryManager = new SharedInventoryManager(this, factionManager);
        teleportManager        = new FactionTeleportManager(this, factionManager);
        actionBarManager       = new ActionBarManager(this, factionManager);
        factionGUI             = new FactionGUI(this, factionManager, sharedInventoryManager, teleportManager);

        FactionCommand factionCommand = new FactionCommand(this, factionManager, sharedInventoryManager, teleportManager, factionGUI);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        getServer().getPluginManager().registerEvents(new PlayerListener(factionManager), this);

        actionBarManager.start();
        getLogger().info("FactionPlugin active !");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null) actionBarManager.stop();
        if (sharedInventoryManager != null) sharedInventoryManager.saveInventories();
        if (factionManager != null) factionManager.saveFactions();
        getLogger().info("FactionPlugin desactive. Donnees sauvegardees.");
    }

    public FactionManager getFactionManager()             { return factionManager; }
    public ActionBarManager getActionBarManager()         { return actionBarManager; }
    public SharedInventoryManager getSharedInvManager()   { return sharedInventoryManager; }
    public FactionTeleportManager getTeleportManager()     { return teleportManager; }
    public FactionGUI getFactionGUI()                     { return factionGUI; }
}
