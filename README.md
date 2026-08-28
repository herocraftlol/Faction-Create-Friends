# 🏰 FactionPlugin

> Plugin Minecraft **tout-en-un** de gestion de factions pour serveur **Spigot/Paper 1.20.4** — factions, alliances, **guerres inter-factions**, claims, économie, commerce, **tri de coffre** et plus encore !

![Version](https://img.shields.io/badge/version-5.3.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.20.4-green)
![Java](https://img.shields.io/badge/java-17%2B-orange)

## 📖 Description

**FactionPlugin** transforme votre serveur Minecraft en une véritable expérience de factions : créez votre faction, recrutez des membres, forgez des **alliances stratégiques**, déclarez la **guerre** aux factions rivales et arrachez-leur leurs territoires, réclamez et protégez vos **claims**, amassez des richesses dans la **banque d'émeraudes**, commercez avec les autres joueurs via le **shop global**, troquez en sécurité avec les autres joueurs, gardez votre **coffre partagé et votre inventaire parfaitement rangés**, et mesurez-vous aux autres factions grâce au **système de puissance** et ses **7 rangs** (de Pierre à Légendaire).

Le tout avec des **interfaces graphiques (GUI)** intuitives et une commande unique : **`/faction`** (alias `/f`).

## ✨ Nouveautés de la v5.3.0

### 🧹 Tri de coffre & d'inventaire (`/faction ranger`)

Fini le coffre partagé en bazar ! La v5.3.0 ajoute un **moteur de tri complet** qui range automatiquement votre **coffre partagé de faction** et votre **inventaire personnel**.

- **6 modes de tri** disponibles dans un **GUI dédié** :
  - **⬡ Similaires regroupés** — fusionne les stacks identiques en un seul paquet
  - **☰ Par catégorie** — regroupe par type : blocs, outils, armures, nourriture, matériaux, potions, redstone, livres…
  - **🔤 Alphabétique A→Z** — trie par nom d'item
  - **📦 Quantité ↓ / ↑** — du plus grand au plus petit stack (ou l'inverse)
  - **✦ Par rareté** — items enchantés et rares en premier
- **Aperçu avant confirmation** : le GUI affiche le nombre d'items présents et le mode choisi avant d'appliquer le tri
- **Bouton « Organiser le coffre »** directement accessible dans le menu principal et dans le coffre partagé
- **Inventaire personnel** : triez aussi votre propre inventaire, hotbar exclue pour ne rien déplacer de sensible

#### 🖥️ Comment ça marche
- `/faction ranger` (ou `/fac trier`, `/fac organiser`, `/fac sort`) ouvre le menu de tri pour le **coffre partagé**
- `/faction ranger perso` (alias `inventaire`) ouvre le tri pour votre **inventaire personnel**
- Choisissez un mode → un **aperçu** s'affiche → confirmez : le tri est appliqué instantanément et l'inventaire est mis à jour

### 🔧 Corrections et améliorations
- Correction du message d'acceptation `/tpa` (affichage propre du nom du demandeur)
- Nettoyage et fiabilisation du tri (fusion des stacks, gestion des items endommagés)

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

### 🧹 Tri de coffre & d'inventaire
- **6 modes de tri** (similaires, catégorie, alphabétique, quantité, rareté) avec GUI dédié et aperçu avant confirmation
- Coffre partagé de faction et inventaire personnel
- `/faction ranger` • `/faction ranger perso` • bouton « Organiser le coffre » dans le menu principal

### 💱 Troc sécurisé
- Échange d'items entre deux joueurs avec double confirmation anti-scam (`/faction troc`)

### 👁️ InvSee (admin)
- Visualisation **en lecture seule** de l'inventaire complet d'un joueur (`/faction invsee <joueur>`)

### 📊 Statistiques joueurs
- `/faction stats [joueur]` : kills, mobs, K/D, blocs, temps de jeu... même pour les joueurs hors-ligne
- `/faction classementjoueurs` : top 10 par catégorie

## 📥 Installation

1. Téléchargez la dernière version : **[FactionPlugin-5.3.0.jar](../../releases/download/v5.3.0/FactionPlugin-5.3.0.jar)**
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
| `/faction ranger` / `ranger perso` | Tri de coffre partagé / inventaire personnel 🧹 |
| `/faction troc <joueur>` | Troc sécurisé |
| `/faction stats [joueur]` / `classementjoueurs` | Statistiques |
| `/faction classement` / `rangs` / `power` | Puissance et classements |

🧹 = nouveau en v5.3.0 • ⚔️ = guerre, ajouté en v5.1.1

## 🔐 Permissions

| Permission | Description | Défaut |
|---|---|---|
| `faction.use` | Utiliser les commandes de faction | tout le monde |
| `faction.admin` | InvSee, bypass claims, ouvrir les coffres privés | op |

## 🛠️ Compilation

```bash
mvn clean package
```
Le JAR est généré dans `target/FactionPlugin-5.3.0.jar` (Java 17+, Maven 3.9+).

## 📜 Historique des versions

| Version | Nouveautés |
|---|---|
| **v5.3.0** | Tri de coffre & d'inventaire : 6 modes de tri avec GUI et aperçu, bouton « Organiser le coffre », fix /tpa |
| **v5.2.0** | Comptoir d'échange : ordres de dépôt de monnaie contre items, GUI paginé, gestion et annulation de vos ordres *(retiré dans le code source v5.3.0)* |
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
