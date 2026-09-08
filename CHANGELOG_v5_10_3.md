# CHANGELOG — FactionPlugin v5.10.3

## 🎨 Nouveauté principale : le chat en couleur est de retour

Depuis le passage à Paper 1.21 et au chat signé (Component / chat sécurisé),
le plugin avait silencieusement perdu la **mise en couleur et le préfixe de
faction dans le tchat mondial** : `AsyncPlayerChatEvent#setFormat()` n'est
plus vraiment respecté par Paper — il est techniquement présent, mais le
rendu final ne tient pas compte du format modifié.

La v5.10.3 corrige ce point en migrant vers l'API moderne Paper :

```java
@EventHandler(priority = EventPriority.HIGH)
public void onPlayerChat(AsyncChatEvent event) {
    // ...
    String head = warTag + rankPrefix + factionTag + " " + rank.couleur + player.getName();
    Component headComponent = LEGACY.deserialize(head);
    Component separator = LEGACY.deserialize(ChatColor.DARK_GRAY + ": " + ChatColor.WHITE);
    event.renderer((source, sourceDisplayName, message, viewer) ->
            headComponent.append(separator).append(message));
}
```

### Rendu concret en jeu
```
[⚔][⚜] [TitanS] Steve : on doit riposter ce soir
[◆] [TitanS] Alex  : j'ai déjà 12 obsidienne en stock
[∅]   Billy        : salut, on se voit demain
```
- `⚔` → icône rouge de guerre (apparaît automatiquement quand la faction est en guerre)
- `⚜` → icône de rang Légendaire (devant le nom du chef / du joueur au rang max)
- `[TitanS]` → tag de faction dans la couleur du rang
- `Steve` → nom du joueur dans la couleur du rang
- Le **contenu** (`on doit riposter ce soir`) est laissé tel quel, signé par le client.

---

## ✅ Liste détaillée des changements v5.10.3

### 🎨 Chat en couleur
- Migration **`AsyncPlayerChatEvent#setFormat()` → `AsyncChatEvent` + `event.renderer(...)`**.
- Le rendu reconstruit un vrai `Component` (donc compatible avec le chat signé).
- Le contenu tapé par le joueur reste intact — aucune modification côté serveur.
- **Tag de guerre** ajouté automatiquement quand la faction est en guerre.
- **Icône Légendaire** `[⚜]` pour les rangs au maximum.
- Couleurs pleinement respectées dans tous les clients (vanilla, moddés, etc.).

### 🌙 Régénération pendant le sommeil (compat Paper 1.21+)
- L'événement Bukkit `EntitySleepEvent` n'est **plus jamais lancé** par Paper 1.21+ (passage des villageois à l'IA Brain). L'ancien `@EventHandler` aurait fait échouer la compilation.
- Remplacement par un **polling `Villager#isSleeping()`** dans la boucle d'IA principale (`VillagerManager#tickAll`).
- L'état de sommeil précédent de chaque villageois est mémorisé dans un champ transitoire `wasSleeping` (non persisté — recalculé à chaud). La transition `awake → sleeping` déclenche la régénération comme en v5.9.x.

### 🧹 Petites corrections de compilation
- Suppression du `@EventHandler` cassé sur `EntitySleepEvent`.
- Les villagcois qui se couchent continuent d'être soignés progressivement dans la limite de 8 ticks configurables (`villager.sleep-heal-ticks`).

### 📦 Migration de données
**Aucune.** Remplacez simplement le `.jar` et redémarrez. Tous les fichiers de
sauvegarde (`factions.yml`, `villagers.yml`, `stats.yml`, etc.) restent
compatibles.

---

## 🔧 Notes techniques

- **API cible** : `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`
- **Java** : 21 (compilé en `target 21`)
- **Taille du JAR** : ≈ 430 KB
- **Compatibilité descendante** : tous les fichiers YAML de la v5.10.2 sont
  rechargés tels quels.

---

## 🙏 Remerciements

Merci à tous les joueurs et administrateurs qui ont signalé la disparition
silencieuse des couleurs dans le tchat après la mise à jour Paper 1.21.
Cette release leur rend ce qu'ils ont perdu sans leur demander de toucher à
leur config.

Pour toute question ou bug, ouvre une **Issue** sur le dépôt GitHub.
