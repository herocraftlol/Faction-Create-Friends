# 🏰 FactionPlugin

> Le plugin Minecraft **tout-en-un** pour gérer des factions, déclarer des guerres, bâtir des alliances, recruter et commander des **villageois guerriers autonomes**, et bâtir des empires sur **Paper 1.21.4**.

![Version](https://img.shields.io/badge/version-5.9.4-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-green)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Statut](https://img.shields.io/badge/status-stable-success)
![Licence](https://img.shields.io/badge/licence-MIT-lightgrey)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** transforme votre serveur Minecraft en une véritable **épopée de factions**. Créez votre clan, recrutez vos membres, scellez des **alliances** ou partez en **guerre**, réclamez et défendez vos **territoires**, amassez une fortune dans la **banque d'émeraudes**, vendez vos trouvailles sur le **shop global**, troquez en toute sécurité avec les autres joueurs, organisez votre coffre et votre inventaire, et mesurez-vous aux autres factions grâce au **système de puissance** à **7 rangs**.

Depuis la **v5.9.0**, vos **villageois** peuvent rejoindre votre faction. Avec la **v5.9.1 — Les Guerriers prennent vie**, ils gagnent un véritable comportement autonome : poste de garde, rayon de défense, patrouille tracée, formation militaire, combat intelligent qui reconnaît vos alliés comme vos ennemis — y compris en pleine guerre inter-factions.

Avec la **v5.9.4 — Les Archers et le Sommeil réparateur**, vos Guerriers apprennent à **tirer à l'arc en gardant leurs distances**, et tous vos villageois recrutés **se soignent paisiblement quand ils dorment dans un lit**. De quoi bâtir une véritable garnison à votre base !

Une seule commande pour tout faire : **`/faction`** (alias `/f`).

---

## 🔥 Nouveautés de la v5.9.4 — *Les Archers & le Sommeil réparateur*

Deux ajouts qui changent le rythme de vie de vos villageois recrutés.

### 🏹 Mode archerie pour les Guerriers

Chaque Guerrier dispose désormais de **deux nouveaux emplacements** dans sa fiche : un slot **Arc** et un slot **Flèches**, plus un **toggle "Mode archerie"** qui passe l'unité en tir à distance.

- **Tir à distance** — quand le mode archerie est activé, qu'un arc est équipé et qu'il reste des flèches, le Guerrier **privilégie le tir à l'arc** et **recule automatiquement** si l'ennemi s'approche trop près.
- **Plage de combat idéale** — il maintient une distance d'engagement configurable (`archer-min-distance` / `archer-max-distance` dans `config.yml`) ; sortez-le de cette plage et il s'ajustera tout seul.
- **Bascule automatique en mêlée** — dès qu'il n'a plus de flèches, ou que l'ennemi est déjà au corps à corps, il **repasse instantanément en mode mêlée** : épée si équipée, sinon à mains nues. Vous n'avez rien à gérer.
- **Aucun changement** si le mode est désactivé : il reste au corps à corps comme avant — pour les configurations classiques.

> 💡 Donnez un arc + des flèches à vos Guerriers patrouillant à l'entrée de votre base : ils tiendront les assailants à distance pendant que les Constructeurs continuent de bâtir derrière eux.

### 🛌 Soin par le sommeil

Quand un villageois recruté va dormir dans un lit (via l'IA vanille de Minecraft, après une longue journée de construction ou de garde), il **regagne progressivement de la vie** pendant qu'il dort.

- **Soin périodique** — tant qu'il reste au lit, ses points de vie remontent par petites doses (`heal-per-sleep-tick`) à intervalles réguliers (`sleep-heal-period`, par défaut toutes les 3 s).
- **Détection automatique** — la détection se fait dans la boucle d'IA sur `LivingEntity.isSleeping()`. Aucun plugin externe, aucune configuration spéciale.
- **Arrêt au réveil** — il n'est pas immortel pour autant : dès qu'il se réveille, le soin reprend sa consommation normale de nourriture.
- **Paramétrable** — tous les seuils (quantité, durée, fréquence) sont exposés dans la section `villager` du `config.yml` pour s'adapter à l'équilibrage de votre serveur.

> 🛏️ Une garnison qui dort, c'est une garnison qui repart en pleine forme : plus besoin de surveiller la barre de vie de chaque villageois, ils se maintiennent tout seuls tant qu'ils ont un lit à disposition.

---

## 🆕 Récapitulatif des ajouts — v5.9.4 vs v5.9.1

| Fonctionnalité | v5.9.3 | **v5.9.4** |
|---|:---:|:---:|
| Toutes les fonctionnalités villageois (poste, rayon, patrouille…) | ✅ | ✅ |
| **Slot Arc + Flèches dans la fiche du Guerrier** | ❌ | 🆕 |
| **Toggle "Mode archerie" (ON / OFF)** | ❌ | 🆕 |
| **Tir à distance avec recul automatique** | ❌ | 🆕 |
| **Bascule automatique en mêlée si plus de flèches** | ❌ | 🆕 |
| **Soin automatique pendant le sommeil** | ❌ | 🆕 |

---

## 🛡️ Les deux rôles de villageois

| Rôle | Comportement | Idéal pour… |
|---|---|---|
| 🪓 **Constructeur** | Reçoit un chantier (2 coins cliqués) et **comble automatiquement tout vide** dans la zone. | Bâtir des murs, remplir des trous, réparer des défenses. |
| ⚔️ **Guerrier** | Équipé d'une arme + armure (+ arc / flèches optionnels), fait sa **ronde autour de son poste** dans son rayon, **détecte et combat** les mobs hostiles et les joueurs ennemis. **Se soigne la nuit en dormant.** | Protéger votre base, escorter, harceler l'ennemi, monter la garde. |

### 🧑‍🌾 Commandes ajoutées

| Commande | Rôle requis | Effet |
|---|---|---|
| `/faction recruter` | Chef / sous-chef | Convertir un villageois ciblé en unité de faction. |
| `/faction villageois` | Tout membre | Ouvre le **GUI** listant les villageois recrutés. |
| `/faction villageois ranger` *ou* `formation` | Chef / sous-chef | Met tous les villageois à 40 blocs en formation devant vous. |
| `/faction annuler` | Tout joueur | Annule la sélection de patrouille / chantier en cours. |

---

## 🆕 Nouveautés de la v5.9.1 — *Les Guerriers prennent vie*

Cette mise à jour transforme vos villageois recrutés en de véritables **soldats autonomes**. Chaque Guerrier peut désormais être posté, configuré et envoyé au combat avec une seule interaction dans le **GUI de gestion**.

### ⚔️ Poste de garde (slot 36)
Définissez en un clic le **poste** d'un guerrier : le villageois y reviendra automatiquement s'il s'en éloigne.

### 📍 Rayon de défense (slot 37)
Le guerrier patrouille dans un **rayon configurable de 4 à 48 blocs** autour de son poste. S'il détecte une menace, il abandonne sa ronde pour l'affronter (jusqu'à 24 blocs de poursuite), puis reprend sa position.

### 🗺️ Patrouille sur zone (slot 38)
Tracez **deux points** dans le monde : le guerrier y effectuera **des allers-retours autonomes**.

### 🛡️ Combat intelligent (slot 39)
Active ou désactive le mode combat. Quand il est actif, le guerrier :

- **Repère** automatiquement les mobs hostiles **et** les joueurs ennemis de votre faction.
- **Vérifie l'état de guerre** via `WarSession`.
- **Poursuit** sa cible jusqu'à 24 blocs, puis reprend sa patrouille.
- **Se soigne** via l'emplacement *Nourriture* tant qu'il n'est pas à pleine vie.

### 🪖 Formation militaire — `/faction villageois formation`
Alignez tous les villageois recrutés à moins de 40 blocs **devant vous**, prêts au combat.

### 🧠 Comportement de suivi (slot 41)
Cliquez sur la tête de votre villageois pour qu'il **vous suive** comme un compagnon loyal.

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

1. Téléchargez la dernière version : **[FactionPlugin-5.9.4.jar](../../releases/download/v5.9.4/FactionPlugin-5.9.4.jar)**
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

🏹 = nouveauté v5.9.4 • 🛌 = sommeil & soin • ⚔️ = guerre

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
Le JAR est généré dans `target/FactionPlugin-5.9.4.jar` (Java 21+, Maven 3.9+, Paper 1.21.4).

---

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.9.4** | **Mode archerie** pour les Guerriers (arc + flèches, recul auto, bascule en mêlée) + **soin par le sommeil** pour tous les villageois recrutés 🏹🛌 |
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
