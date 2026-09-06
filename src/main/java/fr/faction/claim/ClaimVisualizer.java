package fr.faction.claim;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * /fac claimshow — 10 secondes de particules colorées autour de chaque chunk
 * dans un rayon de 4 chunks autour du joueur.
 *
 * Stratégie de rendu :
 *  Pour CHAQUE chunk claimé dans le rayon, on dessine ses 4 arêtes
 *  entières avec la couleur correspondante (les arêtes partagées entre deux
 *  chunks du même propriétaire ne sont dessinées qu'une fois, évitant le
 *  doublon). Les chunks NON claimés ne sont PAS dessinés — le joueur voit
 *  clairement que ce qui brille est claimé, et ce qui est sombre est libre.
 *
 * Couleurs :
 *   VERT  → ma faction          (COLOR_OWN)
 *   JAUNE → faction alliée      (COLOR_ALLY)
 *   ROUGE → faction ennemie     (COLOR_ENEMY)
 *
 * Hauteur des particules :
 *   On cherche le sol réel du chunk (getHighestBlockYAt) au centre,
 *   puis on dessine une colonne de Y_PILLAR blocs vers le haut.
 *   Les particules sont envoyées uniquement au joueur (spawnParticle sur Player).
 *
 * Paper 1.21 : Particle.DUST (remplace l'ancien REDSTONE).
 */
public class ClaimVisualizer {

    // ── Paramètres ───────────────────────────────────────────────────────────────
    private static final int    RADIUS        = 4;    // chunks autour du joueur
    private static final int    DURATION_TICK = 200;  // 10 s × 20 ticks/s
    private static final int    REFRESH_TICK  = 8;    // re-dessiner toutes les 8 ticks (0.4 s)
    private static final double STEP          = 1.0;  // 1 particule par bloc sur l'arête
    private static final int    Y_PILLAR      = 4;    // hauteur de la colonne (blocs)
    private static final float  DUST_SIZE     = 1.5f; // taille DUST

    // Couleurs
    private static final Color C_OWN   = Color.fromRGB(0x00, 0xFF, 0x55); // vert vif
    private static final Color C_ALLY  = Color.fromRGB(0xFF, 0xCC, 0x00); // or/jaune
    private static final Color C_ENEMY = Color.fromRGB(0xFF, 0x22, 0x22); // rouge

    private final JavaPlugin      plugin;
    private final ClaimManager    claimManager;
    private final FactionManager  factionManager;

    private final Map<UUID, BukkitTask> tasks   = new HashMap<>();
    private final Map<UUID, Integer>    elapsed = new HashMap<>();

    public ClaimVisualizer(JavaPlugin plugin, ClaimManager claimManager,
                            FactionManager factionManager) {
        this.plugin         = plugin;
        this.claimManager   = claimManager;
        this.factionManager = factionManager;
    }

    // ── API ──────────────────────────────────────────────────────────────────────

    public void show(Player player) {
        cancel(player.getUniqueId());
        elapsed.put(player.getUniqueId(), 0);

        Faction myFaction = factionManager.getPlayerFaction(player.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { cancel(player.getUniqueId()); return; }
            int e = elapsed.merge(player.getUniqueId(), REFRESH_TICK, Integer::sum);
            if (e > DURATION_TICK)  { cancel(player.getUniqueId()); return; }
            draw(player, myFaction);
        }, 1L, REFRESH_TICK);

        tasks.put(player.getUniqueId(), task);
    }

    public boolean isActive(UUID uuid) { return tasks.containsKey(uuid); }

    public void cancel(UUID uuid) {
        BukkitTask t = tasks.remove(uuid);
        if (t != null) t.cancel();
        elapsed.remove(uuid);
    }

    // ── Rendu principal ───────────────────────────────────────────────────────────

    private void draw(Player player, Faction myFaction) {
        World  world    = player.getWorld();
        String worldName = world.getName();
        int    pcx      = player.getLocation().getBlockX() >> 4;
        int    pcz      = player.getLocation().getBlockZ() >> 4;

        String myName = myFaction != null ? myFaction.getName().toLowerCase() : null;

        // ── Collecte des claims dans le rayon + 1 (pour les voisins) ────────────
        // owner[x][z] : nom faction ou null
        int dim = (RADIUS + 1) * 2 + 1;
        int off = RADIUS + 1;
        String[][] owner = new String[dim][dim];
        for (int dx = -(RADIUS + 1); dx <= RADIUS + 1; dx++) {
            for (int dz = -(RADIUS + 1); dz <= RADIUS + 1; dz++) {
                ClaimManager.ChunkKey key = new ClaimManager.ChunkKey(worldName, pcx + dx, pcz + dz);
                ClaimManager.ClaimData data = claimManager.getAllClaims().get(key);
                owner[dx + off][dz + off] = data != null ? data.getFactionName().toLowerCase() : null;
            }
        }

        // ── Dessin chunk par chunk ────────────────────────────────────────────────
        // On ne dessine que les chunks claimés dans le rayon strict.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                String o = owner[dx + off][dz + off];
                if (o == null) continue; // chunk non claimé → pas de dessin

                // Couleur de ce chunk
                Color color = pickColor(o, myName, myFaction);

                int worldX = (pcx + dx) << 4; // coin X du chunk (bloc W)
                int worldZ = (pcz + dz) << 4; // coin Z du chunk (bloc N)

                // Hauteur de rendu : centre du chunk → sol
                int midX = worldX + 8;
                int midZ = worldZ + 8;
                int baseY = getGroundY(world, midX, midZ, (int) player.getLocation().getY());

                // ── 4 arêtes du chunk ─────────────────────────────────────────────
                // On ne dessine une arête partagée que si le voisin de l'autre côté
                // est différent (pour éviter le double dessin à l'intérieur d'un
                // bloc de claims contigus et améliorer la lisibilité).

                String nN = owner[dx + off][dz + off - 1]; // voisin Nord
                String nS = owner[dx + off][dz + off + 1]; // voisin Sud
                String nW = owner[dx + off - 1][dz + off]; // voisin Ouest
                String nE = owner[dx + off + 1][dz + off]; // voisin Est

                // Arête Nord  z = worldZ,      x : [worldX .. worldX+16]
                if (!o.equals(nN))
                    drawEdge(player, world,
                            worldX, baseY, worldZ,
                            worldX + 16, baseY, worldZ,
                            color);

                // Arête Sud  z = worldZ+16,    x : [worldX .. worldX+16]
                if (!o.equals(nS))
                    drawEdge(player, world,
                            worldX, baseY, worldZ + 16,
                            worldX + 16, baseY, worldZ + 16,
                            color);

                // Arête Ouest  x = worldX,     z : [worldZ .. worldZ+16]
                if (!o.equals(nW))
                    drawEdge(player, world,
                            worldX, baseY, worldZ,
                            worldX, baseY, worldZ + 16,
                            color);

                // Arête Est  x = worldX+16,    z : [worldZ .. worldZ+16]
                if (!o.equals(nE))
                    drawEdge(player, world,
                            worldX + 16, baseY, worldZ,
                            worldX + 16, baseY, worldZ + 16,
                            color);
            }
        }
    }

    // ── Dessin d'une arête ────────────────────────────────────────────────────────

    /**
     * Dessine une ligne de particules DUST entre (x1,y,z1) et (x2,y,z2),
     * en colonne verticale de Y_PILLAR blocs.
     * Toutes les particules sont envoyées uniquement au joueur.
     */
    private void drawEdge(Player player, World world,
                           int x1, int baseY, int z1,
                           int x2, int baseY2, int z2,
                           Color color) {
        double len = Math.sqrt((double)(x2-x1)*(x2-x1) + (double)(z2-z1)*(z2-z1));
        int steps  = (int) Math.ceil(len / STEP);

        Particle.DustOptions dust = new Particle.DustOptions(color, DUST_SIZE);

        for (int s = 0; s <= steps; s++) {
            double t  = steps == 0 ? 0.0 : (double) s / steps;
            double px = x1 + (x2 - x1) * t;
            double pz = z1 + (z2 - z1) * t;

            // Colonne verticale
            for (int h = 0; h < Y_PILLAR; h++) {
                double py = baseY + h + 0.5;
                player.spawnParticle(
                        Particle.DUST,
                        new Location(world, px, py, pz),
                        1,      // count
                        0, 0, 0, // offset
                        0,       // extra / speed
                        dust
                );
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Trouve la hauteur du sol à (x, z). On utilise getHighestBlockYAt pour
     * avoir la vraie surface. On clamp autour de la position Y du joueur pour
     * éviter les problèmes dans les cavernes.
     */
    private int getGroundY(World world, int x, int z, int playerY) {
        try {
            int highest = world.getHighestBlockYAt(x, z);
            // Si le sol est trop loin du joueur (caverne, bâtiment), on utilise
            // la position Y du joueur pour que les particules soient visibles.
            if (Math.abs(highest - playerY) > 24) return playerY;
            return highest;
        } catch (Exception e) {
            return playerY;
        }
    }

    /**
     * Détermine la couleur d'un chunk claimé selon son propriétaire.
     * - Ma faction     → vert
     * - Faction alliée → jaune
     * - Autre          → rouge
     */
    private Color pickColor(String chunkOwner, String myFactionName, Faction myFaction) {
        if (myFactionName != null && myFactionName.equals(chunkOwner)) return C_OWN;
        if (myFaction != null && myFaction.isAlly(chunkOwner))          return C_ALLY;
        return C_ENEMY;
    }
}
