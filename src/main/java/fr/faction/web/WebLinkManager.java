package fr.faction.web;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Liaison compte Minecraft ↔ compte site web sans SQL.
 *
 * Flux :
 *  1. /lier génère un code à 6 chiffres.
 *  2. Le plugin POST le code au backend du site (/api/faction/push/link-code).
 *  3. Le site stocke le code dans data/local-game.json.
 *  4. Le joueur saisit le code sur le site.
 *  5. Le site crée la liaison accountLinks dans le même fichier JSON.
 *
 * Configuration :
 *   site-url: "http://192.168.1.196:3000"
 *   faction-api-key: "la-même-clé-que-dans-le-.env-du-site"
 */
public class WebLinkManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final String siteUrl;
    private final String apiKey;
    private final boolean enabled;

    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final Pattern PSEUDO_PATTERN =
            Pattern.compile("\\\"pseudo\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");
    private static final Random RNG = new Random();

    public WebLinkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.siteUrl = plugin.getConfig().getString("site-url", "").replaceAll("/$", "");
        this.apiKey = plugin.getConfig().getString("faction-api-key", "");
        this.enabled = !siteUrl.isBlank() && !apiKey.isBlank();

        if (enabled) {
            log.info("[WebLink] Liaison web sans SQL activée → " + siteUrl);
        } else {
            log.warning("[WebLink] site-url ou faction-api-key absent du config.yml — /lier désactivé.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Génère un code à 6 chiffres valable 10 minutes et l'enregistre sur le site.
     * Cette méthode est appelée depuis un thread async par LierCommand.
     */
    public String generateCode(UUID uuid, String pseudo) {
        if (!enabled) return null;

        String code = String.format("%06d", RNG.nextInt(1_000_000));
        long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(CODE_EXPIRY_MINUTES).toMillis();
        String json = "{\"uuid\":\"" + esc(uuid.toString()) + "\","
                + "\"pseudo\":\"" + esc(pseudo) + "\","
                + "\"code\":\"" + code + "\","
                + "\"expiresAt\":" + expiresAt + "}";

        try {
            int status = request("POST", "/api/faction/push/link-code", json, null);
            if (status >= 200 && status < 300) return code;
            log.warning("[WebLink] Le site a refusé l'enregistrement du code (HTTP " + status + ").");
            return null;
        } catch (Exception e) {
            log.warning("[WebLink] Site inaccessible pour /lier : " + e.getMessage());
            return null;
        }
    }

    /**
     * Retourne le pseudo du compte site lié à cet UUID, ou null si non lié.
     * Appel HTTP court ; LierCommand l'exécute en async.
     */
    public String getLinkedWebPseudo(UUID uuid) {
        if (!enabled) return null;
        try {
            String query = "?uuid=" + URLEncoder.encode(uuid.toString(), StandardCharsets.UTF_8);
            HttpResult result = requestWithBody("GET", "/api/faction/plugin/link-status" + query, null);
            if (result.status < 200 || result.status >= 300) {
                log.warning("[WebLink] Statut de liaison refusé par le site (HTTP " + result.status + ").");
                return null;
            }
            if (!result.body.contains("\"linked\":true")) return null;
            Matcher m = PSEUDO_PATTERN.matcher(result.body);
            if (!m.find()) return null;
            return unescapeJson(m.group(1));
        } catch (Exception e) {
            log.warning("[WebLink] Erreur getLinkedWebPseudo : " + e.getMessage());
            return null;
        }
    }

    /** Ferme proprement le gestionnaire. Aucun stockage réseau persistant à fermer. */
    public void close() {
        // Rien à fermer : chaque requête HTTP est courte et indépendante.
    }

    private int request(String method, String path, String json, String ignored) throws Exception {
        return requestWithBody(method, path, json).status;
    }

    private HttpResult requestWithBody(String method, String path, String json) throws Exception {
        HttpURLConnection con = (HttpURLConnection) URI.create(siteUrl + path).toURL().openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("X-Faction-Key", apiKey);
        con.setConnectTimeout(CONNECT_TIMEOUT_MS);
        con.setReadTimeout(READ_TIMEOUT_MS);

        if (json != null) {
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }

        int status = con.getResponseCode();
        InputStream stream = status >= 400 ? con.getErrorStream() : con.getInputStream();
        String body = stream == null ? "" : readAll(stream);
        con.disconnect();
        return new HttpResult(status, body);
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private record HttpResult(int status, String body) {}
}
