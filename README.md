# 🏰 FactionPlugin

> **Plugin Minecraft tout-en-un pour Paper 1.21.x** — Factions, alliances, guerres, claims, villages autonomes, banque d'émeraudes, troc sécurisé, **commerce inter-villes**, shop global, statistiques, **tab toujours à jour**, **nettoyage automatique des factions fantômes**, et bien plus encore.

![Version](https://img.shields.io/badge/version-5.14.1-brightgreen) ![Paper](https://img.shields.io/badge/Paper-1.21.x-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![License](https://img.shields.io/badge/license-MIT-green)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** est un plugin Minecraft complet qui transforme votre serveur Paper en un véritable univers de factions. Pensé pour les serveurs survie PvP, il rassemble dans une seule commande `/faction` tout ce qu'il faut pour gérer un mode factions riche : territoires, diplomatie, économie, commerce régional par ports et gares, statistiques, et même des **villageois recrutés** autonomes qui construisent, combattent, récoltent, **transportent des marchandises entre villes** pour vous.

La version actuelle (**5.14.1**) poursuit le nettoyage en profondeur entamé en 5.14.0 : en plus de la dissolution différée d'une heure, **les factions qui se retrouvent vides** (à cause de bugs anciens où la dissolution ne libérait pas tout) sont désormais **automatiquement et entièrement supprimées**, au démarrage puis toutes les 30 minutes — fini les entrées fantômes dans `/faction topbanque`, `/faction classement`, ou les claims « à personne ».

Conçu pour Paper **1.21.4** (API Bukkit + Paper), Java **21**, et prêt à l'emploi : il suffit de poser le `.jar` dans `plugins/`.

---

## 🆕 Nouveautés de la v5.14.1 — *Nettoyage des factions fantômes* 👻🧹

La v5.14.1 complète la v5.14.0 : si la dissolution volontaire d'une faction reste différée d'une heure (pour laisser aux membres le temps de récupérer leurs affaires), **les factions qui n'ont déjà plus aucun membre** — état pathologique laissé par d'anciens bugs où la dissolution ne libérait pas tout — sont maintenant nettoyées d'elles-mêmes, automatiquement.

### Le problème

Au fil des versions, plusieurs bugs historiques avaient laissé des factions dans un état « zombie » :

- 💰 Un **compte en banque** jamais supprimé au disband → entrée orpheline pour toujours dans `/faction topbanque`.
- 🏦 Un **coffre partagé** jamais supprimé au disband.
- 🗺️ Des **claims** jamais libérés (avant la v5.14.0).
- 📊 Un **cache de puissance/classement** jamais purgé au disband ni au renommage → entrée fantôme dans `/faction classement`.

Ces zombies empilaient de la donnée morte et un peu de CPU à chaque rechargement du plugin.

### La solution

- **Toute faction dont la liste de membres est vide** est désormais considérée comme **fantôme** et est **automatiquement et entièrement supprimée** :
  - claims libérés,
  - coffre partagé supprimé,
  - compte en banque supprimé,
  - entrée retirée du classement de puissance,
  - faction réellement supprimée (elle n'existe plus nulle part).
- **Pas de délai d'une heure** dans ce cas (à la différence d'un disband « normal ») : il n'y a personne pour récupérer quoi que ce soit, autant faire le ménage tout de suite.
- **Nettoyage initial au démarrage** : un passage est exécuté **une fois au démarrage du serveur**, pour rattraper les zombies déjà présents dans vos fichiers de sauvegarde.
- **Filet de sécurité** : la même purge est répétée **toutes les 30 minutes** au cas où une faction se retrouverait un jour vide par un autre chemin.

### Pourquoi deux rythmes ?

- **Au démarrage** : indispensable pour purger les zombies hérités des versions précédentes, déjà présents dans les YAML.
- **Toutes les 30 min** : rustine de sécurité pour ne pas avoir à prouver une seule et unique porte d'entrée vers « faction vide » — si une telle situation apparaît par un futur bug, elle sera nettoyée d'elle-même peu après.

ℹ️ Voir [`CHANGELOG_v5_14_1.md`](./CHANGELOG_v5_14_1.md) pour le détail complet et les derniers correctifs de compilation Paper 1.21.4 (déplacement de `JavaPlugin`, renommages `Particle`/`Sound`/`Material`, conversion `MapPalette.matchColor(java.awt.Color)`).

> ℹ️ Tout ce qui faisait la joie des versions précédentes reste évidemment présent : dissolution différée d'une heure (v5.14.0), tab toujours à jour (v5.13.1), commerce inter-villes 🚢🚂 (v5.12.0), villageois autonomes (v5.9.0+), rangs Village/Ville (v5.11), verrou-gui (v5.10.2), banque, shop, troc, guerres, alliances, sous-chefs, et tout le reste.

---

## 📥 Installation

1. Téléchargez la dernière release : [**FactionPlugin-5.14.1.jar**](../../releases/latest)
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
- **Dissolution différée d'une heure** (v5.14.0) — le temps de tout récupérer
- **Nettoyage automatique des factions fantômes** (v5.14.1) — plus de zombies dans les fichiers

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
| 🚢 **Navigateur** | Livre en **bateau** entre les ports de deux villes alliées. |
| 🚂 **Cheminot** | Livre en **minecart** entre les gares de deux villes alliées. |

Bonus :
- 5 **niveaux d'expérience** par villageois, avec soins automatiques et bonus de stats
- **Butin de guerre** : les guerriers ramassent automatiquement l'équipement de leurs victimes
- **Indicateurs visuels** dans les GUIs pour ne plus perdre d'objets par erreur
- **🔒 Verrou de GUI** (depuis la v5.10.2, toujours actif) : un seul joueur à la fois peut gérer un villageois, sans conflit

### 💰 Économie intégrée
- **Banque d'émeraudes** par faction : dépôt, retrait, accès réservé aux membres autorisés
- **Shop global** paginé avec recherche par mot-clé et tri par prix (4 monnaies : fer, or, diamant, émeraude)
- Paiement automatique au vendeur, livraison à la reconnexion si hors-ligne
- Système d'**annonces** avec `/faction vendre` et `/faction acheter`

### 🤝 Troc sécurisé entre joueurs
- Interface GUI dédiée : chacun pose ce qu'il propose et ce qu'il veut
- **Confirmation des deux parties** requise pour finaliser
- Annulation possible à tout moment, **anti-scam** garanti

### 🚢 Commerce inter-villes (depuis la v5.12)
Vos villages ne sont plus des îles économiques. Choisissez une ressource dans un village, expédiez-la dans un port ou une gare, et un **Navigateur** (bateau) ou un **Cheminot** (minecart) la livre jusqu'au village allié. Contrats à sens unique, fret protégé par faction, itinéraire ligne droite à vue — simple, lisible, stable.

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
- **Tab toujours à jour** (v5.13.1) — reflète immédiatement votre faction actuelle, même après un leave / kick / disband
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
| `/faction leave / disband` | Quitter ou dissoudre (différé d'1h en 5.14.0, zombies nettoyés en 5.14.1) |
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
| `/faction contrat` | Commerce inter-villes (5.12) |
| `/faction port` / `faction gare` | Définir ports / gares (5.12) |
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
cd Faction-Create-Friends
mvn clean package
```

Le JAR est produit dans `target/FactionPlugin-5.14.1.jar` (≈ 475 KB).

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

### **v5.14.1** — *Nettoyage des factions fantômes* 👻🧹 *(version actuelle)*
- **🧹 Suppression automatique des factions sans membre** : toute faction dont la liste de membres est vide est désormais traitée comme « fantôme » et intégralement supprimée — claims libérés, coffre partagé supprimé, compte en banque supprimé, entrée retirée du classement, faction réellement détruite.
- **🚀 Pas de délai d'1h dans ce cas** (contrairement à un disband volontaire) : personne n'est là pour récupérer quoi que ce soit, on nettoie tout de suite.
- **⏱️ Purge au démarrage + filet de sécurité toutes les 30 min** : la première passe rattrape les zombies déjà présents dans vos fichiers YAML ; la suivante garantit qu'aucune faction ne reste fantôme si une situation analogue se reproduit par un futur bug.
- **🔧 Derniers correctifs de compilation Paper API 1.21.4** : déplacement de `JavaPlugin` (`org.bukkit.plugin.java`), renommages `Particle.WITCH` / `Particle.HAPPY_VILLAGER` / `Sound.ENTITY_GENERIC_EAT` / `Material.YELLOW_STAINED_GLASS_PANE`, conversion explicite `MapPalette.matchColor(java.awt.Color)`, désactivation du listener `EntitySleepEvent` (événement supprimé en 1.21+).
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit. La purge initiale fait le ménage dans les YAML existants.

### **v5.14.0** — *Dissolution différée d'une heure* ⏳
- **`/faction disband` ne supprime plus rien instantanément** : la faction continue d'exister normalement pendant **1 heure**, le temps de tout récupérer. Chaque membre en ligne est informé au moment de la demande.
- **Après 1 h, suppression définitive et complète** : claims libérés, coffre partagé supprimé, banque supprimée, classement nettoyé, faction réellement détruite.
- **Bugs historiques corrigés au passage** : la faction ne disparaissait jamais complètement — la banque n'était jamais supprimée, les claims jamais libérés, le cache de classement jamais nettoyé, ni au disband ni au renommage. Tout est corrigé ici, et résiste à un redémarrage serveur pendant le délai d'une heure.

### **v5.13.1** — *Tab rafraîchi en quittant la faction* ✅👋
- **🐛 Bug corrigé — le tab gardait l'ancienne faction après un départ** : `/faction leave`, `/faction kick` et `/faction disband` ne rafraîchissaient jamais le tab. Les trois commandes appellent maintenant `tabManager.refresh(...)` pour chaque joueur concerné.
- **🔧 Correctifs de compilation Paper API 1.21.4** : `Material.RED_BED`, `Enchantment.LOOTING`, `PotionEffectType.STRENGTH`/`RESISTANCE`, `BuyResult.NOT_ENOUGH_PAYMENT`, `Particle.WITCH` / `Particle.HAPPY_VILLAGER`, `Sound.ENTITY_GENERIC_EAT`, `Material.YELLOW_STAINED_GLASS_PANE`, `org.bukkit.plugin.java.JavaPlugin`, lambda `final MapView`, conversion `MapPalette.matchColor(java.awt.Color)`.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### **v5.13.0** — *Correctifs classement, renommage, synchronisation*
- **🐛 Bug corrigé — la faction Légendaire ne s'ouvrait pas dans `/faction classement`** : son icône (`NETHER_STAR`) était le même matériau que le bouton-titre décoratif, et le code excluait ce matériau de la détection. Détection désormais basée sur l'emplacement (slot), pas le matériau.
- **Seuil du rang Légendaire** : passé de 60 000 à 100 000 de puissance.
- **Renommage de faction** : cooldown "une fois par jour" réellement appliqué, annonce à tout le serveur, solde de banque déplacé vers le nouveau nom.
- **Tab & site synchronisés instantanément** sur rejoin/kick/renommage (en plus du cycle de 60 s).

### **v5.12.0** — *Commerce inter-villes 🚢🚂📦*
- **🚢 Rôle Navigateur** : villageois qui livre **en bateau** entre les **ports** de deux villes.
- **🚂 Rôle Cheminot** : version **minecart** entre les **gares** des deux villes.
- **📦 Définir un port/gare** : `/faction port definir <village>` et `/faction gare definir <village>`.
- **📜 Contrats** (`/faction contrat …`) : `creer`, `liste`, `assigner`, `annuler` — livraison à sens unique entre deux villes.
- **🚤 Trajet simple** : aller simple en ligne droite, à vue, à petite vitesse.
- **🔒 Fret protégé** : seuls la faction propriétaire et ses alliées peuvent ouvrir le coffre de fret ou monter dans le véhicule.
- **🧹 Correctif important** : troc joueur↔joueur reconstruit à l'identique, nouveau commerce dans son propre package `fr.faction.commerce`.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### **v5.11.0** — *Villages & niveaux* 🏘️
- Niveaux dérivés de la population (1-4 = Village, 5+ = Ville). Débloque l'accès au rôle Navigateur/Cheminot.
- Base de repli par village (un villageois « naît » dans son village, y revient sans tâche).
- Entraide entre villageois (le Récolteur donne jusqu'à 4 nourritures à un guerrier/constructeur à < 70 % de vie, dans un rayon de 48 blocs).

### **v5.10.3** — *Chat en couleur rétabli* 🎨💬
- Migration complète de `AsyncPlayerChatEvent#setFormat()` vers l'API Paper `AsyncChatEvent` + `event.renderer(...)`. Couleurs, tag de guerre (⚔), tag de rang Légendaire (⚜), tag de faction à nouveau visibles.

### **v5.10.2** — *Verrou de GUI pour villageois (verrou-gui)*
- **🔒 Verrouillage d'accès à la GUI d'un villageois** : un seul joueur à la fois peut ouvrir la fiche détaillée.

### **v5.10.0** — *Le Récolteur, la construction instantanée et la fuite des guerriers*
- Nouveau rôle **Récolteur** : mine, coupe du bois, creuse ou cultive selon l'outil.
- **Constructeur — construction instantanée**, échafaudage auto.
- **Guerrier — fuite et discrétion** sous 25 % de vie ou face à 3+ ennemis.

### **v5.9.x** — *Tout tombe au sol à la mort, clic-droit direct, niveaux d'XP, somme réparatrice*
- 5 niveaux d'XP, soins auto, équipements qui s'usent réellement.
- Clic-droit direct sur un villageois recruté, butin de guerre, sommeil réparateur.

### **v5.9.0** — *Villageois recrutés (Constructeur / Guerrier)*
- `/faction recruter` : convertit un villageois en unité de faction.

### **v5.8.4** — *Sous-chefs*
- 2 sous-chefs max, avec permissions granulaires.

### **v4.0.0** — *Shop Global paginé + InvSee admin*
- 45 items/page, recherche par mot-clé, tri prix ↑/↓.
- 4 monnaies : fer, or, diamant, émeraude.

### **v3.x** — *Banque, claims, troc, stats, classements, puissance*

---

## 📄 Licence

Ce projet est sous licence **MIT**. Voir `LICENSE` pour le texte complet.

---

## 🤝 Crédits & contributions

Développé par [FactionDev](https://github.com/herocraftlol). Contributions bienvenues via Pull Requests sur [la page GitHub du projet](https://github.com/herocraftlol/Faction-Create-Friends).

Pour toute question ou bug, ouvre une **Issue** sur le dépôt.

⭐ Si ce plugin t'est utile, n'hésite pas à mettre une étoile au dépôt !