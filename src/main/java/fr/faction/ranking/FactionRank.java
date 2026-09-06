package fr.faction.ranking;

import org.bukkit.ChatColor;

/**
 * Les 7 rangs de faction — Paper 1.21
 *
 * Effets passifs réels (sans fly, sans speed, sans feather falling, sans jump boost) :
 *
 *  PIERRE    → rien (débutants)
 *  BRONZE    → Hâte I (minage légèrement plus rapide)
 *  ARGENT    → Hâte II
 *  OR        → Hâte II + Regen I + tag OR dans le chat/tab
 *  DIAMANT   → Hâte II + Regen I + Slow Falling + Résistance I
 *  ÉMERAUDE  → Hâte III + Regen II + Force I + double drop minerai
 *  LÉGENDAIRE→ Hâte III + Regen II + Force I + Résistance II + aura alliés
 *              + particules + prefix [LÉGENDAIRE] chat/tab
 */
public enum FactionRank {

    PIERRE(0, "Pierre", "◈", ChatColor.GRAY,
            "Aucun avantage — faites vos preuves !"),

    BRONZE(500, "Bronze", "⬡", ChatColor.GOLD,
            "Hâte I passive (minage +10%)",
            "Tag de faction visible dans le chat"),

    ARGENT(2000, "Argent", "✦", ChatColor.WHITE,
            "Hâte II passive (minage +20%)",
            "Téléportation sans cooldown entre membres"),

    OR(5000, "Or", "★", ChatColor.YELLOW,
            "Hâte II + Régénération I passifs",
            "Tag de faction affiché en §eOR §7dans le chat & tab",
            "Rayon actionbar étendu à 300 blocs"),

    DIAMANT(12000, "Diamant", "◆", ChatColor.AQUA,
            "Hâte II + Regen I passifs",
            "Chute amortie passive (aucun dégât de chute)",
            "Résistance I passive (−4% dégâts reçus)",
            "§b2 spawns de faction §7(/fac setspawn 1 et /fac setspawn 2)",
            "§b3 homes personnels",
            "Accès au /faction claimshow (visualisation)"),

    EMERAUDE(25000, "Émeraude", "❋", ChatColor.GREEN,
            "Hâte III + Regen II passifs",
            "Force I passive en combat",
            "Double drop des minerais rares (diamant, émeraude…)",
            "Résistance II passive (−8% dégâts reçus)",
            "§a2 spawns de faction",
            "§a4 homes personnels"),

    LEGENDAIRE(60000, "Légendaire", "⚜", ChatColor.LIGHT_PURPLE,
            "Hâte III + Regen II + Force I + Résistance II passifs",
            "Aura de Regen II sur les alliés à ≤15 blocs",
            "Halo de particules dorées distinctif",
            "§d2 spawns de faction",
            "§d5 homes personnels",
            "§5Prefix §d[LÉGENDAIRE] §7en violet dans le chat & tab");

    // ─────────────────────────────────────────────────────────────────────────
    public final double puissanceMin;
    public final String nom;
    public final String icone;
    public final ChatColor couleur;
    public final String[] avantages;

    FactionRank(double puissanceMin, String nom, String icone,
                ChatColor couleur, String... avantages) {
        this.puissanceMin = puissanceMin;
        this.nom    = nom;
        this.icone  = icone;
        this.couleur = couleur;
        this.avantages = avantages;
    }

    public String getLabel() {
        return couleur + icone + " " + nom;
    }

    public String getLabelBold() {
        return couleur + "" + ChatColor.BOLD + icone + " " + nom + ChatColor.RESET;
    }

    /**
     * Préfixe court pour le chat et le tab-list.
     * Max ~16 chars en code couleur (Scoreboard Team).
     */
    public String getChatPrefix() {
        return switch (this) {
            case PIERRE     -> ChatColor.GRAY        + "[⬡] ";
            case BRONZE     -> ChatColor.GOLD        + "[⬡] ";
            case ARGENT     -> ChatColor.WHITE       + "[✦] ";
            case OR         -> ChatColor.YELLOW + "" + ChatColor.BOLD + "[★] " + ChatColor.RESET;
            case DIAMANT    -> ChatColor.AQUA  + "" + ChatColor.BOLD + "[◆] " + ChatColor.RESET;
            case EMERAUDE   -> ChatColor.GREEN + "" + ChatColor.BOLD + "[❋] " + ChatColor.RESET;
            case LEGENDAIRE -> ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[⚜] " + ChatColor.RESET;
        };
    }

    public static FactionRank fromPower(double power) {
        FactionRank result = PIERRE;
        for (FactionRank r : values()) {
            if (power >= r.puissanceMin) result = r;
        }
        return result;
    }

    /**
     * Nombre de spawns de faction autorisés selon le rang.
     *  Pierre / Bronze / Argent / Or → 1 spawn
     *  Diamant / Émeraude / Légendaire → 2 spawns
     */
    public int getMaxSpawns() {
        return switch (this) {
            case PIERRE, BRONZE, ARGENT, OR -> 1;
            case DIAMANT, EMERAUDE, LEGENDAIRE -> 2;
        };
    }

    /**
     * Nombre de homes personnels autorisés selon le rang de la faction du joueur.
     *  Pierre          → 1 home
     *  Bronze / Argent → 2 homes
     *  Or              → 3 homes
     *  Diamant         → 3 homes  (+1 vs avant)
     *  Émeraude        → 4 homes
     *  Légendaire      → 5 homes
     *
     * Note : sans faction → 1 home (géré dans HomeManager).
     */
    public int getMaxHomes() {
        return switch (this) {
            case PIERRE         -> 1;
            case BRONZE, ARGENT -> 2;
            case OR, DIAMANT    -> 3;
            case EMERAUDE       -> 4;
            case LEGENDAIRE     -> 5;
        };
    }

    public FactionRank next() {
        FactionRank[] vals = values();
        int idx = ordinal() + 1;
        return idx < vals.length ? vals[idx] : null;
    }

    public String progressBar(double currentPower) {
        FactionRank next = next();
        if (next == null) return couleur + "▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰▰ " + ChatColor.GOLD + "MAX";
        double progress = Math.min(1.0, Math.max(0.0,
                (currentPower - puissanceMin) / (next.puissanceMin - puissanceMin)));
        int filled = (int) (progress * 20);
        StringBuilder bar = new StringBuilder(couleur.toString());
        for (int i = 0; i < 20; i++)
            bar.append(i < filled ? "▰" : ChatColor.DARK_GRAY + "▱");
        bar.append(" ").append(ChatColor.WHITE).append(String.format("%.1f%%", progress * 100));
        return bar.toString();
    }
}
