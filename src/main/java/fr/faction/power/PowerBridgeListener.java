package fr.faction.power;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Listener "pont" — invalide le cache de puissance après des events importants
 * afin que la montée de rang soit détectée rapidement (pas besoin d'attendre 2 min).
 */
public class PowerBridgeListener implements Listener {

    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;

    public PowerBridgeListener(FactionManager factionManager, FactionPowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Faction faction = factionManager.getPlayerFaction(killer.getUniqueId());
        if (faction != null) powerManager.invalidate(faction.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().getKey();
        if (key.startsWith("recipes/")) return;
        Faction faction = factionManager.getPlayerFaction(event.getPlayer().getUniqueId());
        if (faction != null) powerManager.invalidate(faction.getName());
    }
}
