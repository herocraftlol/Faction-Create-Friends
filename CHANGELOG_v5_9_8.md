# 📜 CHANGELOG — FactionPlugin v5.9.8

> 💀 *Tout tombe au sol à la mort* — publié le 2026-09-06.
> _Cette publication a été préparée par un agent IA (OpenHands) pour le compte de herocraftlol._

---

## 🎯 Résumé de la version

**FactionPlugin 5.9.8** parachève le système de **villageois recrutés** introduit en 5.9.0 en s'attaquant à un défaut longtemps signalé : **la perte silencieuse d'équipement à la mort d'une recrue**. À partir de cette version, un villageois (Constructeur ou Guerrier) qui meurt **lâche au sol tout ce qu'il avait sur lui** — plus rien ne disparaît avec lui.

Cette release consolide également toutes les nouveautés des versions 5.9.5 à 5.9.7 (niveaux d'XP, points de rassemblement, clic-droit direct, butin de guerre, compteur de kills) en un build stable.

Le plugin reste compatible **Paper 1.21.4** et nécessite **Java 21**.

---

## 💀 Nouveauté — Drop complet à la mort

### Pour le joueur

- Un villageois recruté qui meurt **lâche au sol tout son équipement + sa réserve** :
  - Arme et armure équipées
  - Arc et flèches (pour les Guerriers en mode archerie)
  - Nourriture stockée dans son emplacement dédié
  - Réserve complète (matériaux pour le Constructeur, butin de guerre pour le Guerrier)
- Plus besoin de redonner manuellement un équipement neuf après une perte cuisante — il est récupérable sur place.
- La mécanique vanilla de « chance de drop » est désactivée sur les recrues pour éviter les doublons : c'est désormais **100 % du drop qui est géré par le plugin**.

### Implémentation

- Nouveau listener `EntityDeathEvent` dans `VillagerManager` qui intercepte la mort des villageois enregistrés dans la map `villagers`.
- Pour chaque slot d'équipement (main hand, off hand, helmet, chestplate, leggings, boots) et pour chaque slot de la réserve (nourriture + 8 slots spécifiques au rôle), un `ItemStack` est déposé au sol via `world.dropItemNaturally(location, item)`.
- Désactivation du drop vanilla via l'event : `event.getDrops().clear()` puis redrop manuel des items à notre manière.
- Toggle `disable-vanilla-equipment-drop` exposé dans `config.yml → villager`.

### Réglage disponible (`config.yml → villager`)

```yaml
villager:
  # Désactive la mécanique vanilla de drop d'équipement aléatoire sur les villageois
  # recrutés : c'est désormais 100 % du drop qui est géré par le plugin, pour ne rien
  # perdre dans le vide quand ils meurent.
  disable-vanilla-equipment-drop: true
```

---

## ⭐ Nouveautés cumulées depuis la v5.9.4 (rappel)

Cette release consolide aussi les ajouts des versions 5.9.5 → 5.9.7, déjà présents dans le code mais jamais publiés en release officielle.

### v5.9.5 — Niveaux d'expérience, tag de faction

- **5 niveaux d'XP** pour les villageois recrutés, chacun de plus en plus dur à atteindre. Une montée de niveau **soigne entièrement** le villageois et prévient toute la faction dans le chat (« Je suis maintenant niveau X ! »).
  - **Constructeur** : XP en posant des blocs, en récoltant, et un gros bonus en terminant un chantier. Aux niveaux supérieurs, il pose/récolte plus vite et a plus de vie.
  - **Guerrier** : XP à chaque coup porté, et un gros bonus par ennemi achevé. Plus de dégâts et plus de vie max à chaque niveau. Les armes s'usent à l'usage ; à partir du niveau 4, il se fabrique lui-même une arme neuve dès que la sienne casse.
- **Tag de faction** : le nom affiché au-dessus de chaque recrue inclut le niveau et le nom de sa faction, coloré avec l'icône du rang de cette faction.
- La fiche (`/faction villageois`) affiche le niveau et l'XP de chaque villageois.

### v5.9.6 — Point de rassemblement

- Nouveau bouton **« Point de rassemblement »** dans la fiche de chaque villageois : clique, puis clic-droit sur l'endroit voulu dans le monde.
  - **Constructeur** : une fois sa file de chantiers vide, il s'y rend au lieu de rester sur place.
  - **Guerrier** : une fois libre (plus de cible, pas de patrouille, ne suit personne), il s'y rend en priorité sur son poste habituel.
- **Assignation en groupe** : en mode sélection dans `/faction villageois`, le bouton « Rassemblement commun » assigne le même point à tous les villageois actuellement sélectionnés.

### v5.9.7 — Clic-droit direct, butin de guerre, compteur de kills

- **Clic-droit sur un villageois recruté** ouvre directement sa fiche de gestion (au lieu du commerce vanille), pour les membres de sa faction uniquement.
- **Butin de guerre** : un Guerrier ramasse automatiquement ce que lâchent ses victimes (armes, armures, objets…) dans sa réserve personnelle (8 emplacements visibles dans sa fiche). Si la réserve est pleine, le surplus reste au sol.
- **Compteur de kills individuel** : affiché dans la liste et la fiche de chaque Guerrier (« Ennemis tués : X »).

---

## 🐛 Correctifs techniques transverses

Cette release a aussi servi à aligner le projet sur les changements d'API introduits par Paper 1.21 (renommages et dépréciations), pour garantir une compilation propre.

- `Enchantment.LUCK` → `Enchantment.LUCK_OF_THE_SEA` (Paper 1.21)
- `Material.GOLD_STAINED_GLASS_PANE` → `Material.YELLOW_STAINED_GLASS_PANE` (Paper 1.21)
- `Material.BED` → `Material.RED_BED` (Paper 1.21)
- `PotionEffectType.INCREASE_DAMAGE` → `PotionEffectType.STRENGTH` (Paper 1.21)
- `PotionEffectType.DAMAGE_RESISTANCE` → `PotionEffectType.RESISTANCE` (Paper 1.21)
- `Particle.SPELL_WITCH` → `Particle.WITCH` (Paper 1.21)
- `Particle.VILLAGER_HAPPY` → `Particle.HAPPY_VILLAGER` (Paper 1.21)
- `Sound.ENTITY_PLAYER_EAT` → `Sound.ENTITY_GENERIC_EAT` (Paper 1.21)
- `MapPalette.matchColor(org.bukkit.Color)` → `MapPalette.matchColor(java.awt.Color)` (Paper 1.21)
- Import `org.bukkit.plugin.JavaPlugin` → `org.bukkit.plugin.java.JavaPlugin` (sous-package déplacé)
- Suppression du `EntitySleepEvent` (non disponible en Paper 1.21) au profit d'un polling dans la boucle d'IA (`VillagerManager.sleepTick`)
- Correction d'une **variable non effectivement finale** capturée par une lambda dans `FactionMapManager`
- Ajout d'un import `org.bukkit.Location` manquant dans `PlayerTeleportManager`
- Correction du nom d'enum `NOT_ENOUGH_MONEY` → `NOT_ENOUGH_PAYMENT` dans `ShopGUI`
- Surcharge de `cmdItem(Material, String, boolean, String...)` dans `MainMenuGUI` pour accepter un lore variadique
- Mise à jour de `plugin.yml` (version 5.9.8) et de `config.yml` (section `villager` exposant tous les paramètres nouveaux)

---

## 📦 Contenu de la release

- `FactionPlugin-5.9.8.jar` — binaire compilé pour Paper 1.21.4.
- `FactionPlugin-5.9.8-source.zip` — code source complet (Maven project, prêt à compiler avec `mvn clean package`).
- `CHANGELOG_v5_9_8.md` — ce fichier.

## 🔗 Liens

- 📥 **Téléchargement direct** : voir les *Assets* ci-dessous.
- 📘 **README complet** : [README.md](./README.md)
- 🏷️ **Comparaison** : [v5.9.4…v5.9.8](../../compare/v5.9.4...v5.9.8)

---

_🤖 Cette version et ses notes de version ont été préparées par un agent IA (OpenHands) pour le compte de herocraftlol. Le code source reste sous licence MIT._
