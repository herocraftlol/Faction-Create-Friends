# ⚔️ FactionPlugin

> Plugin Minecraft **tout-en-un** de gestion de factions pour serveurs **Spigot / Paper 1.20.4+**

[![Version](https://img.shields.io/badge/version-4.0.0-blue)](https://github.com/herocraftlol/Faction-Create-Friends/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.20.4+-green)](https://www.spigotmc.org/)
[![Licence](https://img.shields.io/badge/licence-MIT-yellow)](LICENSE)

---

## 📖 Description

**FactionPlugin** regroupe en un seul plugin tout ce qu'il faut pour faire vivre des factions sur votre serveur : création de factions, **claims de territoire**, **banque d'émeraudes**, **échanges sécurisés**, **statistiques joueurs**, **système de puissance et de rangs**, un **marché global** entre joueurs, et un outil d'administration **InvSee**. Le tout piloté par une commande unique : `/faction` — simple pour les joueurs, puissant pour les administrateurs.

---

## ✨ Fonctionnalités

### 🏛️ Gestion de Factions
- Création, invitation, expulsion, transfert de chef et dissolution
- GUI interactif complet via `/faction`
- Inventaire partagé et téléportation entre membres

### 🗺️ Claims (Territoire)
- Réclamez des chunks pour protéger votre territoire
- Permissions par joueur configurables (GUI dédié)
- Coût progressif en émeraudes (configurable dans `config.yml`)

### 🏦 Banque d'Émeraudes
- Coffre de faction partagé avec historique des transactions
- Dépôts/retraits via une GUI intuitive
- Restrictions de retrait configurables (chef uniquement ou tous les membres)

### 💱 Échanges Sécurisés (Trade)
- Échange d'items et d'émeraudes entre deux joueurs via GUI
- Double confirmation, annulation à tout moment
- Protection anti-scam

### ⚡ Système de Puissance
- Chaque joueur génère une Puissance Individuelle (PvP, survie, activité)
- Les factions cumulent la puissance de leurs membres
- **7 rangs de faction** (Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire) avec effets passifs

### 📊 Statistiques Joueurs
- `/faction stats [joueur]` — kills, K/D, mobs tués, dégâts, blocs, temps de jeu
- `/faction classementjoueurs <catégorie>` (alias `cj`) — top 10 joueurs par catégorie
- Fonctionne aussi pour les joueurs hors ligne

### 🛒 Shop Global *(nouveauté v4.0.0)*
- Marché serveur accessible avec `/faction shop`
- GUI paginé (45 items/page) avec **recherche par mot-clé** et **tri par prix**
- 4 monnaies acceptées : fer, or, diamant, émeraude
- Paiement automatique au vendeur (même hors-ligne, livré à la reconnexion)
- `/faction vendre <prix> <monnaie>`, `/faction acheter <ID>`, `/faction recuperer`, `/faction mesannonces`

### 👁️ InvSee *(nouveauté v4.0.0)*
- `/faction invsee <joueur>` — consultation d'inventaire en **lecture seule**
- Réservé aux administrateurs (`faction.admin`)

---

## 📦 Installation

1. Téléchargez la dernière version depuis la [page des releases](https://github.com/herocraftlol/Faction-Create-Friends/releases)
2. Déposez `FactionPlugin-X.X.X.jar` dans le dossier `plugins` de votre serveur
3. Redémarrez le serveur — la configuration est générée dans `plugins/FactionPlugin/`

---

## 🎮 Commandes

| Commande | Description |
|---|---|
| `/faction create <nom>` | Créer une faction |
| `/faction invite <joueur>` | Inviter un joueur |
| `/faction join <faction>` | Rejoindre une faction |
| `/faction leave` | Quitter sa faction |
| `/faction info` | Infos de sa faction |
| `/faction stats [joueur]` | Statistiques joueur |
| `/faction classementjoueurs <catégorie>` | Classement des joueurs |
| `/faction classement` | Classement des factions (GUI) |
| `/faction shop` | Ouvrir le marché global |
| `/faction vendre <prix> <monnaie>` | Vendre l'item en main |
| `/faction acheter <ID>` | Acheter par ID |
| `/faction recuperer [ID]` | Récupérer une annonce |
| `/faction mesannonces` | Voir ses annonces |
| `/faction invsee <joueur>` | Voir l'inventaire d'un joueur (admin) |

## 🔑 Permissions

| Permission | Description | Défaut |
|---|---|---|
| `faction.use` | Utiliser les commandes de faction | tous |
| `faction.admin` | Commandes admin (bypass claims, invsee) | opérateurs |

---

## ⚙️ Configuration

Le `config.yml` permet de personnaliser :
- Tous les messages du plugin (préfixe, format)
- Limites de nom et taille des factions
- Coût des claims (`base-cost` + `cost-increment`)
- Restriction de retrait de la banque (`only-chef-can-withdraw-faction`)

---

## 🛠️ Compilation

Prérequis : **Java 17+** et **Maven 3.9+**

```bash
mvn clean package
```

Le JAR est généré dans `target/FactionPlugin-X.X.X.jar`.

---

## 📜 Historique des Versions

| Version | Nouveautés |
|---|---|
| **4.0.0** | 🛒 Shop Global paginé (recherche + tri) · 👁️ InvSee admin |
| 3.2.4 | Corrections et améliorations finales |
| 3.2.3 | Améliorations utilitaires (MobUtils) |
| 3.2.2 | Optimisations du système de puissance |
| 3.2.1 | Corrections du système de Trade |
| 3.2.0 | 🏦 Banque d'émeraudes · 🗺️ Claims · 💱 Trade |
| 3.1.0 | 📊 Fusion avec FactionStats (stats + classements joueurs) |
| 2.0.0 | ⚡ Système de Puissance, Rangs et Classement |
| 1.1.0 | GUI, téléportation et inventaire partagé |
| 1.0.0 | Version initiale |

📄 Voir aussi : [CHANGELOG v4](CHANGELOG_v4.md) · [CHANGELOG Fusion](CHANGELOG_FUSION.md)

---

## 📄 Licence

Ce projet est sous licence MIT.
