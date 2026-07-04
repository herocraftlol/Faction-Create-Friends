package fr.faction.claim;

import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les chunks claimés par les factions.
 * Coût : base_cost + (n_claims_faction * cost_increment) émeraudes par claim.
 * Permissions par chunk : liste de UUID autorisés à interagir.
 *
 * Règle de proximité : on ne peut pas claimer à moins de MIN_CLAIM_DISTANCE chunks
 * d'un claim adverse, sauf si cette faction adverse a explicitement autorisé la faction
 * qui tente de claimer (via /faction claimallow <faction>).
 */
public class ClaimManager {

    /** Distance minimale (en chunks) entre claims de factions différentes. */
    public static final int MIN_CLAIM_DISTANCE = 6;

    // ── Clé interne d'un chunk ──────────────────────────────────────────────
    public record ChunkKey(String world, int cx, int cz) {
        public static ChunkKey of(Chunk c) { return new ChunkKey(c.getWorld().getName(), c.getX(), c.getZ()); }
        public String serialize() { return world + ":" + cx + ":" + cz; }
        public static ChunkKey deserialize(String s) {
            String[] p = s.split(":");
            return new ChunkKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        }
    }

    // ── Données d'un claim ──────────────────────────────────────────────────
    public static class ClaimData {
        private final String factionName;
        private final Set<UUID> allowedPlayers = new HashSet<>();

        public ClaimData(String factionName) { this.factionName = factionName; }

        public String getFactionName()        { return factionName; }
        public Set<UUID> getAllowedPlayers()  { return allowedPlayers; }

        public void allow(UUID uuid)          { allowedPlayers.add(uuid); }
        public void deny(UUID uuid)           { allowedPlayers.remove(uuid); }
        public boolean isAllowed(UUID uuid)   { return allowedPlayers.contains(uuid); }
    }

    // ── Stockage ─────────────────────────────────────────────────────────────
    private final JavaPlugin plugin;
    /** chunk → données du claim */
    private final Map<ChunkKey, ClaimData> claims = new HashMap<>();
    /** faction → nombre de claims */
    private final Map<String, Integer> claimCounts = new HashMap<>();
    /**
     * Autorisations de proximité inter-factions.
     * claimAlliances.get("factionA") = ensemble des factions que A AUTORISE à claimer près d'elle.
     * Autrement dit : si "factionB" est dans la liste de "factionA", alors factionB peut claimer
     * à moins de MIN_CLAIM_DISTANCE chunks des claims de factionA.
     */
    private final Map<String, Set<String>> claimAlliances = new HashMap<>();

    private File dataFile;

    public ClaimManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "claims.yml");
        load();
    }

    // ── Coût ─────────────────────────────────────────────────────────────────

    /** Retourne le coût en émeraudes pour le prochain claim d'une faction. */
    public int getNextClaimCost(String factionName) {
        int base = plugin.getConfig().getInt("claims.base-cost", 5);
        int incr = plugin.getConfig().getInt("claims.cost-increment", 3);
        int count = claimCounts.getOrDefault(factionName.toLowerCase(), 0);
        return base + (count * incr);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean isClaimed(Chunk chunk) {
        return claims.containsKey(ChunkKey.of(chunk));
    }

    public ClaimData getClaim(Chunk chunk) {
        return claims.get(ChunkKey.of(chunk));
    }

    /** Retourne tous les claims (lecture seule). */
    public Map<ChunkKey, ClaimData> getAllClaims() {
        return Collections.unmodifiableMap(claims);
    }

    /** Retourne tous les ChunkKey claimés par une faction. */
    public List<ChunkKey> getFactionClaims(String factionName) {
        List<ChunkKey> list = new ArrayList<>();
        for (Map.Entry<ChunkKey, ClaimData> e : claims.entrySet())
            if (e.getValue().getFactionName().equalsIgnoreCase(factionName)) list.add(e.getKey());
        return list;
    }

    public int getClaimCount(String factionName) {
        return claimCounts.getOrDefault(factionName.toLowerCase(), 0);
    }

    /**
     * Vérifie si un chunk peut être claimé par factionName en respectant la
     * règle de distance minimale (MIN_CLAIM_DISTANCE chunks) avec les claims des
     * autres factions.
     *
     * @return null si le claim est autorisé, sinon le nom de la faction trop proche
     *         qui bloque le claim (pour afficher un message d'erreur).
     */
    public String checkProximityViolation(String factionName, Chunk chunk) {
        ChunkKey target = ChunkKey.of(chunk);
        for (Map.Entry<ChunkKey, ClaimData> e : claims.entrySet()) {
            ClaimData data = e.getValue();
            // Ignorer les claims de la même faction et les claims d'autres mondes
            if (data.getFactionName().equalsIgnoreCase(factionName)) continue;
            ChunkKey existing = e.getKey();
            if (!existing.world().equals(target.world())) continue;

            int dx = Math.abs(existing.cx() - target.cx());
            int dz = Math.abs(existing.cz() - target.cz());

            if (dx < MIN_CLAIM_DISTANCE && dz < MIN_CLAIM_DISTANCE) {
                // Il y a un claim adverse trop proche — vérifier si la faction adverse autorise
                String ownerFaction = data.getFactionName();
                Set<String> allowedByOwner = claimAlliances.getOrDefault(ownerFaction.toLowerCase(), Collections.emptySet());
                if (!allowedByOwner.contains(factionName.toLowerCase())) {
                    return ownerFaction; // Bloqué par cette faction
                }
            }
        }
        return null; // Aucun blocage
    }

    /**
     * Enregistre un nouveau claim. Ne vérifie pas le paiement (géré en amont).
     * Ne vérifie pas la proximité (géré en amont dans la commande).
     * @return false si le chunk est déjà claimé
     */
    public boolean addClaim(String factionName, Chunk chunk) {
        ChunkKey key = ChunkKey.of(chunk);
        if (claims.containsKey(key)) return false;
        claims.put(key, new ClaimData(factionName.toLowerCase()));
        claimCounts.merge(factionName.toLowerCase(), 1, Integer::sum);
        save();
        return true;
    }

    /**
     * Supprime un claim (unclaim).
     * @return false si le chunk n'était pas claimé par cette faction
     */
    public boolean removeClaim(String factionName, Chunk chunk) {
        ChunkKey key = ChunkKey.of(chunk);
        ClaimData data = claims.get(key);
        if (data == null || !data.getFactionName().equalsIgnoreCase(factionName)) return false;
        claims.remove(key);
        claimCounts.merge(factionName.toLowerCase(), -1, (a, b) -> Math.max(0, a + b));
        save();
        return true;
    }

    /** Supprime tous les claims d'une faction (dissolution). */
    public void removeAllClaims(String factionName) {
        claims.entrySet().removeIf(e -> e.getValue().getFactionName().equalsIgnoreCase(factionName));
        claimCounts.remove(factionName.toLowerCase());
        // Nettoyer aussi les alliances
        claimAlliances.remove(factionName.toLowerCase());
        for (Set<String> allies : claimAlliances.values()) allies.remove(factionName.toLowerCase());
        save();
    }

    // ── Permissions par chunk ─────────────────────────────────────────────────

    public void allowPlayer(Chunk chunk, UUID uuid) {
        ClaimData d = claims.get(ChunkKey.of(chunk));
        if (d != null) { d.allow(uuid); save(); }
    }

    public void denyPlayer(Chunk chunk, UUID uuid) {
        ClaimData d = claims.get(ChunkKey.of(chunk));
        if (d != null) { d.deny(uuid); save(); }
    }

    /**
     * Vérifie si un joueur peut interagir dans un chunk claimé.
     * Membres de la faction propriétaire : toujours autorisés.
     * Autres : uniquement si dans allowedPlayers.
     */
    public boolean canInteract(UUID playerUuid, String playerFaction, Chunk chunk) {
        ClaimData data = claims.get(ChunkKey.of(chunk));
        if (data == null) return true;                   // non claimé
        if (data.getFactionName().equalsIgnoreCase(playerFaction)) return true; // propriétaire
        return data.isAllowed(playerUuid);               // invité explicitement
    }

    // ── Alliances de claim (autorisation de proximité) ────────────────────────

    /**
     * La faction {@code ownerFaction} autorise {@code allowedFaction} à claimer
     * à moins de MIN_CLAIM_DISTANCE chunks de ses propres claims.
     */
    public void allowClaimProximity(String ownerFaction, String allowedFaction) {
        claimAlliances
                .computeIfAbsent(ownerFaction.toLowerCase(), k -> new HashSet<>())
                .add(allowedFaction.toLowerCase());
        save();
    }

    /**
     * La faction {@code ownerFaction} révoque l'autorisation de proximité pour {@code allowedFaction}.
     */
    public void revokeClaimProximity(String ownerFaction, String allowedFaction) {
        Set<String> set = claimAlliances.get(ownerFaction.toLowerCase());
        if (set != null) {
            set.remove(allowedFaction.toLowerCase());
            if (set.isEmpty()) claimAlliances.remove(ownerFaction.toLowerCase());
        }
        save();
    }

    /** Retourne true si ownerFaction a autorisé allowedFaction à claimer près d'elle. */
    public boolean hasClaimProximityAlliance(String ownerFaction, String allowedFaction) {
        return claimAlliances
                .getOrDefault(ownerFaction.toLowerCase(), Collections.emptySet())
                .contains(allowedFaction.toLowerCase());
    }

    /** Retourne la liste des factions autorisées à claimer près de ownerFaction. */
    public Set<String> getClaimAllies(String ownerFaction) {
        return Collections.unmodifiableSet(
                claimAlliances.getOrDefault(ownerFaction.toLowerCase(), Collections.emptySet()));
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<ChunkKey, ClaimData> e : claims.entrySet()) {
            String k = e.getKey().serialize().replace(":", "_");
            cfg.set("claims." + k + ".faction", e.getValue().getFactionName());
            cfg.set("claims." + k + ".chunk",   e.getKey().serialize());
            List<String> allowed = new ArrayList<>();
            for (UUID u : e.getValue().getAllowedPlayers()) allowed.add(u.toString());
            cfg.set("claims." + k + ".allowed", allowed);
        }
        // Sauvegarder les alliances de proximité
        for (Map.Entry<String, Set<String>> e : claimAlliances.entrySet()) {
            cfg.set("alliances." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        try { cfg.save(dataFile); }
        catch (IOException ex) { plugin.getLogger().severe("Erreur sauvegarde claims : " + ex.getMessage()); }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (cfg.contains("claims")) {
            for (String k : Objects.requireNonNull(cfg.getConfigurationSection("claims")).getKeys(false)) {
                String faction  = cfg.getString("claims." + k + ".faction");
                String chunkStr = cfg.getString("claims." + k + ".chunk");
                if (faction == null || chunkStr == null) continue;
                ChunkKey key  = ChunkKey.deserialize(chunkStr);
                ClaimData data = new ClaimData(faction);
                for (String uuidStr : cfg.getStringList("claims." + k + ".allowed"))
                    data.allow(UUID.fromString(uuidStr));
                claims.put(key, data);
                claimCounts.merge(faction, 1, Integer::sum);
            }
        }
        // Charger les alliances de proximité
        if (cfg.contains("alliances")) {
            for (String owner : Objects.requireNonNull(cfg.getConfigurationSection("alliances")).getKeys(false)) {
                List<String> allies = cfg.getStringList("alliances." + owner);
                if (!allies.isEmpty()) {
                    claimAlliances.put(owner, new HashSet<>(allies));
                }
            }
        }
        plugin.getLogger().info(claims.size() + " claim(s) chargé(s).");
    }
}
