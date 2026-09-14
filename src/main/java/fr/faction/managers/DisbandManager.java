package fr.faction.managers;

import fr.faction.claim.ClaimManager;
import fr.faction.economy.EmeraldBankManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.power.FactionTabManager;
import fr.faction.web.WebMapSync;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gère le délai d'une heure entre la demande de dissolution d'une faction
 * (/faction disband) et sa suppression effective : le temps que les membres
 * récupèrent leurs affaires dans les claims/coffres avant que tout ne soit
 * libéré et supprimé — claims, coffre partagé de faction, compte en banque,
 * classement de puissance. Pendant ce délai, la faction continue d'exister
 * normalement (les membres gardent l'accès à leurs claims/coffres).
 */
public class DisbandManager {

    public static final long DELAY_MS = 60L * 60 * 1000; // 1 heure

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final EmeraldBankManager bankManager;
    private final FactionPowerManager powerManager;
    private final SharedInventoryManager sharedInvManager;
    private FactionTabManager tabManager;
    private WebMapSync webMapSync;

    public DisbandManager(JavaPlugin plugin, FactionManager factionManager, ClaimManager claimManager,
                           EmeraldBankManager bankManager, FactionPowerManager powerManager,
                           SharedInventoryManager sharedInvManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.bankManager = bankManager;
        this.powerManager = powerManager;
        this.sharedInvManager = sharedInvManager;
    }

    public void setTabManager(FactionTabManager tm) { this.tabManager = tm; }
    public void setWebMapSync(WebMapSync wms)       { this.webMapSync = wms; }

    /** Démarre le compte à rebours d'une heure pour une faction qui vient d'être dissoute. */
    public void scheduleDisband(Faction faction) {
        faction.setDisbandScheduledAt(System.currentTimeMillis());

        // Autorise explicitement chaque membre actuel sur tous les claims de la faction :
        // s'il quitte pour une autre faction avant la fin de l'heure (ex: pour rerejoindre
        // son ancienne faction), il garde quand même accès à ses coffres/claims jusqu'à ce
        // qu'ils soient réellement libérés — sinon il perdrait l'accès à ses propres affaires
        // en plein milieu du délai censé lui laisser le temps de les récupérer.
        for (UUID uuid : faction.getMembers()) {
            claimManager.allowPlayerOnAllClaims(faction.getName(), uuid);
        }

        factionManager.saveFactions();
        scheduleTask(faction.getName(), DELAY_MS);
    }

    /**
     * À appeler au démarrage du serveur : reprend les dissolutions restées en attente
     * si le serveur a redémarré pendant le délai d'une heure.
     */
    public void resumePendingDisbands() {
        for (Faction faction : new ArrayList<>(factionManager.getAllFactions().values())) {
            if (!faction.isPendingDisband()) continue;
            long elapsed = System.currentTimeMillis() - faction.getDisbandScheduledAt();
            if (elapsed >= DELAY_MS) {
                finalizeDisband(faction.getName());
            } else {
                scheduleTask(faction.getName(), DELAY_MS - elapsed);
            }
        }
    }

    private void scheduleTask(String factionName, long delayMs) {
        long delayTicks = Math.max(1L, delayMs / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> finalizeDisband(factionName), delayTicks);
    }

    /** Suppression effective : claims libérés, coffre partagé et banque supprimés, classement nettoyé. */
    private void finalizeDisband(String factionName) {
        Faction faction = factionManager.getFaction(factionName);
        if (faction == null || !faction.isPendingDisband()) return; // déjà traité entre-temps

        List<UUID> members = new ArrayList<>(faction.getMembers());
        purgeFactionData(factionName);

        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage(ChatColor.RED + "La faction " + factionName + " a été définitivement supprimée "
                    + "(claims et coffres libérés).");
            if (tabManager != null) tabManager.refresh(p);
        }
        if (webMapSync != null) webMapSync.pushSnapshotNow();
    }

    /** Supprime réellement toutes les données d'une faction : claims, coffre partagé, banque, classement, faction elle-même. */
    private void purgeFactionData(String factionName) {
        claimManager.removeAllClaims(factionName);
        sharedInvManager.deleteFactionInventory(factionName);
        bankManager.deleteFactionAccount(factionName);
        powerManager.removeFaction(factionName);
        factionManager.disbandFaction(factionName);
    }

    // ════════════════════════════════════════════════════════════════════════
    // FACTIONS "FANTÔMES" (0 membre) — nettoyage immédiat, sans délai puisqu'il
    // n'y a personne pour venir récupérer quoi que ce soit.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Supprime immédiatement toute faction sans aucun membre : libère ses claims
     * et son coffre protégé, supprime son compte en banque et son entrée de
     * classement, puis la faction elle-même. Pas de délai d'une heure ici,
     * contrairement à une dissolution demandée par un chef : une faction sans
     * membre n'a personne pour venir chercher ses affaires.
     *
     * @return le nombre de factions fantômes supprimées
     */
    public int purgeGhostFactions() {
        List<String> ghostNames = new ArrayList<>();
        for (Faction faction : factionManager.getAllFactions().values()) {
            if (faction.getMembers().isEmpty()) ghostNames.add(faction.getName());
        }
        for (String name : ghostNames) {
            purgeFactionData(name);
        }
        if (!ghostNames.isEmpty() && webMapSync != null) webMapSync.pushSnapshotNow();
        return ghostNames.size();
    }

    /** Programme une vérification périodique des factions fantômes (filet de sécurité, toutes les 30 min). */
    public void startGhostFactionWatch() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int removed = purgeGhostFactions();
            if (removed > 0) plugin.getLogger().info(removed + " faction(s) fantôme(s) (0 membre) nettoyée(s) automatiquement.");
            int orphanedClaims = purgeOrphanedClaims();
            if (orphanedClaims > 0) plugin.getLogger().info(orphanedClaims + " faction(s) disparue(s) avaient encore des claims orphelins : libérés automatiquement.");
        }, 6000L, 36000L); // 5 min après le démarrage, puis toutes les 30 min
    }

    /**
     * Libère les claims "orphelins" : des chunks encore marqués comme claimés par une
     * faction qui n'existe plus (dissoute avant que la libération automatique des claims
     * n'existe). Les coffres qu'ils contenaient redeviennent accessibles à tous, comme
     * n'importe quel chunk non claimé.
     *
     * @return le nombre de factions orphelines dont les claims ont été libérés
     */
    public int purgeOrphanedClaims() {
        java.util.Set<String> orphanFactionNames = new java.util.HashSet<>();
        for (ClaimManager.ClaimData data : claimManager.getAllClaims().values()) {
            String owner = data.getFactionName();
            if (factionManager.getFaction(owner) == null) orphanFactionNames.add(owner);
        }
        for (String name : orphanFactionNames) {
            claimManager.removeAllClaims(name);
        }
        return orphanFactionNames.size();
    }
}
