package fr.factionstats.managers;

import fr.factionstats.models.PlayerStats;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsManager {
    
    public PlayerStats getStats(UUID uuid) {
        return new PlayerStats(uuid, 0, 0, 0, 0, 0, 0);
    }
    
    public void addKill(UUID uuid, int amount) {}
    public void addDeath(UUID uuid, int amount) {}
    public void addKills(UUID uuid, int amount) {}
    public void addDeaths(UUID uuid, int amount) {}
    public void addPlacedBlocks(UUID uuid, int amount) {}
    public void addBrokenBlocks(UUID uuid, int amount) {}
    public void addPlayTime(UUID uuid, long seconds) {}
    
    public void saveAll() {}
}