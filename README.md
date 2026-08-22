# 🏰 FactionPlugin

> Plugin Minecraft **tout-en-un** de gestion de factions pour serveur **Spigot/Paper 1.20.4** — factions, alliances, claims, économie, commerce et bien plus !

![Version](https://img.shields.io/badge/version-5.0.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.20.4-green)
![Java](https://img.shields.io/badge/java-17%2B-orange)

## 📖 Description

**FactionPlugin** transforme votre serveur Minecraft en une véritable expérience de factions : créez votre faction, recrutez des membres, forgez des **alliances stratégiques**, réclamez et protégez votre **territoire**, amassez des richesses dans la **banque d'émeraudes**, commercez avec les autres joueurs via le **shop global** ou le **troc sécurisé**, et mesurez-vous aux autres factions grâce au **système de puissance** et ses **7 rangs** (de Pierre à Légendaire).

Le tout avec des **interfaces graphiques (GUI)** intuitives et une commande unique : **`/faction`** (alias `/f`).

## ✨ Nouveautés de la v5.0.0

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

### 💱 Troc sécurisé
- Échange d'items entre deux joueurs avec double confirmation anti-scam (`/faction troc`)

### 👁️ InvSee (admin)
- Visualisation **en lecture seule** de l'inventaire complet d'un joueur (`/faction invsee <joueur>`)

### 📊 Statistiques joueurs
- `/faction stats [joueur]` : kills, mobs, K/D, blocs, temps de jeu... même pour les joueurs hors-ligne
- `/faction classementjoueurs` : top 10 par catégorie

## 📥 Installation

1. Téléchargez la dernière version : **[FactionPlugin-5.0.0.jar](../../releases/download/v5.0.0/FactionPlugin-5.0.0.jar)**
2. Déposez le JAR dans le dossier `plugins/` de votre serveur
3. Redémarrez le serveur — la configuration est générée dans `plugins/FactionPlugin/`

## ⚙️ Commandes principales

| Commande | Description |
|---|---|
| `/faction create <nom>` | Créer une faction |
| `/faction info [faction]` | Informations d'une faction |
| `/faction invite <joueur>` / `join` / `leave` / `kick` | Gestion des membres |
| `/faction menu` | Interface graphique complète |
| `/faction alliance <action>` | Gérer les alliances ⭐ |
| `/faction setspawn` / `/faction spawn` | Spawn de faction ⭐ |
| `/sethome` `/home` `/delhome` `/homes` | Homes personnels ⭐ |
| `/tpa <joueur>` `/tpaccept` `/tpdeny` | Téléportation entre joueurs ⭐ |
| `/faction claim` / `unclaim` / `claimmap` / `perms` | Territoire |
| `/faction banque` | Banque d'émeraudes |
| `/faction shop` / `vendre` / `acheter` | Shop global |
| `/faction troc <joueur>` | Troc sécurisé |
| `/faction stats [joueur]` / `classementjoueurs` | Statistiques |
| `/faction classement` / `rangs` / `power` | Puissance et classements |

⭐ = nouveau en v5.0.0

## 🔐 Permissions

| Permission | Description | Défaut |
|---|---|---|
| `faction.use` | Utiliser les commandes de faction | tout le monde |
| `faction.admin` | InvSee, bypass claims, ouvrir les coffres privés | op |

## 🛠️ Compilation

```bash
mvn clean package
```
Le JAR est généré dans `target/FactionPlugin-5.0.0.jar` (Java 17+, Maven 3.9+).

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.0.0** | Alliances avec bonus de puissance, homes personnels, spawn de faction, /tpa, coffres privés |
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
