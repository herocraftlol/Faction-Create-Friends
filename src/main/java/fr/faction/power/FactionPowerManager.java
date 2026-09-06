package fr.faction.power;

import fr.faction.alliance.AllianceManager;
import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.models.PlayerStats;
import fr.faction.ranking.FactionRank;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Calcule la puissance des factions, gère les rangs et applique
 * les effets passifs correspondants (Paper 1.21, sans fly ni speed ni jump).
 */
public class FactionPowerManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;
    private AllianceManager allianceManager;
    private FactionTabManager tabManager;

    private final Map<String, Double>      powerCache = new HashMap<>();
    private final Map<String, FactionRank> rankCache  = new HashMap<>();

    private BukkitTask updateTask;
    private BukkitTask effectTask;
    private BukkitTask particleTask;

    private static final double MEMBER_BONUS    = 50.0;
    /** Durée des effets de potion en ticks — re-appliqués toutes les 3s, donc 4s suffit */
    private static final int    EFFECT_DURATION = 80;

    public FactionPowerManager(JavaPlugin plugin, FactionManager factionManager,
                                PlayerStatsManager statsManager) {
        this.plugin         = plugin;
        this.factionManager = factionManager;
        this.statsManager   = statsManager;
    }

    public void setAllianceManager(AllianceManager am) { this.allianceManager = am; }
    public void setTabManager(FactionTabManager tm)     { this.tabManager = tm; }

    // ── Démarrage / Arrêt ────────────────────────────────────────────────────────

    public void start() {
        // Recalcul toutes les 2 minutes
        updateTask   = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::recalculateAll, 40L, 20L * 120);
        // Effets passifs toutes les 3 secondes (60 ticks)
        effectTask   = Bukkit.getScheduler().runTaskTimer(
                plugin, this::applyPassiveEffects, 60L, 60L);
        // Particules LÉGENDAIRE toutes les secondes
        particleTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::applyLegendaryParticles, 20L, 20L);
        // Premier calcul asynchrone après 2 ticks
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::recalculateAll, 40L);
    }

    public void stop() {
        if (updateTask   != null) updateTask.cancel();
        if (effectTask   != null) effectTask.cancel();
        if (particleTask != null) particleTask.cancel();
    }

    // ── Calcul de puissance ───────────────────────────────────────────────────────

    public void recalculateAll() {
        for (Map.Entry<String, Faction> entry : factionManager.getAllFactions().entrySet()) {
            String key    = entry.getKey().toLowerCase();
            double power  = calculateFactionPower(entry.getValue());
            powerCache.put(key, power);

            FactionRank newRank = FactionRank.fromPower(power);
            FactionRank oldRank = rankCache.getOrDefault(key, FactionRank.PIERRE);
            rankCache.put(key, newRank);

            if (newRank.ordinal() > oldRank.ordinal()) {
                final Faction faction = entry.getValue();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    notifyRankUp(faction, newRank);
                    if (tabManager != null) {
                        // Rafraîchir les tabs des membres en ligne
                        for (UUID uuid : faction.getMembers()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) tabManager.refresh(p);
                        }
                    }
                });
            }
        }
    }

    public void invalidate(String factionName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) return;
            String key    = factionName.toLowerCase();
            double power  = calculateFactionPower(faction);
            powerCache.put(key, power);
            FactionRank newRank = FactionRank.fromPower(power);
            FactionRank oldRank = rankCache.getOrDefault(key, FactionRank.PIERRE);
            rankCache.put(key, newRank);
            if (newRank.ordinal() > oldRank.ordinal()) {
                Bukkit.getScheduler().runTask(plugin, () -> notifyRankUp(faction, newRank));
            }
        });
    }

    private double calculateFactionPower(Faction faction) {
        double total = 0;
        for (UUID uuid : faction.getMembers()) {
            PlayerStats stats = statsManager.getStats(uuid);
            total += PlayerPowerCalculator.calculate(stats);
        }
        total += faction.getMemberCount() * MEMBER_BONUS;
        if (allianceManager != null) {
            total += allianceManager.getAlliancePowerBonus(faction.getName());
        }
        return Math.round(total * 100.0) / 100.0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────────

    public double    getFactionPower(String factionName)  { return powerCache.getOrDefault(factionName.toLowerCase(), 0.0); }
    public FactionRank getFactionRank(String factionName) { return rankCache.getOrDefault(factionName.toLowerCase(), FactionRank.PIERRE); }
    public double    getPlayerPower(UUID uuid)            { return PlayerPowerCalculator.calculate(statsManager.getStats(uuid)); }
    public PlayerPowerCalculator.PowerBreakdown getPlayerBreakdown(UUID uuid) { return PlayerPowerCalculator.breakdown(statsManager.getStats(uuid)); }

    public List<Map.Entry<String, Double>> getLeaderboard() {
        List<Map.Entry<String, Double>> list = new ArrayList<>(powerCache.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    public int getFactionPosition(String factionName) {
        List<Map.Entry<String, Double>> lb = getLeaderboard();
        for (int i = 0; i < lb.size(); i++)
            if (lb.get(i).getKey().equalsIgnoreCase(factionName)) return i + 1;
        return -1;
    }

    // ── Effets passifs ────────────────────────────────────────────────────────────
    /**
     * Effets par rang (cumulatifs vers le haut) — Paper 1.21 :
     *
     * BRONZE     → Hâte I
     * ARGENT     → Hâte II
     * OR         → Hâte II + Regen I
     * DIAMANT    → Hâte II + Regen I + Slow Falling + Résistance I
     * ÉMERAUDE   → Hâte III + Regen II + Force I + Résistance II
     * LÉGENDAIRE → Hâte III + Regen II + Force I + Résistance II + aura alliés
     *
     * Pas de Speed, pas de Jump Boost, pas de Fly.
     * PotionEffectType utilise les noms 1.21.
     */
    /**
     * Effets bannis — retirés à chaque cycle pour purger les joueurs
     * qui auraient encore des effets d'anciennes versions du plugin.
     */
    private static final java.util.List<PotionEffectType> BANNED_EFFECTS = java.util.List.of(
            PotionEffectType.SPEED,
            PotionEffectType.JUMP_BOOST
    );

    private void applyPassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Retirer systématiquement les effets bannis avant tout
            for (PotionEffectType banned : BANNED_EFFECTS) {
                player.removePotionEffect(banned);
            }
            // Retirer SLOW_FALLING pour le ré-appliquer proprement si rang Diamant+
            // (évite les conflits avec d'éventuels amplifiers résiduels)
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);

            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            if (faction == null) continue;
            FactionRank rank = getFactionRank(faction.getName());
            applyEffectsForRank(player, rank, faction);
        }
    }

    private void applyEffectsForRank(Player player, FactionRank rank, Faction faction) {
        final boolean ambient = true, particles = false;

        switch (rank) {
            case LEGENDAIRE -> {
                effect(player, PotionEffectType.HASTE,             2, ambient, particles);
                effect(player, PotionEffectType.REGENERATION,      1, ambient, particles);
                effect(player, PotionEffectType.STRENGTH,   0, ambient, particles);
                effect(player, PotionEffectType.RESISTANCE, 1, ambient, particles);
                applyAllyAura(player, faction);
            }
            case EMERAUDE -> {
                effect(player, PotionEffectType.HASTE,             2, ambient, particles);
                effect(player, PotionEffectType.REGENERATION,      1, ambient, particles);
                effect(player, PotionEffectType.STRENGTH,   0, ambient, particles);
                effect(player, PotionEffectType.RESISTANCE, 1, ambient, particles);
            }
            case DIAMANT -> {
                effect(player, PotionEffectType.HASTE,             1, ambient, particles);
                effect(player, PotionEffectType.REGENERATION,      0, ambient, particles);
                effect(player, PotionEffectType.SLOW_FALLING,      0, ambient, particles);
                effect(player, PotionEffectType.RESISTANCE, 0, ambient, particles);
            }
            case OR -> {
                effect(player, PotionEffectType.HASTE,        1, ambient, particles);
                effect(player, PotionEffectType.REGENERATION, 0, ambient, particles);
            }
            case ARGENT -> effect(player, PotionEffectType.HASTE, 1, ambient, particles);
            case BRONZE  -> effect(player, PotionEffectType.HASTE, 0, ambient, particles);
            case PIERRE  -> { /* rien */ }
        }
    }

    private void effect(Player p, PotionEffectType type, int amplifier,
                         boolean ambient, boolean particles) {
        p.addPotionEffect(new PotionEffect(type, EFFECT_DURATION, amplifier,
                ambient, particles, true));
    }

    /** Regen II pour les membres alliés proches (≤15 blocs) */
    private void applyAllyAura(Player source, Faction faction) {
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(source.getUniqueId())) continue;
            Player ally = Bukkit.getPlayer(uuid);
            if (ally == null || !ally.getWorld().equals(source.getWorld())) continue;
            if (ally.getLocation().distanceSquared(source.getLocation()) <= 225) {
                effect(ally, PotionEffectType.REGENERATION, 1, true, false);
            }
        }
    }

    // ── Particules LÉGENDAIRE ────────────────────────────────────────────────────

    private void applyLegendaryParticles(  ) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            if (faction == null) continue;
            if (getFactionRank(faction.getName()) != FactionRank.LEGENDAIRE) continue;

            // Halo doré tournant autour du joueur
            double angle = (System.currentTimeMillis() % 4000) / 4000.0 * 2 * Math.PI;
            for (int i = 0; i < 6; i++) {
                double a = angle + i * Math.PI / 3;
                double px = player.getLocation().getX() + 0.9 * Math.cos(a);
                double pz = player.getLocation().getZ() + 0.9 * Math.sin(a);
                double py = player.getLocation().getY() + 1.1;
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        new Location(player.getWorld(), px, py, pz),
                        1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(0xFF, 0xD7, 0x00), 1.2f)
                );
            }
        }
    }

    // ── Notification de montée en rang ───────────────────────────────────────────

    private void notifyRankUp(Faction faction, FactionRank rank) {
        String sep = ChatColor.GOLD + "══════════════════════════════════════";
        StringBuilder msg = new StringBuilder();
        msg.append(sep).append("\n");
        msg.append(ChatColor.YELLOW).append(ChatColor.BOLD)
           .append("  ✦ RANG DE FACTION AUGMENTÉ ! ✦\n").append(ChatColor.RESET);
        msg.append(ChatColor.GRAY).append("  La faction ").append(ChatColor.WHITE)
           .append(ChatColor.BOLD).append(faction.getName()).append(ChatColor.RESET)
           .append(ChatColor.GRAY).append(" a atteint\n  le rang ")
           .append(rank.getLabelBold()).append(ChatColor.GRAY).append(" !\n");
        msg.append(ChatColor.GREEN).append("  Nouveaux avantages :\n");
        for (String av : rank.avantages)
            msg.append(ChatColor.YELLOW).append("  › ").append(ChatColor.WHITE).append(av).append("\n");
        msg.append(sep);

        for (UUID uuid : faction.getMembers()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) {
                m.sendMessage(msg.toString());
                m.sendTitle(
                    rank.couleur + "" + ChatColor.BOLD + rank.icone + " " + rank.nom,
                    ChatColor.GRAY + "Votre faction a monté de rang !",
                    10, 80, 20
                );
            }
        }
        Bukkit.broadcastMessage(
            ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Factions" + ChatColor.DARK_GRAY + "] "
            + ChatColor.WHITE + "La faction " + ChatColor.YELLOW + ChatColor.BOLD
            + faction.getName() + ChatColor.RESET + " vient d'atteindre le rang "
            + rank.getLabel() + ChatColor.WHITE + " !"
        );
    }
}
