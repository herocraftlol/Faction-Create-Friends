# 🏰 FactionPlugin

> Le plugin Minecraft **tout-en-un** pour gérer des factions, déclarer des guerres, bâtir des alliances, **recruter et commander des villageois autonomes**, et bâtir des empires sur **Paper 1.21.4**.

![Version](https://img.shields.io/badge/version-5.10.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-green)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Statut](https://img.shields.io/badge/status-stable-success)
![Licence](https://img.shields.io/badge/licence-MIT-lightgrey)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** transforme votre serveur Minecraft en une véritable **épopée de factions**. Créez votre clan, recrutez vos membres, scellez des **alliances** ou partez en **guerre**, réclamez et défendez vos **territoires**, amassez une fortune dans la **banque d'émeraudes**, vendez vos trouvailles sur le **shop global**, troquez en toute sécurité avec les autres joueurs, organisez votre coffre et votre inventaire, et mesurez-vous aux autres factions grâce au **système de puissance** à **7 rangs**.

Et depuis la **v5.9.0**, vos **villageois** peuvent rejoindre votre faction : en **v5.10.0**, un tout nouveau rôle — le **Récolteur** — s'ajoute au Constructeur et au Guerrier pour **miner, couper le bois, creuser et cultiver à votre place**. Pendant ce temps, le Constructeur bâtit désormais **instantanément** (téléportations bloc-par-bloc), et le Guerrier **fuit** intelligemment lorsqu'il est en danger plutôt que de mourir stupidement.

Une seule commande pour tout faire : **`/faction`** (alias `/f`).

---

## 🌾 Nouveautés de la v5.10.0 — *Le Récolteur, la construction instantanée et la fuite des guerriers*

Une mise à jour centrée sur la **boucle de vie de vos villageois recrutés** : plus d'autonomie, plus de productivité, et un comportement de combat enfin **intelligent**.

### 🌾 Nouveau rôle : Récolteur

Donnez-lui un outil, il fait le reste — **seul, en autonomie complète** :

- ⛏️ **Pioche** → il mine les minerais de la zone (charbon, fer, or, diamant, redstone, lapis, émeraudes…)
- 🪓 **Hache** → il coupe le bois (toutes les essences)
- 🪣 **Pelle** → il creuse terre, sable, gravier, argile, neige…
- 🌾 **Champ** → il sème, récolte à maturité, replante, et sème les cases vides tout seul (blé, carottes, pommes de terre, betteraves)

Vous choisissez la **zone de récolte** en cliquant deux coins dans le monde, vous désignez un **coffre de dépôt** (clic-droit dessus) : tout ce qu'il récolte y est déposé automatiquement. Les graines restent sur lui pour qu'il puisse **replanter sans intervention**.

À partir du **niveau 4**, son outil s'use mais il se **fabrique lui-même un nouvel outil** dès qu'il casse.

### 🏗️ Constructeur — construction instantanée

Fini les allers-retours à pied :

- Il se **téléporte directement** sur chaque bloc à poser ou à miner, quelle que soit la distance : **chantier terminé 10× plus vite** sur les grandes zones.
- S'il doit atteindre une hauteur difficile, il **pose un échafaudage en bambou** sous lui pour ne pas tomber, puis le **retire** (sans le faire tomber au sol) une fois le chantier terminé.
- **Récolte en un seul passage** : il calcule combien il lui manque pour finir **tout** le chantier, et récolte ce lot d'un coup — au lieu de repartir à la mine bloc par bloc.

### ⚔️ Guerrier — immobile par défaut, fuite & discrétion

- Le **vagabondage aléatoire** vanille est désactivé (best-effort) : il ne bouge plus que sur ordre (poste, patrouille, suivi, rassemblement).
- **Fuite intelligente** : sous **25 % de vie** ou face à **3+ ennemis**, il **fuit** (loin de la menace ou vers son poste) jusqu'à retrouver **60 % de vie** et **plus aucune menace proche**. Plus de morts stupides.

### 🛡️ Formations en cercle

- `/faction villageois ranger cercle` (ou le bouton dédié) dispose vos villageois en **cercle autour du joueur**, en plus de la formation en ligne.

---

## 🌾🥇⚔️ Les trois rôles de villageois

| Rôle | Comportement | Idéal pour… |
|---|---|---|
| 🌾 **Récolteur** | Donnez-lui un outil + une zone, il mine / coupe / creuse / cultive **seul**, indéfiniment, et dépose tout dans un coffre. | Remplir vos coffres de ressources sans effort. |
| 🪓 **Constructeur** | Reçoit un chantier (2 coins cliqués) et **comble automatiquement tout vide** dans la zone, en se téléportant bloc-par-bloc. Monte de niveau en posant des blocs. | Bâtir des murs, remplir des trous, réparer des défenses. |
| ⚔️ **Guerrier** | Équipé d'une arme + armure (+ arc / flèches optionnels), fait sa **ronde autour de son poste** dans son rayon, **détecte et combat** les mobs hostiles et les joueurs ennemis. **Fuit intelligemment** s'il est en danger. Monte de niveau au combat. | Protéger votre base, escorter, harceler l'ennemi, monter la garde. |

### 🧑‍🌾 Commandes ajoutées (rappel)

| Commande | Rôle requis | Effet |
|---|---|---|
| `/faction recruter` | Chef / sous-chef | Convertir un villageois ciblé en unité de faction. |
| `/faction villageois` | Tout membre | Ouvre le **GUI** listant les villageois recrutés. |
| `/faction villageois ranger` *ou* `formation` | Chef / sous-chef | Met tous les villageois à 40 blocs en **ligne** devant vous. |
| `/faction villageois ranger cercle` | Chef / sous-chef | Met tous les villageois à 40 blocs en **cercle** autour de vous. |
| `/faction annuler` | Tout joueur | Annule la sélection de patrouille / chantier en cours. |

---

## 🎯 Fonctionnalités principales

### 🏰 Factions

Création, invitation, expulsion, dissolution, transfert de chef, **sous-chefs** (jusqu'à 2), **inventaire partagé** (`/faction coffre`), **menu GUI complet** (`/faction` ou `/faction menu`).

### ⚡ Système de puissance

- **Puissance Individuelle (PI)** : PvP + survie + progression + activité
- **Puissance Globale (PG)** : somme des PI + bonus de taille
- **7 rangs** : Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire
- Effets passifs croissants : Strength, Resistance, Haste, Regeneration
- Classements (`/faction classement`, `/faction power`)

### 🗺️ Claims (territoire)

Chunks protégés, permissions par joueur (`/faction perms`), alliés autorisés (`claimallies`), mini-map visuelle (`/faction claimmap`).

### ⚔️ Guerres inter-factions (v5.1.1)

- Déclaration **négociée** : `claims:0-5`, `pillage`, `kills:5-50`
- Score en direct dans l'**action bar**
- Les villageois guerriers traitent automatiquement les belligérants comme hostiles
- Transfert automatique des claims du perdant, capitulation, match nul, anti-abus intégrés

### 🤝 Alliances (v5.0.0)

Bonus de puissance par allié (+500, +1 200, +2 500…), homes personnels étendus pour les membres de factions alliées.

### 🏦 Banque d'émeraudes

Coffre de faction partagé (GUI), historique des transactions, classement des plus riches (`/faction topbanque`).

### 🛒 Shop global

GUI paginé 45 items/page, recherche par mot-clé, tri par prix, monnaies : fer / or / diamant / émeraude.

### 🧹 Tri de coffre & inventaire (v5.3.0)

6 modes de tri avec aperçu avant confirmation, coffre partagé **et** inventaire personnel.

### 💱 Troc sécurisé

Échange d'items entre deux joueurs avec double confirmation anti-scam.

### 👁️ InvSee (admin)

Visualisation **en lecture seule** de l'inventaire complet d'un joueur.

### 📊 Statistiques joueurs

Kills, mobs tués, K/D, blocs posés/cassés, temps de jeu, top 10 par catégorie.

---

## 📥 Installation

1. Téléchargez la dernière version : **[FactionPlugin-5.10.0.jar](../../releases/download/v5.10.0/FactionPlugin-5.10.0.jar)**
2. Déposez le JAR dans le dossier `plugins/` de votre serveur **Paper 1.21.4**
3. Redémarrez le serveur — la configuration est générée dans `plugins/FactionPlugin/`

---

## ⚙️ Commandes principales (résumé)

| Commande | Description |
|---|---|
| `/faction create <nom>` | Créer une faction |
| `/faction info [faction]` | Voir les informations d'une faction |
| `/faction invite` / `join` / `leave` / `kick` | Gestion des membres |
| `/faction menu` | Interface graphique complète |
| `/faction souschef <action>` | Gérer les sous-chefs |
| `/faction recruter` | Recruter un villageois |
| `/faction villageois` / `formation` / `ranger cercle` | GUI villageois / formations |
| `/faction annuler` | Annuler une sélection en cours |
| `/faction guerre <action>` | Gestion des guerres ⚔️ |
| `/faction alliance <action>` | Gestion des alliances |
| `/faction setspawn` / `/faction spawn` | Spawn de faction |
| `/sethome` `/home` `/delhome` `/homes` | Homes personnels |
| `/tpa <joueur>` `/tpaccept` `/tpdeny` | Téléportation entre joueurs |
| `/faction claim` / `unclaim` / `claimmap` / `perms` | Territoire |
| `/faction banque` | Banque d'émeraudes |
| `/faction shop` / `vendre` / `acheter` / `mesannonces` | Shop global |
| `/faction ranger` / `ranger perso` | Tri de coffre / inventaire |
| `/faction troc <joueur>` | Troc sécurisé |
| `/faction stats` / `classementjoueurs` | Statistiques |
| `/faction classement` / `rangs` / `power` | Puissance |

🌾 = nouveauté v5.10.0 • ⚡ = construction instantanée • 🏃 = fuite intelligente • ⚔️ = guerre

---

## 🔐 Permissions

| Permission | Description | Défaut |
|---|---|---|
| `faction.use` | Commandes de base | tout le monde |
| `faction.admin` | InvSee, bypass des coffres privés, recrutement sans limite | op |

---

## 🛠️ Compilation

```bash
mvn clean package
```
Le JAR est généré dans `target/FactionPlugin-5.10.0.jar` (Java 21+, Maven 3.9+, Paper 1.21.4).

---

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.10.0** | **🌾 Nouveau rôle Récolteur** (mine/coupe/creuse/cultive tout seul) • ⚡ **Construction instantanée** (TP bloc-par-bloc + échafaudage bambou) • 📦 **Récolte en un passage** pour le Constructeur • 📍 **Guerrier immobile par défaut** • 🏃 **Fuite intelligente** (sous 25 % HP ou 3+ ennemis) • ⭕ **Formation en cercle** |
| v5.9.8 | Drop complet à la mort des villageois recrutés (équipement + réserve), désactivation du drop vanilla 💀 |
| v5.9.7 | Clic-droit direct sur le villageois, butin de guerre ramassé par le Guerrier, compteur de kills individuel |
| v5.9.6 | Point de rassemblement individuel + assignation de groupe pour Constructeurs & Guerriers |
| v5.9.5 | 5 niveaux d'XP pour les villageois, tag de faction coloré au-dessus de chaque recrue |
| v5.9.4 | Mode archerie pour les Guerriers (arc + flèches, recul auto, bascule en mêlée) + soin par le sommeil 🏹🛌 |
| v5.9.3 | Suivi longue distance, chantiers n'importe où sur la carte, assignations de groupe |
| v5.9.2 | File de chantiers Constructeur, récolte autonome, zones libres hors claims |
| v5.9.1 | Guerriers autonomes : poste, rayon, patrouille tracée, formation militaire, détection d'ennemi |
| v5.9.0 | Villageois recrutés : `/faction recruter`, GUI de gestion, rôles Constructeur & Guerrier |
| v5.8.4 | Sous-chefs : promotion / destitution, jusqu'à 2 sous-chefs |
| v5.3.0 | Tri de coffre & d'inventaire : 6 modes, GUI d'aperçu |
| v5.2.0 | Comptoir d'échange (retiré ensuite) |
| v5.1.1 | Guerre inter-factions avec enjeux négociables |
| v5.0.0 | Alliances, homes personnels, spawn, /tpa, coffres privés |
| v4.0.0 | Shop global paginé + InvSee admin |
| v3.2.x | Banque d'émeraudes, optimisations diverses |
| v3.1.0 | Fusion avec FactionStats : stats joueurs et classements intégrés |
| v2.0.0 | Système de puissance, rangs, classement des factions |
| v1.1.0 | GUI, téléportation intérieure, inventaire partagé |
| v1.0.0 | Version initiale |

---

## 📄 Licence

Ce projet est sous licence **MIT**.