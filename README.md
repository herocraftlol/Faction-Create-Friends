# 🏰 FactionPlugin

> **Plugin Minecraft tout-en-un pour Paper 1.21.x** — Factions, alliances, guerres, claims, villages autonomes, banque d'émeraudes, troc sécurisé, shop global, statistiques et bien plus encore.

![Version](https://img.shields.io/badge/version-5.10.3-brightgreen) ![Paper](https://img.shields.io/badge/Paper-1.21.x-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![License](https://img.shields.io/badge/license-MIT-green)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** est un plugin Minecraft complet qui transforme votre serveur Paper en un véritable univers de factions. Pensé pour les serveurs survie PvP, il rassemble dans une seule commande `/faction` tout ce qu'il faut pour gérer un mode factions riche : territoires, diplomatie, économie, commerce, statistiques, et même des **villageois recrutés** autonomes qui construisent, combattent et récoltent pour vous.

Conçu pour Paper **1.21.4** (API Bukkit + Paper), Java **21**, et prêt à l'emploi : il suffit de poser le `.jar` dans `plugins/`.

---

## 🆕 Nouveautés de la v5.10.3 — *Chat en couleur rétabli* 🎨💬

Cette version règle un problème très visible en jeu : depuis le passage à Paper 1.21 et au chat signé (`Component`), la couleur et le préfixe de faction dans le tchat mondial avaient **disparu silencieusement**, parce que l'API historique `AsyncPlayerChatEvent#setFormat()` n'est plus vraiment respectée.

- **🎨 Couleurs et préfixe de faction rétablis dans le tchat** : passage de `AsyncPlayerChatEvent#setFormat(...)` à la nouvelle API Paper `io.papermc.paper.event.player.AsyncChatEvent` + `event.renderer(...)`. Le rendu reconstruit un vrai `Component` (donc compatible avec le chat signé par le client) tout en gardant le **contenu du message tel que tapé par le joueur** — sans y toucher.
- **🪖 Icône de guerre en préfixe** : si ta faction est en guerre, un tag rouge ⚔ apparaît automatiquement devant le préfixe pour le signaler à tous.
- **⭐ Icône de rang Légendaire** : les joueurs au rang max ont `[⚜]` en doré devant leur nom.
- **🔧 Soin pendant le sommeil** : le polling `Villager#isSleeping()` détecte maintenant correctement le coucher (l'événement Bukkit `EntitySleepEvent` n'est plus jamais lancé sur Paper 1.21+) et déclenche la régénération nocturne — comme en v5.9.x.
- **🔧 Petite compilation propre** : suppression du `@EventHandler` cassé sur `EntitySleepEvent` (n'existe plus dans l'API), et la régénération reste inline dans la boucle d'IA principale (`sleepPolling`).
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### Rendu concret du tchat en jeu

```
[⚔][⚜] [TitanS] Steve : on doit riposter ce soir
[◆] [TitanS] Alex  : j'ai déjà 12 obsidienne en stock
[∅]   Billy        : salut, on se voit demain
```

> ℹ️ Tout ce qui faisait la joie des versions précédentes reste évidemment présent : verrou-gui des villageois, indicateurs visuels dans les GUIs, mains occupées, anti-disparition d'objets, Récolteur, guerre automatique, etc.

---

## 📥 Installation

1. Téléchargez la dernière release : [**FactionPlugin-5.10.3.jar**](../../releases/latest)
2. Placez le fichier dans le dossier `plugins/` de votre serveur Paper 1.21.4+
3. Démarrez (ou redémarrez) le serveur — la configuration se génère automatiquement dans `plugins/FactionPlugin/`
4. Configurez `config.yml` selon vos besoins (messages, limites, coûts, etc.)

> 🛠️ Requis : serveur **Paper 1.21.4+**, **Java 21+**, aucun autre plugin de factions requis.

---

## 🎮 Fonctionnalités principales

### 🏛️ Système de factions complet
- Création, dissolution, renommage de factions
- Gestion fine des membres : invitation, expulsion, transfert de chef
- Système de **sous-chefs** (jusqu'à 2) avec permissions granulaires
- Chat de faction, ranks visuels, GUI intuitive
- Classement des factions par puissance

### ⚔️ Alliances & Guerres
- Proposez, acceptez, refusez et rompez des **alliances** avec d'autres factions
- Déclarez, acceptez et refusez des **guerres**
- Pendant la guerre : défense automatique, riposte, **pillage du coffre du vaincu** (si négocié)
- Bouton **"Capituler"** pour le chef uniquement (abandon propre)

### 🗺️ Système de claims & territoire
- Claim/unclaim de chunks avec permissions par joueur
- GUI dédiée pour gérer qui peut construire/casser où
- Alliances → permissions croisées configurables
- **Mini-map de faction** : carte visuelle temps réel de vos claims (carte vanilla Minecraft augmentée)

### 👥 Villages de faction (la grosse nouveauté)
Recrutez des villageois vanilla et attribuez-leur un rôle : ils deviennent autonomes !

| Rôle | Fait quoi ? |
|------|-------------|
| 🔨 **Constructeur** | Remplit des zones définies (châteaux, murs, repairs). Pose des blocs, se téléporte instantanément, monte un échafaudage pour les endroits difficiles. |
| ⚔️ **Guerrier** | Patrouille, défend un périmètre, attaque les mobs hostiles et les joueurs ennemis en guerre. Riposte automatique. Mode mêlée/archerie au choix. |
| 🌾 **Récolteur** | Mine, coupe du bois, creuse ou récolte des cultures selon l'outil qu'on lui donne. Dépose le butin dans un coffre, replante tout seul. |

Bonus :
- 5 **niveaux d'expérience** par villageois, avec soins automatiques et bonus de stats
- **Butin de guerre** : les guerriers ramassent automatiquement l'équipement de leurs victimes
- **Indicateurs visuels** dans les GUIs pour ne plus perdre d'objets par erreur
- **🔒 Verrou de GUI** (depuis la v5.10.2, toujours actif en 5.10.3) : un seul joueur à la fois peut gérer un villageois, sans conflit

### 💰 Économie intégrée
- **Banque d'émeraudes** par faction : dépôt, retrait, accès réservé aux membres autorisés
- **Shop global** paginé avec recherche par mot-clé et tri par prix (4 monnaies : fer, or, diamant, émeraude)
- Paiement automatique au vendeur, livraison à la reconnexion si hors-ligne
- Système d'**annonces** avec `/faction vendre` et `/faction acheter`

### 🤝 Troc sécurisé entre joueurs
- Interface GUI dédiée : chacun pose ce qu'il propose et ce qu'il veut
- **Confirmation des deux parties** requise pour finaliser
- Annulation possible à tout moment, **anti-scam** garanti

### 🏠 Homes & téléportation
- `/sethome`, `/home`, `/delhome`, `/homes` — homes personnels
- Nombre de homes selon le rang (1 sans faction → 5 au rang max)
- **TPA** entre joueurs avec warmup et cooldown
- **Spawn de faction** (1 ou 2 selon le rang, configurable)

### ⚡ Système de puissance & rangs
- 7 rangs de faction : **Pierre → Bronze → Argent → Or → Diamant → Émeraude → Légendaire**
- La puissance globale est calculée à partir des stats individuelles
- Chaque rang apporte des **effets passifs** : Speed, Strength, Resistance, Jump Boost, Haste, Regeneration
- Bonus selon la taille de la faction

### 📊 Statistiques & classements
- `/faction stats [joueur]` : kills, mobs tués, dégâts, blocs, temps de jeu, K/D ratio, advancements
- `/faction classementjoueurs <cat>` : top 10 par catégorie (`mobs`, `pvp`, `morts`, `blocs`, `temps`, `dommages`, `kd`, `advancements`)
- `/faction classement` : top 10 des factions par puissance (GUI)
- Persistance complète dans `stats.yml`

### 🔌 Liaisons externes (web map / site)
- `/lier [statut]` : lie ton compte Minecraft au compte du site web
- Synchronisation web ↔ serveur (claims, factions, joueurs)
- Drivers MySQL inclus

### 🔒 Coffres privés & tri
- **Coffres privés** : shift + clic droit sur un coffre avec un panneau pour le verrouiller
- **Tri automatique** des coffres avec `/sort` : regroupe, range, classe les items proprement

---

## 📜 Commandes

| Commande | Description |
|----------|-------------|
| `/faction create <nom>` | Crée une faction |
| `/faction info [nom]` | Infos d'une faction |
| `/faction list` | Liste des factions |
| `/faction invite / kick / setchef` | Gestion des membres |
| `/faction rename <nom>` | Renomme la faction (chef) |
| `/faction leave / disband` | Quitter ou dissoudre |
| `/faction claim / unclaim / claims` | Gestion des claims |
| `/faction claimmap` | Mini-map des claims |
| `/faction perms` | Permissions du chunk |
| `/faction setspawn [1\|2]` | Définir spawn (1 ou 2) |
| `/faction spawn [1\|2]` | TP au spawn |
| `/faction alliance inviter/accepter/refuser/rompre` | Alliances |
| `/faction guerre declarer/accepter/refuser/capituler/piller` | Guerres |
| `/faction souschef promouvoir/retirer/liste/limite` | Sous-chefs |
| `/faction stats [joueur]` | Stats joueur |
| `/faction classementjoueurs <cat>` | Top 10 joueurs |
| `/faction classement` | Top 10 factions |
| `/faction shop / vendre / acheter / recuperer / mesannonces` | Shop |
| `/faction recruter / villageois` | Recrutement villageois (verrou-gui depuis v5.10.2) |
| `/faction power [joueur]` | Puissance |
| `/faction setchest` / `faction chest` | Coffres privés |
| `/faction sort` | Tri d'inventaire/coffre |
| `/faction lier [statut]` | Liaison web |
| `/tpa <joueur> / tpaccept / tpdeny` | Téléportation |
| `/sethome [nom] / home [nom] / delhome / homes` | Homes |
| `/sort` | Tri inventaire/coffre |

> Alias : `/f`, `/fac`

---

## 🔐 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `faction.use` | Utiliser les commandes de faction | ✅ |
| `faction.create` | Créer une faction | ✅ |
| `faction.join` | Rejoindre une faction | ✅ |
| `faction.leave` | Quitter une faction | ✅ |
| `faction.admin` | Commandes admin (InvSee, bypass coffres) | OP |

---

## 🛠️ Compilation & développement

### Prérequis
- **Java 21+** (JDK)
- **Maven 3.9+**
- Accès réseau aux dépôts PaperMC et Maven Central

### Build

```bash
git clone https://github.com/herocraftlol/Faction-Create-Friends.git
cd Faction-Create-Friends/FactionPlugin-v4
mvn clean package
```

Le JAR est produit dans `target/FactionPlugin-5.10.3.jar` (≈ 430 KB).

### Stack technique
- **Paper API 1.21.4** (`io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`)
- **Java 21** (compilé en target 21)
- **Shaded JAR** : aucun driver MySQL embarqué (Paper le fournit)
- **YAML** pour toute la persistance (`factions.yml`, `stats.yml`, `villagers.yml`, `shop.yml`, etc.)

---

## 📁 Structure des fichiers de données

Tous les fichiers sont générés dans `plugins/FactionPlugin/` au premier lancement :

| Fichier | Contenu |
|---------|---------|
| `config.yml` | Configuration globale (messages, limites, paramètres IA) |
| `factions.yml` | Factions, claims, alliances, guerres, sous-chefs |
| `stats.yml` | Statistiques de chaque joueur |
| `villagers.yml` | Villageois recrutés, équipement, chantiers, niveaux |
| `shop.yml` | Annonces du shop global |
| `homes.yml` | Homes personnels et spawn de faction |
| `privatechests.yml` | Coffres verrouillés |
| `warps.yml` | Warps supplémentaires (si activés) |

---

## 🆕 Historique des versions

### **v5.10.3** — *Chat en couleur rétabli* 🎨💬
- **🎨 Couleurs & préfixe de faction dans le tchat** : migration complète de `AsyncPlayerChatEvent#setFormat()` vers l'API moderne Paper `AsyncChatEvent` + `event.renderer(...)`. Les couleurs, le tag de guerre (⚔), le tag de rang Légendaire (⚜), et le tag de faction (ex. `[TitanS]`) sont à nouveau visibles — alors qu'ils avaient silencieusement disparu depuis le passage au chat signé sous Paper 1.21.
- **✉️ Message du joueur inchangé** : le contenu tapé par le joueur passe tel quel dans le `Component` rendu, sans aucune modification (compatibilité totale avec le système de signature).
- **🌙 Soin nocturne** : la régénération pendant le sommeil est de nouveau déclenchée pour les villageois (via polling `Villager#isSleeping()` dans la boucle d'IA principale — l'événement Bukkit `EntitySleepEvent` n'est plus jamais lancé sur Paper 1.21+).
- **🧹 Code allégé** : suppression du `@EventHandler` cassé sur `EntitySleepEvent`, remplacement par un simple `sleepPolling(rv, v)` qui tourne dans `tickAll()`.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### **v5.10.2** — *Verrou de GUI pour villageois (verrou-gui)*
- **🔒 Verrouillage d'accès à la GUI d'un villageois** : un seul joueur à la fois peut ouvrir la fiche détaillée d'un villageois recruté. Les autres reçoivent *« Ce villageois est déjà géré par Pseudo en ce moment. Réessaie dans un instant. »*
- **🛡️ Libération automatique du verrou** : sur fermeture de la GUI, sur `/reload`, et sur déconnexion du joueur — aucune GUI « bloquée ».
- **⚡ Aucune action perdue** : les clics du propriétaire sont exécutés normalement ; les autres reçoivent simplement un refus poli.
- **🔧 Correctifs de compilation Paper API 1.21.4** : `LUCK_OF_THE_SEA`, `HAPPY_VILLAGER`, `YELLOW_STAINED_GLASS_PANE`, `WHITE_BED`, `RESISTANCE`/`STRENGTH`, `MOVE_BACK_TO_VILLAGE`, `WATER_AVOIDING_RANDOM_STROLL`, `MapPalette.matchColor(java.awt.Color)`, lambda `final MapView finalView`, `Sound.ENTITY_PLAYER_BURP`, etc.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### **v5.10.1** — *Indicateurs d'emplacement, correctifs anti-disparition, mains occupées*
- **Indicateurs visuels** dans chaque emplacement vide des GUIs de villageois (arc, flèches, épée, casque, blocs, outil, nourriture, graines…) : une icône-repère grisée indique précisément quoi y déposer. Disparaît dès qu'un vrai objet est posé, et ne peut jamais être ramassée par erreur.
- **Correctifs anti-disparition d'objets** : le glisser-déposer (drag) est désormais bloqué dans les GUI de villageois — il pouvait faire atterrir un objet dans un emplacement décoratif invisible, perdu au rafraîchissement suivant. L'emplacement transitoire "type de bloc du prochain chantier" rend désormais l'objet posé au joueur si inutilisé.
- **Les villageois tiennent maintenant ce qu'ils utilisent** : le constructeur tient en main le bloc de son chantier actif (le guerrier et le récolteur le faisaient déjà).

### **v5.10.0** — *Le Récolteur, la construction instantanée et la fuite des guerriers*
- Nouveau rôle **Récolteur** : mine, coupe du bois, creuse ou cultive selon l'outil ; dépose dans un coffre, replante automatiquement.
- **Constructeur — construction instantanée** : téléportation directe sur chaque bloc à poser ou à miner, échafaudage en bambou auto-installé pour les endroits difficiles.
- **Guerrier — fuite et discrétion** : sous 25 % de vie ou face à 3+ ennemis, il fuit au lieu de se battre.
- **Formations en cercle** : `/faction villageois ranger cercle`.

### **v5.9.x** — *Tout tombe au sol à la mort, clic-droit direct, niveaux d'XP, somme réparatrice, longue distance, rassemblement*
- 5 niveaux d'expérience pour les villageois, soins auto, équipements qui s'usent réellement
- Clic-droit direct sur un villageois recruté pour ouvrir sa fiche
- Butin de guerre (les guerriers ramassent automatiquement l'équipement de leurs victimes)
- Sommeil réparateur (le villageois soigné pendant qu'il dort dans un lit)
- Mode archerie pour les guerriers (tir à distance, garde ses distances, repasse en mêlée si besoin)
- Suivi longue distance, retour au poste, chantiers n'importe où
- Assignation en groupe (poste commun, chantier commun, rassemblement commun)

### **v5.9.0** — *Villageois recrutés (Constructeur / Guerrier)*
- `/faction recruter` : convertit un villageois en unité de faction
- `/faction villageois` : GUI de gestion (nom, rôle, équipement, chantier, libération)

### **v5.8.4** — *Sous-chefs*
- 2 sous-chefs max, avec permissions granulaires (invitations, kick, alliances, guerres, claims)

### **v4.0.0** — *Shop Global paginé + InvSee admin*
- 45 items/page, recherche par mot-clé, tri prix ↑/↓
- 4 monnaies : fer, or, diamant, émeraude
- InvSee admin en lecture seule

### **v3.x** — *Banque, claims, troc, stats, classements, puissance*

---

## 📄 Licence

Ce projet est sous licence **MIT**. Voir `LICENSE` pour le texte complet.

---

## 🤝 Crédits & contributions

Développé par [FactionDev](https://github.com/herocraftlol). Contributions bienvenues via Pull Requests sur [la page GitHub du projet](https://github.com/herocraftlol/Faction-Create-Friends).

Pour toute question ou bug, ouvre une **Issue** sur le dépôt.

⭐ Si ce plugin t'est utile, n'hésite pas à mettre une étoile au dépôt !
