package fr.faction.power;

import fr.faction.managers.FactionManager;
import fr.faction.models.Faction;
import fr.faction.ranking.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Gère les préfixes du chat et du tab-list via Scoreboard Teams.
 *
 * Paper 1.21 : la méthode fiable pour afficher un préfixe dans le tab ET
 * dans le chat est de créer un scoreboard global partagé entre tous les
 * joueurs, avec une Team par (rang × nom de faction).
 *
 * Format d'une Team : "F_<rang_ordinal>_<factionName_slug>"
 * (limité à 16 chars — on troncature le nom de faction si nécessaire)
 *
 * Chaque joueur est dans exactement une Team.
 * Un joueur sans faction est dans la Team "F_none".
 *
 * Appelé lors de :
 *  - join / quit d'un joueur
 *  - changement de rang
 *  - changement de faction
 */
public class FactionTabManager {

    // Scoreboard partagé entre tous les joueurs (créé une seule fois)
    private final Scoreboard board;
    private final JavaPlugin plugin;
    private final FactionManager factionManager;
    private final FactionPowerManager powerManager;

    private static final String TEAM_NO_FACTION = "F_none";
    private static final String BOARD_NAME      = "FactionPlugin";

    public FactionTabManager(JavaPlugin plugin,
                              FactionManager factionManager,
                              FactionPowerManager powerManager) {
        this.plugin          = plugin;
        this.factionManager  = factionManager;
        this.powerManager    = powerManager;

        // Créer ou récupérer un scoreboard dédié
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        this.board = sbm.getNewScoreboard();

        // Pré-créer la team "sans faction"
        ensureTeam(TEAM_NO_FACTION,
                ChatColor.GRAY + "",   // prefix
                "",                    // suffix
                ChatColor.GRAY);       // nameColor
    }

    // ── API ──────────────────────────────────────────────────────────────────────

    /**
     * Actualise la team d'un joueur (préfixe + couleur de nom dans le tab).
     * À appeler : onJoin, après changement de rang, après changement de faction.
     */
    public void refresh(Player player) {
        Faction faction    = factionManager.getPlayerFaction(player.getUniqueId());
        FactionRank rank   = faction != null
                ? powerManager.getFactionRank(faction.getName())
                : null;

        String prefix;
        String teamId;
        ChatColor nameColor;

        if (faction == null) {
            prefix    = ChatColor.GRAY + "";
            teamId    = TEAM_NO_FACTION;
            nameColor = ChatColor.GRAY;
        } else {
            rank = rank != null ? rank : FactionRank.PIERRE;
            // Construire le préfixe court : [icone_rang][nom_faction]
            // Ex : "§e§l[★] §e[TitanS]§r "
            String factionTag = rank.couleur + "[" + faction.getName() + "] ";

            // Pour LÉGENDAIRE, ajouter le label spécial
            if (rank == FactionRank.LEGENDAIRE) {
                prefix = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[⚜] "
                        + ChatColor.RESET + ChatColor.LIGHT_PURPLE + "[" + faction.getName() + "] ";
            } else {
                prefix    = rank.getChatPrefix() + factionTag;
            }
            teamId    = buildTeamId(rank, faction.getName());
            nameColor = rank.couleur;
        }

        Team team = ensureTeam(teamId, prefix, "", nameColor);
        assignToTeam(player, team);

        // Partager le scoreboard au joueur
        player.setScoreboard(board);
    }

    /**
     * Actualise tous les joueurs en ligne (après un changement de rang global).
     */
    public void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    /**
     * Retire un joueur de toutes les teams (onQuit).
     */
    public void remove(Player player) {
        for (Team team : board.getTeams()) {
            team.removePlayer(player);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String buildTeamId(FactionRank rank, String factionName) {
        // Format limité à 16 chars : "F_<rang>_<facSlug>"
        String slug = factionName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (slug.length() > 8) slug = slug.substring(0, 8);
        String id = "F_" + rank.ordinal() + "_" + slug;
        return id.length() > 16 ? id.substring(0, 16) : id;
    }

    private Team ensureTeam(String teamId, String prefix, String suffix, ChatColor color) {
        Team team = board.getTeam(teamId);
        if (team == null) {
            team = board.registerNewTeam(teamId);
        }

        // Tronquer prefix à 64 chars (limite Bukkit)
        String p = prefix.length() > 64 ? prefix.substring(0, 64) : prefix;
        team.setPrefix(p);
        team.setSuffix(suffix);
        team.setColor(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.setOption(Team.Option.COLLISION_RULE,      Team.OptionStatus.ALWAYS);
        return team;
    }

    private void assignToTeam(Player player, Team target) {
        // Retirer de toutes les autres teams d'abord
        for (Team t : board.getTeams()) {
            if (t.hasPlayer(player) && !t.equals(target)) t.removePlayer(player);
        }
        target.addPlayer(player);
    }

    /**
     * Nettoie les teams vides (optionnel, appelé périodiquement).
     */
    public void pruneEmptyTeams() {
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getSize() == 0 && !team.getName().equals(TEAM_NO_FACTION)) {
                team.unregister();
            }
        }
    }

    public Scoreboard getScoreboard() { return board; }
}
