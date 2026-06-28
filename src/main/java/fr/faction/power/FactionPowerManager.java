package fr.faction.power;

import fr.faction.managers.FactionManager;
import fr.faction.managers.PlayerStatsManager;
import fr.faction.models.Faction;
import fr.faction.models.PlayerStats;
import fr.faction.ranking.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FactionPowerManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final PlayerStatsManager statsManager;

    private final Map<String, Double>       powerCache = new HashMap<>();
    private final Map<String, FactionRank>  rankCache  = new HashMap<>();

    private BukkitTask updateTask;
    private BukkitTask effectTask;

    private static final double MEMBER_BONUS   = 50.0;
    private static final int    EFFECT_DURATION = 60;

    public FactionPowerManager(JavaPlugin plugin, FactionManager factionManager, PlayerStatsManager statsManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.statsManager = statsManager;
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::recalculateAll, 20L, 20L * 120);
        effectTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyPassiveEffects, 60L, 60L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::recalculateAll, 40L);
    }

    public void stop() {
        if (updateTask != null) updateTask.cancel();
        if (effectTask != null) effectTask.cancel();
    }

    public void recalculateAll() {
        for (Map.Entry<String, Faction> entry : factionManager.getAllFactions().entrySet()) {
            String name = entry.getKey().toLowerCase();
            double power = calculateFactionPower(entry.getValue());
            powerCache.put(name, power);
            FactionRank newRank = FactionRank.fromPower(power);
            FactionRank oldRank = rankCache.getOrDefault(name, FactionRank.PIERRE);
            rankCache.put(name, newRank);
            if (newRank.ordinal() > oldRank.ordinal()) {
                final Faction faction = entry.getValue();
                Bukkit.getScheduler().runTask(plugin, () -> notifyRankUp(faction, newRank));
            }
        }
    }

    private double calculateFactionPower(Faction faction) {
        double total = 0;
        for (UUID uuid : faction.getMembers()) {
            PlayerStats stats = statsManager.getStats(uuid);
            total += PlayerPowerCalculator.calculate(stats);
        }
        total += faction.getMemberCount() * MEMBER_BONUS;
        return Math.round(total * 100.0) / 100.0;
    }

    public double getFactionPower(String factionName) {
        return powerCache.getOrDefault(factionName.toLowerCase(), 0.0);
    }

    public FactionRank getFactionRank(String factionName) {
        return rankCache.getOrDefault(factionName.toLowerCase(), FactionRank.PIERRE);
    }

    public double getPlayerPower(UUID uuid) {
        return PlayerPowerCalculator.calculate(statsManager.getStats(uuid));
    }

    public PlayerPowerCalculator.PowerBreakdown getPlayerBreakdown(UUID uuid) {
        return PlayerPowerCalculator.breakdown(statsManager.getStats(uuid));
    }

    public List<Map.Entry<String, Double>> getLeaderboard() {
        List<Map.Entry<String, Double>> list = new ArrayList<>(powerCache.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    public int getFactionPosition(String factionName) {
        List<Map.Entry<String, Double>> lb = getLeaderboard();
        for (int i = 0; i < lb.size(); i++) {
            if (lb.get(i).getKey().equalsIgnoreCase(factionName)) return i + 1;
        }
        return -1;
    }

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
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("STRENGTH"),     EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("JUMP_BOOST"),   EFFECT_DURATION, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),        EFFECT_DURATION, 2, true, false, true));
                applyAllyAura(player, faction, EFFECT_DURATION);
                break;
            case EMERAUDE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("STRENGTH"),  EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,     EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),     EFFECT_DURATION, 1, true, false, true));
                break;
            case DIAMANT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),        EFFECT_DURATION, 1, true, false, true));
                break;
            case OR:
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"),        EFFECT_DURATION, 0, true, false, true));
                break;
            case ARGENT:
            case BRONZE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HASTE"), EFFECT_DURATION, 0, true, false, true));
                break;
            default:
                break;
        }
    }

    private void applyAllyAura(Player source, Faction faction, int duration) {
        for (UUID uuid : faction.getMembers()) {
            if (uuid.equals(source.getUniqueId())) continue;
            Player ally = Bukkit.getPlayer(uuid);
            if (ally == null || !ally.isOnline()) continue;
            if (!ally.getWorld().equals(source.getWorld())) continue;
            if (ally.getLocation().distanceSquared(source.getLocation()) <= 225) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, true, false, true));
            }
        }
    }

    private void notifyRankUp(Faction faction, FactionRank rank) {
        String msg = ChatColor.GOLD + "═══════════════════════════════════════\n"
                + ChatColor.YELLOW + ChatColor.BOLD + "  ✦ RANG DE FACTION AUGMENTÉ ! ✦\n"
                + ChatColor.RESET + ChatColor.GRAY + "  La faction " + ChatColor.WHITE + ChatColor.BOLD
                + faction.getName() + ChatColor.RESET + ChatColor.GRAY + " a atteint\n"
                + "  le rang " + rank.getLabelBold() + ChatColor.GRAY + " !\n"
                + ChatColor.GREEN + "  Nouveaux avantages débloqués :\n";
        StringBuilder sb = new StringBuilder(msg);
        for (String av : rank.avantages) sb.append(ChatColor.YELLOW).append("  › ").append(ChatColor.WHITE).append(av).append("\n");
        sb.append(ChatColor.GOLD).append("═══════════════════════════════════════");
        for (UUID uuid : faction.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(sb.toString());
                member.sendTitle(rank.couleur + "" + ChatColor.BOLD + rank.icone + " " + rank.nom,
                        ChatColor.GRAY + "Votre faction a monté de rang !", 10, 80, 20);
            }
        }
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Factions" + ChatColor.DARK_GRAY + "] "
                + ChatColor.WHITE + "La faction " + ChatColor.YELLOW + ChatColor.BOLD + faction.getName()
                + ChatColor.RESET + " vient d'atteindre le rang " + rank.getLabel() + ChatColor.WHITE + " !");
    }

    public void invalidate(String factionName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction faction = factionManager.getFaction(factionName);
            if (faction == null) return;
            double power = calculateFactionPower(faction);
            String key = factionName.toLowerCase();
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
