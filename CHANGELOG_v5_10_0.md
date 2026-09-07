# CHANGELOG — FactionPlugin v5.10.0

## v5.10.0 — *Le Récolteur, la construction instantanée et la fuite des guerriers*

Une mise à jour qui transforme en profondeur la boucle de vie des **villageois recrutés** et la productivité de votre faction : un tout nouveau rôle autonome, des Constructeurs qui ne perdent plus de temps à marcher, et des Guerriers qui savent désormais quand il faut **prendre la fuite**.

---

## 🌾 Nouveau rôle : Récolteur

Troisième rôle jouable pour vos villageois recrutés, à côté du **Constructeur** et du **Guerrier** : le **Récolteur**.

Donnez-lui un outil, il fait le reste :

| Outil en main | Ce qu'il récolte automatiquement |
|---|---|
| ⛏️ Pioche | Minerais (charbon, fer, or, diamant, redstone, lapis, émeraudes…) |
| 🪓 Hache | Bois (toutes les essences, tous les types de troncs) |
| 🪣 Pelle | Terre, sable, gravier, argile, neige… |
| 🌾 Champ de blé / carottes / pommes de terre / betteraves | Il sème, récolte à maturité, replante, et sème les cases vides — tout seul |

Vous choisissez une **zone de récolte** en cliquant deux coins dans le monde : il y travaille tout seul, sans intervention, jusqu'à ce qu'elle soit terminée.

Vous pouvez aussi désigner un **coffre de dépôt** (clic-droit dessus) : tout ce qu'il récolte y est déposé automatiquement. **Les graines qu'il a sur lui sont conservées** pour pouvoir replanter, mais rien d'autre n'est gaspillé.

Comme le Guerrier, son outil **s'use** au travail. À partir du **niveau 4**, il se **fabrique lui-même** un nouvel outil dès qu'il casse — fini les interruptions parce qu'il a perdu sa pioche.

---

## 🏗️ Constructeur — construction instantanée

Plus de temps perdu à marcher entre les blocs :

- Le Constructeur se **téléporte directement** sur chaque bloc à poser ou à miner, quelle que soit la distance : chantier terminé 10× plus vite sur les grandes zones.
- S'il doit atteindre une hauteur difficile, il **pose un échafaudage en bambou** sous lui pour ne pas tomber, puis le **retire** (sans le faire tomber au sol) une fois le chantier terminé — propre et net.

Et pour les chantiers massifs, il ne fait plus d'aller-retour :

- **Récolte en un seul passage** : il calcule exactement combien il lui manque pour finir **tout** le chantier, et il récolte ce lot d'un coup — au lieu de repartir à la mine à chaque bloc manquant.

---

## ⚔️ Guerrier — immobile par défaut, fuite et discrétion

Deux changements majeurs dans le comportement de combat :

### 📍 Immobile par défaut

- Le **vagabondage aléatoire** du Guerrier (IA vanille) est désactivé (best-effort).
- Il ne bouge **que sur ordre** : à son poste, en patrouille, en suivi de joueur, ou vers son point de rassemblement.
- Plus de perte de temps à errer loin de son poste pendant qu'un ennemi vous attaque ailleurs.

### 🏃 Fuite & discrétion

- Si sa vie tombe **sous 25 %**, ou s'il se retrouve face à **3 ennemis ou plus** dans son périmètre, il **fuit** :
  - Soit loin de la menace,
  - Soit vers son poste / point de rassemblement.
- Il **revient au combat** uniquement lorsqu'il a **plus de 60 %** de vie **et** plus aucune menace proche.
- Résultat : votre Guerrier ne meurt plus stupidement parce qu'il s'est obstiné à attaquer 5 creepers en même temps.

---

## 🛡️ Formations en cercle

En plus de la ligne (`/faction villageois ranger`), un nouveau mode de formation :

- `/faction villageois ranger cercle` (ou le bouton dédié) dispose vos villageois en **cercle autour du joueur**.
- Idéal pour escorter en formation défensive, ou simplement pour le style.

---

## ✅ Récapitulatif des ajouts

| Nouveauté | Détails |
|---|---|
| 🌾 **Rôle Récolteur** | Mine / coupe / creuse selon l'outil, cultive automatiquement, dépose dans un coffre |
| ⚡ **Construction instantanée** | Le Constructeur se TP sur chaque bloc au lieu de marcher |
| 🪜 **Échafaudage en bambou** | Posé et retiré proprement pour les chantiers en hauteur |
| 📦 **Récolte en un passage** | Il ramasse tout ce qu'il manque d'un coup, pas bloc par bloc |
| 📍 **Guerrier immobile** | Plus de vagabondage vanille — il n'agit que sur ordre |
| 🏃 **Fuite du Guerrier** | Fuit sous 25 % HP ou face à 3+ ennemis, reprend le combat à 60 % HP |
| ⭕ **Formation en cercle** | `/faction villageois ranger cercle` |

---

## 🛠️ Correctifs techniques

- Alignement Paper 1.21 : `Material.BED` → `RED_BED`, `Material.GOLD_STAINED_GLASS_PANE` → `YELLOW_STAINED_GLASS_PANE`, `Enchantment.LUCK` → `LUCK_OF_THE_SEA`, `PotionEffectType.INCREASE_DAMAGE` → `STRENGTH`, `PotionEffectType.DAMAGE_RESISTANCE` → `RESISTANCE`, `Particle.SPELL_WITCH` → `WITCH`, `Particle.VILLAGER_HAPPY` → `HAPPY_VILLAGER`, `Sound.ENTITY_PLAYER_EAT` → `ENTITY_GENERIC_EAT`, `MapPalette.matchColor(org.bukkit.Color)` → `matchColor(java.awt.Color)`.
- `org.bukkit.plugin.JavaPlugin` migré vers `org.bukkit.plugin.java.JavaPlugin`.
- `EntitySleepEvent` (retiré en Paper 1.21) remplacé par un suivi basé sur `PlayerBedEnterEvent` + position du lit.
- Suppression des goals Paper (`RANDOM_STROLL`, `WALK_TO_VILLAGE`, etc.) **via réflexion** pour rester compatible entre versions de Paper (1.20 / 1.21).
- Correction d'un lambda capturant une variable non finale dans `FactionMapManager`.
- Renommage `BuyResult.NOT_ENOUGH_MONEY` → `NOT_ENOUGH_PAYMENT` dans `ShopGUI`.
- Surcharge varargs `cmdItem(Material, String, boolean, String...)` dans `MainMenuGUI`.
- Mise à jour de `plugin.yml` → `5.10.0`.

---

## 📦 Téléchargement

- `FactionPlugin-5.10.0.jar` — binaire compilé pour Paper 1.21.4 (Java 21 requis)
- `FactionPlugin-5.10.0-source.zip` — code source complet (Maven project)

## 📥 Installation

1. Téléchargez `FactionPlugin-5.10.0.jar` dans la section *Assets* ci-dessous.
2. Déposez-le dans le dossier `plugins/` de votre serveur **Paper 1.21.4**.
3. Redémarrez — la configuration est générée dans `plugins/FactionPlugin/`.

---

_🤖 Cette release et son fichier descriptif ont été préparés par un agent IA (OpenHands) pour le compte de herocraftlol._