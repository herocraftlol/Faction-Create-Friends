package fr.faction;

import fr.faction.commands.FactionCommand;
import fr.faction.listeners.PlayerListener;
import fr.faction.managers.ActionBarManager;
import fr.faction.managers.FactionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionPlugin extends JavaPlugin {

    private FactionManager factionManager;
    private ActionBarManager actionBarManager;

    @Override
    public void onEnable() {
        // Config par défaut
        saveDefaultConfig();

        // Managers
        factionManager = new FactionManager(this);
        actionBarManager = new ActionBarManager(this, factionManager);

        // Commandes
        FactionCommand factionCommand = new FactionCommand(this, factionManager);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(factionManager), this);

        // ActionBar task
        actionBarManager.start();

        getLogger().info("FactionPlugin activé !");
    }

    @Override
    public void onDisable() {
        if (actionBarManager != null) actionBarManager.stop();
        if (factionManager != null) factionManager.saveFactions();
        getLogger().info("FactionPlugin désactivé. Données sauvegardées.");
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }
}
