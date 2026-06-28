package fr.faction.power;

import fr.faction.models.PlayerStats;

/**
 * Calcule la Puissance Individuelle (PI) d'un joueur à partir des stats internes.
 *
 * PI = PvP_score + Survie_score + Progression_score + Activite_score
 */
public class PlayerPowerCalculator {

    private static final double KILLS_WEIGHT    = 10.0;
    private static final double DAMAGE_WEIGHT   = 0.01;
    private static final double DEATH_PENALTY   = 2.0;
    private static final double BLOCKS_BROKEN_W = 0.002;
    private static final double BLOCKS_PLACED_W = 0.002;
    private static final double DISTANCE_W      = 0.001;
    private static final double ADVANCEMENT_W   = 15.0;
    private static final double CRAFT_W         = 0.02;
    private static final double PLAYTIME_W      = 2.0;
    private static final double ITEMS_W         = 0.005;

    public static double calculate(PlayerStats stats) {
        if (stats == null) return 0;
        double pvp = Math.max(0,
                (stats.getKills()         * KILLS_WEIGHT)
              + (stats.getDamageDealt()   * DAMAGE_WEIGHT)
              - (stats.getDeaths()        * DEATH_PENALTY));
        double survie =
                (stats.getBlocksBroken()      * BLOCKS_BROKEN_W)
              + (stats.getBlocksPlaced()      * BLOCKS_PLACED_W)
              + (stats.getDistanceTravelled() * DISTANCE_W);
        double progression =
                (stats.getAdvancements() * ADVANCEMENT_W)
              + (stats.getItemsCrafted() * CRAFT_W);
        double hours = stats.getTicksPlayed() / (20.0 * 3600.0);
        double activite =
                (hours                   * PLAYTIME_W)
              + (stats.getItemsPickedUp() * ITEMS_W);
        return Math.round((pvp + survie + progression + activite) * 100.0) / 100.0;
    }

    public static PowerBreakdown breakdown(PlayerStats stats) {
        if (stats == null) return new PowerBreakdown(0, 0, 0, 0);
        double pvp = Math.max(0,
                (stats.getKills()         * KILLS_WEIGHT)
              + (stats.getDamageDealt()   * DAMAGE_WEIGHT)
              - (stats.getDeaths()        * DEATH_PENALTY));
        double survie =
                (stats.getBlocksBroken()      * BLOCKS_BROKEN_W)
              + (stats.getBlocksPlaced()      * BLOCKS_PLACED_W)
              + (stats.getDistanceTravelled() * DISTANCE_W);
        double progression =
                (stats.getAdvancements() * ADVANCEMENT_W)
              + (stats.getItemsCrafted() * CRAFT_W);
        double hours = stats.getTicksPlayed() / (20.0 * 3600.0);
        double activite =
                (hours                   * PLAYTIME_W)
              + (stats.getItemsPickedUp() * ITEMS_W);
        return new PowerBreakdown(
                Math.round(pvp * 10.0) / 10.0,
                Math.round(survie * 10.0) / 10.0,
                Math.round(progression * 10.0) / 10.0,
                Math.round(activite * 10.0) / 10.0
        );
    }

    public record PowerBreakdown(double pvp, double survie, double progression, double activite) {
        public double total() { return pvp + survie + progression + activite; }
    }
}
