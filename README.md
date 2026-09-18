# 🏰 FactionPlugin

> **Plugin Minecraft tout-en-un pour Paper 1.21.x** — Factions, alliances, guerres, claims, villages autonomes, banque d'émeraudes, troc sécurisé, **commerce inter-villes** 🚢🚂, shop global, statistiques, **tab HeroTab synchronisé à la seconde**, **nettoyage automatique des factions fantômes**, **accès aux coffres préservé pendant la dissolution différée**, et bien plus encore.

![Version](https://img.shields.io/badge/version-5.15.1-brightgreen) ![Paper](https://img.shields.io/badge/Paper-1.21.x-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![License](https://img.shields.io/badge/license-MIT-green)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** est un plugin Minecraft complet qui transforme votre serveur Paper en un véritable univers de factions. Pensé pour les serveurs survie PvP, il rassemble dans une seule commande `/faction` (avec ses alias `/f` et `/fac`) tout ce qu'il faut pour gérer un mode factions riche : territoires, diplomatie, économie, **commerce régional par ports et gares**, statistiques, et même des **villageois recrutés** autonomes qui construisent, combattent, récoltent, et **transportent des marchandises entre villes** pour vous.

La version actuelle (**5.15.1**) **pousse les villageois recrutés dans leurs derniers retranchements** : il n'y a plus aucune limite au nombre de villageois qu'une faction peut aligner (utile pour les grands empires), et la barre d'expérience maximale grimpe à **100** au lieu de 5 — un villageois « vétéran » peut donc réellement devenir une force redoutable, comparable à un joueur bien équipé.

Conçu pour Paper **1.21.4** (API Bukkit + Paper), Java **21**, et prêt à l'emploi : il suffit de poser le `.jar` dans `plugins/`.

---

## 🆕 Nouveautés de la v5.15.1 — *Villageois illimités, niveau max 100* 🏘️♾️📈

Cette version est entièrement consacrée aux **villageois recrutés** : on
leur retire la bride pour permettre aux factions les plus ambitieuses de
bâtir de véritables armées villageoises, et on repousse très loin leur
progression pour qu'un vétéran soit enfin à la hauteur d'un joueur
aguerri.

### 🏘️ Villageois recrutés : plus aucune limite par faction

- **♾️ Recrutement illimité** : la limite dure `FACTION_FULL` qui empêchait
  une faction d'aligner plus de quelques villageois a été retirée. Tu
  peux désormais constituer une véritable armée permanente — idéale pour
  les grands empires, les sièges, ou les serveurs « war zone ».
- **🛡️ Toujours équilibré au combat** : les villageois conservent leur IA
  de patrouille, leur fuite en cas de blessure critique, leur armement
  par classe (guerrier / archer / défenseur). Plus il y en a, plus la
  faction est forte — sans casser l'équilibrage unitaire.

### 📈 Progression jusqu'au niveau 100

- **🌟 Niveau maximum repoussé à 100** (au lieu de 5). Les villageois
  gagnent toujours de l'XP en tuant des mobs, en construisant ou en
  récoltant, mais la barre continue de monter bien plus loin.
- **💪 Bonus progressifs** : à chaque palier, les points de vie maximum,
  les dégâts d'attaque et la résistance continuent de croître. Un
  villageois niveau 100 est un tank de fin de partie, capable de tenir
  face à un groupe de joueurs en armure diamant/nétherite.
- **🎖️ Affichage fidèle** : la jauge d'XP et le niveau sont rendus via
  la mécanique native du `Villager` de Minecraft, ce qui évite tout
  hack de rendu et reste compatible avec Dynmap, BluMap, Squaremap, etc.

### 🐛 Correctifs & compatibilité Paper 1.21.4

- **🔧 Compilation contre Paper 1.21.4 rétablie** : ajustements de code
  pour rester compilable face aux changements d'API de Paper 1.21
  (`Enchantment.LUCK`, `PotionEffectType.INCREASE_DAMAGE`, etc.).
- **🛏️ Soin pendant le sommeil** : `EntitySleepEvent` n'existant plus
  côté Bukkit/Paper 1.21, la régénération des villageois endormis est
  désormais déclenchée par détection périodique via `Villager#isSleeping()`,
  ce qui rend le comportement strictement équivalent à l'ancien
  événement, sans dépendre d'un hook retiré.
- **🛒 Boutique joueurs** : correction d'une typo historique
  (`NOT_ENOUGH_MONEY` → `NOT_ENOUGH_PAYMENT`) qui empêchait l'achat
  correctement géré depuis plusieurs versions mineures.

### Avant / Après

| Aspect | En v5.15.0 | En v5.15.1 |
| --- | --- | --- |
| Nombre de villageois / faction | Plafonné | **Illimité** |
| Niveau max d'un villageois | 5 | **100** |
| Soin pendant le sommeil | Event Bukkit | **Détection périodique** |
| Compilation Paper 1.21.4 | OK | **OK (cette release)** |

---

## 🆕 Nouveautés de la v5.15.0 — *La vraie cause du tab HeroTab corrigée* 🏷️✨🐛

### Le problème

Depuis plusieurs versions, malgré des correctifs successifs censés rendre le tab
« instantané », le tab HeroTab restait désespérément figé après un recrutement,
un départ, un kick, une dissolution, un renommage ou une montée de rang. La
cause — très simple et qui a échappé à toutes les analyses précédentes — est la
suivante :

- HeroTab lit une table MySQL `faction_tab_sync`.
- Cette table était censée être écrite par une classe `FactionTabSync` côté
  FactionPlugin.
- **Cette classe n'existait tout simplement pas.**

Tous les correctifs précédents déclenchaient bien une synchronisation
immédiate, mais vers `WebMapSync` — un système totalement différent qui
alimente la carte du site web, sans aucun rapport avec le tab HeroTab. **Rien
n'écrivait donc jamais dans la table `faction_tab_sync`**, et le tab ne
pouvait pas se mettre à jour, quel que soit le nombre de « correctifs de
synchro » apportés.

### Ce qui change

- **Nouvelle classe `FactionTabSync`** : elle écrit maintenant réellement dans
  la table `faction_tab_sync` lue par HeroTab. Elle réutilise la connexion
  MySQL déjà configurée pour `/lier` (section `mysql:` du `config.yml`) —
  aucune configuration supplémentaire n'est nécessaire si `/lier` fonctionne
  déjà chez toi.
- **Table créée automatiquement** si elle n'existe pas, avec exactement les
  colonnes attendues par HeroTab : `uuid`, `faction_name`, `rank_name`,
  `rank_color` (au format `&x` legacy), `rank_icon`.
- **Tâche de fond toutes les 30 secondes** + déclenchement immédiat sur les
  mêmes événements que `WebMapSync` : recrutement, départ, kick, disband,
  renommage, montée de rang. **Local, site, onglet HeroTab, sous-serveurs —
  tout est cohérent en permanence, sans aucun délai.**
- **Bug de compilation latent corrigé en passant** : `FactionPlugin.java`
  (la classe principale) référençait `Bukkit` sans l'importer — un import
  manquant qui aurait empêché le plugin de se compiler dans certaines
  configurations. Un balayage complet a confirmé qu'aucun autre fichier ne
  présente ce problème.

### Avant / Après

| Action | En v5.14.3 | En v5.15.0 |
|--------|------------|------------|
| Onglet HeroTab après un recrutement | ❌ jamais mis à jour | ✅ instantané |
| Onglet HeroTab après un départ / kick | ❌ jamais mis à jour | ✅ instantané |
| Onglet HeroTab après un disband | ❌ jamais mis à jour | ✅ instantané |
| Onglet HeroTab après un renommage | ❌ jamais mis à jour | ✅ instantané |
| Onglet HeroTab après une montée de rang | ⏳ jusqu'à 60 s | ✅ instantané |
| Onglet HeroTab cycle normal | toutes les 60 s | toutes les 30 s |
| Compilation `mvn clean package` | ⚠️ import manquant aléatoire | ✅ toujours propre |

ℹ️ Voir [`CHANGELOG_v5_15_0.md`](./CHANGELOG_v5_15_0.md) pour le détail complet.

---

## 🆕 Nouveautés de la v5.14.3 — *Synchronisation immédiate du tab à la promotion de rang* 🏷️✨

La v5.14.3 ferme le dernier délai perceptible sur le tab-list. Quand une
faction gagne assez de puissance pour franchir un cap (par exemple
`BRONZE → ARGENT`, `OR → DIAMANT`, …), **le tab local** se mettait déjà à
jour instantanément. Mais le **tab global HeroTab** et **la carte web**
attendaient le cycle de synchronisation de 60 s. Résultat : un coéquipier
pouvait rester affiché `[⬡ Bronze]` pendant près d'une minute après la
promotion.

À partir de la v5.14.3, **toute montée de rang déclenche un
`FactionTabManager.refresh(...)` immédiat**, exactement comme pour un
recrutement, un départ, un renommage ou une dissolution. Les onglets du
serveur, le site et le backing-service HeroTab sont maintenant cohérents
**sans aucun délai**.

### Avant / Après

| Action | En v5.14.2 | En v5.14.3 |
|--------|------------|------------|
| Tab **local** lors d'une promotion de rang | ✅ instantané | ✅ instantané |
| Tab **global** HeroTab lors d'une promotion | ⏳ jusqu'à 60 s | ✅ instantané |
| `/fac top` après promotion | ✅ cohérent | ✅ identique + cache invalidé |
| Site web et carte web après promotion | ⏳ cyclique (60 s) | ✅ propage tout de suite |

> ℹ️ Cette version reste utile pour le tab local et l'invalidation du cache,
> mais la v5.15.0 va plus loin : elle remplace ce mécanisme en s'attaquant à
> la cause racine — la table `faction_tab_sync` lue par HeroTab n'était tout
> simplement jamais écrite par FactionPlugin.

ℹ️ Voir [`CHANGELOG_v5_14_3.md`](./CHANGELOG_v5_14_3.md) pour le détail complet.

---

## 🆕 Nouveautés de la v5.14.2 — *Accès aux coffres pendant la dissolution* 🧳🔓

La v5.14.0 introduit la dissolution différée d'une heure — le temps de tout
récupérer. La v5.14.2 ferme un trou important : si un membre **quittait** la
faction (ou en était exclu) pendant cette heure, il **perdait instantanément
l'accès à ses propres coffres et claims**, alors que la faction existait
encore techniquement et que les claims n'étaient pas libérés.

### Avant / Après

| Situation | En v5.14.0 | En v5.14.2 |
|-----------|------------|------------|
| Tu fais `/faction disband`, attends 30 min, puis `/faction leave` pour rejoindre une autre faction | ❌ Tes coffres et claims deviennent inaccessibles pendant les 30 min restantes — tu perds ton propre butin | ✅ Tes coffres et claims restent accessibles jusqu'à la libération effective des claims (fin du compte à rebours) |
| Un chef te fait `/faction kick` pendant l'heure de grâce | ⚠️ Tu perdais l'accès à tes coffres le temps qu'il reste | ✅ Tu gardes l'accès jusqu'à la fin du compte à rebours |
| Le serveur redémarre pendant l'heure | ✅ Reprise correcte | ✅ Identique, plus nettoyage automatique des claims orphelins au démarrage |

ℹ️ Voir [`CHANGELOG_v5_14_2.md`](./CHANGELOG_v5_14_2.md) pour le détail complet.

---

## 🆕 Nouveautés de la v5.14.1 — *Nettoyage des factions fantômes* 👻🧹

La v5.14.1 complète la v5.14.0 : si la dissolution volontaire d'une faction
reste différée d'une heure (pour laisser aux membres le temps de récupérer
leurs affaires), **les factions qui n'ont déjà plus aucun membre** — état
pathologique laissé par d'anciens bugs où la dissolution ne libérait pas tout
— sont maintenant nettoyées d'elles-mêmes, automatiquement.

### Le problème

Au fil des versions, plusieurs bugs historiques avaient laissé des factions
dans un état « zombie » :

- 💰 Un **compte en banque** jamais supprimé au disband → entrée orpheline
  pour toujours dans `/faction topbanque`.
- 🏦 Un **coffre partagé** jamais supprimé au disband.
- 🗺️ Des **claims** jamais libérés (avant la v5.14.0).
- 📊 Un **cache de puissance/classement** jamais purgé au disband ni au
  renommage → entrée fantôme dans `/faction classement`.

Ces zombies empilaient de la donnée morte et un peu de CPU à chaque
rechargement du plugin.

### La solution

- **Toute faction dont la liste de membres est vide** est désormais
  considérée comme **fantôme** et est **automatiquement et entièrement
  supprimée** :
  - claims libérés,
  - coffre partagé supprimé,
  - compte en banque supprimé,
  - entrée retirée du classement de puissance,
  - faction réellement supprimée (elle n'existe plus nulle part).
- **Pas de délai d'une heure** dans ce cas : il n'y a personne pour
  récupérer quoi que ce soit, autant faire le ménage tout de suite.
- **Nettoyage initial au démarrage** : un passage est exécuté **une fois au
  démarrage du serveur**, pour rattraper les zombies déjà présents dans vos
  fichiers de sauvegarde.
- **Filet de sécurité** : la même purge est répétée **toutes les 30 minutes**
  au cas où une faction se retrouverait un jour vide par un autre chemin.

ℹ️ Voir [`CHANGELOG_v5_14_1.md`](./CHANGELOG_v5_14_1.md) pour le détail complet.

---

## 📥 Installation

1. Téléchargez la dernière release :
   [**FactionPlugin-5.15.1.jar**](../../releases/download/v5.15.1/FactionPlugin-5.15.1.jar)
2. Placez le fichier dans le dossier `plugins/` de votre serveur Paper 1.21.4+
3. Démarrez (ou redémarrez) le serveur — la configuration se génère
   automatiquement dans `plugins/FactionPlugin/`
4. Configurez `config.yml` selon vos besoins (messages, limites, paramètres,
   section `mysql:` si vous voulez activer la synchro du tab HeroTab)

> 🛠️ Requis : serveur **Paper 1.21.4+**, **Java 21+**, aucun autre plugin de
> factions requis.

---

## 🎮 Fonctionnalités principales

### 🏛️ Système de factions complet
- Création, dissolution, renommage de factions
- Gestion fine des membres : invitation, expulsion, transfert de chef
- Système de **sous-chefs** (jusqu'à 2) avec permissions granulaires
- Chat de faction, ranks visuels, GUI intuitive
- Classement des factions par puissance
- **Dissolution différée d'une heure** (v5.14.0) — le temps de tout récupérer
- **Nettoyage automatique des factions fantômes** (v5.14.1) — plus de zombies
  dans les fichiers

### ⚔️ Alliances & Guerres
- Proposez, acceptez, refusez et rompez des **alliances** avec d'autres
  factions
- Déclarez, acceptez et refusez des **guerres**
- Pendant la guerre : défense automatique, riposte, **pillage du coffre du
  vaincu** (si négocié)
- Bouton **« Capituler »** pour le chef uniquement (abandon propre)

### 🗺️ Système de claims & territoire
- Claim/unclaim de chunks avec permissions par joueur
- GUI dédiée pour gérer qui peut construire/casser où
- Alliances → permissions croisées configurables
- **Mini-map de faction** : carte visuelle temps réel de vos claims (carte
  vanilla Minecraft augmentée)

### 👥 Villages de faction (la grosse nouveauté)
Recrutez des villageois vanilla et attribuez-leur un rôle : ils deviennent
autonomes !

| Rôle | Fait quoi ? |
|------|-------------|
| 🔨 **Constructeur** | Remplit des zones définies (châteaux, murs, repairs). Pose des blocs, se téléporte instantanément, monte un échafaudage pour les endroits difficiles. |
| ⚔️ **Guerrier** | Patrouille, défend un périmètre, attaque les mobs hostiles et les joueurs ennemis en guerre. Riposte automatique. Mode mêlée/archerie au choix. |
| 🌾 **Récolteur** | Mine, coupe du bois, creuse ou récolte des cultures selon l'outil qu'on lui donne. Dépose le butin dans un coffre, replante tout seul. |
| 🚢 **Navigateur** | Livre en **bateau** entre les ports de deux villes alliées. |
| 🚂 **Cheminot** | Livre en **minecart** entre les gares de deux villes alliées. |

Bonus :
- 5 **niveaux d'expérience** par villageois, avec soins automatiques et bonus
  de stats
- **Butin de guerre** : les guerriers ramassent automatiquement l'équipement
  de leurs victimes
- **Indicateurs visuels** dans les GUIs pour ne plus perdre d'objets par
  erreur
- **🔒 Verrou de GUI** (depuis la v5.10.2, toujours actif) : un seul joueur à
  la fois peut gérer un villageois, sans conflit

### 💰 Économie intégrée
- **Banque d'émeraudes** par faction : dépôt, retrait, accès réservé aux
  membres autorisés
- **Shop global** paginé avec recherche par mot-clé et tri par prix (4
  monnaies : fer, or, diamant, émeraude)
- Paiement automatique au vendeur, livraison à la reconnexion si hors-ligne
- Système d'**annonces** avec `/faction vendre` et `/faction acheter`

### 🤝 Troc sécurisé entre joueurs
- Interface GUI dédiée : chacun pose ce qu'il propose et ce qu'il veut
- **Confirmation des deux parties** requise pour finaliser
- Annulation possible à tout moment, **anti-scam** garanti

### 🚢 Commerce inter-villes (depuis la v5.12)
Vos villages ne sont plus des îles économiques. Choisissez une ressource dans
un village, expédiez-la dans un port ou une gare, et un **Navigateur**
(bateau) ou un **Cheminot** (minecart) la livre jusqu'au village allié.
Contrats à sens unique, fret protégé par faction, itinéraire ligne droite à
vue — simple, lisible, stable.

### 🏠 Homes & téléportation
- `/sethome`, `/home`, `/delhome`, `/homes` — homes personnels
- Nombre de homes selon le rang (1 sans faction → 5 au rang max)
- **TPA** entre joueurs avec warmup et cooldown
- **Spawn de faction** (1 ou 2 selon le rang, configurable)

### ⚡ Système de puissance & rangs
- 7 rangs de faction : **Pierre → Bronze → Argent → Or → Diamant → Émeraude
  → Légendaire**
- La puissance globale est calculée à partir des stats individuelles
- Chaque rang apporte des **effets passifs** : Speed, Strength, Resistance,
  Jump Boost, Haste, Regeneration
- Bonus selon la taille de la faction

### 📊 Statistiques & classements
- `/faction stats [joueur]` : kills, mobs tués, dégâts, blocs, temps de jeu,
  K/D ratio, advancements
- `/faction classementjoueurs <cat>` : top 10 par catégorie (`mobs`, `pvp`,
  `morts`, `blocs`, `temps`, `dommages`, `kd`, `advancements`)
- `/faction classement` : top 10 des factions par puissance (GUI)
- **Tab toujours à jour** (v5.13.1) — reflète immédiatement votre faction
  actuelle, même après un leave / kick / disband
- **Onglet global HeroTab synchronisé à la seconde** (v5.15.0) — la classe
  `FactionTabSync` écrit enfin dans la bonne table MySQL
- Persistance complète dans `stats.yml`

### 🔌 Liaisons externes (web map / site)
- `/lier [statut]` : lie ton compte Minecraft au compte du site web
- Synchronisation web ↔ serveur (claims, factions, joueurs)
- Drivers MySQL inclus (le `faction_tab_sync` est créé automatiquement)

### 🔒 Coffres privés & tri
- **Coffres privés** : shift + clic droit sur un coffre avec un panneau pour
  le verrouiller
- **Tri automatique** des coffres avec `/sort` : regroupe, range, classe les
  items proprement

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

Le JAR est produit dans `target/FactionPlugin-5.15.1.jar` (≈ 480 KB).

### Stack technique
- **Paper API 1.21.4** (`io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`)
- **Java 21** (compilé en target 21)
- **Shaded JAR** : aucun driver MySQL embarqué (Paper le fournit)
- **YAML** pour toute la persistance (`factions.yml`, `stats.yml`,
  `villagers.yml`, `shop.yml`, etc.)

---

## 📁 Structure des fichiers de données

Tous les fichiers sont générés dans `plugins/FactionPlugin/` au premier
lancement :

| Fichier | Contenu |
|---------|---------|
| `config.yml` | Configuration globale (messages, limites, paramètres IA, section `mysql:` pour le site/HeroTab) |
| `factions.yml` | Factions, claims, alliances, guerres, sous-chefs |
| `stats.yml` | Statistiques de chaque joueur |
| `villagers.yml` | Villageois recrutés, équipement, chantiers, niveaux |
| `shop.yml` | Annonces du shop global |
| `homes.yml` | Homes personnels et spawn de faction |
| `privatechests.yml` | Coffres verrouillés |
| `warps.yml` | Warps supplémentaires (si activés) |

---

## 🆕 Historique des versions

### **v5.15.1** — *Villageois illimités, niveau max 100* 🏘️♾️📈 *(version actuelle)*
- **♾️ Villageois recrutés : recrutement illimité** — la limite `FACTION_FULL`
  a été retirée. Tu peux désormais aligner autant de villageois que ta
  faction peut en entretenir, idéal pour les grands empires et les serveurs
  orientés war / sièges.
- **📈 Niveau maximum 100** (au lieu de 5) — un villageois vétéran devient
  une véritable force de fin de partie, avec PV max, dégâts et résistance
  qui continuent de monter à chaque palier.
- **🛏️ Soin pendant le sommeil** — `EntitySleepEvent` n'existant plus dans
  Paper 1.21, la régénération pendant le sommeil est désormais déclenchée
  via une détection périodique `Villager#isSleeping()` dans le tick IA.
  Comportement strictement équivalent à l'ancien événement.
- **🛒 Boutique joueurs** — correction d'une typo historique : l'enum
  `NOT_ENOUGH_MONEY` (inexistant) est remplacé par `NOT_ENOUGH_PAYMENT`,
  pour que les messages d'erreur lors d'un achat soient à nouveau
  corrects.
- **🔧 Compilation Paper 1.21.4** — adaptations aux changements d'API :
  `Enchantment.LUCK` → `Enchantment.LUCK_OF_THE_SEA`, `Material.BED` →
  `Material.RED_BED`, `Material.GOLD_STAINED_GLASS_PANE` →
  `Material.YELLOW_STAINED_GLASS_PANE`, `Sound.ENTITY_PLAYER_EAT` →
  `Sound.ENTITY_GENERIC_EAT`, `PotionEffectType.INCREASE_DAMAGE` /
  `DAMAGE_RESISTANCE` → `STRENGTH` / `RESISTANCE`, et conversion
  `MapPalette.matchColor(Color)` → `matchColor(r,g,b)` (Bukkit Color
  → java.awt.Color).
- **📦 Aucune migration de données** — il suffit de remplacer le `.jar` et
  de redémarrer le serveur. Les fichiers `data/` (factions, contrats,
  villages, alliances) restent parfaitement compatibles.

### **v5.15.0** — *La vraie cause du tab HeroTab corrigée* 🏷️✨🐛
- **🐛 Bug fondamental corrigé — `FactionTabSync` créée** : la classe censée
  écrire dans la table MySQL `faction_tab_sync` lue par HeroTab
  **n'existait tout simplement pas**. Tous les correctifs précédents
  déclenchaient une synchronisation immédiate, mais vers `WebMapSync` — un
  système différent qui alimente la carte du site, sans aucun rapport avec
  le tab. Résultat : le tab ne se rafraîchissait jamais, peu importe les
  correctifs.
- **✅ Onglet HeroTab réellement synchronisé** : `FactionTabSync` écrit
  désormais dans la bonne table (créée automatiquement si absente), en
  réutilisant la connexion MySQL déjà configurée pour `/lier`. Cycle de 30 s
  + push immédiat sur les mêmes événements que `WebMapSync` (recrutement,
  départ, kick, disband, renommage, montée de rang).
- **🔧 Bug de compilation latent corrigé** : `FactionPlugin.java` référençait
  `Bukkit` sans l'importer — un import manquant qui aurait empêché la
  compilation dans certaines configurations. Balayage complet : aucun autre
  fichier ne présente ce problème.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer
  suffit. La table `faction_tab_sync` est créée automatiquement au premier
  démarrage si elle n'existe pas.

### **v5.14.3** — *Synchronisation immédiate du tab à la promotion de rang* 🏷️✨
- **⚡ Tab global synchrone lors d'une promotion** : `FactionTabManager.refresh(factionName)` est désormais appelé dès qu'une faction franchit un cap de puissance (`BRONZE → ARGENT`, `OR → DIAMANT`, …). Fini l'attente pouvant aller jusqu'à 60 s côté HeroTab / carte web.
- **🔄 `/fac top` et cache de puissance** : le cache local de `FactionPowerManager` est invalidé sur tout franchissement de cap — `/fac top`, l'écran de détail d'une faction et la carte web s'alignent immédiatement.
- **🛡️ Cohérence totale local / web / tab** : la classe de team (`faction.Officier`, `faction.Membre`, `faction.Chef`, `faction.None`) reste identique partout, sans fenêtre de bascule visible pour les autres joueurs.

> ℹ️ Cette version reste utile pour le tab local et l'invalidation du cache,
> mais **la v5.15.0 va plus loin** : elle remplace ce mécanisme en
> s'attaquant à la cause racine — la table `faction_tab_sync` lue par HeroTab
> n'était tout simplement jamais écrite par FactionPlugin.

### **v5.14.2** — *Accès aux coffres pendant la dissolution* 🧳🔓
- **🔓 Tu gardes l'accès à tes coffres pendant l'heure de grâce** : au moment d'un `/faction disband`, chaque membre actuel est explicitement autorisé sur tous les claims de la faction. Cette autorisation est attachée au claim, pas à l'appartenance — donc même si tu quittes ou que tu te fais exclure pendant l'heure, tu peux revenir prendre tes affaires tant que les claims ne sont pas libérés.
- **🧹 `purgeOrphanedClaims` au démarrage + toutes les 30 min** : les chunks encore marqués comme claimés par une faction qui n'existe plus sont automatiquement libérés, comme n'importe quel chunk non claimé. Les coffres qu'ils contenaient redeviennent accessibles à tous.
- **🔧 Petits correctifs** : `WebMapSync.java` corrigé (erreur de syntaxe JSON), import manquant `PostType` dans `Contract.java`, `DisbandManager.resumePendingDisbands()` réautorise les membres si la dissolution a été demandée avant la mise à jour.
- **📦 Aucune migration de données** : remplacer le `.jar` et redémarrer suffit.

### **v5.14.1** — *Nettoyage des factions fantômes* 👻🧹
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

Développé par [FactionDev](https://github.com/herocraftlol). Contributions
bienvenues via Pull Requests sur [la page GitHub du
projet](https://github.com/herocraftlol/Faction-Create-Friends).

Pour toute question ou bug, ouvre une **Issue** sur le dépôt.

⭐ Si ce plugin t'est utile, n'hésite pas à mettre une étoile au dépôt !
