package fr.faction.sort;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de tri pour les coffres Minecraft.
 *
 * Modes disponibles :
 *  CATEGORIE   → regrouper par type (outils, armures, blocs, nourriture, matériaux, divers)
 *  ALPHABETIQUE → A→Z par nom d'affichage (ou clé Bukkit)
 *  QUANTITE_ASC → du moins au plus nombreux
 *  QUANTITE_DESC→ du plus au moins nombreux
 *  RARETE       → Commun → Non Commun → Rare → Épique → Légendaire (enchantements, rareté)
 *  SIMILAIRE    → fusionner tous les stacks identiques, puis compacter
 *
 * Dans tous les modes, les stacks identiques sont d'abord fusionnés (stack-merge)
 * avant d'être triés et disposés.
 */
public class ChestSorter {

    public enum SortMode {
        SIMILAIRE   ("§b⬡ Similaires regroupés",  "§7Fusionne et regroupe les items identiques"),
        CATEGORIE   ("§a☰ Par catégorie",          "§7Blocs, outils, armures, nourriture, matériaux…"),
        ALPHABETIQUE("§e🔤 Alphabétique A→Z",      "§7Trie par nom d'item"),
        QUANTITE_DESC("§6📦 Quantité ↓",           "§7Du plus grand au plus petit stack"),
        QUANTITE_ASC ("§6📦 Quantité ↑",           "§7Du plus petit au plus grand stack"),
        RARETE      ("§d✦ Par rareté",             "§7Items enchantés et rares en premier");

        public final String label;
        public final String desc;
        SortMode(String label, String desc) { this.label = label; this.desc = desc; }
    }

    // ── Catégories ──────────────────────────────────────────────────────────────

    public enum ItemCategory {
        OUTIL      ("§e⛏ Outils",      0),
        ARME       ("§c⚔ Armes",       1),
        ARMURE     ("§b🛡 Armures",     2),
        NOURRITURE ("§a🍖 Nourriture",  3),
        POTION     ("§d🧪 Potions",     4),
        MINERAL    ("§6⛏ Minéraux",    5),
        BLOC       ("§7🪨 Blocs",       6),
        PLANTE     ("§2🌿 Plantes",     7),
        REDSTONE   ("§c⚙ Redstone",    8),
        LIVRE      ("§f📖 Livres",      9),
        MATERIAU   ("§e🔧 Matériaux",  10),
        DIVERS     ("§8✦ Divers",      11);

        public final String label;
        public final int order;
        ItemCategory(String label, int order) { this.label = label; this.order = order; }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // API PRINCIPALE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Trie les slots [startSlot, endSlot[ de l'inventaire selon le mode donné.
     * Retourne la liste ordonnée d'ItemStack résultante (les slots sont mis à jour).
     */
    public static void sort(Inventory inv, int startSlot, int endSlot, SortMode mode) {
        // 1. Extraire les items dans la plage
        List<ItemStack> items = extractItems(inv, startSlot, endSlot);

        // 2. Fusionner les stacks identiques (toujours, quel que soit le mode)
        items = mergeStacks(items);

        // 3. Trier selon le mode
        items = switch (mode) {
            case SIMILAIRE    -> sortBySimilar(items);
            case CATEGORIE    -> sortByCategory(items);
            case ALPHABETIQUE -> sortAlphabetically(items);
            case QUANTITE_DESC -> sortByQuantity(items, true);
            case QUANTITE_ASC  -> sortByQuantity(items, false);
            case RARETE       -> sortByRarity(items);
        };

        // 4. Replacer dans l'inventaire
        clearSlots(inv, startSlot, endSlot);
        for (int i = 0; i < items.size() && (startSlot + i) < endSlot; i++) {
            inv.setItem(startSlot + i, items.get(i));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MERGE : fusionner les stacks du même type
    // ════════════════════════════════════════════════════════════════════════════

    public static List<ItemStack> mergeStacks(List<ItemStack> input) {
        // Grouper par empreinte (type + meta)
        LinkedHashMap<String, ItemStack> merged = new LinkedHashMap<>();
        List<ItemStack> nonStackable = new ArrayList<>(); // items avec meta unique (enchantés, etc.)

        for (ItemStack is : input) {
            if (is == null || is.getType() == Material.AIR) continue;

            if (!canFullyStack(is)) {
                // Items non stackables ou avec méta unique : on les garde bruts
                nonStackable.add(is.clone());
                continue;
            }

            String key = fingerprint(is);
            if (merged.containsKey(key)) {
                ItemStack existing = merged.get(key);
                int maxStack = existing.getMaxStackSize();
                int total = existing.getAmount() + is.getAmount();
                if (total <= maxStack) {
                    existing.setAmount(total);
                } else {
                    existing.setAmount(maxStack);
                    ItemStack rest = is.clone();
                    rest.setAmount(total - maxStack);
                    // Ajouter un nouveau slot pour le reste
                    merged.put(key + "#" + System.nanoTime(), rest);
                }
            } else {
                merged.put(key, is.clone());
            }
        }

        List<ItemStack> result = new ArrayList<>(merged.values());
        result.addAll(nonStackable);
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STRATEGIES DE TRI
    // ════════════════════════════════════════════════════════════════════════════

    /** Similaire : regroupe les mêmes types côte à côte (sans autre critère) */
    private static List<ItemStack> sortBySimilar(List<ItemStack> items) {
        return items.stream()
                .sorted(Comparator
                        .comparing((ItemStack i) -> i.getType().name())
                        .thenComparingInt(i -> -i.getAmount()))
                .collect(Collectors.toList());
    }

    /** Catégorie : trie d'abord par catégorie puis par nom dans la catégorie */
    private static List<ItemStack> sortByCategory(List<ItemStack> items) {
        return items.stream()
                .sorted(Comparator
                        .comparingInt((ItemStack i) -> getCategory(i.getType()).order)
                        .thenComparing(i -> displayName(i))
                        .thenComparingInt(i -> -i.getAmount()))
                .collect(Collectors.toList());
    }

    /** Alphabétique : A→Z par nom d'affichage */
    private static List<ItemStack> sortAlphabetically(List<ItemStack> items) {
        return items.stream()
                .sorted(Comparator
                        .comparing(ChestSorter::displayName)
                        .thenComparingInt(i -> -i.getAmount()))
                .collect(Collectors.toList());
    }

    /** Quantité : desc=true pour plus grand en premier */
    private static List<ItemStack> sortByQuantity(List<ItemStack> items, boolean desc) {
        Comparator<ItemStack> cmp = Comparator.comparingInt(ItemStack::getAmount);
        if (desc) cmp = cmp.reversed();
        return items.stream()
                .sorted(cmp.thenComparing(i -> displayName(i)))
                .collect(Collectors.toList());
    }

    /** Rareté : enchanté > stackmax faible (armes/outils/armures) > reste */
    private static List<ItemStack> sortByRarity(List<ItemStack> items) {
        return items.stream()
                .sorted(Comparator
                        .comparingInt((ItemStack i) -> -rarityScore(i))
                        .thenComparing(i -> displayName(i))
                        .thenComparingInt(i -> -i.getAmount()))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLASSIFIERS
    // ════════════════════════════════════════════════════════════════════════════

    public static ItemCategory getCategory(Material mat) {
        String name = mat.name();
        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL")
                || name.equals("FISHING_ROD") || name.equals("BRUSH") || name.equals("SPYGLASS"))
            return ItemCategory.OUTIL;
        if (name.endsWith("_SWORD") || name.endsWith("_BOW") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("ARROW")
                || name.contains("ARROW"))
            return ItemCategory.ARME;
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("SHIELD") || name.contains("ELYTRA"))
            return ItemCategory.ARMURE;
        if (mat.isEdible() || name.contains("BOWL") && !name.equals("BOWL"))
            return ItemCategory.NOURRITURE;
        if (name.contains("POTION") || name.contains("TIPPED_ARROW"))
            return ItemCategory.POTION;
        if (name.contains("ORE") || name.contains("RAW_") || name.equals("COAL")
                || name.equals("IRON_INGOT") || name.equals("GOLD_INGOT")
                || name.equals("DIAMOND") || name.equals("EMERALD")
                || name.equals("NETHERITE_INGOT") || name.equals("NETHERITE_SCRAP")
                || name.equals("LAPIS_LAZULI") || name.equals("QUARTZ")
                || name.equals("AMETHYST_SHARD") || name.equals("COPPER_INGOT")
                || name.contains("CRYSTAL"))
            return ItemCategory.MINERAL;
        if (name.contains("REDSTONE") || name.contains("PISTON") || name.contains("DISPENSER")
                || name.contains("DROPPER") || name.contains("HOPPER") || name.contains("OBSERVER")
                || name.contains("COMPARATOR") || name.contains("REPEATER")
                || name.equals("LEVER") || name.contains("BUTTON") || name.contains("RAIL")
                || name.contains("LAMP") || name.equals("TNT") || name.equals("TRIPWIRE_HOOK"))
            return ItemCategory.REDSTONE;
        if (name.contains("BOOK") || name.equals("MAP") || name.contains("BANNER_PATTERN"))
            return ItemCategory.LIVRE;
        if (mat.isBlock())
            return ItemCategory.BLOC;
        if (name.contains("SEED") || name.contains("SAPLING") || name.contains("FLOWER")
                || name.contains("LEAVES") || name.contains("MUSHROOM") || name.contains("VINE")
                || name.contains("BAMBOO") || name.contains("KELP") || name.contains("GRASS")
                || name.contains("FERN") || name.contains("AZALEA") || name.contains("SPORE")
                || name.equals("CACTUS") || name.equals("SUGAR_CANE") || name.contains("CROP"))
            return ItemCategory.PLANTE;
        if (name.contains("INGOT") || name.contains("NUGGET") || name.contains("STICK")
                || name.contains("STRING") || name.contains("LEATHER") || name.contains("BONE")
                || name.contains("FEATHER") || name.contains("INK") || name.contains("DYE")
                || name.contains("WOOL") || name.contains("GLASS"))
            return ItemCategory.MATERIAU;
        return ItemCategory.DIVERS;
    }

    private static int rarityScore(ItemStack is) {
        int score = 0;
        // Enchantements
        if (!is.getEnchantments().isEmpty()) {
            score += 100 + is.getEnchantments().size() * 10;
        }
        // Meta custom (nom, lore) → légendaire
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) score += 50;
            if (meta.hasLore()) score += 20;
        }
        // Items non stackables (armes, outils, armures) → plus rares
        if (is.getType().getMaxStackSize() == 1) score += 30;
        // Durabilité : items endommagés ont une rareté technique
        if (meta instanceof Damageable d && d.hasDamage()) score += 10;
        return score;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UTILS
    // ════════════════════════════════════════════════════════════════════════════

    private static List<ItemStack> extractItems(Inventory inv, int start, int end) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = start; i < end && i < inv.getSize(); i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType() != Material.AIR) result.add(is.clone());
        }
        return result;
    }

    private static void clearSlots(Inventory inv, int start, int end) {
        for (int i = start; i < end && i < inv.getSize(); i++) inv.setItem(i, null);
    }

    /** Empreinte unique pour deux items identiques (type + meta sérialisée) */
    private static String fingerprint(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        String metaStr = meta != null ? meta.toString() : "";
        return is.getType().name() + "::" + metaStr;
    }

    /** Un item peut être fusionné s'il n'a pas de méta complexe distinctive */
    private static boolean canFullyStack(ItemStack is) {
        if (is.getMaxStackSize() <= 1) return false;
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return true;
        // Si enchantement, nom ou lore custom → on ne fusionne pas
        if (!is.getEnchantments().isEmpty()) return false;
        if (meta.hasDisplayName()) return false;
        if (meta.hasLore()) return false;
        return true;
    }

    /** Nom à afficher : custom si défini, sinon nom Minecraft formaté */
    public static String displayName(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        }
        // Convertir DIAMOND_SWORD → Diamond Sword
        String name = is.getType().name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
