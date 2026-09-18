# 🏰 FactionPlugin

> **Le plugin Minecraft tout-en-un pour Paper 1.21.x** — Factions, alliances, guerres, claims, villages autonomes, banque d'émeraudes, troc sécurisé, **commerce inter-villes** 🚢🚂, shop global, statistiques, **tab HeroTab synchronisé à la seconde**, et plus encore.

![Version](https://img.shields.io/badge/version-5.15.0-brightgreen) ![Paper](https://img.shields.io/badge/Paper-1.21.x-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![License](https://img.shields.io/badge/license-MIT-green)

---

## ✨ Qu'est-ce que FactionPlugin ?

**FactionPlugin** est un plugin Minecraft complet pensé pour transformer votre serveur Paper en un véritable univers de factions. Conçu pour les serveurs survie PvP, il réunit dans une seule commande `/faction` (et ses alias `/f`, `/fac`) tout ce qu'il faut pour faire vivre un mode factions riche et moderne : **territoires**, **diplomatie**, **économie**, **villageois recrutés autonomes**, **commerce régional entre villes** par ports et gares, **statistiques**, et même la **synchronisation de l'onglet HeroTab** avec votre site web.

Une seule commande pour piloter une faction, ses villages, ses alliances, ses guerres, son économie, ses contrats, ses villageois, ses homes, sa boutique, ses stats et son onglet global. **Pas une demi-douzaine de plugins à installer, configurer et faire cohabiter** — un seul `.jar`, une seule base de données YAML, et c'est parti.

Conçu pour **Paper 1.21.4** (API Bukkit + Paper), **Java 21**, prêt à l'emploi : posez le `.jar` dans `plugins/`, redémarrez, et tout est en place.

---

## 🎯 Pour qui ?

- 🎮 **Petits serveurs survie PvP** qui veulent un mode factions riche sans empiler 5 plugins incompatibles.
- 🏰 **Serveurs « war zone »** qui ont besoin d'alliances, de guerres, de territoires protégés et de grandes armées de villageois.
- 💰 **Serveurs économie** avec shop global paginé, banque de faction, troc sécurisé, et commerce inter-villes par contrats.
- 🌐 **Réseaux avec site web** qui veulent synchroniser factions, rangs et carte en direct avec un back-office (HeroTab, WebMap).

---

## 🆕 Nouveautés de la v5.15.0 — *La vraie cause du tab HeroTab enfin corrigée* 🏷️✨🐛

Cette version s'attaque **à la vraie cause racine** d'un bug que toutes les versions précédentes tentaient de corriger — sans y parvenir, parce qu'elles visaient la mauvaise cible.

### 🎯 En deux mots

HeroTab (le plugin proxy qui affiche votre faction et votre rang dans l'onglet global du serveur) lit une table MySQL `faction_tab_sync` qui était censée être alimentée par une classe `FactionTabSync` côté FactionPlugin — **et cette classe n'avait tout simplement jamais été écrite**. Résultat : le tab restait figé, peu importe les correctifs de « synchro » apportés.

Cette version **crée enfin la classe manquante**. Onglet HeroTab, carte web, sous-serveurs du réseau : tout est désormais cohérent en permanence, sans aucun délai perceptible.

### ✨ Ce que ça change concrètement

| Événement | Avant v5.15.0 | Depuis v5.15.0 |
|---|---|---|
| Onglet HeroTab après un **recrutement** | ❌ jamais | ✅ instantané |
| Onglet HeroTab après un **départ / kick** | ❌ jamais | ✅ instantané |
| Onglet HeroTab après un **disband** | ❌ jamais | ✅ instantané |
| Onglet HeroTab après un **renommage** | ❌ jamais | ✅ instantané |
| Onglet HeroTab après une **montée de rang** | ⏳ jusqu'à 60 s | ✅ instantané |
| Onglet HeroTab cycle normal | toutes les 60 s | toutes les 30 s |
| `mvn clean package` | ⚠️ import manquant aléatoire | ✅ toujours propre |

### 📜 Détails du correctif

- 🐛 **Cause racine identifiée et corrigée** : la classe `FactionTabSync` est créée. Elle écrit dans la table `faction_tab_sync` lue par HeroTab, en réutilisant la connexion MySQL déjà configurée pour `/lier` (section `mysql:` du `config.yml`) — **aucune configuration supplémentaire n'est nécessaire si `/lier` fonctionne déjà chez toi**.
- 🛡️ **Table créée automatiquement** si elle n'existe pas, avec exactement les colonnes attendues par HeroTab : `uuid`, `faction_name`, `rank_name`, `rank_color` (au format `&x` legacy), `rank_icon`.
- ⚡ **Tâche de fond toutes les 30 s** + push immédiat sur les mêmes événements que `WebMapSync` : recrutement, départ, kick, disband, renommage, montée de rang. L'onglet est synchronisé **sans aucun délai**, où que soit le joueur sur le réseau.
- 🔧 **Bug de compilation latent corrigé en passant** : `FactionPlugin.java` référençait la classe `Bukkit` sans l'importer — un import manquant qui aurait empêché toute compilation propre dans certaines configurations. Un balayage complet du projet a confirmé qu'aucun autre fichier ne présente ce problème. `mvn clean package` produit désormais un `.jar` valide du premier coup.

---

## 🌟 Fonctionnalités à découvrir

### 🏛️ Factions complètes
Crée ta faction (`/faction create <nom>`), invite des joueurs, désigne un sous-chef, transfère le rôle de chef, quitte ou dissous ta faction. La dissolution peut être immédiate ou **différée d'une heure** (le temps de tout récupérer : coffre de faction, banque, claims, homes).

### 🗺️ Claims & territoires
Réclame des chunks (`/faction claim`), configure des **permissions fines par joueur** sur chaque claim, visualise tes territoires en direct avec la **mini-map de faction** (`/faction claimmap`), protège l'accès aux non-membres et aux alliés.

### 🤝 Alliances & guerres
Propose une alliance à une autre faction, accepte ou refuse, romps quand tu veux. Déclare la guerre, combats avec buffs de faction, capitule ou pille le coffre du vaincu (si négocié). Le chat et le tag de guerre (⚔) sont automatiquement appliqués aux combatants.

### 👥 Villages autonomes
Recrute des villageois vanilla et attribue-leur un rôle : **Constructeur** 🏗️, **Guerrier** ⚔️, **Archer** 🏹, **Défenseur** 🛡️, **Récolteur** 🌾, **Navigateur** 🚢 ou **Cheminot** 🚂. Ils construisent, combattent, récoltent, transportent — tout seuls. Ils montent de niveau, s'équipent, se reposent et partagent la nourriture entre eux. Aucun plugin d'IA à part : c'est **dans FactionPlugin**.

### 🚢 Commerce inter-villes
Définis un **port** ou une **gare** dans chaque village, crée un **contrat de livraison** entre deux villes, assigne-le à un villageois **Navigateur** (bateau) ou **Cheminot** (minecart) — il livrera la marchandise automatiquement, fret protégé par alliance.

### 💰 Économie intégrée
**Banque d'émeraudes** par faction (dépôt, retrait, historique), **shop global paginé** avec recherche par mot-clé et tri prix ↑/↓, 4 monnaies (fer, or, diamant, émeraude), **troc sécurisé joueur↔joueur** avec confirmation des deux parties et anti-scam.

### ⚡ Système de puissance & rangs
Chaque joueur génère de la **Puissance Individuelle** basée sur ses kills, ses blocs cassés, ses avancements, son temps de jeu, etc. La somme forme la **Puissance Globale** de la faction. **8 rangs** de Pierre à **Mythique ☄** (1 000 000 pts), avec **effets passifs** (Speed, Strength, Resistance, Regeneration, Haste) qui augmentent à mesure que tu montes.

### 📊 Statistiques & classements
`/faction stats [joueur]` : kills, morts, mobs tués, dégâts, blocs, temps de jeu, K/D, advancements, dates de connexion. `/faction classementjoueurs <categorie>` : top 10 joueurs par catégorie (mobs, pvp, morts, blocs, temps, dégâts, kd, advancements).

### 🏠 Homes & TPA
Homes personnels (jusqu'à 6 au rang Mythique), `/sethome`, `/home`, `/delhome`. Téléportation entre joueurs avec `/tpa`, `/tpaccept`, `/tpdeny`, warmup et cooldown.

### 🔌 Liaison site web
`/lier` synchronise ton compte Minecraft avec le site web. Depuis la v5.15.0, **l'onglet HeroTab est lui aussi synchronisé** via la classe `FactionTabSync` — finies les déconnexions entre l'onglet, la carte et le site.

---

## 📥 Installation

1. Téléchargez la dernière version depuis la **[page des releases](../../releases)**.
2. Placez le fichier `FactionPlugin-X.X.X.jar` dans le dossier `plugins/` de votre serveur Paper 1.21.x.
3. Redémarrez le serveur.
4. Le fichier `config.yml` est généré automatiquement dans `plugins/FactionPlugin/`.

> 📌 **Aucun changement de format de données** : vos fichiers `factions.yml`, `claims.yml`, `bank.yml`, `ranking.yml`, `map.yml`, etc. restent compatibles. La table MySQL `faction_tab_sync` est créée automatiquement au premier démarrage si elle n'existe pas encore.

### 🔧 Configuration

Le fichier `config.yml` vous permet de personnaliser :

- les messages (préfixe, format, langue)
- les permissions par rôle
- les paramètres de faction (coûts, limites, taille de faction max)
- les seuils de rangs et les buffs passifs
- la **section `mysql:`** pour `/lier` et la synchro `FactionTabSync` (optionnel)

---

## 🎮 Commandes principales

| Commande | Alias | Description |
|---|---|---|
| `/faction create <nom>` | `/f create`, `/fac create` | Crée une nouvelle faction |
| `/faction invite <joueur>` | — | Invite un joueur dans votre faction |
| `/faction kick <joueur>` | — | Expulse un membre |
| `/faction setchef <joueur>` | — | Transfère le rôle de chef |
| `/faction leave` | — | Quitte votre faction |
| `/faction disband` | — | Dissout la faction (différée d'1 h) |
| `/faction info [nom]` | — | Affiche les infos d'une faction |
| `/faction list` | — | Liste toutes les factions |
| `/faction top` | — | Top 10 des factions par puissance |
| `/faction claim` / `unclaim` | — | Réclame / libère le chunk sous vos pieds |
| `/faction claims` | — | Liste tous les claims de votre faction |
| `/faction claimmap` | — | Donne une mini-map live des claims |
| `/faction perms` | — | Ouvre la GUI des permissions du chunk |
| `/faction stats [joueur]` | — | Affiche les stats complètes d'un joueur |
| `/faction classementjoueurs <cat>` | `cj` | Top 10 joueurs par catégorie |
| `/faction power` | — | Affiche votre puissance et celle de la faction |
| `/faction alliance <inviter\|accepter\|refuser\|rompre>` | — | Gestion des alliances |
| `/faction guerre <déclarer\|accepter\|refuser\|capituler\|piller>` | — | Gestion des guerres |
| `/faction recruter` | — | Recrute le villageois ciblé |
| `/faction village <create\|list\|info>` | — | Gestion des villages |
| `/faction contrat <creer\|liste\|assigner\|annuler>` | — | Contrats de livraison inter-villes |
| `/faction shop` | — | Ouvre la boutique globale |
| `/faction bank` | — | Ouvre la banque d'émeraudes de la faction |
| `/faction trade <joueur>` | — | Ouvre un troc sécurisé avec un joueur |
| `/faction sethome [nom]`, `/faction home [nom]`, `/faction delhome <nom>` | `/sethome`, `/home`, `/delhome` | Gestion des homes |
| `/faction tpa <joueur>`, `tpaccept`, `tpdeny` | `/tpa`, `/tpaccept`, `/tpdeny` | Téléportation entre joueurs |
| `/faction invsee <joueur>` | — | Ouvre l'inventaire d'un joueur (admin) |
| `/faction lier [statut]` | `/lier` | Lie le compte Minecraft au compte site web |
| `/faction rename <nom>` | — | Renomme la faction |
| `/faction setspawn [1\|2]` | — | Définit un spawn de faction |

---

## 🔐 Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `faction.use` | Utiliser les commandes de faction | ✅ true |
| `faction.admin` | Commandes admin (invsee, bypass coffres privés) | 🔒 op |

---

## 🛠️ Développement

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

Le JAR est produit dans `target/FactionPlugin-5.15.0.jar` (≈ 480 KB).

### Stack technique
- **Paper API 1.21.4** (`io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`)
- **Java 21** (compilé en target 21)
- **Shaded JAR** : aucun driver MySQL embarqué (Paper le fournit)
- **YAML** pour toute la persistance locale

---

## 🗂️ Fichiers de données générés

Tous les fichiers sont créés dans `plugins/FactionPlugin/` au premier lancement :

| Fichier | Contenu |
|---|---|
| `config.yml` | Configuration globale (messages, limites, paramètres IA, section `mysql:`) |
| `factions.yml` | Factions, claims, alliances, guerres, sous-chefs |
| `stats.yml` | Statistiques de chaque joueur |
| `villagers.yml` | Villageois recrutés, équipement, niveaux, chantiers |
| `shop.yml` | Annonces du shop global |
| `homes.yml` | Homes personnels et spawn de faction |
| `privatechests.yml` | Coffres verrouillés |
| `contracts.yml` | Contrats de livraison inter-villes en cours |

---

## 🆕 Historique des versions

### **v5.15.0** — *La vraie cause du tab HeroTab corrigée* 🏷️✨🐛 *(version actuelle)*
- 🐛 **Cause racine identifiée et corrigée** : la classe `FactionTabSync` est créée. Elle écrit dans la table MySQL `faction_tab_sync` lue par HeroTab, en réutilisant la connexion MySQL déjà configurée pour `/lier`. **Aucune configuration supplémentaire n'est nécessaire** si `/lier` fonctionne déjà chez toi.
- 🛡️ **Table créée automatiquement** si elle n'existe pas, avec exactement les colonnes attendues par HeroTab : `uuid`, `faction_name`, `rank_name`, `rank_color`, `rank_icon`.
- ⚡ **Tâche de fond toutes les 30 s** + push immédiat sur recrutement, départ, kick, disband, renommage, montée de rang. **L'onglet est synchronisé sans aucun délai**, où que soit le joueur sur le réseau.
- 🔧 **Bug de compilation latent corrigé en passant** : `FactionPlugin.java` référençait `Bukkit` sans l'importer — un balayage complet du projet a confirmé qu'aucun autre fichier ne présente ce problème. `mvn clean package` produit un `.jar` valide du premier coup.

> ℹ️ Cette version remplace tous les correctifs précédents de synchro tab en s'attaquant à la vraie cause racine.

### **v5.14.x** — *Dissolution différée, nettoyage des factions fantômes, coffres préservés*
- Dissolution différée d'une heure (`/faction disband`) avec libération automatique des claims + coffres personnels.
- Nettoyage automatique des factions sans membre (sans délai d'1 h).
- Accès aux coffres préservé pendant l'heure de grâce, peu importe les factions rejointes entre-temps.
- Synchronisation immédiate du tab à la promotion de rang.

### **v5.13.x** — *Tab rafraîchi en quittant la faction*
- Bug corrigé : `/faction leave`, `/faction kick` et `/faction disband` rafraîchissent maintenant l'onglet correctement.
- Compat Paper API 1.21.4 (renommages `Material`, `Enchantment`, `PotionEffectType`, `Sound`, `Particle`).

### **v5.12.0** — *Commerce inter-villes 🚢🚂📦*
- Rôles **Navigateur** (bateau entre ports) et **Cheminot** (minecart entre gares).
- Définition de ports/gares, contrats de livraison (`/faction contrat …`), fret protégé par alliance.

### **v5.11.0** — *Villages & niveaux* 🏘️
- Niveaux dérivés de la population (1-4 = Village, 5+ = Ville).
- Base de repli par village, entraide entre villageois (le Récolteur donne de la nourriture aux alliés blessés).

### **v5.10.x** — *Récolteur, construction instantanée, fuite des guerriers*
- Rôle Récolteur (mine, bois, creuse, cultive selon l'outil).
- Constructeur : construction instantanée, échafaudage auto.
- Guerrier : fuite et discrétion sous 25 % de vie ou face à 3+ ennemis.
- Verrou de GUI pour villageois, chat en couleur rétabli.

### **v5.9.x** — *Villageois recrutés, niveaux d'XP, somme réparatrice*
- `/faction recruter` : convertit un villageois vanilla en unité de faction.
- 5 niveaux d'XP, soins auto, équipements qui s'usent réellement.
- Clic-droit direct sur un villageois recruté, butin de guerre, sommeil réparateur.

### **v5.8.4** — *Sous-chefs*
- 2 sous-chefs max, permissions granulaires.

### **v5.0.0** — *Alliances & Homes* 🏠
- Système d'alliances, homes personnels, coffres privés, TPA entre joueurs.

### **v4.0.0** — *Shop Global paginé + InvSee admin* 🛒
- 45 items/page, recherche par mot-clé, tri prix ↑/↓. 4 monnaies.

### **v3.x** — *Banque, claims, troc, stats, classements, puissance*
- Banque d'émeraudes, système de claims, commerce entre joueurs, statistiques intégrées, classements.

---

## 📄 Licence

Ce projet est sous licence **MIT**. Voir `LICENSE` pour le texte complet.

---

## 🤝 Crédits & contributions

Développé par [FactionDev](https://github.com/herocraftlol). Contributions bienvenues via Pull Requests sur [la page GitHub du projet](https://github.com/herocraftlol/Faction-Create-Friends).

Pour toute question ou bug, ouvre une **Issue** sur le dépôt.

⭐ Si ce plugin t'est utile, n'hésite pas à mettre une étoile au dépôt !
