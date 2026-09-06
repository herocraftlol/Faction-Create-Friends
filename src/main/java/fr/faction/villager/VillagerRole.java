package fr.faction.villager;

/**
 * Rôle assigné à un villageois recruté par une faction.
 */
public enum VillagerRole {
    AUCUN,
    CONSTRUCTEUR,
    GUERRIER;

    public String displayName() {
        return switch (this) {
            case AUCUN -> "Aucun rôle";
            case CONSTRUCTEUR -> "Constructeur";
            case GUERRIER -> "Guerrier";
        };
    }

    public static VillagerRole fromString(String s) {
        try {
            return VillagerRole.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return AUCUN;
        }
    }
}
