package fr.faction.web;

import fr.faction.ranking.FactionRank;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Ecrit faction + rang de chaque joueur dans une table MySQL partagee
 * ("faction_tab_sync") afin que le proxy Velocity (plugin HeroTab) puisse
 * les afficher dans le tab-list reseau, quel que soit le sous-serveur sur
 * lequel se trouve le joueur.
 *
 * Reutilise la meme section "mysql:" du config.yml que WebLinkManager
 * (memes identifiants, meme base "herocraft" par defaut) : une seule
 * configuration MySQL a renseigner pour tout le plugin.
 *
 * Ecriture uniquement — ce plugin ne lit jamais cette table, il ne fait
 * que la tenir a jour a chaque changement de faction/rang/connexion.
 */
public class FactionTabSync {

    private final JavaPlugin plugin;
    private final Logger log;
    private Connection connection;
    private boolean enabled = false;

    public FactionTabSync(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        connect();
    }

    private void connect() {
        String host = plugin.getConfig().getString("mysql.host", "");
        if (host == null || host.isBlank()) {
            log.warning("[FactionTabSync] Section 'mysql' absente du config.yml — synchronisation du tab reseau desactivee.");
            return;
        }

        String port = plugin.getConfig().getString("mysql.port", "3306");
        String database = plugin.getConfig().getString("mysql.database", "herocraft");
        String user = plugin.getConfig().getString("mysql.user", "root");
        String password = plugin.getConfig().getString("mysql.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";

        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e1) {
                Class.forName("com.mysql.jdbc.Driver");
            }
            connection = DriverManager.getConnection(url, user, password);
            ensureTable();
            enabled = true;
            log.info("[FactionTabSync] Connecte a MySQL (" + host + ":" + port + "/" + database + "). Sync tab reseau active.");
        } catch (Exception e) {
            log.severe("[FactionTabSync] Impossible de se connecter a MySQL : " + e.getMessage());
        }
    }

    private void ensureTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS faction_tab_sync (
                    uuid         VARCHAR(36)  NOT NULL PRIMARY KEY,
                    faction_name VARCHAR(64)  DEFAULT NULL,
                    rank_name    VARCHAR(32)  DEFAULT NULL,
                    rank_color   VARCHAR(8)   DEFAULT NULL,
                    rank_icon    VARCHAR(8)   DEFAULT NULL,
                    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        }
    }

    /** A appeler a chaque refresh() de FactionTabManager (join, changement de faction, changement de rang). */
    public void syncPlayer(UUID uuid, String factionName, FactionRank rank) {
        if (!enabled) return;

        String sql = """
            INSERT INTO faction_tab_sync (uuid, faction_name, rank_name, rank_color, rank_icon)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                faction_name = VALUES(faction_name),
                rank_name    = VALUES(rank_name),
                rank_color   = VALUES(rank_color),
                rank_icon    = VALUES(rank_icon)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, factionName);
            ps.setString(3, rank != null ? rank.nom : null);
            ps.setString(4, rank != null ? colorCode(rank) : null);
            ps.setString(5, rank != null ? rank.icone : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[FactionTabSync] Erreur d'ecriture pour " + uuid + " : " + e.getMessage());
        }
    }

    private String colorCode(FactionRank rank) {
        // ChatColor#toString() renvoie deja le code section (§x) — on le convertit en '&x' pour rester
        // dans un format portable qu'un plugin externe (hors classpath Bukkit) peut lire sans dependance.
        String legacy = rank.couleur.toString();
        return legacy.replace('\u00a7', '&');
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
