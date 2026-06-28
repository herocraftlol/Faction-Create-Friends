package fr.factionstats;

import fr.factionstats.managers.StatsManager;

public class FactionStats {
    
    public StatsManager getStatsManager() {
        return new StatsManager();
    }
}