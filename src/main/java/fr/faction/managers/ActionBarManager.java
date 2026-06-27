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

    // Flèches directionnelles Unicode (8 directions)
    private static final String[] DIRECTION_ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    // Labels des 8 directions cardinales
    private static final String[] DIRECTION_LABELS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    public ActionBarManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("faction.actionbar-update-interval", 20);
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
        if (task != null) {
            task.cancel();
        }
    }

    // ─── Résultat de la recherche du membre le plus proche ─────────────────────

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

    // ─── Calcul de la direction exacte ─────────────────────────────────────────

    /**
     * Calcule l'angle horizontal (yaw) entre self et la cible,
     * en tenant compte de la rotation de la caméra du joueur.
     * Retourne un angle en degrés [-180, 180] relatif au regard du joueur.
     */
    private double getRelativeAngle(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();

        // Angle absolu vers la cible (atan2 dans l'espace Minecraft : Z est le sud)
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz)); // [-180, 180]

        // Yaw du joueur (Minecraft : 0=S, -90=E, 90=W, ±180=N)
        // On le convertit en convention mathématique standard
        float playerYaw = selfLoc.getYaw(); // [-180, 180]

        // Angle relatif = direction cible - regard joueur
        double relAngle = angleToTarget - playerYaw;

        // Normaliser dans [-180, 180]
        relAngle = ((relAngle + 540) % 360) - 180;
        return relAngle;
    }

    /**
     * Renvoie la flèche directionnelle correspondant à l'angle absolu
     * (nord = 0°, sens horaire). Utilisée pour la boussole absolue.
     */
    private String getCardinalArrow(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();

        // Angle depuis le Nord (0) en sens horaire
        double angle = Math.toDegrees(Math.atan2(dx, -dz)); // Nord = 0
        angle = (angle + 360) % 360;

        // 8 secteurs de 45° chacun, décalés de 22.5°
        int idx = (int) ((angle + 22.5) / 45) % 8;
        return DIRECTION_ARROWS[idx];
    }

    /**
     * Renvoie le label cardinal (N, NE, E…) de la direction vers la cible.
     */
    private String getCardinalLabel(Player self, Location target) {
        Location selfLoc = self.getLocation();
        double dx = target.getX() - selfLoc.getX();
        double dz = target.getZ() - selfLoc.getZ();

        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        angle = (angle + 360) % 360;

        int idx = (int) ((angle + 22.5) / 45) % 8;
        return DIRECTION_LABELS[idx];
    }

    /**
     * Crée une mini-boussole de 5 caractères centrée sur la direction relative.
     * Exemple : "← · ↑ · →" avec la flèche vers la cible mise en évidence.
     */
    private String buildRelativeCompass(double relAngle) {
        // On représente [-180,180] sur 5 slots : gauche, légèrement gauche, devant, légèrement droite, droite
        // Mais on simplifie à un indicateur visuel compact
        String indicator;
        if (relAngle < -135 || relAngle >= 135)       indicator = "◄◄";
        else if (relAngle < -90)                        indicator = "◄·";
        else if (relAngle < -45)                        indicator = "·◄";
        else if (relAngle < -10)                        indicator = " ◄";
        else if (relAngle <= 10)                        indicator = " ▲";
        else if (relAngle <= 45)                        indicator = "► ";
        else if (relAngle <= 90)                        indicator = "·►";
        else if (relAngle <= 135)                       indicator = "►·";
        else                                            indicator = "►►";
        return indicator;
    }

    // ─── Construction du message action bar ────────────────────────────────────

    private String buildActionBarMessage(Player self, NearestResult result) {
        if (result == null) {
            // Aucun allié à portée
            return ChatColor.GRAY + "✦ " + ChatColor.DARK_GRAY + "Aucun allié à portée";
        }

        Player nearest = result.player;
        double dist = result.distance;
        Location targetLoc = nearest.getLocation();

        // Distance affichée avec 1 décimale si < 10m, sinon entière
        String distStr;
        if (dist < 10.0) {
            distStr = String.format("%.1f", dist) + "m";
        } else {
            distStr = (int) Math.round(dist) + "m";
        }

        // Couleur de la distance
        String distColor;
        if (dist <= 15)       distColor = ChatColor.GREEN.toString();
        else if (dist <= 50)  distColor = ChatColor.YELLOW.toString();
        else if (dist <= 100) distColor = ChatColor.GOLD.toString();
        else                  distColor = ChatColor.RED.toString();

        // Direction absolue (boussole cardinale)
        String arrow     = getCardinalArrow(self, targetLoc);
        String cardinal  = getCardinalLabel(self, targetLoc);

        // Direction relative au regard du joueur (mini-boussole)
        double relAngle  = getRelativeAngle(self, targetLoc);
        String compass   = buildRelativeCompass(relAngle);

        // Différence de hauteur
        double dy = targetLoc.getY() - self.getLocation().getY();
        String yIndicator = "";
        if (dy > 3)        yIndicator = ChatColor.AQUA + " ⬆" + (int) dy + "m";
        else if (dy < -3)  yIndicator = ChatColor.AQUA + " ⬇" + (int) Math.abs(dy) + "m";

        // Message final : nom | distance | direction absolue | boussole relative | altitude
        return ChatColor.AQUA + "⬡ " + ChatColor.WHITE + ChatColor.BOLD + nearest.getName()
                + ChatColor.RESET + "  "
                + distColor + distStr
                + "  "
                + ChatColor.YELLOW + arrow + " " + cardinal
                + "  "
                + ChatColor.WHITE + "[" + ChatColor.GOLD + compass + ChatColor.WHITE + "]"
                + yIndicator;
    }

    // ─── Envoi de l'action bar ─────────────────────────────────────────────────

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message)
        );
    }
}
