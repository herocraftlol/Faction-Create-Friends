package fr.faction.map;

import fr.faction.alliance.HomeManager;
import fr.faction.managers.FactionManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les mini-cartes de faction.
 *
 * Fonctionnalités :
 *  - Création d'une carte /fac map → item MapView avec FactionMapRenderer
 *  - Un seul MapView par joueur (réutilisé si déjà créé)
 *  - /fac map partage → active/désactive le partage de position aux alliés
 *  - Persistance dans map_data.yml (IDs de MapView + préférences de partage)
 *  - Refresh automatique toutes les 2 s via BukkitTask
 */
public class FactionMapManager {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private HomeManager homeManager; // injecté après construction

    // UUID joueur → MapView ID (int, persisté pour survie aux redémarrages)
    private final Map<UUID, Integer> playerMapIds = new HashMap<>();

    // UUID joueur → partage de position actif (pour les alliés)
    private final Set<UUID> sharingPosition = new HashSet<>();

    // Cache des renderers actifs (UUID → renderer)
    private final Map<UUID, FactionMapRenderer> renderers = new HashMap<>();

    private File dataFile;

    public FactionMapManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin         = plugin;
        this.factionManager = factionManager;
        this.dataFile       = new File(plugin.getDataFolder(), "map_data.yml");
        load();
    }

    public void setHomeManager(HomeManager hm) { this.homeManager = hm; }

    // ── Création / récupération de la carte ──────────────────────────────────────

    /**
     * Donne au joueur sa mini-map de faction.
     * Si une carte existe déjà pour ce joueur, recrée le renderer
     * (utile après un redémarrage).
     */
    public ItemStack getOrCreateMap(Player player) {
        World world = player.getWorld();
        Integer existingId = playerMapIds.get(player.getUniqueId());

        MapView view;
        if (existingId != null) {
            // Récupérer la MapView existante
            @SuppressWarnings("deprecation")
            MapView existing = Bukkit.getMap(existingId);
            view = existing;
        }

        if (existingId == null || Bukkit.getMap(existingId) == null) {
            // Créer une nouvelle MapView
            view = Bukkit.createMap(world);
            view.setScale(MapView.Scale.FARTHEST); // échelle max (inutilisée — on override)
            view.setTrackingPosition(false);       // ne pas bouger le centre
            view.setUnlimitedTracking(false);
            view.setCenterX(0);
            view.setCenterZ(0);
            playerMapIds.put(player.getUniqueId(), view.getId());
            save();
        } else {
            @SuppressWarnings("deprecation")
            MapView v = Bukkit.getMap(existingId);
            view = v;
        }

        if (view == null) {
            // Fallback si la MapView a été supprimée du serveur
            view = Bukkit.createMap(world);
            view.setTrackingPosition(false);
            view.setCenterX(0);
            view.setCenterZ(0);
            playerMapIds.put(player.getUniqueId(), view.getId());
            save();
        }

        // Retirer les renderers par défaut (fond vanilla gris)
        final MapView finalView = view;
        finalView.getRenderers().forEach(r -> finalView.removeRenderer(r));

        // Créer et attacher notre renderer
        FactionMapRenderer renderer = new FactionMapRenderer(
                factionManager, this, player.getUniqueId(), world.getName());
        renderers.put(player.getUniqueId(), renderer);
        view.addRenderer(renderer);

        // Construire l'item carte
        @SuppressWarnings("deprecation")
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(view);
            meta.setDisplayName(ChatColor.GOLD + "⬡ " + ChatColor.YELLOW + "Mini-Map de Faction");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Zone : §e-10 000 §7à §e+10 000 §7(X/Z)",
                    ChatColor.GREEN + "● " + ChatColor.WHITE + "Toi",
                    ChatColor.GREEN + "● " + ChatColor.WHITE + "Membres de ta faction",
                    ChatColor.YELLOW + "● " + ChatColor.WHITE + "Alliés (si partage activé)",
                    ChatColor.RED + "✚ " + ChatColor.WHITE + "Spawn faction",
                    ChatColor.AQUA + "✚ " + ChatColor.WHITE + "Tes homes",
                    "",
                    ChatColor.GRAY + "/fac map partage → partager ta position aux alliés"
            ));
            mapItem.setItemMeta(meta);
        }

        return mapItem;
    }

    // ── Partage de position ───────────────────────────────────────────────────────

    public boolean isSharingPosition(UUID uuid) {
        return sharingPosition.contains(uuid);
    }

    /** Toggle le partage. Retourne true si maintenant actif, false sinon. */
    public boolean toggleSharing(Player player) {
        UUID uuid = player.getUniqueId();
        if (sharingPosition.contains(uuid)) {
            sharingPosition.remove(uuid);
            save();
            return false;
        } else {
            sharingPosition.add(uuid);
            save();
            return true;
        }
    }

    // ── Homes pour la carte ───────────────────────────────────────────────────────

    public List<Location> getPlayerHomes(UUID uuid, String worldName) {
        if (homeManager == null) return Collections.emptyList();
        List<Location> result = new ArrayList<>();
        for (HomeManager.NamedHome h : homeManager.getHomes(uuid)) {
            if (worldName.equals(h.location.getWorld().getName())) {
                result.add(h.location);
            }
        }
        return result;
    }

    // ── Invalidation du fond ──────────────────────────────────────────────────────

    /** Invalide le cache de fond de tous les renderers actifs */
    public void invalidateAllBackgrounds() {
        renderers.values().forEach(FactionMapRenderer::invalidateBackground);
    }

    // ── Persistance ───────────────────────────────────────────────────────────────

    public void save() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        playerMapIds.forEach((uuid, id) -> cfg.set("maps." + uuid, id));
        cfg.set("sharing", new ArrayList<>(sharingPosition.stream().map(UUID::toString).toList()));
        try { cfg.save(dataFile); } catch (IOException e) {
            plugin.getLogger().warning("Erreur sauvegarde map_data.yml : " + e.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        if (cfg.contains("maps")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("maps")).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int id    = cfg.getInt("maps." + key);
                    playerMapIds.put(uuid, id);
                } catch (Exception ignored) {}
            }
        }
        for (String s : cfg.getStringList("sharing")) {
            try { sharingPosition.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
        plugin.getLogger().info("FactionMap : " + playerMapIds.size() + " carte(s) chargée(s), "
                + sharingPosition.size() + " partage(s) actif(s).");
    }
}
