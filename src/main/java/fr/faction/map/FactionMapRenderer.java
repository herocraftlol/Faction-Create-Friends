package fr.faction.map;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.*;

/**
 * MapRenderer personnalisé pour la mini-map de faction.
 *
 * La carte Minecraft est 128×128 pixels, couvrant une région configurable
 * centrée sur (0, 0). Avec WORLD_HALF = 10 000, un pixel = 156.25 blocs.
 *
 * Contenu dessiné :
 *  ──────────────────────────────────────────────────────────────────
 *  Fond     : biome approximatif (couleur Overworld, Nether, End)
 *              via getBlockAt en surface → vert=herbe, bleu=eau, gris=pierre…
 *              rendu une seule fois puis mis en cache (lourd à calculer).
 *
 *  Claims   : pixels légèrement plus clairs dans les couleurs de faction
 *              (non implémenté dans cette version — le fond suffit pour la lisibilité)
 *
 *  Joueurs  : points 3×3 pixels colorés
 *    - Blanc    → soi-même (clignotant toutes les secondes)
 *    - Vert     → membre de sa faction
 *    - Jaune    → membre d'une faction alliée ayant activé le partage
 *    - Rouge    → aucun (les ennemis ne sont pas visibles)
 *
 *  Croix    → spawn de la faction si défini
 *  Maison   → homes du joueur (petite croix blanche)
 * ──────────────────────────────────────────────────────────────────
 *
 * Mise à jour : toutes les 2 secondes (40 ticks).
 * Le fond n'est PAS recalculé à chaque frame (cache persistant par MapView).
 */
public class FactionMapRenderer extends MapRenderer {

    // ── Paramètres ───────────────────────────────────────────────────────────────
    public static final int MAP_SIZE    = 128;       // pixels de la carte
    public static final int WORLD_HALF  = 10_000;    // demi-côté du monde couvert

    /** Taille d'un pixel en blocs : 10000*2 / 128 ≈ 156.25 */
    private static final double SCALE   = (double)(WORLD_HALF * 2) / MAP_SIZE;

    // ── Couleurs MapPalette ───────────────────────────────────────────────────────
    // MapPalette.getColor() retourne l'index de la couleur la plus proche
    private static final byte C_SELF_A   = mapColor(Color.WHITE);
    private static final byte C_SELF_B   = mapColor(Color.fromRGB(180, 180, 180));
    private static final byte C_MEMBER   = mapColor(Color.fromRGB(0x00, 0xFF, 0x55));
    private static final byte C_ALLY     = mapColor(Color.fromRGB(0xFF, 0xD7, 0x00));
    private static final byte C_SPAWN    = mapColor(Color.fromRGB(0xFF, 0x44, 0x44));
    private static final byte C_HOME     = mapColor(Color.fromRGB(0x88, 0xFF, 0xFF));
    private static final byte C_WATER    = mapColor(Color.fromRGB(0x3D, 0x6B, 0xB5));
    private static final byte C_GRASS    = mapColor(Color.fromRGB(0x55, 0x9A, 0x25));
    private static final byte C_FOREST   = mapColor(Color.fromRGB(0x2D, 0x62, 0x10));
    private static final byte C_MOUNTAIN = mapColor(Color.fromRGB(0x88, 0x88, 0x88));
    private static final byte C_SAND     = mapColor(Color.fromRGB(0xDB, 0xD2, 0x8A));
    private static final byte C_SNOW     = mapColor(Color.fromRGB(0xEE, 0xEE, 0xEE));
    private static final byte C_NETHER   = mapColor(Color.fromRGB(0x6E, 0x1A, 0x10));
    private static final byte C_VOID     = mapColor(Color.fromRGB(0x10, 0x0A, 0x1A));
    private static final byte C_BORDER   = mapColor(Color.fromRGB(0x33, 0x33, 0x33));

    // ── État ─────────────────────────────────────────────────────────────────────
    private final FactionManager factionManager;
    private final FactionMapManager mapManager;
    private final UUID ownerUUID;
    private final String worldName;

    /** Cache du fond — calculé une seule fois, null = pas encore rendu */
    private byte[] backgroundCache = null;
    private boolean backgroundDirty = true;

    /** Compteur de ticks pour le clignotement du joueur lui-même */
    private int tickCount = 0;

    public FactionMapRenderer(FactionManager factionManager, FactionMapManager mapManager,
                               UUID ownerUUID, String worldName) {
        super(true); // contextual = true → render appelé par joueur
        this.factionManager = factionManager;
        this.mapManager     = mapManager;
        this.ownerUUID      = ownerUUID;
        this.worldName      = worldName;
    }

    // ── Rendu principal ───────────────────────────────────────────────────────────

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        tickCount++;

        // ── 1. Fond ──────────────────────────────────────────────────────────────
        if (backgroundDirty || backgroundCache == null) {
            backgroundCache = buildBackground(view.getWorld());
            backgroundDirty = false;
        }
        // Dessiner le fond depuis le cache
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                canvas.setPixel(x, z, backgroundCache[x * MAP_SIZE + z]);
            }
        }

        // ── 2. Joueurs ───────────────────────────────────────────────────────────
        Faction myFaction = factionManager.getPlayerFaction(ownerUUID);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getWorld().getName().equals(worldName)) continue;

            UUID uid = online.getUniqueId();
            int px = worldToPixel(online.getLocation().getX());
            int pz = worldToPixel(online.getLocation().getZ());
            if (outOfMap(px, pz)) continue;

            byte color;
            int dotSize;

            if (uid.equals(ownerUUID)) {
                // Soi-même → blanc clignotant
                color   = (tickCount / 20) % 2 == 0 ? C_SELF_A : C_SELF_B;
                dotSize = 3;
            } else if (myFaction != null && myFaction.isMember(uid)) {
                // Membre de ma faction
                color   = C_MEMBER;
                dotSize = 3;
            } else {
                // Allié qui partage sa position ?
                Faction theirFaction = factionManager.getPlayerFaction(uid);
                if (myFaction != null && theirFaction != null
                        && myFaction.isAlly(theirFaction.getName())
                        && mapManager.isSharingPosition(uid)) {
                    color   = C_ALLY;
                    dotSize = 2;
                } else {
                    continue; // inconnu / ennemi → invisibles
                }
            }

            drawDot(canvas, px, pz, dotSize, color);
        }

        // ── 3. Spawn faction ──────────────────────────────────────────────────────
        if (myFaction != null && myFaction.hasSpawn()) {
            Location spawn = myFaction.getFactionSpawn();
            if (worldName.equals(spawn.getWorld().getName())) {
                int sx = worldToPixel(spawn.getX());
                int sz = worldToPixel(spawn.getZ());
                if (!outOfMap(sx, sz)) drawCross(canvas, sx, sz, C_SPAWN);
            }
        }

        // ── 4. Homes du joueur ────────────────────────────────────────────────────
        List<org.bukkit.Location> homes = mapManager.getPlayerHomes(ownerUUID, worldName);
        for (Location home : homes) {
            int hx = worldToPixel(home.getX());
            int hz = worldToPixel(home.getZ());
            if (!outOfMap(hx, hz)) drawCross(canvas, hx, hz, C_HOME);
        }

        // ── 5. Bordure ───────────────────────────────────────────────────────────
        for (int i = 0; i < MAP_SIZE; i++) {
            canvas.setPixel(i, 0, C_BORDER);
            canvas.setPixel(i, MAP_SIZE - 1, C_BORDER);
            canvas.setPixel(0, i, C_BORDER);
            canvas.setPixel(MAP_SIZE - 1, i, C_BORDER);
        }

        // ── 6. Curseur du joueur (croix centrale) ─────────────────────────────────
        // Déjà géré par le dot ci-dessus, pas besoin d'un curseur de carte
        // (les curseurs de carte sont surchargés par Paper et peu fiables sur 1.21)
    }

    // ── Fond ─────────────────────────────────────────────────────────────────────

    /**
     * Calcule le fond de la carte pixel par pixel.
     * Pour chaque pixel on obtient le biome au centre de la zone correspondante.
     * C'est assez rapide car on n'utilise pas getHighestBlockAt (trop lourd).
     */
    private byte[] buildBackground(World world) {
        byte[] bg = new byte[MAP_SIZE * MAP_SIZE];

        if (world == null) {
            Arrays.fill(bg, C_VOID);
            return bg;
        }

        World.Environment env = world.getEnvironment();

        for (int px = 0; px < MAP_SIZE; px++) {
            for (int pz = 0; pz < MAP_SIZE; pz++) {
                byte color;
                if (env == World.Environment.NETHER) {
                    color = C_NETHER;
                } else if (env == World.Environment.THE_END) {
                    color = C_VOID;
                } else {
                    // Overworld : couleur par biome
                    int worldX = pixelToWorld(px);
                    int worldZ = pixelToWorld(pz);
                    try {
                        Object bObj = world.getClass()
                            .getMethod("getBiome", int.class, int.class, int.class)
                            .invoke(world, worldX, 64, worldZ);
                        Object bKey = bObj.getClass().getMethod("getKey").invoke(bObj);
                        String bName = bKey.getClass().getMethod("getKey").invoke(bKey).toString().toUpperCase();
                        color = biomeColor(bName);
                    } catch (Exception e) {
                        color = C_GRASS;
                    }
                }
                bg[px * MAP_SIZE + pz] = color;
            }
        }
        return bg;
    }

    private byte biomeColor(String name) {
        if (name == null) name = "PLAINS";
        if (name.contains("OCEAN") || name.contains("RIVER") || name.contains("DEEP")) return C_WATER;
        if (name.contains("BEACH") || name.contains("DESERT") || name.contains("BADLANDS")
                || name.contains("SAVANNA")) return C_SAND;
        if (name.contains("SNOWY") || name.contains("ICE") || name.contains("FROZEN")
                || name.contains("COLD")) return C_SNOW;
        if (name.contains("PEAK") || name.contains("MOUNTAIN") || name.contains("STONY")
                || name.contains("HIGHLANDS")) return C_MOUNTAIN;
        if (name.contains("FOREST") || name.contains("TAIGA") || name.contains("JUNGLE")
                || name.contains("SWAMP") || name.contains("MANGROVE")) return C_FOREST;
        if (name.contains("NETHER") || name.contains("BASALT") || name.contains("CRIMSON")
                || name.contains("WARPED") || name.contains("SOUL")) return C_NETHER;
        if (name.contains("END") || name.contains("VOID")) return C_VOID;
        return C_GRASS; // meadow, plains, etc.
    }

    /** Invalide le cache de fond (utile si le monde change — peu fréquent) */
    public void invalidateBackground() { backgroundDirty = true; }

    // ── Primitives de dessin ──────────────────────────────────────────────────────

    private void drawDot(MapCanvas canvas, int cx, int cz, int size, byte color) {
        int half = size / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int x = cx + dx, z = cz + dz;
                if (!outOfMap(x, z)) canvas.setPixel(x, z, color);
            }
        }
    }

    private void drawCross(MapCanvas canvas, int cx, int cz, byte color) {
        for (int d = -2; d <= 2; d++) {
            if (!outOfMap(cx + d, cz)) canvas.setPixel(cx + d, cz, color);
            if (!outOfMap(cx, cz + d)) canvas.setPixel(cx, cz + d, color);
        }
    }

    // ── Conversion coordonnées ────────────────────────────────────────────────────

    /** Coordonnée monde X ou Z → pixel sur la carte [0, MAP_SIZE[ */
    public static int worldToPixel(double worldCoord) {
        return (int) Math.floor((worldCoord + WORLD_HALF) / SCALE);
    }

    /** Centre d'un pixel → coordonnée monde correspondante */
    public static int pixelToWorld(int pixel) {
        return (int) ((pixel + 0.5) * SCALE - WORLD_HALF);
    }

    private static boolean outOfMap(int x, int z) {
        return x < 0 || x >= MAP_SIZE || z < 0 || z >= MAP_SIZE;
    }

    /** Convertit une couleur Bukkit Color en index de palette MapPalette */
    @SuppressWarnings("deprecation")
    private static byte mapColor(Color color) {
        return MapPalette.matchColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()));
    }
}
