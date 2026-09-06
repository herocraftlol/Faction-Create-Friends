# 🏰 FactionPlugin

> Le plugin Minecraft **tout-en-un** pour gérer des factions, déclarer des guerres, bâtir des alliances, recruter et commander des **villageois autonomes**, et bâtir des empires sur **Paper 1.21.4**.

![Version](https://img.shields.io/badge/version-5.9.8-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-green)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Statut](https://img.shields.io/badge/status-stable-success)
![Licence](https://img.shields.io/badge/licence-MIT-lightgrey)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** transforme votre serveur Minecraft en une véritable **épopée de factions**. Créez votre clan, recrutez vos membres, scellez des **alliances** ou partez en **guerre**, réclamez et défendez vos **territoires**, amassez une fortune dans la **banque d'émeraudes**, vendez vos trouvailles sur le **shop global**, troquez en toute sécurité avec les autres joueurs, organisez votre coffre et votre inventaire, et mesurez-vous aux autres factions grâce au **système de puissance** à **7 rangs**.

Depuis la **v5.9.0**, vos **villageois** peuvent rejoindre votre faction. Avec la **v5.9.8 — Tout tombe au sol à la mort**, ils montent en niveaux, gagnent de l'expérience, constituent leur propre réserve de guerre, et **ne perdent plus rien à la mort** : armes, armures, flèches, nourriture, matériaux — tout ce qu'ils avaient sur eux est désormais lâché au sol.

Une seule commande pour tout faire : **`/faction`** (alias `/f`).

---

## 💀 Nouveautés de la v5.9.8 — *Tout tombe au sol à la mort*

Une mise à jour centrée sur la **boucle d'IA de vos villageois recrutés** : moins de pertes, plus de personnalisation, plus d'autonomie.

### 💀 Drop complet à la mort d'un villageois recruté

Quand un villageois (Constructeur ou Guerrier) meurt, il **lâche désormais tout ce qu'il avait sur lui** :

- **Arme et armure** équipées
- **Arc et flèches** (pour les Guerriers)
- **Nourriture** stockée dans son emplacement dédié
- **Réserve** complète : matériaux de construction pour le Constructeur, ou butin de guerre ramassé pour le Guerrier

Plus rien n'est perdu silencieusement dans le vide. Vous pouvez récupérer son équipement sur son cadavre, ou le laisser à un autre villageois de passage.

> ⚙️ La mécanique vanilla de « chance de drop d'équipement » est désactivée sur les recrues (`disable-vanilla-equipment-drop: true` dans `config.yml`) pour éviter tout doublon : c'est désormais **100 % du drop qui est géré par le plugin**.

---

## ⭐ Récapitulatif des ajouts depuis la v5.9.4

| Fonctionnalité | v5.9.4 | v5.9.5 | v5.9.6 | v5.9.7 | **v5.9.8** |
|---|:---:|:---:|:---:|:---:|:---:|
| Mode archerie + soin par le sommeil | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Niveaux d'XP pour villageois (5 niveaux)** | ❌ | 🆕 | ✅ | ✅ | ✅ |
| **Tag de faction sur le nom des villageois** | ❌ | 🆕 | ✅ | ✅ | ✅ |
| **Point de rassemblement** | ❌ | ❌ | 🆕 | ✅ | ✅ |
| **Assignation de groupe (rassemblement commun)** | ❌ | ❌ | 🆕 | ✅ | ✅ |
| **Clic-droit direct sur le villageois = sa fiche** | ❌ | ❌ | ❌ | 🆕 | ✅ |
| **Butin de guerre ramassé par le Guerrier** | ❌ | ❌ | ❌ | 🆕 | ✅ |
| **Compteur de kills individuel par Guerrier** | ❌ | ❌ | ❌ | 🆕 | ✅ |
| **Drop complet à la mort (équipement + réserve)** | ❌ | ❌ | ❌ | ❌ | 💀 |

---

## 🛡️ Les deux rôles de villageois

| Rôle | Comportement | Idéal pour… |
|---|---|---|
| 🪓 **Constructeur** | Reçoit un chantier (2 coins cliqués) et **comble automatiquement tout vide** dans la zone. Monte de niveau en posant des blocs. | Bâtir des murs, remplir des trous, réparer des défenses. |
| ⚔️ **Guerrier** | Équipé d'une arme + armure (+ arc / flèches optionnels), fait sa **ronde autour de son poste** dans son rayon, **détecte et combat** les mobs hostiles et les joueurs ennemis. Monte de niveau au combat. | Protéger votre base, escorter, harceler l'ennemi, monter la garde. |

### 🧑‍🌾 Commandes ajoutées

| Commande | Rôle requis | Effet |
|---|---|---|
| `/faction recruter` | Chef / sous-chef | Convertir un villageois ciblé en unité de faction. |
| `/faction villageois` | Tout membre | Ouvre le **GUI** listant les villageois recrutés. |
| `/faction villageois ranger` *ou* `formation` | Chef / sous-chef | Met tous les villageois à 40 blocs en formation devant vous. |
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

1. Téléchargez la dernière version : **[FactionPlugin-5.9.8.jar](../../releases/download/v5.9.8/FactionPlugin-5.9.8.jar)**
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
| `/faction villageois` / `formation` | GUI villageois / formation militaire |
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

💀 = nouveauté v5.9.8 • 🏹 = v5.9.4 • 🛌 = sommeil & soin • ⚔️ = guerre

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
Le JAR est généré dans `target/FactionPlugin-5.9.8.jar` (Java 21+, Maven 3.9+, Paper 1.21.4).

---

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.9.8** | **Drop complet à la mort** des villageois recrutés (équipement + réserve), désactivation du drop vanilla 💀 |
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
