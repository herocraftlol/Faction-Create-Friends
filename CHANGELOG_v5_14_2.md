# FactionPlugin v5.14.2 — *Accès aux coffres pendant la dissolution* 🧳🔓

## 🧳 Nouveauté : tu gardes l'accès à tes coffres pendant l'heure qui suit le `/faction disband`

La v5.14.0 introduit la dissolution différée d'une heure — la faction continue d'exister pendant une heure le temps de tout récupérer. La v5.14.2 ferme un cas qui restait bancal : si un membre **quittait** la faction pour une autre (ou était exclu) pendant cette heure, il **perdait instantanément l'accès aux coffres et chunks de son ancienne faction**, alors que celle-ci existait encore techniquement et que ses claims n'étaient pas encore libérés.

C'est désormais corrigé : pendant toute la durée du compte à rebours d'une heure, **chaque membre de la faction reste explicitement autorisé sur tous les claims de la faction**, même s'il l'a quittée (volontairement ou par exclusion) avant l'échéance.

### Avant / Après

| Situation | En v5.14.0 | En v5.14.2 |
|-----------|------------|------------|
| Tu fais `/faction disband`, attends 30 min, puis `/faction leave` et rejoins une autre faction | ❌ Tes coffres & claims dans l'ancienne faction deviennent inaccessibles pendant les 30 min restantes — tu perds ton propre butin | ✅ Tes coffres & claims restent accessibles jusqu'à la fin du compte à rebours (claims réellement libérés) |
| Tu fais `/faction disband` mais ne quitte pas la faction | ✅ Aucun souci : tu es toujours membre | ✅ Identique : tu es toujours membre |
| Un chef te fait `/faction kick` pendant l'heure de grâce | ⚠️ Tu perdais l'accès à tes coffres le temps qu'il reste | ✅ Tu gardes l'accès jusqu'à la libération effective |
| Le serveur redémarre pendant l'heure | ✅ Reprise correcte de la dissolution | ✅ Identique, plus `purgeOrphanedClaims` au démarrage |

### Pourquoi ce changement ?

Avant la v5.14.2, il y avait un trou temporel embêtant : le **délai de grâce d'une heure** était conçu pour que tout le monde puisse récupérer ses affaires, mais **un membre qui changeait d'avis** (par exemple pour rejoindre une faction rivale, ou par exclusion d'un chef qui change de stratégie) **se faisait littéralement voler ses coffres au milieu du délai censé le protéger**. Aucune action ne permettait de les rouvrir, même si la faction existait encore techniquement.

La v5.14.2 résout ça en **attachant l'autorisation d'accès au claim, pas à l'appartenance à la faction** :

- Au moment du `scheduleDisband(...)`, le plugin ajoute chaque membre actuel à la liste des joueurs autorisés sur **tous les claims de la faction**.
- Cette autorisation persiste **sur chaque chunk** : elle ne disparaît pas quand le joueur quitte la faction — elle disparaît seulement quand le claim est réellement libéré (à la fin du compte à rebours, ou lors d'un `removeAllClaims(...)`).
- Le joueur peut donc quitter, rejoindre une autre faction, voyager, mourir — **ses anciens coffres l'attendent toujours**, jusqu'à la fin du délai ou la libération effective.

### Détails techniques

- Nouvelle méthode dans `ClaimManager` :
  ```java
  /** Autorise ce joueur sur TOUS les chunks actuellement claimés par cette faction. */
  public void allowPlayerOnAllClaims(String factionName, UUID uuid);
  ```
- Appelée depuis `DisbandManager.scheduleDisband(...)` pour chaque membre au moment de la demande de dissolution.
- Sauvegarde immédiate pour résister à un redémarrage serveur.
- Aucun coût en performance : la liste d'autorisations est juste un `Set<UUID>` par `ClaimData`, déjà supporté par le système de claims.

---

## 🧹 Filet de sécurité : `purgeOrphanedClaims` au démarrage + toutes les 30 min

La v5.14.1 introduisait `purgeGhostFactions` : toute faction à 0 membre est désormais traitée comme « fantôme » et intégralement supprimée. La v5.14.2 ajoute un autre cas pathologique qui restait possible : **des claims orphelins** — des chunks encore marqués comme claimés par une faction qui n'existe plus (par exemple dissoute avant que la libération automatique des claims n'existe, ou à cause d'un autre bug historique).

Ces claims orphelins étaient **deux problèmes** :
1. Le chunk restait « claimé » par personne — son contenu était inutilisable par qui que ce soit, sans qu'aucune faction n'en soit propriétaire.
2. Le coffre que tu y avais mis n'était plus accessible à personne.

La v5.14.2 ajoute :

- **Au démarrage du serveur**, une passe `purgeOrphanedClaims()` identifie toutes les factions propriétaires de claims qui n'existent plus dans `factions.yml`, et libère leurs claims (équivalent d'un `removeAllClaims`).
- **Toutes les 30 minutes** (en parallèle de `purgeGhostFactions`), la même vérification est effectuée pour rattraper le cas où une faction serait supprimée par un autre chemin que `purgeGhostFactions`.

ℹ️ Aucune action requise côté admin — la purge s'exécute toute seule.

---

## 🔧 Petits correctifs complémentaires

- **`DisbandManager.resumePendingDisbands()`** rejoue correctement la dissolution différée après un redémarrage, et applique la nouvelle autorisation `allowPlayerOnAllClaims` si la dissolution avait été demandée avant que la v5.14.2 ne soit en place (cas d'une mise à jour pendant le délai d'1 h).
- **`WebMapSync.java`** : correction d'une erreur de syntaxe dans le sérialiseur JSON (`{"cx":...` mal échappé en `{"cx":...`).
- **`Contract.java`** : import manquant `fr.faction.village.PostType` (qui était utilisé sans import) — désormais corrigé.

---

## 📦 Installation & migration

- **Aucune action requise** côté admin : remplacer le `.jar` et redémarrer suffit.
- **Aucune migration de données** : remplacer le `.jar` et redémarrer suffit — `purgeOrphanedClaims` fait le ménage dans les chunks orphelins au démarrage, s'il y en a.
- Les YAML des versions ≥ v5.0.0 restent compatibles.

---

## 📥 Téléchargements

- **`FactionPlugin-5.14.2.jar`** — à déposer dans `plugins/` de votre serveur Paper 1.21.4+
- **`FactionPlugin-v5.14.2-source.zip`** — code source complet (Maven)

---

*Cette release ne modifie aucune autre commande ou comportement du plugin — uniquement le cas particulier de l'accès aux coffres/claims pendant la dissolution différée, plus le nettoyage automatique des claims orphelins au démarrage.*
