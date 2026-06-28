package fr.faction.power;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.ranking.FactionRank;
import fr.factionstats.managers.StatsManager;
import fr.factionstats.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Calcule et maintient la Puissance Globale (PG) de chaque faction,
 * applique les effets passifs selon le rang, et notifie les montées de rang.
 *
 * PG = Σ(PI_membres) + bonus_taille
 * bonus_taille = nb_membres * 50 (encourage les grandes factions)
 */
public class FactionPowerManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final StatsManager statsManager; // peut être null si FactionStats absent

    // Cache : factionName(lowercase) -> puissance globale calculée
    private final Map<String, Double> powerCache = new HashMap<>();
    // Cache : factionName -> rang actuel (pour détecter les montées)
    private final Map<String, FactionRank> rankCache = new HashMap<>();

    private BukkitTask updateTask;
    private BukkitTask effectTask;

    private static final double MEMBER_BONUS = 50.0;
    // Durée des effets en ticks (2 secondes, renouvelés toutes les 3 sec)
    private static final int EFFECT_DURATION = 60;

    public FactionPowerManager(JavaPlugin plugin, FactionManager factionManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.statsManager = statsManager;
    }

    public void start() {
        // Recalcul des puissances toutes les 2 minutes
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::recalculateAll, 20L, 20L * 120);

        // Application des effets passifs toutes les 3 secondes
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyPassiveEffects, 60L, 60L);

        // Premier calcul immédiat
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::recalculateAll, 40L);
    }

    public void stop() {
        if (updateTask != null) updateTask.cancel();
        if (effectTask != null) effectTask.cancel();
    }

    // ─── Calcul ──────────────────────────────────────────────────────────────────

    public void recalculateAll() {
        for (Map.Entry<String, Faction> entry : factionManager.getAllFactions().entrySet()) {
            String name = entry.getKey().toLowerCase();
            double power = calculateFactionPower(entry.getValue());
            double oldPower = powerCache.getOrDefault(name, 0.0);
            powerCache.put(name, power);

            // Vérifier si le rang a changé
            FactionRank newRank = FactionRank.fromPower(power);
            FactionRank oldRank = rankCache.getOrDefault(name, FactionRank.PIERRE);
            rankCache.put(name, newRank);

            if (newRank.ordinal() > oldRank.ordinal()) {
                // Montée de rang → notifier sur le thread principal
                final Faction faction = entry.getValue();
                final FactionRank rank = newRank;
                Bukkit.getScheduler().runTask(plugin, () -> notifyRankUp(faction, rank));
            }
        }
    }

    private double calculateFactionPower(Faction faction) {
        double total = 0;
        int memberCount = 0;

        for (UUID uuid : faction.getMembers()) {
            if (statsManager != null) {
                PlayerStats stats = statsManager.getStats(uuid);
                if (stats != null) {
                    total += PlayerPowerCalculator.calculate(stats);
                }
            }
            // Même sans FactionStats, on compte les membres
            memberCount++;
        }

        // Bonus de taille
        total += memberCount * MEMBER_BONUS;
        return Math.round(total * 100.0) / 100.0;
    }

    // ─── Accès publics ───────────────────────────────────────────────────────────

    public double getFactionPower(String factionName) {
        return powerCache.getOrDefault(factionName.toLowerCase(), 0.0);
    }

    public FactionRank getFactionRank(String factionName) {
        return rankCache.getOrDefault(factionName.toLowerCase(), FactionRank.PIERRE);
    }

    public double getPlayerPower(UUID uuid) {
        if (statsManager == null) return 0;
        PlayerStats stats = statsManager.getStats(uuid);
        return PlayerPowerCalculator.calculate(stats);
    }

    public PlayerPowerCalculator.PowerBreakdown getPlayerBreakdown(UUID uuid) {
        if (statsManager == null) return new PlayerPowerCalculator.PowerBreakdown(0, 0, 0, 0);
        return PlayerPowerCalculator.breakdown(statsManager.getStats(uuid));
    }

    /** Classement de toutes les factions par puissance décroissante */
    public List<Map.Entry<String, Double>> getLeaderboard() {
        List<Map.Entry<String, Double>> list = new ArrayList<>(powerCache.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    /** Rang (position) d'une faction dans le classement (1-based) */
    public int getFactionPosition(String factionName) {
        List<Map.Entry<String, Double>> lb = getLeaderboard();
        for (int i = 0; i < lb.size(); i++) {
            if (lb.get(i).getKey().equalsIgnoreCase(factionName)) return i + 1;
        }
        return -1;
    }

    // ─── Effets passifs ──────────────────────────────────────────────────────────

    private void applyPassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            if (faction == null) continue;

            FactionRank rank = getFactionRank(faction.getName());
            applyEffectsForRank(player, rank, faction);
        }
    }

    private void applyEffectsForRank(Player player, FactionRank rank, Faction faction) {
        switch (rank) {
            case LEGENDAIRE:
                // Force I + Regen II + Speed I + Jump II
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("STRENGTH"),         EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,     EFFECT_DURATION, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,            EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("JUMP_BOOST"),       EFFECT_DURATION, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),            EFFECT_DURATION, 2, true, false, true));
                // Aura de soin alliés proches
                applyAllyAura(player, faction, EFFECT_DURATION);
                break;

            case EMERAUDE:
                // Force I + Speed I + Haste II
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("STRENGTH"),  EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,     EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),     EFFECT_DURATION, 1, true, false, true));
                break;

            case DIAMANT:
                // Feather Falling passif (résistance chute) + Speed + Haste
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),        EFFECT_DURATION, 1, true, false, true));
                break;

            case OR:
                // Regen I + Haste I
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),        EFFECT_DURATION, 0, true, false, true));
                break;

            case ARGENT:
                // Haste I
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"), EFFECT_DURATION, 0, true, false, true));
                break;

            case BRONZE:
                // Haste 0 léger
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"), EFFECT_DURATION, 0, true, false, true));
                break;

            default:
                break;
        }
    }

    /** Applique Regen I aux alliés dans un rayon de 15 blocs (rang Légendaire) */
    private void applyAllyAura(Player source, Faction faction, int duration) {
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(source.getUniqueId())) continue;
            Player ally = Bukkit.getPlayer(uuid);
            if (ally == null || !ally.isOnline()) continue;
            if (!ally.getWorld().equals(source.getWorld())) continue;
            if (ally.getLocation().distanceSquared(source.getLocation()) <= 225) { // 15^2
                ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, true, false, true));
            }
        }
    }

    // ─── Notification de montée de rang ──────────────────────────────────────────

    private void notifyRankUp(Faction faction, FactionRank rank) {
        String msg = ChatColor.GOLD + "═══════════════════════════════════════\n"
                + ChatColor.YELLOW + ChatColor.BOLD + "  ✦ RANG DE FACTION AUGMENTE ! ✦\n"
                + ChatColor.RESET + ChatColor.GRAY + "  La faction " + ChatColor.WHITE + ChatColor.BOLD + faction.getName() + ChatColor.RESET + ChatColor.GRAY + " a atteint\n"
                + "  le rang " + rank.getLabelBold() + ChatColor.GRAY + " !\n"
                + ChatColor.GREEN + "  Nouveaux avantages debloqués :\n";

        StringBuilder sb = new StringBuilder(msg);
        for (String av : rank.avantages) {
            sb.append(ChatColor.YELLOW).append("  › ").append(ChatColor.WHITE).append(av).append("\n");
        }
        sb.append(ChatColor.GOLD).append("═══════════════════════════════════════");

        for (UUID uuid : faction.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(sb.toString());
                // Titre écran
                member.sendTitle(
                        rank.couleur + "" + ChatColor.BOLD + rank.icone + " " + rank.nom,
                        ChatColor.GRAY + "Votre faction a monte de rang !",
                        10, 80, 20
                );
            }
        }

        // Annonce globale
        Bukkit.broadcastMessage(
                ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Factions" + ChatColor.DARK_GRAY + "] "
                + ChatColor.WHITE + "La faction " + ChatColor.YELLOW + ChatColor.BOLD + faction.getName()
                + ChatColor.RESET + " vient d'atteindre le rang " + rank.getLabel() + ChatColor.WHITE + " !"
        );
    }

    /** Force le recalcul d'une faction spécifique (ex: après un kill ou avancement) */
    public void invalidate(String factionName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) return;
            double power = calculateFactionPower(faction);
            String key = factionName.toLowerCase();
            double oldPower = powerCache.getOrDefault(key, 0.0);
            powerCache.put(key, power);
            FactionRank newRank = FactionRank.fromPower(power);
            FactionRank oldRank = rankCache.getOrDefault(key, FactionRank.PIERRE);
            rankCache.put(key, newRank);
            if (newRank.ordinal() > oldRank.ordinal()) {
                Bukkit.getScheduler().runTask(plugin, () -> notifyRankUp(faction, newRank));
            }
        });
    }
}
