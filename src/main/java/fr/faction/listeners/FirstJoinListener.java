package fr.faction.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Téléporte les nouveaux joueurs aléatoirement entre ±10 000 en X et Z,
 * sur un bloc solide non dangereux. Le spawn aléatoire est leur point de
 * réapparition par défaut jusqu'à ce qu'ils en définissent un.
 *
 * Ne s'applique qu'à la PREMIÈRE connexion (hasPlayedBefore() == false).
 */
public class FirstJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    private static final int RANGE      = 10_000; // ±10 000 blocs
    private static final int MAX_TRIES  = 25;     // tentatives max pour trouver un sol sûr

    // Materials dangereux : eau, lave, vide, feu
    private static final java.util.Set<Material> DANGER = java.util.Set.of(
            Material.WATER, Material.LAVA, Material.AIR,
            Material.VOID_AIR, Material.CAVE_AIR,
            Material.FIRE, Material.SOUL_FIRE
    );

    public FirstJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) return; // pas la première fois

        // Différé d'1 tick pour que le joueur soit bien chargé côté serveur
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                teleportRandom(player);
            }
        }.runTaskLater(plugin, 5L);
    }

    private void teleportRandom(Player player) {
        World world = Bukkit.getWorlds().get(0); // monde principal

        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            int x = random.nextInt(RANGE * 2 + 1) - RANGE; // -10000 à +10000
            int z = random.nextInt(RANGE * 2 + 1) - RANGE;

            // Hauteur du sol
            Location candidate = findSafeSpot(world, x, z);
            if (candidate == null) continue;

            player.teleport(candidate);
            player.setBedSpawnLocation(candidate, true); // spawn par défaut

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.YELLOW + ChatColor.BOLD
                    + "Bienvenue sur le serveur !");
            player.sendMessage(ChatColor.GRAY + "Tu as été téléporté en un endroit aléatoire.");
            player.sendMessage(ChatColor.GRAY + "Ce point sera ton spawn par défaut jusqu'à ce que tu fasses §e/sethome§7.");
            player.sendMessage(ChatColor.GRAY + "Tape §e/fac §7pour découvrir les factions !");
            player.sendMessage("");

            player.playSound(candidate, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
            player.spawnParticle(Particle.PORTAL, candidate.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
            return;
        }

        // Fallback : spawn du monde
        player.sendMessage(ChatColor.YELLOW + "⚠ Impossible de trouver un spawn sûr, tu apparais au spawn du monde.");
    }

    private Location findSafeSpot(World world, int x, int z) {
        // getHighestBlockYAt retourne le Y du bloc le plus haut non-air
        try {
            int y = world.getHighestBlockYAt(x, z);
            if (y < -60 || y > 320) return null;

            Block ground = world.getBlockAt(x, y, z);
            Block feet   = world.getBlockAt(x, y + 1, z);
            Block head   = world.getBlockAt(x, y + 2, z);

            // Sol doit être solide et non dangereux
            if (!ground.getType().isSolid())        return null;
            if (DANGER.contains(ground.getType()))  return null;
            // Espace pour le joueur (2 blocs libres au-dessus)
            if (!feet.getType().isAir())             return null;
            if (!head.getType().isAir())             return null;

            return new Location(world, x + 0.5, y + 1, z + 0.5, 0f, 0f);
        } catch (Exception e) {
            return null;
        }
    }
}
