package fr.faction.web;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.power.FactionPowerManager;
import fr.faction.ranking.FactionRank;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Écrit dans la table "faction_tab_sync" — lue par le plugin proxy HeroTab (sa
 * classe FactionSync) pour afficher la faction et le rang de chaque joueur dans
 * le tab, réseau entier, peu importe le sous-serveur sur lequel il se trouve.
 *
 * Cette classe n'existait tout simplement pas avant : HeroTab attendait une
 * table que rien n'écrivait jamais, quel que soit le nombre de correctifs faits
 * sur WebMapSync (qui alimente un système totalement différent — la carte du
 * site — et n'a jamais eu de rapport avec le tab HeroTab).
 *
 * Réutilise la même configuration mysql: que WebLinkManager (section déjà
 * présente pour /lier) :
 *   mysql:
 *     host: 127.0.0.1
 *     port: 3306
 *     database: herocraft
 *     user: herocraft_user
 *     password: mot-de-passe
 *
 * La table est créée automatiquement si elle n'existe pas, avec exactement les
 * colonnes attendues par HeroTab (uuid, faction_name, rank_name, rank_color,
 * rank_icon). rank_color est stocké au format "&x" (legacy), pas "§x", pour
 * correspondre à ce que HeroTab attend de parser côté proxy.
 */
public class FactionTabSync {

    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;
    private final Logger log;
    private Connection connection;
    private boolean enabled = false;

    public FactionTabSync(JavaPlugin plugin, FactionManager factionManager, FactionPowerManager powerManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.log = plugin.getLogger();
        connect();
    }

    // ── Connexion MySQL ───────────────────────────────────────────────────────────

    private void connect() {
        String host = plugin.getConfig().getString("mysql.host", "");
        if (host.isBlank()) {
            log.warning("[FactionTabSync] Section 'mysql' absente du config.yml — synchro tab HeroTab désactivée.");
            return;
        }

        String port     = plugin.getConfig().getString("mysql.port", "3306");
        String database = plugin.getConfig().getString("mysql.database", "herocraft");
        String user     = plugin.getConfig().getString("mysql.user",     "root");
        String password = plugin.getConfig().getString("mysql.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
        try {
            try { Class.forName("com.mysql.cj.jdbc.Driver"); }
            catch (ClassNotFoundException e1) {
                try { Class.forName("com.mysql.jdbc.Driver"); }
                catch (ClassNotFoundException e2) {
                    log.severe("[FactionTabSync] Driver MySQL introuvable. Assure-toi que mysql-connector-j"
                            + " est dans le classpath ou que Paper 1.21 l'inclut.");
                    return;
                }
            }
            connection = DriverManager.getConnection(url, user, password);
            ensureTable();
            enabled = true;
            log.info("[FactionTabSync] Connecté à MySQL (" + host + ":" + port + "/" + database + "). Synchro tab HeroTab activée.");
        } catch (Exception e) {
            log.severe("[FactionTabSync] Impossible de se connecter à MySQL : " + e.getMessage());
            log.severe("[FactionTabSync] Vérifie mysql.host/user/password/database dans config.yml.");
        }
    }

    private void ensureTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS faction_tab_sync (
                    uuid         VARCHAR(36) NOT NULL PRIMARY KEY,
                    faction_name VARCHAR(64),
                    rank_name    VARCHAR(32),
                    rank_color   VARCHAR(8),
                    rank_icon    VARCHAR(8)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /**
     * Réécrit entièrement la table à partir de l'état actuel de toutes les factions.
     * Appelé périodiquement, et immédiatement après tout événement qui doit se
     * refléter sans délai dans le tab (recrutement, départ, renommage, dissolution,
     * montée de rang...).
     */
    public void pushNow() {
        if (!enabled) return;
        try {
            ensureConnected();
            connection.setAutoCommit(false);
            try {
                try (Statement st = connection.createStatement()) {
                    st.execute("DELETE FROM faction_tab_sync");
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO faction_tab_sync (uuid, faction_name, rank_name, rank_color, rank_icon) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    for (Faction faction : factionManager.getAllFactions().values()) {
                        FactionRank rank = powerManager.getFactionRank(faction.getName());
                        String colorCode = "&" + rank.couleur.getChar();
                        for (UUID uuid : faction.getMembers()) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, faction.getName());
                            ps.setString(3, rank.nom);
                            ps.setString(4, colorCode);
                            ps.setString(5, rank.icone);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            log.warning("[FactionTabSync] Erreur pushNow : " + e.getMessage());
        }
    }

    /** Ferme la connexion proprement. */
    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (Exception ignored) {}
    }

    // ── Helpers privés ────────────────────────────────────────────────────────────

    private void ensureConnected() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = null; enabled = false;
            connect();
            if (!enabled) throw new SQLException("Connexion MySQL perdue.");
        }
    }
}
