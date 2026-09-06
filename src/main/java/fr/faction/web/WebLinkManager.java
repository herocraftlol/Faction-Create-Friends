package fr.faction.web;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gère la liaison compte-en-jeu ↔ compte site web.
 *
 * Flux complet :
 *  1. Joueur tape /lier en jeu
 *  2. FactionPlugin génère un code à 6 chiffres et l'insère dans `web_link_codes`
 *     (avec expiration dans 10 minutes)
 *  3. Joueur va sur le site, se connecte avec son compte web, entre le code
 *  4. Le site lit `web_link_codes`, vérifie le code, insère dans `account_links`
 *     et supprime le code
 *
 * Configuration dans config.yml du plugin (section mysql:) :
 *   mysql:
 *     host: 127.0.0.1
 *     port: 3306
 *     database: herocraft          ← MÊME base que le site (GAME_DB_NAME dans .env)
 *     user: herocraft_user
 *     password: mot-de-passe
 *
 * La table web_link_codes est créée automatiquement si elle n'existe pas.
 * Elle est identique à celle attendue par le backend Node.js.
 */
public class WebLinkManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private Connection connection;
    private boolean enabled = false;

    // Durée de validité d'un code en minutes
    private static final int CODE_EXPIRY_MINUTES = 10;

    public WebLinkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
        connect();
    }

    // ── Connexion MySQL ───────────────────────────────────────────────────────────

    private void connect() {
        String host = plugin.getConfig().getString("mysql.host", "");
        if (host.isBlank()) {
            log.warning("[WebLink] Section 'mysql' absente du config.yml — /lier désactivé.");
            return;
        }

        String port     = plugin.getConfig().getString("mysql.port", "3306");
        String database = plugin.getConfig().getString("mysql.database", "herocraft");
        String user     = plugin.getConfig().getString("mysql.user",     "root");
        String password = plugin.getConfig().getString("mysql.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
        try {
            // Essayer le nouveau driver d'abord (mysql-connector-j 8+), puis l'ancien
            try { Class.forName("com.mysql.cj.jdbc.Driver"); }
            catch (ClassNotFoundException e1) {
                try { Class.forName("com.mysql.jdbc.Driver"); }
                catch (ClassNotFoundException e2) {
                    log.severe("[WebLink] Driver MySQL introuvable. Assure-toi que mysql-connector-j"
                            + " est dans le classpath ou que Paper 1.21 l'inclut.");
                    return;
                }
            }
            connection = DriverManager.getConnection(url, user, password);
            ensureTable();
            enabled = true;
            log.info("[WebLink] Connecté à MySQL (" + host + ":" + port + "/" + database + "). /lier activé.");
        } catch (Exception e) {
            log.severe("[WebLink] Impossible de se connecter à MySQL : " + e.getMessage());
            log.severe("[WebLink] Vérifie mysql.host/user/password/database dans config.yml.");
        }
    }

    private void ensureTable() throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Table identique à celle que le plugin LoyaltyMobs / le site Node attend
            st.execute("""
                CREATE TABLE IF NOT EXISTS web_link_codes (
                    code       CHAR(6)     NOT NULL PRIMARY KEY,
                    uuid       VARCHAR(36) NOT NULL,
                    pseudo     VARCHAR(16) NOT NULL,
                    expires_at DATETIME    NOT NULL,
                    INDEX idx_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            // Table des comptes web (créée par le site, on la crée aussi par sécurité)
            st.execute("""
                CREATE TABLE IF NOT EXISTS web_accounts (
                    id            INT AUTO_INCREMENT PRIMARY KEY,
                    pseudo        VARCHAR(32) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            // Table des liaisons
            st.execute("""
                CREATE TABLE IF NOT EXISTS account_links (
                    account_id INT PRIMARY KEY,
                    mc_uuid    VARCHAR(36) NOT NULL UNIQUE,
                    mc_pseudo  VARCHAR(16) NOT NULL,
                    linked_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (account_id) REFERENCES web_accounts(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    // ── API publique ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /**
     * Génère un code à 6 chiffres pour ce joueur, valable {@link #CODE_EXPIRY_MINUTES} minutes.
     * Supprime d'abord tout code existant pour ce UUID.
     *
     * @return le code généré (ex. "482951"), ou null si la connexion est indisponible
     */
    public String generateCode(UUID uuid, String pseudo) {
        if (!enabled) return null;
        try {
            ensureConnected();
            // Supprimer les anciens codes pour ce joueur
            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM web_link_codes WHERE uuid = ?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            // Générer un code unique
            String code;
            Random rng = new Random();
            do {
                code = String.format("%06d", rng.nextInt(1_000_000));
            } while (codeExists(code));

            // Insérer
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO web_link_codes (code, uuid, pseudo, expires_at) " +
                    "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))")) {
                ins.setString(1, code);
                ins.setString(2, uuid.toString());
                ins.setString(3, pseudo);
                ins.setInt(4, CODE_EXPIRY_MINUTES);
                ins.executeUpdate();
            }
            return code;
        } catch (Exception e) {
            log.warning("[WebLink] Erreur generateCode : " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si le compte est déjà lié et retourne le pseudo web, ou null.
     */
    public String getLinkedWebPseudo(UUID uuid) {
        if (!enabled) return null;
        try {
            ensureConnected();
            String sql = "SELECT wa.pseudo FROM account_links al " +
                         "JOIN web_accounts wa ON wa.id = al.account_id " +
                         "WHERE al.mc_uuid = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("pseudo");
                }
            }
        } catch (Exception e) {
            log.warning("[WebLink] Erreur getLinkedWebPseudo : " + e.getMessage());
        }
        return null;
    }

    /** Ferme la connexion proprement. */
    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (Exception ignored) {}
    }

    // ── Helpers privés ────────────────────────────────────────────────────────────

    private boolean codeExists(String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM web_link_codes WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private void ensureConnected() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = null; enabled = false;
            connect();
            if (!enabled) throw new SQLException("Connexion MySQL perdue.");
        }
    }
}
