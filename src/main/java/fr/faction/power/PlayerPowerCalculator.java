package fr.faction.power;

import fr.factionstats.models.PlayerStats;

/**
 * Calcule la Puissance Individuelle (PI) d'un joueur à partir de ses stats FactionStats.
 *
 * Formule :
 *   PI = PvP_score + Survie_score + Progression_score + Activite_score
 *
 *   PvP_score       = (kills * 10) + (dommages / 100) - (morts * 2)   [min 0]
 *   Survie_score    = (blocs_casses / 500) + (blocs_places / 500) + (distance / 1000)
 *   Progression_score = advancements * 15 + items_craftes / 50
 *   Activite_score  = (temps_joue_heures * 2) + (items_ramasees / 200)
 */
public class PlayerPowerCalculator {

    // Poids configurables
    private static final double KILLS_WEIGHT       = 10.0;
    private static final double DAMAGE_WEIGHT      = 0.01;
    private static final double DEATH_PENALTY      = 2.0;
    private static final double BLOCS_CASSES_W     = 0.002;
    private static final double BLOCS_PLACES_W     = 0.002;
    private static final double DISTANCE_W         = 0.001;
    private static final double ADVANCEMENT_W      = 15.0;
    private static final double CRAFT_W            = 0.02;
    private static final double PLAYTIME_W         = 2.0;   // par heure
    private static final double ITEMS_W            = 0.005;

    /**
     * Retourne la puissance d'un joueur, arrondie à 2 décimales.
     */
    public static double calculate(PlayerStats stats) {
        if (stats == null) return 0;

        // PvP
        double pvp = (stats.getJoueursTues() * KILLS_WEIGHT)
                + (stats.getDommagesInfliges() * DAMAGE_WEIGHT)
                - (stats.getMorts() * DEATH_PENALTY);
        pvp = Math.max(0, pvp);

        // Survie
        double survie = (stats.getBlocsCasses() * BLOCS_CASSES_W)
                + (stats.getBlocsPlaces() * BLOCS_PLACES_W)
                + (stats.getDistanceParcourue() * DISTANCE_W);

        // Progression
        double progression = (stats.getAdvancementsAccomplis() * ADVANCEMENT_W)
                + (stats.getItemsCraftes() * CRAFT_W);

        // Activité
        double heuresJouees = stats.getTempsJoue() / (20.0 * 3600.0); // ticks → heures
        double activite = (heuresJouees * PLAYTIME_W)
                + (stats.getItemsRamasees() * ITEMS_W);

        double total = pvp + survie + progression + activite;
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Retourne un breakdown textuel pour l'affichage (debug/stats)
     */
    public static PowerBreakdown breakdown(PlayerStats stats) {
        if (stats == null) return new PowerBreakdown(0, 0, 0, 0);

        double pvp = Math.max(0,
                (stats.getJoueursTues() * KILLS_WEIGHT)
                + (stats.getDommagesInfliges() * DAMAGE_WEIGHT)
                - (stats.getMorts() * DEATH_PENALTY));

        double survie = (stats.getBlocsCasses() * BLOCS_CASSES_W)
                + (stats.getBlocsPlaces() * BLOCS_PLACES_W)
                + (stats.getDistanceParcourue() * DISTANCE_W);

        double progression = (stats.getAdvancementsAccomplis() * ADVANCEMENT_W)
                + (stats.getItemsCraftes() * CRAFT_W);

        double heures = stats.getTempsJoue() / (20.0 * 3600.0);
        double activite = (heures * PLAYTIME_W) + (stats.getItemsRamasees() * ITEMS_W);

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
