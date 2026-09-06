# 🏰 FactionPlugin

> Le plugin Minecraft **tout-en-un** pour gérer des factions, déclarer des guerres, bâtir des alliances, recruter et commander des **villageois guerriers** autonomes, et bâtir des empires sur **Paper 1.21**.

![Version](https://img.shields.io/badge/version-5.9.1-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Statut](https://img.shields.io/badge/status-stable-success)
![Licence](https://img.shields.io/badge/licence-MIT-lightgrey)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** transforme votre serveur Minecraft en une véritable **épopée de factions**. Créez votre clan, recrutez vos membres, scellez des **alliances** ou partez en **guerre**, réclamez et défendez vos **territoires**, amassez une fortune dans la **banque d'émeraudes**, vendez vos trouvailles sur le **shop global**, troquez en toute sécurité avec les autres joueurs, organisez votre coffre et votre inventaire, et mesurez-vous aux autres factions grâce au **système de puissance** à **7 rangs**.

Et depuis la **v5.9.0**, vos **villageois** peuvent rejoindre votre faction. Avec la **v5.9.1 — Les Guerriers**, ils prennent véritablement vie : **poste de garde**, **rayon de défense**, **patrouille sur zone tracée**, **formation militaire**, **combat intelligent** qui reconnaît vos alliés comme vos ennemis — y compris en pleine guerre inter-factions.

Une seule commande pour tout faire : **`/faction`** (alias `/f`).

---

## 🔥 Nouveautés de la v5.9.1 — *Les Guerriers prennent vie*

Cette mise à jour transforme vos villageois recrutés en de véritables **soldats autonomes**. Chaque guerrier peut désormais être posté, configuré et envoyé au combat avec une seule interaction dans le **GUI de gestion**.

### ⚔️ Poste de garde (slot 36)

Définissez en un clic le **poste** d'un guerrier : le villageois y reviendra automatiquement s'il s'en éloigne. Idéal pour garder l'entrée d'une base, un pont, un portail ou un point stratégique.

### 📍 Rayon de défense (slot 37)

Le guerrier patrouille autour de son poste dans un **rayon configurable de 4 à 48 blocs**. S'il détecte un mob hostile ou un joueur ennemi de votre faction, il **abandonne sa ronde pour l'affronter**, puis reprend sa position. Réglable directement depuis le GUI ou via le chat.

### 🗺️ Patrouille sur zone (slot 38)

Pour les bases complexes, tracez **deux points** dans le monde : le guerrier y effectuera **des allers-retours autonomes** en restant à l'intérieur du périmètre ainsi défini. Sélection annulable à tout moment avec `/faction annuler`.

### 🛡️ Combat intelligent (slot 39)

Activez ou désactivez le mode combat. Quand il est actif, le guerrier :

- **Repère** automatiquement les mobs hostiles **et** les joueurs ennemis de votre faction (vérification via `FactionManager`).
- **Vérifie l'état de guerre** : pendant une `WarSession`, les factions belligérantes sont automatiquement traitées comme hostiles.
- **Poursuit** sa cible jusqu'à 24 blocs, l'attaque avec son arme équipée, puis reprend sa patrouille.
- **Soin automatique** via l'emplacement *Nourriture* du GUI tant qu'il n'est pas à pleine vie.

### 🪖 Formation militaire — `/faction villageois formation`

Alignez en un clin d'œil **tous les villageois recrutés dans un rayon de 40 blocs** devant vous, à 1,5 blocs d'espacement, prêts à partir au combat ou à défiler. Parfait pour les cérémonies, les entraînements, ou les embuscades.

### 🧠 Comportement de suivi (slot 41)

Cliquez sur la tête de votre villageois pour qu'il **vous suive** comme un compagnon loyal. Re-cliquez pour annuler. Très utile pour escorter un Constructeur jusqu'à un nouveau chantier, ou simplement vous déplacer avec votre garde personnelle.

### 🩹 Correctif du double-clic

Les anciens écrans souffraient d'un bug où deux clics rapides exécutaient deux actions (le second cliquait à travers le menu qui se fermait). Le **double-clic est désormais neutralisé** par un *cooldown GUI de 150 ms* côté client, ce qui rend toutes les interactions fluides et prévisibles.

---

## 🆕 Récapitulatif des ajouts — v5.9.1 vs v5.9.0

| Fonctionnalité | v5.9.0 (Villageois recrutés) | **v5.9.1 (Guerriers)** |
|---|:---:|:---:|
| `/faction recruter` — convertir un villageois en unité | ✅ | ✅ |
| `/faction villageois` — GUI de gestion | ✅ | ✅ |
| Rôles (Aucun / Constructeur / Guerrier) | ✅ | ✅ |
| Chantier du Constructeur (zone cliquée) | ✅ | ✅ |
| Équipement par rôle + Nourriture soignant | ✅ | ✅ |
| Limite configurable par faction | ✅ | ✅ |
| **Poste de garde** | ❌ | 🆕 |
| **Rayon de défense (4 – 48 blocs)** | ❌ | 🆕 |
| **Patrouille tracée par 2 clics** | ❌ | 🆕 |
| **Détection d'ennemi via la faction** | ❌ | 🆕 |
| **Compatibilité avec les guerres** | ❌ | 🆕 |
| **Formation militaire (`/faction villageois formation`)** | ❌ | 🆕 |
| **Mode suivi du chef/du joueur** | ❌ | 🆕 |
| **Correctif double-clic des GUIs** | ❌ | 🆕 |

---

## 🛡️ Les deux rôles disponibles

| Rôle | Comportement | Idéal pour… |
|---|---|---|
| 🪓 **Constructeur** | Reçoit un chantier (2 coins cliqués) dans un chunk claimé et **comble automatiquement tout vide** dans la zone. | Bâtir des murs, remplir des trous, réparer des défenses. |
| ⚔️ **Guerrier** | Équipé d'une arme + d'une armure, il fait sa **ronde autour de son poste** dans son rayon, **détecte et combat** les mobs hostiles et les joueurs ennemis (vérification de faction + état de guerre). | Protéger votre base, escorter, harceler l'ennemi, monter la garde. |

> Les dégâts et la résistance du guerrier dépendent de la qualité de son équipement : `bois < pierre/or < fer < diamant < netherite`.

### 🧑‍🌾 Commandes ajoutées

| Commande | Rôle requis | Effet |
|---|---|---|
| `/faction recruter` | Chef / sous-chef | Convertir un villageois ciblé en unité de faction. |
| `/faction villageois` | Tout membre | Ouvre le **GUI** listant les villageois recrutés. |
| `/faction villageois ranger` *ou* `formation` | Chef / sous-chef | Met tous les villageois à 40 blocs en formation devant vous. |
| `/faction annuler` | Tout joueur | Annule la sélection de patrouille / chantier en cours. |

---

## 🆕 Nouveautés de la v5.8.4 — *Les Sous-chefs*

Le chef peut désormais **déléguer** une partie de son pouvoir à **jusqu'à 2 sous-chefs** :

- `/faction souschef promouvoir <joueur>` — nommer un sous-chef (chef uniquement)
- `/faction souschef retirer <joueur>` — retirer le rang (chef uniquement)
- `/faction souschef liste` — voir les sous-chefs actuels
- `/faction souschef limite <0-2>` — régler la limite (plafond absolu : 2)

🔑 **Ce qu'un sous-chef peut faire** : inviter, expulser (sauf le chef), alliances, déclarations de guerre, définir le spawn, claim / unclaim, recruter des villageois.

🔒 **Réservé au chef** : `setchef`, `rename`, `disband`, `claimallow` / `claimdeny`, `perms`, capitulation en guerre.

---

## 🎯 Fonctionnalités principales

### 🏰 Factions
Création, invitation, expulsion, dissolution, transfert de chef, **sous-chefs** (jusqu'à 2), **inventaire partagé** (`/faction coffre`), **menu GUI complet** (`/faction` ou `/faction menu`).

### ⚡ Système de puissance
- **Puissance Individuelle (PI)** : PvP + survie + progression + activité
- **Puissance Globale (PG)** : somme des PI + bonus de taille
- **7 rangs** : Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire
- Effets passifs croissants : Speed, Strength, Resistance, Jump Boost, Haste, Regeneration
- Classements (`/faction classement`, `/faction power`)

### 🗺️ Claims (territoire)
Chunks protégés, permissions par joueur (`/faction perms`), alliés autorisés (`claimallies`), mini-map visuelle (`/faction claimmap`).

### ⚔️ Guerres inter-factions (v5.1.1)
- Déclaration **négociée** : `claims:0-5`, `pillage`, `kills:5-50`
- Score en direct dans l'**action bar**
- Les villageois guerriers (v5.9.1) traitent automatiquement les belligérants comme hostiles
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

1. Téléchargez la dernière version : **[FactionPlugin-5.9.1.jar](../../releases/download/v5.9.1/FactionPlugin-5.9.1.jar)**
2. Déposez le JAR dans le dossier `plugins/` de votre serveur **Paper 1.21**
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
| `/faction villageois` / `formation` | GUI villageois / formation militaire 🆕 |
| `/faction annuler` | Annuler une sélection en cours 🆕 |
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

🆕 = nouveauté v5.9.1 • ⚔️ = guerre (v5.1.1)

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
Le JAR est généré dans `target/FactionPlugin-5.9.1.jar` (Java 21+, Maven 3.9+).

---

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.9.1** | **Guerriers autonomes** : poste, rayon, patrouille tracée, formation militaire, détection d'ennemi, compatibilité guerre, suivi du chef, correctif double-clic 🆕 |
| v5.9.0 | Villageois recrutés : `/faction recruter`, GUI de gestion, rôles Constructeur & Guerrier, chantier, équipement, nourriture |
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
