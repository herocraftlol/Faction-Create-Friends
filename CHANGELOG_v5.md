# CHANGELOG — FactionPlugin v5.1.1

## Nouveautés v5.1.1

### ⚔️ Système de Guerre inter-factions (`/faction guerre`)
- **Déclaration négociée** : `/fac guerre declarer <faction> [claims:0-5] [pillage] [kills:5-50]`
  - `claims:` — nombre de chunks que le perdant cède au vainqueur (0 à 5)
  - `pillage` — accès temporaire au coffre partagé du perdant (max 27 items)
  - `kills:` — objectif de kills PvP pour gagner (défaut 20, min 5, max 50)
- **Acceptation obligatoire** : le chef adverse doit accepter ou refuser la déclaration — aucune guerre forcée
- **Zone de combat** : seuls les kills effectués sur un chunk claimé par l'une des deux factions comptent au score
- **Score public** : broadcast toutes les 5 kills, affichage permanent dans l'**action bar** (score, objectif, temps restant) et indicateur ⚔ dans le chat
- **Résolution automatique** :
  - Victoire → transfert automatique des claims en jeu (les plus éloignés du centre du perdant en premier) + pillage du coffre si négocié
  - Capitulation via `/fac guerre capituler` (acceptée automatiquement)
  - Match nul si la durée maximale (72h) est dépassée
- **Anti-abus** :
  - Cooldown de 48h par faction après une guerre
  - Maximum 1 guerre active par faction
  - Impossible de déclarer la guerre à un allié
  - Écart de rang limité (max 2 rangs de puissance d'écart)
  - La cible doit avoir au moins 2 membres actifs

### 🖥️ Nouveau menu principal (`MainMenuGUI`)
- `/faction` et `/fac menu` ouvrent un menu d'accueil repensé
- Accès rapide à toutes les fonctionnalités : guerre, claims, banque, shop, homes, alliances, stats...
- Les commandes sont affichées directement dans la description des items

### 🔧 Corrections
- **Coffres privés** : nouveau format de persistance (liste YAML) — corrige le rechargement des coffres situés dans des mondes dont le nom contient des virgules ou des underscores
- **plugin.yml** : alias `/tpac` (`/tpaccept`) et `/tpd` (`/tpdeny`), descriptions enrichies
- Corrections de compilation : `Material.BED` → `Material.RED_BED` (API 1.20), import `Location` manquant, pattern `instanceof` incompatible Java 17

## Fichiers ajoutés
```
src/main/java/fr/faction/war/
  ├── WarSession.java       ← État d'une guerre (enjeux, score, timers)
  └── WarManager.java       ← Logique guerre : déclaration, kills, résolution
src/main/java/fr/faction/gui/
  └── MainMenuGUI.java      ← Menu principal repensé
```

## Fichiers modifiés
- `FactionPlugin.java` : initialisation WarManager + MainMenuGUI
- `FactionCommand.java` : sous-commandes `/fac guerre` (declarer, accepter, refuser, capituler, statut)
- `ClaimManager.java` : `transferClaim()` (transfert de territoire en résolution de guerre)
- `SharedInventoryManager.java` : accès coffre partagé pour le pillage
- `ActionBarManager.java` : affichage du score de guerre
- `PlayerListener.java` : indicateur de guerre dans le chat
- `PrivateChestManager.java` : nouveau format de sauvegarde
- `plugin.yml` : version 5.1.1, alias tpa, descriptions

---

# CHANGELOG — FactionPlugin v5.0.0

## Nouveautés v5.0.0

### 🤝 Système d'Alliances (`/faction alliance`)
- **Alliances entre factions** : proposez, acceptez ou rompez des pactes entre factions
- **Bonus de puissance progressif** : plus vous avez d'alliés, plus votre faction gagne de puissance bonus
  - 1 allié → +500 power • 2 alliés → +1 200 • 3 alliés → +2 500 • +500 par allié supplémentaire
- **Interface graphique** de gestion des alliances (`/faction alliance gui`)
- Commandes : `/faction alliance inviter|accepter|refuser|rompre|liste <faction>`

### 🏠 Homes personnels (`/sethome`, `/home`)
- **Homes nommés** : plusieurs points de téléportation par joueur avec persistance (`homes.yml`)
- **Limites dynamiques** : 1 home sans faction, **3 homes si votre faction a des alliés**
- **Sécurité anti-abus** : distance minimale de 10 chunks (160 blocs) entre deux homes, warmup de 5 secondes
- Commandes : `/sethome [nom]`, `/home [nom]`, `/delhome <nom>`, `/homes` (aussi via `/faction`)

### 📦 Spawn de Faction (`/faction setspawn` / `/faction spawn`)
- Le chef définit un point de ralliement pour sa faction
- Tous les membres peuvent s'y téléporter avec `/faction spawn`

### 🚀 Téléportation entre joueurs (`/tpa`)
- **Demandes de téléportation** joueur-à-joueur avec acceptation/refus
- Expiration après 30 secondes, warmup de 3 secondes, anti-spam par cooldown
- Commandes : `/tpa <joueur>`, `/tpaccept`, `/tpdeny` (aussi via `/faction`)

### 🔒 Coffres Privés
- **Verrouillage instantané** : sneak + clic droit avec un panneau sur un coffre (normal ou piégé)
- Seuls le propriétaire (et les admins) peuvent ouvrir le coffre
- Protection contre la casse du bloc par les autres joueurs
- Persistance dans `private_chests.yml`
- Permission admin : `faction.admin` peut ouvrir/casser les coffres privés

### 🗺️ Claims & Alliés
- Nouvelle permission de claim `claimallies` : autorisez vos factions alliées dans vos territoires

## Correction de compilation
- Ajout de l'import `org.bukkit.Location` manquant dans `PlayerTeleportManager`

## Fichiers ajoutés
```
src/main/java/fr/faction/alliance/
  ├── AllianceManager.java        ← Alliances + bonus power + GUI
  ├── HomeManager.java            ← Homes nommés avec limites
  ├── PlayerTeleportManager.java  ← /tpa avec accept/refus/cooldown
  └── PrivateChestManager.java    ← Coffres verrouillables
```

## Commandes ajoutées
| Commande | Description |
|---|---|
| `/faction alliance [inviter\|accepter\|refuser\|rompre\|liste\|gui]` | Gérer les alliances |
| `/faction setspawn` / `/faction spawn` | Spawn de faction |
| `/sethome [nom]`, `/home [nom]`, `/delhome <nom>`, `/homes` | Homes personnels |
| `/tpa <joueur>`, `/tpaccept`, `/tpdeny` | Téléportation entre joueurs |
