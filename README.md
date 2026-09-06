# 🏰 FactionPlugin

> Le plugin Minecraft **tout-en-un** pour gérer des factions, déclarer des guerres, bâtir des alliances, recruter des villageois et bâtir des empires sur **Spigot / Paper 1.21**.

![Version](https://img.shields.io/badge/version-5.9.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Statut](https://img.shields.io/badge/status-stable-success)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** transforme votre serveur Minecraft en une véritable **épopée de factions**. Créez votre clan, recrutez vos membres, scellez des **alliances** ou partez en **guerre**, réclamez et défendez vos **territoires**, amassez une fortune dans la **banque d'émeraudes**, vendez vos trouvailles sur le **shop global**, troquez en toute sécurité avec les autres joueurs, organisez votre coffre et votre inventaire, et mesurez-vous aux autres factions grâce au **système de puissance** et à ses **7 rangs**.

Et depuis la **v5.9.0**, donnez vie à votre faction en **recrutant des villageois** qui construiront vos murs et défendront votre base pendant que vous explorez, minez ou affrontez vos ennemis.

Une seule commande pour tout faire : **`/faction`** (alias `/f`).

---

## 🌟 Nouveautés de la v5.9.0 — *Les Villageois recrutés*

Cette mise à jour introduit un tout nouveau système de **PNJ alliés** : vos villageois peuvent désormais rejoindre votre faction, recevoir un rôle, un équipement et des ordres — puis agir **autonomement** dans le monde.

### 🛡️ Deux rôles spécialisés

| Rôle | Comportement | Idéal pour… |
|---|---|---|
| 🪓 **Constructeur** | Reçoit un chantier (2 coins cliqués) dans un chunk claimé, et **comble automatiquement tout vide** qu'il rencontre dans la zone — il construit et répare en continu. | Bâtir des murs, remplir des trous, réparer des défenses. |
| ⚔️ **Guerrier** | Équipé d'une arme + d'une armure, il **repère les mobs hostiles à proximité**, les poursuit et les combat — défendant au passage les villageois et autres alliés autour de lui. | Protéger votre base, escorter vos coéquipiers, monter la garde. |

> Les dégâts et la résistance du guerrier dépendent de la qualité de son équipement : `bois < pierre/or < fer < diamant < netherite`.

### 🧑‍🌾 Commandes ajoutées

| Commande | Rôle requis | Effet |
|---|---|---|
| `/faction recruter` | Chef / sous-chef | Vise un villageois (à 8 blocs max) pour le convertir en unité de faction. |
| `/faction villageois` | Tout membre | Ouvre le **GUI** listant les villageois recrutés (nom, rôle, vie, statut). |

### 🎛️ GUI Villageois

Une interface claire pour chaque villageois recruté :

- **Renommer** (clic sur le nom → tape dans le chat)
- **Changer le rôle** : Aucun / Constructeur / Guerrier
- **Gérer l'équipement** :
  - Slots restreints selon le rôle (seuls des blocs pour le Constructeur, arme + armure aux bons emplacements pour le Guerrier)
  - Un emplacement **Nourriture** commun : le villageois se **soigne automatiquement** tant qu'il n'est pas à pleine vie
- **Définir le chantier** du Constructeur (2 clics dans un chunk claimé par votre faction, volume plafonné)
- **Libérer** un villageois (chef/sous-chef uniquement)

### 📏 Limites & équilibrage

- **Limite configurable** de villageois recrutés par faction (par défaut : **5**).
- Un villageois **meurt normalement** (mobs hostiles, PvP, etc.) : la faction est notifiée et l'unité disparaît des données.
- **Persistance complète** dans `villagers.yml` (rôle, équipement, chantier, ressources).

---

## 🆕 Nouveautés de la v5.8.4 — *Les Sous-chefs*

Le chef peut désormais **déléguer** une partie de son pouvoir à **jusqu'à 2 sous-chefs** :

- `/faction souschef promouvoir <joueur>` — nommer un sous-chef (chef uniquement)
- `/faction souschef retirer <joueur>` — retirer le rang (chef uniquement)
- `/faction souschef liste` — voir les sous-chefs actuels
- `/faction souschef limite <0-2>` — régler la limite (plafond absolu : 2)

### 🔑 Ce qu'un sous-chef peut faire

- ✅ Inviter des joueurs (`/faction invite`)
- ✅ Expulser des membres, **sauf le chef** (`/faction kick`)
- ✅ Proposer, accepter, refuser et rompre des **alliances**
- ✅ Déclarer, accepter et refuser des **guerres**
- ✅ Définir le **spawn de faction** (`/faction setspawn`)
- ✅ **Claim / unclaim** des chunks
- ✅ **Recruter des villageois** (nouveau !)

### 🔒 Réservé au chef uniquement

- `setchef` (transférer le leadership)
- `rename` (renommer la faction)
- `disband` (dissoudre la faction)
- `claimallow` / `claimdeny` (permissions de claim)
- `perms` (permissions)
- La **capitulation** en guerre (`/faction guerre capituler`)

---

## 🎯 Fonctionnalités principales (rappel)

### 🏰 Factions
- Création, invitation, expulsion, dissolution, transfert de chef
- **Sous-chefs** (jusqu'à 2, v5.8.4)
- **Inventaire partagé** (`/faction coffre`)
- **Menu GUI complet** (`/faction` ou `/faction menu`)

### ⚡ Système de puissance
- **Puissance Individuelle (PI)** basée sur le PvP, la survie, la progression et l'activité
- **Puissance Globale (PG)** : somme des PI + bonus de taille de faction
- **7 rangs** : Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire
- **Effets passifs** croissants : Speed, Strength, Resistance, Jump Boost…
- Classement (`/faction classement`, `/faction power`)

### 🗺️ Claims (territoire)
- Réclamez et protégez vos chunks
- Permissions par joueur via GUI (`/faction perms`)
- Autorisez vos **alliés** sur vos claims (`claimallies`)
- **Mini-map** visuelle (`/faction claimmap`)

### ⚔️ Guerres inter-factions (v5.1.1)
- Déclaration **négociée** : `claims:0-5`, `pillage`, `kills:5-50`
- Score en direct dans l'**action bar**
- Transfert automatique des claims du perdant
- Capitulation, match nul, anti-abus intégrés

### 🤝 Alliances (v5.0.0)
- Bonus de puissance par allié (+500, +1 200, +2 500…)
- Homes personnels étendus avec alliés

### 🏦 Banque d'émeraudes
- Coffre de faction partagé (GUI)
- Historique des transactions
- Classement des plus riches (`/faction topbanque`)

### 🛒 Shop global
- GUI paginé 45 items/page, recherche par mot-clé, tri par prix
- Monnaies : fer, or, diamant, émeraude
- Paiement automatique du vendeur

### 🧹 Tri de coffre & inventaire (v5.3.0)
- 6 modes de tri avec aperçu avant confirmation
- Coffre partagé **et** inventaire personnel

### 💱 Troc sécurisé
- Échange d'items entre deux joueurs avec double confirmation anti-scam

### 👁️ InvSee (admin)
- Visualisation **en lecture seule** de l'inventaire complet d'un joueur

### 📊 Statistiques joueurs
- `/faction stats [joueur]` : kills, mobs, K/D, blocs, temps de jeu…
- `/faction classementjoueurs` : top 10 par catégorie

---

## 📥 Installation

1. Téléchargez la dernière version : **[FactionPlugin-5.9.0.jar](../../releases/download/v5.9.0/FactionPlugin-5.9.0.jar)**
2. Déposez le JAR dans le dossier `plugins/` de votre serveur Paper 1.21
3. Redémarrez le serveur — la configuration est générée dans `plugins/FactionPlugin/`

---

## ⚙️ Commandes principales

| Commande | Description |
|---|---|
| `/faction create <nom>` | Créer une faction |
| `/faction info [faction]` | Voir les informations d'une faction |
| `/faction invite` / `join` / `leave` / `kick` | Gestion des membres |
| `/faction menu` | Interface graphique complète |
| `/faction souschef <action>` | Gérer les sous-chefs 🆕 |
| `/faction recruter` | Recruter un villageois visé 🆕 |
| `/faction villageois` | GUI des villageois recrutés 🆕 |
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

🆕 = nouveau en v5.8.x / v5.9.0 • ⚔️ = guerre (v5.1.1)

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
Le JAR est généré dans `target/FactionPlugin-5.9.0.jar` (Java 21+, Maven 3.9+).

---

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.9.0** | **Villageois recrutés** : `/faction recruter`, GUI de gestion, rôles Constructeur & Guerrier, chantier, équipement, nourriture 🆕 |
| **v5.8.4** | **Sous-chefs** : promotion / destitution, jusqu'à 2 sous-chefs, GUI mises à jour |
| v5.3.0 | Tri de coffre & d'inventaire : 6 modes, GUI d'aperçu |
| v5.2.0 | Comptoir d'échange (retiré ensuite) |
| v5.1.1 | Guerre inter-factions avec enjeux négociables |
| v5.0.0 | Alliances, homes personnels, spawn, /tpa, coffres privés |
| v4.0.0 | Shop global paginé + InvSee admin |
| v3.2.4 | Corrections et améliorations finales |
| v3.2.x | Optimisations du système de puissance, fixes du troc |
| v3.2.0 | Banque d'émeraudes, claims, commerce entre joueurs |
| v3.1.0 | Fusion avec FactionStats : stats joueurs et classements intégrés |
| v2.0.0 | Système de puissance, rangs, classement des factions |
| v1.1.0 | GUI, téléportation intérieure, inventaire partagé |
| v1.0.0 | Version initiale |

---

## 📄 Licence

Ce projet est sous licence **MIT**.
