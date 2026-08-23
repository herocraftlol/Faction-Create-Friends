# 🏰 FactionPlugin

> Plugin Minecraft **tout-en-un** de gestion de factions pour serveur **Spigot/Paper 1.20.4** — factions, alliances, **guerres inter-factions**, claims, économie, commerce et bien plus !

![Version](https://img.shields.io/badge/version-5.2.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.20.4-green)
![Java](https://img.shields.io/badge/java-17%2B-orange)

## 📖 Description

**FactionPlugin** transforme votre serveur Minecraft en une véritable expérience de factions : créez votre faction, recrutez des membres, forgez des **alliances stratégiques**, déclarez la **guerre** aux factions rivales et arrachez-leur leurs territoires, réclamez et protégez vos **claims**, amassez des richesses dans la **banque d'émeraudes**, commercez avec les autres joueurs via le **shop global**, passez des **ordres d'échange** au comptoir ou troquez en sécurité, et mesurez-vous aux autres factions grâce au **système de puissance** et ses **7 rangs** (de Pierre à Légendaire).

Le tout avec des **interfaces graphiques (GUI)** intuitives et une commande unique : **`/faction`** (alias `/f`).

## ✨ Nouveautés de la v5.2.0

### 🏪 Comptoir d'échange — déposez votre monnaie contre un item

En plus du shop classique, un joueur peut maintenant créer un **ordre d'échange** (`/faction echange`, alias `/fac comptoir`) : il dépose une réserve de monnaie (fer, or, diamant ou émeraude) et demande en retour un item précis, à un taux fixé par « lot ». N'importe quel joueur peut alors **fournir** cet item pour recevoir la monnaie correspondante, jusqu'à épuisement du stock déposé.

**Exemple concret :** tenez **60 fer** en main et tapez `/fac deposer pierre 32 5` → l'ordre « 32 pierre → 5 fer par lot » est créé avec 60 fer en réserve (12 lots). Les autres joueurs fournissent de la pierre (`/fac fournir <ID>` ou clic dans le GUI) et reçoivent 5 fer par lot. Vous collectez la pierre reçue avec `/fac collecter <ID>` — sans fermer l'ordre s'il reste du fer à distribuer. Et si vous changez d'avis, `/fac retirerordre <ID>` vous rembourse la monnaie restante **et** les items déjà reçus.

#### 🖥️ Interface graphique dédiée
- `/faction echange` ouvre un **GUI paginé** listant tous les ordres actifs — un clic sur un ordre le remplit avec les items de votre inventaire
- Bouton **➕ Créer un ordre** : tenez votre monnaie en main, puis tapez dans le chat `<item> <quantité_par_lot> <prix_par_lot>` (ex : `pierre 32 5`) — l'ordre est créé et le GUI se rouvre automatiquement
- Bouton **📦 Mes ordres** (`/fac mesordres`) : la liste de vos ordres — **clic gauche** pour collecter les items reçus, **clic droit** pour annuler l'ordre

#### 📋 Nouvelles commandes

| Commande | Description |
|---|---|
| `/fac echange` (alias `comptoir`) | Ouvrir le comptoir d'échange (GUI paginé) |
| `/fac deposer <item> <quantité/lot> <prix/lot>` (alias `depot`) | Créer un ordre en déposant le stack de monnaie tenu en main |
| `/fac fournir <ID>` (alias `livrer`) | Fournir l'item demandé (plusieurs lots d'un coup si possible) |
| `/fac collecter [ID]` | Récupérer les items reçus ; sans ID, liste vos ordres actifs |
| `/fac retirerordre <ID>` (alias `annulerordre`) | Annuler un ordre (rembourse monnaie restante + items reçus) |
| `/fac mesordres` | GUI de vos ordres : collecte (clic gauche) et annulation (clic droit) |

**Bonne à savoir :** la monnaie déposée doit être l'une des **4 monnaies** déjà utilisées par le shop (fer, or, diamant, émeraude), tenue en main au moment du dépôt. L'item demandé peut être **n'importe quel item du jeu** — les noms français courants (`pierre`, `bois`, `charbon`...) comme les noms techniques Bukkit sont reconnus. Les ordres sont **persistants** (`plugins/FactionPlugin/exchange.yml`) et survivent aux redémarrages.

---

## 📜 Rappel — Nouveautés de la v5.1.1

### ⚔️ Guerre inter-factions (`/faction guerre`)
Déclarez une guerre **négociée** à une faction rivale et remportez ses territoires !

- **Déclaration avec enjeux au choix** : `/fac guerre declarer <faction> [claims:0-5] [pillage] [kills:5-50]`
  - `claims:` nombre de territoires (chunks) que le perdant devra céder au vainqueur
  - `pillage` : le vainqueur obtient un accès temporaire au coffre partagé du perdant (max 27 items)
  - `kills:` nombre de kills PvP nécessaires pour remporter la guerre (défaut : 20)
- **Acceptation requise** : le chef adverse doit accepter (`/fac guerre accepter`) ou refuser — pas de guerre forcée
- **Score en direct** : le score de guerre s'affiche dans l'**action bar** (kills, objectif, temps restant) et un indicateur ⚔ apparaît dans le chat
- **Zone de combat** : seuls les kills en territoire claimé par l'une des deux factions comptent — les kills ailleurs ne gonflent pas le score
- **Résolution automatique** : victoire par score atteint, **capitulation** (`/fac guerre capituler`), ou match nul après 72h — les claims perdus sont **transférés automatiquement** (les plus éloignés du centre d'abord)
- **Anti-abus intégré** : cooldown de 48h, 1 seule guerre active par faction, impossible de déclarer la guerre à un allié, écart de rang limité entre les deux factions, cible avec au moins 2 membres actifs

### 🖥️ Nouveau menu principal
`/faction` (ou `/fac menu`) ouvre un **menu d'accueil repensé** : accès rapide à toutes les fonctionnalités (guerre, claims, banque, shop, homes, alliances...) avec les commandes affichées directement dans les items.

### 🔧 Corrections et améliorations
- **Coffres privés** : correction du format de sauvegarde (les coffres situés dans des mondes dont le nom contient des virgules ou des underscores étaient mal rechargés)
- **plugin.yml** : ajout des alias `/tpac` et `/tpdeny` (`/tpd`), descriptions enrichies
- Diverses corrections internes et amélioration de la stabilité

---

## 📜 Rappel — Nouveautés de la v5.0.0

### 🤝 Alliances entre factions
Forgez des pactes avec d'autres factions et renforcez votre puissance commune !

| Nombre d'alliés | Bonus de puissance |
|---|---|
| 1 allié | **+500 power** |
| 2 alliés | **+1 200 power** |
| 3 alliés | **+2 500 power** |
| 4+ alliés | **+500 par allié en plus** |

Gérez vos alliances par commande ou via un GUI dédié : `/faction alliance inviter|accepter|refuser|rompre|liste|gui`

### 🏠 Homes personnels
Plusieurs **homes nommés** par joueur avec des règles anti-abus (distance min. 160 blocs, warmup 5 s) :
- Sans faction : **1 home**
- Faction avec alliés : **3 homes**

`/sethome [nom]` • `/home [nom]` • `/delhome <nom>` • `/homes`

### 📦 Spawn de faction
Le chef définit un point de ralliement (`/faction setspawn`) auquel tous les membres peuvent se téléporter (`/faction spawn`).

### 🚀 Téléportation entre joueurs
Système **/tpa** complet : demande avec expiration (30 s), acceptation/refus, warmup de sécurité (3 s) et cooldown anti-spam.
`/tpa <joueur>` • `/tpaccept` • `/tpdeny`

### 🔒 Coffres privés
**Sneak + clic droit avec un panneau** sur un coffre pour le verrouiller ! Seuls vous (et les admins `faction.admin`) pouvez l'ouvrir, et personne ne peut le casser.

---

## 🎯 Fonctionnalités principales

### 🏰 Factions
- Création, invitation, expulsion, dissolution, transfert de chef
- Inventaire partagé (`/faction coffre`)
- Interface GUI complète (`/faction menu`)

### ⚡ Système de puissance
- **Puissance Individuelle (PI)** calculée sur le PvP, la survie, la progression et l'activité
- **7 rangs de faction** : Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire
- **Effets passifs** croissants : speed, force, résistance...
- Classement des factions (`/faction classement`)

### 🗺️ Claims (territoire)
- Réclamez des chunks et protégez votre territoire contre les intrus
- Permissions par joueur via GUI (`/faction perms`)
- Autorisez vos **alliés** sur vos claims (`claimallies`)
- Carte des claims (`/faction claimmap`)

### 🏦 Banque d'émeraudes
- Coffre de faction pour déposer/retirer des émeraudes (GUI)
- Historique des transactions, top richesse (`/faction topbanque`)

### 🛒 Shop global
- Vendez et achetez avec un GUI paginé (45 items/page), recherche et tri par prix
- Monnaies : fer, or, diamant, émeraude
- Paiement automatique du vendeur, livraison même hors-ligne
- `/faction shop` • `/faction vendre <prix> <monnaie>` • `/faction mesannonces`

### 🏪 Comptoir d'échange
- Créez des **ordres d'échange** : déposez une monnaie, demandez un item, fixez le taux par lot
- GUI paginé des ordres actifs (`/faction echange`) et gestion de vos ordres (`/faction mesordres`)
- Collecte des items reçus et annulation avec remboursement à tout moment

### 💱 Troc sécurisé
- Échange d'items entre deux joueurs avec double confirmation anti-scam (`/faction troc`)

### 👁️ InvSee (admin)
- Visualisation **en lecture seule** de l'inventaire complet d'un joueur (`/faction invsee <joueur>`)

### 📊 Statistiques joueurs
- `/faction stats [joueur]` : kills, mobs, K/D, blocs, temps de jeu... même pour les joueurs hors-ligne
- `/faction classementjoueurs` : top 10 par catégorie

## 📥 Installation

1. Téléchargez la dernière version : **[FactionPlugin-5.2.0.jar](../../releases/download/v5.2.0/FactionPlugin-5.2.0.jar)**
2. Déposez le JAR dans le dossier `plugins/` de votre serveur
3. Redémarrez le serveur — la configuration est générée dans `plugins/FactionPlugin/`

## ⚙️ Commandes principales

| Commande | Description |
|---|---|
| `/faction create <nom>` | Créer une faction |
| `/faction info [faction]` | Informations d'une faction |
| `/faction invite <joueur>` / `join` / `leave` / `kick` | Gestion des membres |
| `/faction menu` | Interface graphique complète |
| `/faction guerre declarer <fac> [claims:n] [pillage] [kills:n]` | Déclarer une guerre ⚔️ |
| `/faction guerre accepter` / `refuser` / `capituler` / `statut` | Gérer une guerre ⚔️ |
| `/faction alliance <action>` | Gérer les alliances |
| `/faction setspawn` / `/faction spawn` | Spawn de faction |
| `/sethome` `/home` `/delhome` `/homes` | Homes personnels |
| `/tpa <joueur>` `/tpaccept` `/tpdeny` | Téléportation entre joueurs |
| `/faction claim` / `unclaim` / `claimmap` / `perms` | Territoire |
| `/faction banque` | Banque d'émeraudes |
| `/faction shop` / `vendre` / `acheter` | Shop global |
| `/faction echange` / `deposer` / `fournir` / `collecter` / `retirerordre` / `mesordres` | Comptoir d'échange 💱 |
| `/faction troc <joueur>` | Troc sécurisé |
| `/faction stats [joueur]` / `classementjoueurs` | Statistiques |
| `/faction classement` / `rangs` / `power` | Puissance et classements |

💱 = nouveau en v5.2.0 • ⚔️ = guerre, ajouté en v5.1.1

## 🔐 Permissions

| Permission | Description | Défaut |
|---|---|---|
| `faction.use` | Utiliser les commandes de faction | tout le monde |
| `faction.admin` | InvSee, bypass claims, ouvrir les coffres privés | op |

## 🛠️ Compilation

```bash
mvn clean package
```
Le JAR est généré dans `target/FactionPlugin-5.2.0.jar` (Java 17+, Maven 3.9+).

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.2.0** | Comptoir d'échange : ordres de dépôt de monnaie contre items, GUI paginé, gestion et annulation de vos ordres |
| **v5.1.1** | Guerre inter-factions avec enjeux négociables (claims, pillage, kills), nouveau menu principal, fix coffres privés |
| v5.0.0 | Alliances avec bonus de puissance, homes personnels, spawn de faction, /tpa, coffres privés |
| v4.0.0 | Shop global paginé avec recherche, InvSee admin |
| v3.2.4 | Corrections et améliorations finales |
| v3.2.x | Optimisations du système de puissance, fixes du troc |
| v3.2.0 | Banque d'émeraudes, claims, commerce entre joueurs |
| v3.1.0 | Fusion avec FactionStats : stats joueurs et classements intégrés |
| v2.0.0 | Système de puissance, rangs, classement des factions |
| v1.1.0 | GUI, téléportation intérieure, inventaire partagé |
| v1.0.0 | Version initiale |

## 📄 Licence

Ce projet est sous licence **MIT**.
