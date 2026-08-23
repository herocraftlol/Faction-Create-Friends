package fr.faction.war;

import fr.faction.claim.ClaimManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.SharedInventoryManager;
import fr.faction.models.Faction;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Système de Guerre entre super-factions (alliances de factions).
 *
 * ── DÉCLENCHEMENT ───────────────────────────────────────────────────────────
 *  Chef A : /fac guerre declarer <factionB> [claims:<0-5>] [pillage] [kills:<10-50>]
 *  Chef B : /fac guerre accepter | /fac guerre refuser
 *
 * ── RÈGLES ──────────────────────────────────────────────────────────────────
 *  • Les deux chefs doivent être du même rang ou d'un rang proche (pas plus de
 *    2 rangs d'écart en puissance) — évite les guerres inégales forcées.
 *  • Kills comptent UNIQUEMENT si la victime est dans un chunk claimé par
 *    l'une des deux factions en guerre (zone de combat).
 *  • Le score est public et broadcast toutes les 5 kills.
 *  • Durée max : 72h (configurable). Si temps écoulé → match nul.
 *  • Cooldown après guerre : 48h avant de pouvoir re-déclarer.
 *
 * ── FIN ─────────────────────────────────────────────────────────────────────
 *  Victoire → claim transfer automatique (chunks les plus éloignés du centre
 *  du perdant transférés en premier) + accès coffre si négocié.
 *  Capitulation → /fac guerre capituler (acceptée automatiquement).
 *
 * ── ANTI-ABUS ───────────────────────────────────────────────────────────────
 *  • Cooldown 48h par faction
 *  • Max 1 guerre active par faction
 *  • Pas de guerre contre un allié
 *  • Claims au max 5
 *  • Pillage max 27 items (1 inventaire)
 *  • Pas de guerre si la faction cible a < 2 membres actifs
 */
public class WarManager implements Listener {

    // ── Constantes anti-abus ────────────────────────────────────────────────────
    public static final int  MAX_CLAIMS_AT_STAKE  = 5;
    public static final int  MAX_RAID_ITEMS       = 27;
    public static final int  DEFAULT_KILLS_TO_WIN = 20;
    public static final int  MIN_KILLS_TO_WIN     = 5;
    public static final int  MAX_KILLS_TO_WIN     = 50;
    public static final long WAR_COOLDOWN_MS      = 48L * 3600 * 1000;   // 48h
    public static final long MAX_WAR_DURATION_MS  = 72L * 3600 * 1000;   // 72h
    public static final long PENDING_TIMEOUT_MS   = 24L * 3600 * 1000;   // 24h pour accepter
    public static final int  MIN_POWER_RATIO      = 3;  // ratio max de puissance (3:1)

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final SharedInventoryManager sharedInvManager;

    // Guerres actives/en attente : id → session
    private final Map<String, WarSession> sessions = new LinkedHashMap<>();

    // Cooldowns post-guerre : factionName → fin cooldown (ms)
    private final Map<String, Long> cooldowns = new HashMap<>();

    // Joueurs en mode raid (coffre ouvert) : UUID → warId
    private final Map<UUID, String> activeRaiders = new HashMap<>();

    private BukkitTask timeoutTask;
    private File dataFile;

    public WarManager(JavaPlugin plugin, FactionManager factionManager,
                       ClaimManager claimManager, SharedInventoryManager sharedInvManager) {
        this.plugin          = plugin;
        this.factionManager  = factionManager;
        this.claimManager    = claimManager;
        this.sharedInvManager = sharedInvManager;
        this.dataFile = new File(plugin.getDataFolder(), "wars.yml");
        load();
        startTimeoutWatcher();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // DÉCLARATION
    // ════════════════════════════════════════════════════════════════════════════

    public enum DeclareResult {
        SUCCESS, NOT_IN_FACTION, NOT_CHEF, TARGET_NOT_FOUND,
        ALREADY_AT_WAR, ON_COOLDOWN, TARGET_ON_COOLDOWN,
        IS_ALLY, POWER_RATIO_TOO_HIGH, TARGET_TOO_SMALL,
        INVALID_CLAIMS, INVALID_KILLS
    }

    /**
     * Déclare une guerre.
     * @param claimsAtStake  0-5 claims à céder
     * @param chestRaid      le vainqueur peut-il piller le coffre partagé ?
     * @param killsToWin     5-50 kills pour gagner
     */
    public DeclareResult declare(Player declarer, String targetFactionName,
                                  int claimsAtStake, boolean chestRaid, int killsToWin,
                                  FactionManager factionManager, fr.faction.power.FactionPowerManager powerManager) {
        Faction myFaction = factionManager.getPlayerFaction(declarer.getUniqueId());
        if (myFaction == null)                        return DeclareResult.NOT_IN_FACTION;
        if (!myFaction.isChef(declarer.getUniqueId())) return DeclareResult.NOT_CHEF;

        Faction target = factionManager.getFaction(targetFactionName);
        if (target == null)                           return DeclareResult.TARGET_NOT_FOUND;
        if (target.getMemberCount() < 2)              return DeclareResult.TARGET_TOO_SMALL;

        if (myFaction.isAlly(targetFactionName))      return DeclareResult.IS_ALLY;
        if (isAtWar(myFaction.getName()))             return DeclareResult.ALREADY_AT_WAR;
        if (isAtWar(targetFactionName))               return DeclareResult.ALREADY_AT_WAR;

        // Cooldown
        if (isOnCooldown(myFaction.getName()))        return DeclareResult.ON_COOLDOWN;
        if (isOnCooldown(targetFactionName))          return DeclareResult.TARGET_ON_COOLDOWN;

        // Ratio de puissance (anti-farming de petites factions)
        if (powerManager != null) {
            double myPower  = powerManager.getFactionPower(myFaction.getName());
            double tgPower  = powerManager.getFactionPower(targetFactionName);
            if (myPower > 0 && tgPower > 0) {
                double ratio = Math.max(myPower, tgPower) / Math.min(myPower, tgPower);
                if (ratio > MIN_POWER_RATIO)          return DeclareResult.POWER_RATIO_TOO_HIGH;
            }
        }

        if (claimsAtStake < 0 || claimsAtStake > MAX_CLAIMS_AT_STAKE) return DeclareResult.INVALID_CLAIMS;
        if (killsToWin < MIN_KILLS_TO_WIN || killsToWin > MAX_KILLS_TO_WIN) return DeclareResult.INVALID_KILLS;

        int raidLimit = chestRaid ? MAX_RAID_ITEMS : 0;
        WarSession session = new WarSession(myFaction.getName(), targetFactionName,
                claimsAtStake, chestRaid, raidLimit, 500, killsToWin, MAX_WAR_DURATION_MS);
        sessions.put(session.getId(), session);
        save();

        // Notifier le chef adverse
        Player targetChef = Bukkit.getPlayer(target.getChef());
        String terms = buildTermsLine(session);
        if (targetChef != null) {
            targetChef.sendMessage(warPrefix() + "§c⚔ " + myFaction.getName() + " §cdéclare la guerre à ta faction !");
            targetChef.sendMessage(warPrefix() + "§7Conditions : " + terms);
            targetChef.sendMessage(warPrefix() + "§e/fac guerre accepter §7ou §c/fac guerre refuser §7(24h pour répondre)");
            targetChef.playSound(targetChef.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }
        notifyFaction(target, "§c⚔ " + myFaction.getName() + " vous a déclaré la guerre ! Ton chef doit accepter ou refuser.");
        notifyFaction(myFaction, "§a⚔ Déclaration de guerre envoyée à §c" + target.getName() + "§a. (ID: §f" + session.getId() + "§a)");

        // Timeout de l'invitation
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            WarSession s = sessions.get(session.getId());
            if (s != null && s.isPending()) {
                s.setState(WarSession.State.CANCELLED);
                sessions.remove(s.getId());
                save();
                notifyFaction(myFaction, "§7La déclaration de guerre contre §c" + target.getName() + " §7n'a pas été acceptée (timeout).");
                notifyFaction(target, "§7La déclaration de guerre de §c" + myFaction.getName() + " §7a expiré.");
            }
        }, PENDING_TIMEOUT_MS / 50); // ticks

        return DeclareResult.SUCCESS;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ACCEPTATION / REFUS
    // ════════════════════════════════════════════════════════════════════════════

    public enum AcceptResult { SUCCESS, NOT_CHEF, NO_PENDING_WAR, NOT_IN_FACTION }

    public AcceptResult accept(Player player, FactionManager fm) {
        Faction faction = fm.getPlayerFaction(player.getUniqueId());
        if (faction == null)                          return AcceptResult.NOT_IN_FACTION;
        if (!faction.isChef(player.getUniqueId()))    return AcceptResult.NOT_CHEF;

        WarSession session = getPendingWarAgainst(faction.getName());
        if (session == null)                          return AcceptResult.NO_PENDING_WAR;

        session.setState(WarSession.State.ACTIVE);
        session.setAcceptedByB(true);
        session.setStartedAt(System.currentTimeMillis());
        save();

        Faction attacker = fm.getFaction(session.getFactionA());
        broadcastWar("§c§l⚔ GUERRE DÉCLARÉE ⚔");
        broadcastWar("§f" + session.getFactionA().toUpperCase() + " §7contre §f" + session.getFactionB().toUpperCase());
        broadcastWar("§7Enjeu : §e" + session.getClaimsAtStake() + " claim(s)"
                + (session.isChestRaidAllowed() ? " + pillage coffre" : "")
                + " §7— Victoire à §e" + session.getKillsToWin() + " kills");
        broadcastWar("§7Durée max : §e72h §7(ID : §f" + session.getId() + "§7)");

        return AcceptResult.SUCCESS;
    }

    public boolean decline(Player player, FactionManager fm) {
        Faction faction = fm.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.isChef(player.getUniqueId())) return false;
        WarSession session = getPendingWarAgainst(faction.getName());
        if (session == null) return false;

        sessions.remove(session.getId());
        save();
        Faction attacker = fm.getFaction(session.getFactionA());
        notifyFaction(faction, "§7Tu as refusé la guerre de §c" + session.getFactionA() + "§7.");
        if (attacker != null) notifyFaction(attacker, "§c" + faction.getName() + " §7a refusé ta déclaration de guerre.");
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CAPITULATION
    // ════════════════════════════════════════════════════════════════════════════

    public boolean surrender(Player player, FactionManager fm) {
        Faction faction = fm.getPlayerFaction(player.getUniqueId());
        if (faction == null || !faction.isChef(player.getUniqueId())) return false;
        WarSession session = getActiveWarOf(faction.getName());
        if (session == null) return false;

        String opponent = session.getOpponent(faction.getName());
        boolean aWins = opponent.equals(session.getFactionA());
        session.setState(aWins ? WarSession.State.FINISHED_A_WINS : WarSession.State.FINISHED_B_WINS);

        resolveWar(session, aWins ? session.getFactionA() : session.getFactionB(), fm);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // KILLS PVP
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        Faction victimFaction  = factionManager.getPlayerFaction(victim.getUniqueId());
        Faction killerFaction  = factionManager.getPlayerFaction(killer.getUniqueId());
        if (victimFaction == null || killerFaction == null) return;
        if (victimFaction.getName().equalsIgnoreCase(killerFaction.getName())) return;

        // Chercher une guerre active entre ces deux factions
        WarSession session = getActiveWarBetween(killerFaction.getName(), victimFaction.getName());
        if (session == null) return;

        // Vérifier que le kill est dans un chunk claimé par l'une des factions en guerre
        Chunk chunk = victim.getLocation().getChunk();
        ClaimManager.ChunkKey key = ClaimManager.ChunkKey.of(chunk);
        ClaimManager.ClaimData claim = claimManager.getClaimAt(key);
        if (claim == null) return; // kill hors claim → ne compte pas
        if (!claim.getFactionName().equalsIgnoreCase(session.getFactionA())
                && !claim.getFactionName().equalsIgnoreCase(session.getFactionB())) return;

        // Ajouter le kill
        boolean killerIsA = session.getFactionA().equalsIgnoreCase(killerFaction.getName());
        if (killerIsA) session.addKillA(); else session.addKillB();
        save();

        int myKills  = killerIsA ? session.getKillsA() : session.getKillsB();
        int oppKills = killerIsA ? session.getKillsB() : session.getKillsA();

        killer.sendMessage(warPrefix() + "§a+1 kill ! §7Score : §f" + myKills + "§7/§f" + session.getKillsToWin()
                + " §8— Adversaire : §f" + oppKills);

        // Broadcast tous les 5 kills
        if (myKills % 5 == 0) {
            broadcastWar("§7⚔ §f" + killerFaction.getName() + " §e" + myKills + " §7— §f"
                    + victimFaction.getName() + " §e" + oppKills + " §7(objectif : §f" + session.getKillsToWin() + "§7)");
        }

        // Vérifier victoire
        if (myKills >= session.getKillsToWin()) {
            session.setState(killerIsA ? WarSession.State.FINISHED_A_WINS : WarSession.State.FINISHED_B_WINS);
            resolveWar(session, killerFaction.getName(), factionManager);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PILLAGE DU COFFRE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Ouvre le coffre partagé de la faction vaincue pour le pillage.
     * Appelé après résolution si chestRaidAllowed.
     */
    public void openRaidChest(Player player, WarSession session, String loserFaction, FactionManager fm) {
        Faction loser = fm.getFaction(loserFaction);
        if (loser == null) { player.sendMessage(warPrefix() + "§cFaction introuvable."); return; }

        Faction playerFaction = fm.getPlayerFaction(player.getUniqueId());
        if (playerFaction == null) { player.sendMessage(warPrefix() + "§cTu n'es pas dans une faction."); return; }

        String winnerFaction = session.getOpponent(loserFaction);
        if (!playerFaction.getName().equalsIgnoreCase(winnerFaction)) {
            player.sendMessage(warPrefix() + "§cSeule la faction victorieuse peut piller ce coffre.");
            return;
        }

        // Vérifier le quota de pillage
        int raidedSoFar = session.getItemsRaidedByA(); // simplifié : on compte globalement
        if (raidedSoFar >= session.getChestRaidLimit()) {
            player.sendMessage(warPrefix() + "§cLe quota de pillage a déjà été atteint (" + session.getChestRaidLimit() + " items).");
            return;
        }

        // Ouvrir une copie limitée du coffre partagé du perdant
        Inventory sharedInv = sharedInvManager.getOrCreateSharedInventory(loserFaction.toLowerCase());
        int remaining = session.getChestRaidLimit() - raidedSoFar;

        // Créer un GUI de pillage temporaire (27 slots, items limités)
        Inventory raidInv = Bukkit.createInventory(null, 27,
                "§c⚔ Pillage — " + loser.getName() + " §8[" + remaining + " items restants]");

        // Copier les N premiers items disponibles
        int copied = 0;
        ItemStack[] contents = sharedInv.getContents();
        for (int i = 0; i < contents.length && copied < Math.min(remaining, 27); i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                raidInv.setItem(copied++, contents[i].clone());
            }
        }

        activeRaiders.put(player.getUniqueId(), session.getId());
        player.openInventory(raidInv);
        player.sendMessage(warPrefix() + "§aTu peux prendre jusqu'à §e" + remaining + " §aitem(s). Ferme l'inventaire pour valider.");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RÉSOLUTION DE GUERRE
    // ════════════════════════════════════════════════════════════════════════════

    private void resolveWar(WarSession session, String winnerFaction, FactionManager fm) {
        String loserFaction = session.getOpponent(winnerFaction);
        sessions.remove(session.getId());
        // Cooldown 48h sur les deux factions
        long until = System.currentTimeMillis() + WAR_COOLDOWN_MS;
        cooldowns.put(winnerFaction.toLowerCase(), until);
        cooldowns.put(loserFaction.toLowerCase(), until);
        save();

        // Broadcast victoire
        broadcastWar("§6§l★ FIN DE GUERRE ★");
        broadcastWar("§aVainqueur : §f§l" + winnerFaction.toUpperCase());
        broadcastWar("§cDéfait : §f§l" + loserFaction.toUpperCase());

        Faction winner = fm.getFaction(winnerFaction);
        Faction loser  = fm.getFaction(loserFaction);

        // Transfer de claims
        if (session.getClaimsAtStake() > 0 && loser != null && winner != null) {
            int transferred = transferClaims(loserFaction, winnerFaction, session.getClaimsAtStake());
            broadcastWar("§e" + transferred + " claim(s) transféré(s) de §c" + loserFaction + " §à §a" + winnerFaction + "§e.");
        }

        // Notifier pour le pillage
        if (session.isChestRaidAllowed() && winner != null) {
            notifyFaction(winner, "§a⚔ Droit de pillage actif ! Utilise §e/fac guerre piller §apour accéder au coffre de §c" + loserFaction + "§a.");
            // Garder la session en mémoire courte pour le pillage (5 minutes)
            String sessionId = session.getId();
            sessions.put(sessionId, session); // réinjecter temporairement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sessions.remove(sessionId);
                if (winner != null) notifyFaction(winner, "§7Le droit de pillage contre §c" + loserFaction + " §7a expiré.");
            }, 20L * 60 * 5); // 5 min
        }

        // Notifier les factions
        if (winner != null) notifyFaction(winner, "§a⚔ Victoire ! Votre faction a gagné la guerre contre §c" + loserFaction + "§a !");
        if (loser != null)  notifyFaction(loser,  "§c⚔ Défaite ! Votre faction a perdu la guerre contre §a" + winnerFaction + "§c.");

        // Effets sonores
        broadcastToFaction(winner, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        broadcastToFaction(loser, Sound.ENTITY_WITHER_DEATH);
    }

    /** Transfère N claims du perdant au gagnant (les plus loin du centre du perdant) */
    private int transferClaims(String loserFaction, String winnerFaction, int count) {
        List<ClaimManager.ChunkKey> loserClaims = claimManager.getClaimsOf(loserFaction);
        if (loserClaims.isEmpty()) return 0;

        // Trier par distance depuis le centre (les périphériques partent en premier)
        // Calculer barycentre
        double avgX = loserClaims.stream().mapToInt(k -> k.cx()).average().orElse(0);
        double avgZ = loserClaims.stream().mapToInt(k -> k.cz()).average().orElse(0);
        loserClaims.sort((a, b) -> {
            double dA = Math.hypot(a.cx() - avgX, a.cz() - avgZ);
            double dB = Math.hypot(b.cx() - avgX, b.cz() - avgZ);
            return Double.compare(dB, dA); // plus loin en premier
        });

        int transferred = 0;
        for (int i = 0; i < Math.min(count, loserClaims.size()); i++) {
            claimManager.transferClaim(loserClaims.get(i), winnerFaction);
            transferred++;
        }
        return transferred;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATUS & INFO
    // ════════════════════════════════════════════════════════════════════════════

    public void showStatus(Player player, FactionManager fm) {
        Faction faction = fm.getPlayerFaction(player.getUniqueId());
        if (faction == null) { player.sendMessage(warPrefix() + "§cTu n'es pas dans une faction."); return; }

        WarSession session = getActiveOrPendingWarOf(faction.getName());
        if (session == null) {
            player.sendMessage(warPrefix() + "§7Aucune guerre active ou en attente pour ta faction.");
            // Cooldown info
            Long cd = cooldowns.get(faction.getName().toLowerCase());
            if (cd != null && cd > System.currentTimeMillis()) {
                long hrs = (cd - System.currentTimeMillis()) / 3600000;
                player.sendMessage(warPrefix() + "§7Cooldown avant prochaine guerre : §e" + hrs + "h");
            }
            return;
        }

        String opp = session.getOpponent(faction.getName());
        player.sendMessage("§c§l⚔ GUERRE : §f" + faction.getName() + " §7vs §f" + opp);
        player.sendMessage("§7État : " + stateLabel(session.getState()));
        if (session.isActive()) {
            player.sendMessage("§7Score : §f" + session.getKillsFor(faction.getName().toLowerCase())
                    + "/" + session.getKillsToWin() + " §7kills §8— Adversaire : §f"
                    + session.getKillsFor(opp.toLowerCase()) + "/" + session.getKillsToWin());
            long remH = session.getRemainingMs() / 3600000;
            long remM = (session.getRemainingMs() % 3600000) / 60000;
            player.sendMessage("§7Temps restant : §e" + remH + "h " + remM + "m");
            player.sendMessage("§7Enjeu : §e" + session.getClaimsAtStake() + " claim(s)"
                    + (session.isChestRaidAllowed() ? " §8+ §epillage coffre" : ""));
            player.sendMessage("§8ID : §7" + session.getId());
        }
    }

    public void showHelp(Player player) {
        player.sendMessage("§c§l⚔ Commandes de Guerre ⚔");
        player.sendMessage("§e/fac guerre declarer <faction> [claims:<0-5>] [pillage] [kills:<5-50>]");
        player.sendMessage("§7  Déclarer la guerre à une faction");
        player.sendMessage("§e/fac guerre accepter §7— Accepter une déclaration de guerre");
        player.sendMessage("§e/fac guerre refuser  §7— Refuser une déclaration de guerre");
        player.sendMessage("§e/fac guerre capituler §7— Capituler (perds automatiquement)");
        player.sendMessage("§e/fac guerre statut    §7— Voir le score & les conditions");
        player.sendMessage("§e/fac guerre piller    §7— Piller le coffre du vaincu (si négocié)");
        player.sendMessage("§e/fac guerre liste     §7— Lister les guerres actives du serveur");
        player.sendMessage("§7");
        player.sendMessage("§8Règles : kills uniquement en zone claimée • cooldown 48h • ratio de puissance max 3:1");
    }

    public void listWars(Player player) {
        List<WarSession> active = sessions.values().stream()
                .filter(s -> s.isActive() || s.isPending())
                .collect(Collectors.toList());
        if (active.isEmpty()) { player.sendMessage(warPrefix() + "§7Aucune guerre active sur le serveur."); return; }
        player.sendMessage("§c§l⚔ Guerres en cours (" + active.size() + ") :");
        for (WarSession s : active) {
            player.sendMessage("  §8[§f" + s.getId() + "§8] §c" + s.getFactionA() + " §7vs §c" + s.getFactionB()
                    + " §8— " + stateLabel(s.getState())
                    + (s.isActive() ? " §7(" + s.getKillsA() + "-" + s.getKillsB() + ")" : ""));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TIMEOUT WATCHER
    // ════════════════════════════════════════════════════════════════════════════

    private void startTimeoutWatcher() {
        timeoutTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (WarSession session : new ArrayList<>(sessions.values())) {
                if (session.isTimedOut()) {
                    session.setState(WarSession.State.DRAW);
                    sessions.remove(session.getId());
                    long until = System.currentTimeMillis() + WAR_COOLDOWN_MS;
                    cooldowns.put(session.getFactionA(), until);
                    cooldowns.put(session.getFactionB(), until);
                    save();
                    broadcastWar("§7⚔ Match nul — la guerre entre §f" + session.getFactionA()
                            + " §7et §f" + session.getFactionB() + " §7a expiré sans vainqueur.");
                    Faction fa = factionManager.getFaction(session.getFactionA());
                    Faction fb = factionManager.getFaction(session.getFactionB());
                    if (fa != null) notifyFaction(fa, "§7La guerre contre §c" + session.getFactionB() + " §7s'est terminée en match nul.");
                    if (fb != null) notifyFaction(fb, "§7La guerre contre §c" + session.getFactionA() + " §7s'est terminée en match nul.");
                }
            }
        }, 20L * 60, 20L * 60); // vérifier toutes les minutes
    }

    public void stop() { if (timeoutTask != null) timeoutTask.cancel(); }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════════

    public boolean isAtWar(String factionName) {
        return sessions.values().stream().anyMatch(s ->
                (s.isActive() || s.isPending()) && s.involves(factionName));
    }

    public boolean isOnCooldown(String factionName) {
        Long cd = cooldowns.get(factionName.toLowerCase());
        return cd != null && System.currentTimeMillis() < cd;
    }

    public long getCooldownRemaining(String factionName) {
        Long cd = cooldowns.get(factionName.toLowerCase());
        if (cd == null) return 0;
        return Math.max(0, cd - System.currentTimeMillis());
    }

    private WarSession getPendingWarAgainst(String factionName) {
        return sessions.values().stream()
                .filter(s -> s.isPending() && s.getFactionB().equalsIgnoreCase(factionName))
                .findFirst().orElse(null);
    }

    public WarSession getActiveWarOf(String factionName) {
        return sessions.values().stream()
                .filter(s -> s.isActive() && s.involves(factionName))
                .findFirst().orElse(null);
    }

    private WarSession getActiveOrPendingWarOf(String factionName) {
        return sessions.values().stream()
                .filter(s -> (s.isActive() || s.isPending()) && s.involves(factionName))
                .findFirst().orElse(null);
    }

    private WarSession getActiveWarBetween(String fa, String fb) {
        return sessions.values().stream()
                .filter(s -> s.isActive()
                        && ((s.getFactionA().equalsIgnoreCase(fa) && s.getFactionB().equalsIgnoreCase(fb))
                        ||  (s.getFactionA().equalsIgnoreCase(fb) && s.getFactionB().equalsIgnoreCase(fa))))
                .findFirst().orElse(null);
    }

    /** Renvoie la session de pillage active pour une faction gagnante */
    public WarSession getRaidableWarFor(String winnerFaction) {
        return sessions.values().stream()
                .filter(s -> s.isChestRaidAllowed() && s.involves(winnerFaction)
                        && (s.getState() == WarSession.State.FINISHED_A_WINS
                        ||  s.getState() == WarSession.State.FINISHED_B_WINS))
                .findFirst().orElse(null);
    }

    private void notifyFaction(Faction faction, String message) {
        if (faction == null) return;
        for (UUID uuid : faction.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(warPrefix() + message);
        }
    }

    private void broadcastToFaction(Faction faction, Sound sound) {
        if (faction == null) return;
        for (UUID uuid : faction.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), sound, 1f, 1f);
        }
    }

    private void broadcastWar(String message) {
        Bukkit.broadcastMessage(warPrefix() + message);
    }

    private String buildTermsLine(WarSession s) {
        return "§e" + s.getClaimsAtStake() + " claim(s)§7"
                + (s.isChestRaidAllowed() ? " + §epillage coffre" : "")
                + " §7— victoire à §e" + s.getKillsToWin() + " kills";
    }

    private String stateLabel(WarSession.State state) {
        return switch (state) {
            case PENDING_ACCEPTANCE -> "§eEn attente d'acceptation";
            case ACTIVE             -> "§c§lEN COURS";
            case FINISHED_A_WINS    -> "§aTerminée";
            case FINISHED_B_WINS    -> "§aTerminée";
            case DRAW               -> "§7Match nul";
            case CANCELLED          -> "§8Annulée";
        };
    }

    private String warPrefix() { return "§8[§c⚔ Guerre§8] §r"; }

    // ════════════════════════════════════════════════════════════════════════════
    // PERSISTANCE
    // ════════════════════════════════════════════════════════════════════════════

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (WarSession s : sessions.values()) {
            if (s.getState() == WarSession.State.CANCELLED) continue;
            String base = "wars." + i;
            cfg.set(base + ".id", s.getId());
            cfg.set(base + ".factionA", s.getFactionA());
            cfg.set(base + ".factionB", s.getFactionB());
            cfg.set(base + ".claimsAtStake", s.getClaimsAtStake());
            cfg.set(base + ".chestRaid", s.isChestRaidAllowed());
            cfg.set(base + ".chestRaidLimit", s.getChestRaidLimit());
            cfg.set(base + ".powerBonus", s.getPowerBonusWinner());
            cfg.set(base + ".killsToWin", s.getKillsToWin());
            cfg.set(base + ".maxDuration", s.getMaxDurationMs());
            cfg.set(base + ".killsA", s.getKillsA());
            cfg.set(base + ".killsB", s.getKillsB());
            cfg.set(base + ".startedAt", s.getStartedAt());
            cfg.set(base + ".state", s.getState().name());
            cfg.set(base + ".acceptedByB", s.isAcceptedByB());
            i++;
        }
        // Cooldowns
        cooldowns.forEach((k, v) -> cfg.set("cooldowns." + k, v));
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Erreur sauvegarde guerres : " + e.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (cfg.contains("wars")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("wars")).getKeys(false)) {
                try {
                    String base = "wars." + key;
                    String id     = cfg.getString(base + ".id");
                    String fa     = cfg.getString(base + ".factionA");
                    String fb     = cfg.getString(base + ".factionB");
                    int claims    = cfg.getInt(base + ".claimsAtStake");
                    boolean chest = cfg.getBoolean(base + ".chestRaid");
                    int chestLim  = cfg.getInt(base + ".chestRaidLimit");
                    int pwBonus   = cfg.getInt(base + ".powerBonus");
                    int kills     = cfg.getInt(base + ".killsToWin");
                    long maxDur   = cfg.getLong(base + ".maxDuration");
                    int ka        = cfg.getInt(base + ".killsA");
                    int kb        = cfg.getInt(base + ".killsB");
                    long start    = cfg.getLong(base + ".startedAt");
                    WarSession.State state = WarSession.State.valueOf(cfg.getString(base + ".state", "PENDING_ACCEPTANCE"));
                    boolean acc   = cfg.getBoolean(base + ".acceptedByB");
                    if (state == WarSession.State.ACTIVE || state == WarSession.State.PENDING_ACCEPTANCE) {
                        sessions.put(id, new WarSession(id, fa, fb, claims, chest, chestLim, pwBonus, kills, maxDur, ka, kb, start, state, acc));
                    }
                } catch (Exception e) { plugin.getLogger().warning("Erreur chargement guerre : " + e.getMessage()); }
            }
        }
        if (cfg.contains("cooldowns")) {
            for (String k : Objects.requireNonNull(cfg.getConfigurationSection("cooldowns")).getKeys(false)) {
                cooldowns.put(k, cfg.getLong("cooldowns." + k));
            }
        }
        plugin.getLogger().info("Guerres : " + sessions.size() + " session(s) chargée(s).");
    }
}
