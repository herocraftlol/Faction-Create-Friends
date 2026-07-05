package fr.faction.util;

import org.bukkit.entity.*;

/**
 * Utilitaires liés aux entités (mobs).
 * Centralise la définition de "mob hostile" utilisée à plusieurs endroits du plugin
 * (statistiques de kill, protection des claims, etc.).
 */
public final class MobUtils {

    private MobUtils() {}

    /**
     * @return true si l'entité est un mob hostile (monstre).
     *         Les animaux, villageois, golems de fer, etc. sont considérés non-hostiles.
     */
    public static boolean isHostileMob(LivingEntity entity) {
        return entity instanceof Monster
                || entity instanceof Ghast
                || entity instanceof Slime
                || entity instanceof Phantom
                || entity instanceof Shulker
                || entity instanceof ElderGuardian
                || entity instanceof Warden
                || entity instanceof EnderDragon
                || entity instanceof WitherSkeleton
                || entity instanceof Wither
                || entity instanceof PiglinBrute
                || entity instanceof Hoglin
                || entity instanceof Zoglin;
    }

    /** @return true si l'entité n'est ni un joueur, ni un mob hostile (donc un mob "amical"/passif). */
    public static boolean isFriendlyMob(LivingEntity entity) {
        return !(entity instanceof Player) && !isHostileMob(entity);
    }
}
