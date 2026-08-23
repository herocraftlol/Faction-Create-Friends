package fr.faction.shop;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Résout un nom d'item saisi par un joueur (français courant, ou nom Bukkit anglais)
 * vers un {@link Material}. Utilisé notamment par le comptoir d'échange pour
 * permettre de demander n'importe quel item sans connaître le nom technique anglais.
 */
public final class ItemAliasUtil {

    private static final Map<String, Material> ALIASES = new HashMap<>();

    static {
        put(Material.STONE, "pierre");
        put(Material.COBBLESTONE, "caillou", "cobble", "cailloux");
        put(Material.DIRT, "terre");
        put(Material.SAND, "sable");
        put(Material.GRAVEL, "gravier");
        put(Material.OAK_LOG, "bois", "buche", "tronc");
        put(Material.OAK_PLANKS, "planche", "planches");
        put(Material.GLASS, "verre");
        put(Material.CLAY_BALL, "argile");
        put(Material.COAL, "charbon");
        put(Material.RAW_IRON, "fer_brut", "ferbrut", "minerai_de_fer");
        put(Material.RAW_GOLD, "or_brut", "orbrut", "minerai_d_or");
        put(Material.RAW_COPPER, "cuivre_brut", "cuivrebrut");
        put(Material.COPPER_INGOT, "lingot_de_cuivre", "cuivre");
        put(Material.IRON_INGOT, "fer", "lingot_de_fer");
        put(Material.GOLD_INGOT, "or", "lingot_d_or");
        put(Material.DIAMOND, "diamant");
        put(Material.EMERALD, "emeraude");
        put(Material.REDSTONE, "redstone");
        put(Material.LAPIS_LAZULI, "lapis");
        put(Material.QUARTZ, "quartz");
        put(Material.NETHERITE_SCRAP, "netherite_brute", "eclat_de_netherite");
        put(Material.NETHERITE_INGOT, "netherite", "lingot_de_netherite");
        put(Material.OBSIDIAN, "obsidienne");
        put(Material.NETHERRACK, "netherrack");
        put(Material.BLAZE_ROD, "baton_de_blaze", "blaze");
        put(Material.ENDER_PEARL, "perle_de_l_ender", "perle_ender", "perle");
        put(Material.WHEAT, "ble");
        put(Material.CARROT, "carotte");
        put(Material.POTATO, "patate", "pomme_de_terre");
        put(Material.SUGAR_CANE, "canne_a_sucre", "canne");
        put(Material.BAMBOO, "bambou");
        put(Material.LEATHER, "cuir");
        put(Material.STRING, "fil", "ficelle");
        put(Material.BONE, "os");
        put(Material.GUNPOWDER, "poudre", "poudre_a_canon");
        put(Material.SLIME_BALL, "boule_de_slime", "slime");
        put(Material.PAPER, "papier");
        put(Material.BOOK, "livre");
        put(Material.APPLE, "pomme");
        put(Material.BREAD, "pain");
        put(Material.EGG, "oeuf");
        put(Material.FEATHER, "plume");
    }

    private ItemAliasUtil() {}

    private static void put(Material material, String... names) {
        for (String n : names) ALIASES.put(n.toLowerCase(), material);
    }

    /**
     * Résout un nom saisi par un joueur vers un Material.
     * Essaie d'abord les alias français, puis le nom technique Bukkit (ex: "STONE", "oak_log").
     * @return le Material trouvé, ou null si rien ne correspond.
     */
    public static Material resolve(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = input.trim().toLowerCase().replace(" ", "_");

        Material alias = ALIASES.get(normalized);
        if (alias != null) return alias;

        Material direct = Material.matchMaterial(normalized);
        if (direct != null) return direct;

        return Material.matchMaterial(normalized.toUpperCase());
    }
}
