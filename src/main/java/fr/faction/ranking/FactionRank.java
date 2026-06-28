package fr.faction.ranking;

import org.bukkit.ChatColor;

/**
 * Les 7 rangs de faction, chacun avec :
 *  - un seuil de puissance globale minimum
 *  - un nom coloré
 *  - une icône unicode
 *  - une couleur principale
 *  - la liste des avantages débloqués
 */
public enum FactionRank {

    // ── Rang 0 ─────────────────────────────────────────────────────────────────
    PIERRE(0, "Pierre", "◈", ChatColor.GRAY,
            "Aucun avantage spécial — faites vos preuves !"),

    // ── Rang 1 ─────────────────────────────────────────────────────────────────
    BRONZE(500, "Bronze", "⬡", ChatColor.GOLD,
            "+5% de vitesse de minage",
            "Coffre partagé étendu (27 slots de plus)"),

    // ── Rang 2 ─────────────────────────────────────────────────────────────────
    ARGENT(2000, "Argent", "✦", ChatColor.WHITE,
            "+10% de vitesse de minage",
            "Téléportation sans cooldown entre membres",
            "Accès au /faction home (point de ralliement)"),

    // ── Rang 3 ─────────────────────────────────────────────────────────────────
    OR(5000, "Or", "★", ChatColor.YELLOW,
            "+15% de vitesse de minage",
            "Régénération de vie lente passive (Regen I)",
            "Tag de faction affiché en OR dans le chat",
            "Rayon d'action bar étendu à 300 blocs"),

    // ── Rang 4 ─────────────────────────────────────────────────────────────────
    DIAMANT(12000, "Diamant", "◆", ChatColor.AQUA,
            "+20% de vitesse de minage",
            "Résistance aux chutes (Feather Falling II passif)",
            "Vitesse de déplacement +10%",
            "Accès à la commande /faction fly (vol 30s, cd 5min)"),

    // ── Rang 5 ─────────────────────────────────────────────────────────────────
    EMERAUDE(25000, "Emeraude", "❋", ChatColor.GREEN,
            "+25% de vitesse de minage",
            "Force I passive en combat",
            "Double drop de minerais rares",
            "Accès au warp de faction inter-monde",
            "Coffre partagé illimité"),

    // ── Rang 6 ─────────────────────────────────────────────────────────────────
    LEGENDAIRE(60000, "Legendaire", "⚜", ChatColor.LIGHT_PURPLE,
            "+30% de vitesse de minage",
            "Aura de soin des alliés proches (Regen II dans 15 blocs)",
            "Vitesse I + Saut II passifs permanents",
            "Halo de particules distinctif",
            "Prefix [LEGENDAIRE] en violet dans le chat",
            "Accès a /faction raid (declare la guerre a une faction)");

    // ─────────────────────────────────────────────────────────────────────────

    public final double puissanceMin;
    public final String nom;
    public final String icone;
    public final ChatColor couleur;
    public final String[] avantages;

    FactionRank(double puissanceMin, String nom, String icone,
                ChatColor couleur, String... avantages) {
        this.puissanceMin = puissanceMin;
        this.nom = nom;
        this.icone = icone;
        this.couleur = couleur;
        this.avantages = avantages;
    }

    /** Retourne le label coloré complet : ex §b◆ Diamant */
    public String getLabel() {
        return couleur + icone + " " + nom;
    }

    /** Retourne le label en gras */
    public String getLabelBold() {
        return couleur + "" + ChatColor.BOLD + icone + " " + nom + ChatColor.RESET;
    }

    /** Détermine le rang correspondant à une puissance donnée */
    public static FactionRank fromPower(double power) {
        FactionRank result = PIERRE;
        for (FactionRank r : values()) {
            if (power >= r.puissanceMin) result = r;
        }
        return result;
    }

    /** Retourne le prochain rang (null si déjà Légendaire) */
    public FactionRank next() {
        FactionRank[] vals = values();
        int idx = ordinal() + 1;
        return idx < vals.length ? vals[idx] : null;
    }

    /** Barre de progression vers le prochain rang (20 caractères) */
    public String progressBar(double currentPower) {
        FactionRank next = next();
        if (next == null) return couleur + "▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰ " + ChatColor.GOLD + "MAX";

        double progress = (currentPower - puissanceMin) / (next.puissanceMin - puissanceMin);
        progress = Math.min(1.0, Math.max(0.0, progress));
        int filled = (int) (progress * 20);

        StringBuilder bar = new StringBuilder(couleur.toString());
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? "▰" : ChatColor.DARK_GRAY + "▱");
        }
        bar.append(" ").append(ChatColor.WHITE).append(String.format("%.1f%%", progress * 100));
        return bar.toString();
    }
}
