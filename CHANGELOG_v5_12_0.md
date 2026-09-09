# CHANGELOG — FactionPlugin v5.12.0

## 🚢 Nouveauté principale : le Commerce inter-villes 🚢

La v5.12.0 ouvre un tout nouveau système économique qui relie enfin les villages entre eux. Fini de produire en vase clos : vos surplus peuvent maintenant prendre la route, le rail ou la mer, pour aller nourrir une autre ville, alliée ou voisine — et créer une véritable économie régionale.

---

## 🚢 Commerce inter-villes (ports, gares, contrats)

### Deux nouveaux rôles de villageois : Navigateur & Cheminot

- **🚢 Navigateur** — un villageois qui livre des marchandises **en bateau** entre les **ports** de deux villes alliées (ou de la même faction). Il prend la cargaison, embarque, traverse à vue, accoste, dépose, puis revient à vide.
- **🚂 Cheminot** — la version ferroviaire, qui livre entre les **gares** des deux villes en train (minecart), à vue également.
- Les deux rôles sont débloqués en même temps que les villages de niveau "Ville" (5+), via un nouveau bouton dans la fiche du villageois.

### 📦 Définir un port ou une gare

- `/faction port definir <village>` — pose un port (clic-droit dans un claim de la faction, dans la zone du village ciblé).
- `/faction gare definir <village>` — pose une gare, même principe.
- Le bloc posé à cet endroit fait office **d'entrepôt / coffre de fret** pour le commerce. C'est lui qui contient la cargaison des contrats à l'arrivée comme au départ.

### 📜 Contrats commerciaux (`/faction contrat …`)

Un **contrat** = une livraison **à sens unique** d'une ressource entre deux villes. Il se gère avec une petite famille de sous-commandes :

| Commande | Effet |
|----------|-------|
| `/faction contrat creer <fromVillage> <toVillage> <resource> <quantite>` | Crée un contrat en attente (chef / sous-chef uniquement). |
| `/faction contrat liste` | Liste tous les contrats de la faction (en attente, en transit, livrés). |
| `/faction contrat assigner <id>` | Assigne un navigateur ou un cheminot au contrat — il part avec la cargaison. |
| `/faction contrat annuler <id>` | Annule un contrat (chef / sous-chef uniquement). |

**Limitation volontaire :** un contrat = **un aller simple**. Pour faire un aller-retour, il faut créer un second contrat. Cela garde un système simple, lisible, et sans couplage physique réel entre wagons/bateaux (pas de "train de marchandises" qui se forme tout seul). Le trajet se fait **en ligne droite**, à petite vitesse, à vue — il faut donc un chenal maritime ou une voie ferrée **dégagée** entre le port/gare de départ et celui d'arrivée.

### 🛡️ Fret protégé

Seuls les membres de la **faction propriétaire** et de ses **alliées** peuvent :
- Ouvrir le coffre de fret (port ou gare),
- Monter dans le véhicule (bateau ou minecart) qui transporte la cargaison.

Aucun risque de vol en chemin par une faction hostile.

---

## 🏘️ Villes & niveaux (rappel v5.11 — conservé en 5.12)

- `/faction village fonder <nom>` — fonde un village dans 2 coins sélectionnés.
- `/faction village liste` — niveau + population.
- `/faction village dissoudre <nom>` — dissout (chef / sous-chef uniquement).
- **Niveaux** dérivés de la population : 1-4 = *Village*, 5+ = *Ville*.
- Un villageois rattaché à un village y "naît" automatiquement, et y retourne comme **base de repli** quand il n'a plus de tâche.
- Entraide entre villageois : un récolteur donne automatiquement de la nourriture (jusqu'à 4) à un guerrier/constructeur de sa faction sous 70% de vie à moins de 48 blocs.

---

## 🛠️ Correctif critique en cours de session

Un système de commerce inter-villes précédemment développé avait par erreur **écrasé un fichier portant le même nom** (`TradeManager`) qui appartient à une fonctionnalité totalement différente et déjà existante : le **troc joueur-à-joueur** (`/faction troc`, `/faction accepter`).

Le fichier de troc a été **reconstruit à l'identique** et le nouveau système de commerce a été déplacé dans son **propre package** (`fr.faction.commerce`, classes `CommerceManager` + `Contract`) pour ne plus jamais entrer en conflit. Les deux fonctionnalités sont maintenant **strictement indépendantes**.

---

## 🆕 Récapitulatif des changements v5.12.0

- ✅ Nouveaux rôles **Navigateur** et **Cheminot** dans la fiche villageois.
- ✅ Commandes `/faction port definir`, `/faction gare definir`, `/faction contrat creer|liste|assigner|annuler`.
- ✅ Système de contrats avec 4 états : `WAITING` → `IN_TRANSIT` → `DELIVERED` / `CANCELLED`.
- ✅ Voyage en ligne droite, à vue, à petite vitesse (pas de pathfinding complexe).
- ✅ Fret protégé par faction (la faction propriétaire + ses alliées uniquement).
- ✅ Déplacement du commerce dans `fr.faction.commerce` pour éviter tout conflit futur avec le troc.
- ✅ `TradeManager` du troc reconstruit à l'identique dans son package d'origine.
- ✅ Version `5.12.0` dans `pom.xml` et `plugin.yml`.

---

## 🧹 Petites corrections de compilation

Cette version corrige également plusieurs incompatibilités avec **Paper 1.21.4** introduites par les renommages d'enums / particules / matériaux entre 1.20 et 1.21 :

| Ancien nom (1.20) | Nouveau nom (1.21.4) | Fichier |
|---|---|---|
| `Particle.VILLAGER_HAPPY` | `Particle.HAPPY_VILLAGER` | `PrivateChestManager` |
| `Particle.SPELL_WITCH` | `Particle.WITCH` | `SortMenuGUI` |
| `Sound.ENTITY_PLAYER_EAT` | `Sound.ENTITY_GENERIC_EAT` | `VillagerManager` |
| `Enchantment.LUCK` | `Enchantment.LOOTING` | `MainMenuGUI`, `FactionGUI`, `FactionRankingGUI`, `ShopCreateGUI` |
| `Material.GOLD_STAINED_GLASS_PANE` | `Material.YELLOW_STAINED_GLASS_PANE` | `ShopCreateGUI` |
| `Material.BED` | `Material.RED_BED` | `MainMenuGUI` |
| `PotionEffectType.INCREASE_DAMAGE` | `PotionEffectType.STRENGTH` | `FactionPowerManager` |
| `PotionEffectType.DAMAGE_RESISTANCE` | `PotionEffectType.RESISTANCE` | `FactionPowerManager` |
| `ShopManager.BuyResult.NOT_ENOUGH_MONEY` | `ShopManager.BuyResult.NOT_ENOUGH_PAYMENT` | `ShopGUI` |
| `org.bukkit.plugin.JavaPlugin` (mauvais package) | `org.bukkit.plugin.java.JavaPlugin` | `ShopCreateGUI`, `SortMenuGUI` |
| `EntitySleepEvent` (n'existe plus en 1.21) | polling `Villager#isSleeping()` dans `tickAll` | `VillagerManager` |
| `MapPalette.matchColor(org.bukkit.Color)` | conversion explicite en `java.awt.Color` | `FactionMapRenderer` |

Import `org.bukkit.Location` ajouté à `PlayerTeleportManager`, et un cast `final MapView` ajouté à `FactionMapManager` pour permettre la `forEach`/`lambda` (la variable `view` est réassignée dans plusieurs branches).

---

## 📦 Migration de données

**Aucune migration nécessaire.** Remplacez simplement le `.jar` et redémarrez. Tous les fichiers YAML existants (`factions.yml`, `villagers.yml`, `stats.yml`, `villages.yml`, `commerce.yml`, `shop.yml`, `claims.yml`, `stats.yml`, etc.) sont rechargés tels quels.

Les **contrats en transit** au moment de la mise à jour seront perdus (pas de persistance encore — à venir si besoin). Les contrats en attente ou déjà livrés sont conservés.

---

## 📥 Installation

1. Téléchargez **`FactionPlugin-5.12.0.jar`** (colonne de droite de cette release).
2. Posez-le dans `plugins/` de votre serveur **Paper 1.21.4+** (Java 21).
3. Démarrez (ou redémarrez) — la config se crée dans `plugins/FactionPlugin/`.
4. Tapez `/faction` en jeu pour ouvrir le menu principal.

> 💡 Les codeurs voudront aussi le **code source** : `FactionPlugin-v5.12.0-source.zip` est attaché en bas. Pour recompiler : `cd FactionPlugin-v5.12.0 && mvn clean package`.

---

## 🔧 Notes techniques

- **API cible** : `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`
- **Java** : 21 (compilé en `target 21`)
- **Taille du JAR** : ≈ 470 KB
- **Compatibilité descendante** : tous les fichiers YAML des versions précédentes (>= v5.0.0) sont rechargés tels quels.
- **Nouveau package** : `fr.faction.commerce` (CommerceManager + Contract + PostType importé de `fr.faction.village`).
