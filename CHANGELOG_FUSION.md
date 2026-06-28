# Fusion FactionPlugin v3.0 + FactionStats v1.0.0 → FactionPlugin v3.1.0

## Résumé

Le plugin **FactionStats** (commandes `/stats` et `/classement` séparées) a été
entièrement intégré dans **FactionPlugin**, qui conserve sa commande unique
`/faction`. FactionStats n'est plus un plugin à part : son code a été fusionné
dans le système de stats déjà présent dans FactionPlugin (`PlayerStatsManager`
/ `PlayerStats`), qui était auparavant plus basique.

## Nouvelles commandes / sous-commandes

- `/faction stats [joueur]` — **enrichie** : ajoute mobs hostiles tués, dégâts
  reçus, dates de première/dernière connexion, classements affichés en ligne,
  et fonctionne désormais aussi pour les joueurs **hors ligne** (auparavant
  limité aux joueurs connectés).
- `/faction classementjoueurs <categorie>` (alias `cj`) — **nouvelle**. Top 10
  joueurs par catégorie : `mobs`, `pvp`, `advancements`, `morts`, `blocs`,
  `temps`, `dommages`, `kd`.

⚠️ **Note de nommage** : `/faction classement` existait déjà et affiche le
classement **des factions** par puissance (GUI). Pour éviter un conflit, le
classement **des joueurs** par statistique a été nommé `classementjoueurs`
(et non `classement`, comme dans FactionStats d'origine).

## Changements techniques

- `PlayerStats` (modèle) : ajout de `playerName`, `damageTaken`, `mobsKilled`,
  `firstJoin`, `lastJoin`, et de `getFormattedPlaytime()`.
- `PlayerStatsManager` : ajout de `getOrCreateStats(uuid, nom)`,
  `getStatsByName(nom)`, `resolveStats(nom)` (cherche en cache → en ligne →
  `OfflinePlayer`), et de toutes les méthodes de classement
  (`getTopMobsKilled`, `getTopKills`, `getTopAdvancements`, `getTopDeaths`,
  `getTopBlocksBroken`, `getTopPlaytime`, `getTopDamageDealt`, `getTopKDR`,
  `getRank(uuid, categorie)`).
- `PowerBridgeListener` : ajout du tracking des mobs hostiles tués
  (`addMobKill`) et des dégâts reçus, y compris hors combat direct (chute,
  feu, noyade...).
- **Temps de jeu** : remplacement du calcul ponctuel à la déconnexion par une
  tâche périodique (`PlaytimeTracker`, +20 ticks/seconde pour chaque joueur
  en ligne), comme demandé — plus précis et robuste en cas de crash serveur.
- Nouveau fichier `StatsMessageUtil` (anciennement `MessageManager` dans
  FactionStats) : formatage des nombres, séparateurs, médailles de classement.
- `stats.yml` (fichier de sauvegarde) : nouveaux champs `name`, `damageTaken`,
  `mobsKilled`, `firstJoin`, `lastJoin`. Rétrocompatible : les anciens
  fichiers `stats.yml` du plugin Faction se rechargent sans erreur (valeurs
  par défaut à 0 / timestamp courant pour les champs absents).

## Comportement volontairement INCHANGÉ

- `addKill()` (compteur "Kills" affiché dans `/faction stats`) compte **toute
  mise à mort**, mob ou joueur confondus — c'était déjà le comportement du
  plugin Faction d'origine (`PowerBridgeListener`), avant même la fusion. Le
  nouveau compteur `mobsKilled` est une statistique **additionnelle**
  distincte, il ne remplace pas ni ne corrige ce comportement existant.
- Le calcul de Puissance Individuelle (`PlayerPowerCalculator`) n'a pas été
  modifié : `mobsKilled` et `damageTaken` restent purement informatifs et
  n'influencent pas la puissance, comme dans FactionStats d'origine.

## ⚠️ Important : compilation non vérifiée par exécution

L'environnement de cette fusion n'avait **ni accès réseau ni Maven/jar
Spigot installés**, donc le projet n'a **pas pu être compilé avec
`mvn package`** pour confirmer qu'il produit un `.jar` valide.

À la place, une relecture statique exhaustive a été faite : équilibre des
accolades, vérification croisée de chaque appel de méthode contre sa
signature réelle, vérification des imports, et diff fichier-par-fichier pour
garantir qu'aucun fichier non concerné n'a été modifié par erreur.

**Avant mise en production, merci de lancer toi-même :**
```
cd FactionPluginFinal
mvn clean package
```
et de tester au minimum `/faction stats`, `/faction classementjoueurs mobs`,
et `/faction power` sur un serveur de test.
