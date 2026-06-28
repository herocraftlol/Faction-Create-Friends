package fr.faction.managers;

import fr.faction.models.Faction;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class ActionBarManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private BukkitTask task;

    private static final String[] DIRECTION_ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final String[] DIRECTION_LABELS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    public ActionBarManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
    }

    public void start() {
        // 2 ticks = ~10 updates/sec pour un affichage très fluide
        int interval = plugin.getConfig().getInt("faction.actionbar-update-interval", 2);
        double maxRange = plugin.getConfig().getDouble("faction.actionbar-range", 200);

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
                if (faction == null) continue;
                NearestResult result = findNearestMember(player, faction, maxRange);
                String message = buildActionBarMessage(player, result);
                sendActionBar(player, message);
            }
        }, 0L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private static class NearestResult {
        final Player player;
        final double distance;
        NearestResult(Player player, double distance) {
            this.player = player;
            this.distance = distance;
        }
    }

    private NearestResult findNearestMember(Player self, Faction faction, double maxRange) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (UUID memberUUID : faction.getMembers()) {
            if (memberUUID.equals(self.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(memberUUID);
            if (member == null || !member.isOnline()) continue;
            if (!member.getWorld().equals(self.getWorld())) continue;
            double dist = self.getLocation().distance(member.getLocation());
            if (dist < nearestDist && dist <= maxRange) {
                nearestDist = dist;
                nearest = member;
            }
        }
        return nearest != null ? new NearestResult(nearest, nearestDist) : null;
    }

    private double getRelativeAngle(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        float playerYaw = selfLoc.getYaw();
        double relAngle = angleToTarget - playerYaw;
        return ((relAngle + 540) % 360) - 180;
    }

    private String getCardinalArrow(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        angle = (angle + 360) % 360;
        int idx = (int) ((angle + 22.5) / 45) % 8;
        return DIRECTION_ARROWS[idx];
    }

    private String getCardinalLabel(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        angle = (angle + 360) % 360;
        int idx = (int) ((angle + 22.5) / 45) % 8;
        return DIRECTION_LABELS[idx];
    }

    private String buildRelativeCompass(double relAngle) {
        if (relAngle < -135 || relAngle >= 135)  return "◄◄";
        else if (relAngle < -90)                  return "◄·";
        else if (relAngle < -45)                  return "·◄";
        else if (relAngle < -10)                  return " ◄";
        else if (relAngle <= 10)                  return " ▲";
        else if (relAngle <= 45)                  return "► ";
        else if (relAngle <= 90)                  return "·►";
        else if (relAngle <= 135)                 return "►·";
        else                                      return "►►";
    }

    private String buildActionBarMessage(Player self, NearestResult result) {
        if (result == null) {
            return ChatColor.GRAY + "✦ " + ChatColor.DARK_GRAY + "Aucun allié à portée";
        }
        Player nearest = result.player;
        double dist = result.distance;
        Location targetLoc = nearest.getLocation();

        String distStr = dist < 10.0
                ? String.format("%.1f", dist) + "m"
                : (int) Math.round(dist) + "m";

        String distColor;
        if (dist <= 15)       distColor = ChatColor.GREEN.toString();
        else if (dist <= 50)  distColor = ChatColor.YELLOW.toString();
        else if (dist <= 100) distColor = ChatColor.GOLD.toString();
        else                  distColor = ChatColor.RED.toString();

        String arrow    = getCardinalArrow(self, targetLoc);
        String cardinal = getCardinalLabel(self, targetLoc);
        double relAngle = getRelativeAngle(self, targetLoc);
        String compass  = buildRelativeCompass(relAngle);

        double dy = targetLoc.getY() - self.getLocation().getY();
        String yIndicator = "";
        if (dy > 3)       yIndicator = ChatColor.AQUA + " ⬆" + (int) dy + "m";
        else if (dy < -3) yIndicator = ChatColor.AQUA + " ⬇" + (int) Math.abs(dy) + "m";

        return ChatColor.AQUA + "⬡ " + ChatColor.WHITE + ChatColor.BOLD + nearest.getName()
                + ChatColor.RESET + "  "
                + distColor + distStr
                + "  "
                + ChatColor.YELLOW + arrow + " " + cardinal
                + "  "
                + ChatColor.WHITE + "[" + ChatColor.GOLD + compass + ChatColor.WHITE + "]"
                + yIndicator;
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
