package fr.faction.alliance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Système de téléportation entre joueurs (/tpa, /tpaccept, /tpdeny).
 *
 * Flux :
 *   A fait /tpa B → B reçoit une demande (expire après 30s)
 *   B fait /tpaccept  → A est téléporté vers B (après 3s de warmup)
 *   B fait /tpdeny    → demande refusée
 *
 * Intégré aussi via /faction tpa, /faction tpaccept, /faction tpdeny.
 */
public class PlayerTeleportManager {

    private static final int REQUEST_TIMEOUT = 30;  // secondes
    private static final int WARMUP_SECONDS  = 3;
    private static final int COOLDOWN_SECONDS = 60;

    private final JavaPlugin plugin;

    // requester UUID → target UUID
    private final Map<UUID, UUID>  pendingRequests = new HashMap<>();
    // target UUID → requester UUID (inverse pour l'acceptation rapide)
    private final Map<UUID, UUID>  incomingRequest = new HashMap<>();
    // cooldowns : UUID → fin (ms)
    private final Map<UUID, Long>  cooldowns       = new HashMap<>();

    public PlayerTeleportManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── /TPA ───────────────────────────────────────────────────────────────────

    public void sendRequest(Player requester, String targetName) {
        // Cooldown
        long now = System.currentTimeMillis();
        Long cd = cooldowns.get(requester.getUniqueId());
        if (cd != null && now < cd) {
            long rem = (cd - now) / 1000 + 1;
            send(requester, "§cCooldown : encore §e" + rem + "s §cavant d'envoyer une demande.");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            send(requester, "§cJoueur introuvable ou hors ligne.");
            return;
        }
        if (target.equals(requester)) {
            send(requester, "§cTu ne peux pas te téléporter à toi-même.");
            return;
        }

        UUID rUUID = requester.getUniqueId();
        UUID tUUID = target.getUniqueId();

        pendingRequests.put(rUUID, tUUID);
        incomingRequest.put(tUUID, rUUID);

        send(requester, "§aDemande de téléportation envoyée à §e" + target.getName() + "§a. (expire dans " + REQUEST_TIMEOUT + "s)");
        target.sendMessage(prefix() + "§e" + requester.getName() + " §aveut se téléporter à toi !");
        target.sendMessage(prefix() + "§7Tape §b/tpaccept §7pour accepter ou §c/tpdeny §7pour refuser.");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);

        // Expiration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(rUUID) && tUUID.equals(pendingRequests.get(rUUID))) {
                pendingRequests.remove(rUUID);
                incomingRequest.remove(tUUID, rUUID);
                if (requester.isOnline()) send(requester, "§7Ta demande de téléportation vers §f" + target.getName() + " §7a expiré.");
                if (target.isOnline()) target.sendMessage(prefix() + "§7La demande de §f" + requester.getName() + " §7a expiré.");
            }
        }, REQUEST_TIMEOUT * 20L);
    }

    // ─── /TPACCEPT ──────────────────────────────────────────────────────────────

    public void acceptRequest(Player target) {
        UUID tUUID = target.getUniqueId();
        UUID rUUID = incomingRequest.get(tUUID);
        if (rUUID == null) {
            send(target, "§cAucune demande de téléportation en attente.");
            return;
        }

        Player requester = Bukkit.getPlayer(rUUID);
        pendingRequests.remove(rUUID);
        incomingRequest.remove(tUUID);

        if (requester == null || !requester.isOnline()) {
            send(target, "§cLe joueur qui a fait la demande s'est déconnecté.");
            return;
        }

        send(target, "§aTu as accepté la demande de §e" + requester.getName() + "§a.");
        send(requester, "§a" + target.getName() + " §aa accepté ta demande ! Téléportation dans §e" + WARMUP_SECONDS + "s§a...");
        cooldowns.put(rUUID, System.currentTimeMillis() + COOLDOWN_SECONDS * 1000L);

        Location beforeLoc = requester.getLocation().clone();
        final Location dest = target.getLocation();

        new BukkitRunnable() {
            int ticks = WARMUP_SECONDS;
            @Override
            public void run() {
                if (!requester.isOnline()) { cancel(); return; }
                if (requester.getLocation().distanceSquared(beforeLoc) > 1) {
                    send(requester, "§cTéléportation annulée (tu as bougé).");
                    cancel(); return;
                }
                if (--ticks <= 0) {
                    requester.teleport(target.isOnline() ? target.getLocation() : dest);
                    send(requester, "§aTéléporté à §e" + target.getName() + "§a !");
                    requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ─── /TPDENY ────────────────────────────────────────────────────────────────

    public void denyRequest(Player target) {
        UUID tUUID = target.getUniqueId();
        UUID rUUID = incomingRequest.remove(tUUID);
        if (rUUID == null) {
            send(target, "§cAucune demande de téléportation en attente.");
            return;
        }
        pendingRequests.remove(rUUID);
        send(target, "§cTu as refusé la demande de téléportation.");
        Player requester = Bukkit.getPlayer(rUUID);
        if (requester != null && requester.isOnline()) {
            send(requester, "§c" + target.getName() + " a refusé ta demande de téléportation.");
        }
    }

    // ─── UTILS ──────────────────────────────────────────────────────────────────

    private void send(Player p, String msg) { p.sendMessage(prefix() + msg); }
    private String prefix() { return "§8[§b✈ TP§8] §r"; }
}
