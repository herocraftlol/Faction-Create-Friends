# 📜 CHANGELOG — FactionPlugin v5.9.4

> 🏹🛌 *Les Archers et le Sommeil réparateur* — publié le 2026‑09‑06.
> _Cette publication a été préparée par un agent IA (OpenHands) pour le compte de herocraftlol, à partir de la pull request `release/v5.9.4`._

---

## 🎯 Résumé de la version

**FactionPlugin 5.9.4** enrichit le système de **villageois recrutés** introduit en 5.9.0 et perfectionné en 5.9.1 (Guerriers autonomes) et 5.9.2/5.9.3 (file de chantiers, assignations de groupe, suivi longue distance). Cette version se concentre sur deux axes :

1. **Le mode archerie 🏹** — les Guerriers peuvent désormais tirer à distance tout en maintenant une distance d'engagement idéale, et basculent automatiquement en mêlée s'ils sont à court de flèches ou si l'ennemi se rapproche.
2. **Le sommeil réparateur 🛌** — tous les villageois recrutés regagnent progressivement de la vie tant qu'ils dorment dans un lit, via l'IA vanille de Minecraft.

Le plugin reste compatible **Paper 1.21.4** et nécessite **Java 21**.

---

## 🏹 Nouveauté 1 — Mode archerie pour les Guerriers

### Pour le joueur

- Deux nouveaux emplacements dans la fiche du Guerrier : **Arc / Arbalète** et **Flèches**.
- Un toggle dédié **« Mode archerie »** active ou désactive le comportement à distance.
- Le Guerrier **choisit automatiquement** son mode de combat selon la situation :
  - **Mode archerie actif + arc + flèches disponibles + ennemi ≥ 6 blocs** → tir à l'arc en maintenant la distance (recule si l'ennemi approche).
  - **Mode archerie actif mais plus de flèches** → bascule immédiate en mêlée (épée si équipée, sinon à mains nues).
  - **Ennemi déjà au corps à corps (< 2 blocs)** → bascule immédiate en mêlée, même avec des flèches en stock, pour éviter de se tirer dessus.
  - **Mode archerie désactivé** → comportement de mêlée d'origine, inchangé.
- Aucune commande en plus — tout se règle depuis le GUI existant ou via le slot toggle « Mode archerie ».

### Implémentation

- Ajout des champs `bow`, `arrows`, `archeryMode` dans `RecruitedVillager` (persistés dans `villagers.yml`).
- Détection d'arc / arbalète via `Material.BOW` / `Material.CROSSBOW`.
- Détection de flèches via suffixe `ARROW` (couvre `ARROW`, `SPECTRAL_ARROW`, etc.).
- Boucle d'IA de combat (`warriorTick`) étendue :
  - Évaluation `canArcher = archeryMode && bow != null && countArrows(rv) > 0`.
  - Si `useArcher && dist > 2.2` → tir (cooldown `archer-cooldown-ms`) avec recul automatique vers `archer-min-distance`.
  - Sinon → comportement de mêlée d'origine.
- Tir : génération d'une `Arrow` (vitesse + dommage configurables), `playSound(Sound.ENTITY_ARROW_SHOOT)`.

### Réglages disponibles (`config.yml → villager`)

```yaml
villager:
  archer-min-distance: 6.0   # distance d'engagement idéale
  archer-max-distance: 18.0  # portée de tir maximale
  archer-cooldown-ms: 1500   # temps minimum entre deux tirs
  archer-damage: 3.0         # dégâts de la flèche tirée par le villageois
```

---

## 🛌 Nouveauté 2 — Soin par le sommeil

### Pour le joueur

- Quand un villageois recruté va dormir dans un lit (la nuit ou après une longue journée de travail), **il regagne progressivement de la vie** pendant qu'il dort.
- Aucun plugin supplémentaire, aucune commande : le comportement s'active automatiquement dès que la v5.9.4 est installée.
- Configurable via `config.yml` pour s'adapter à l'équilibrage de votre serveur.

### Implémentation

- L'événement `EntitySleepEvent`, utilisé dans le code initial, a été retiré (non disponible sur Paper 1.21.4).
- Remplacement par une **détection dans la boucle d'IA** : chaque tick, on consulte `LivingEntity.isSleeping()` pour le villageois.
- Un `Set<UUID>` (`sleepingVillagers`) évite de relancer le soin si l'unité passe la nuit entière couchée.
- Quand le villageois se réveille, l'entrée est retirée et il reprend sa consommation de nourriture normale.

### Réglages disponibles (`config.yml → villager`)

```yaml
villager:
  heal-per-sleep-tick: 2.0    # points de vie rendus par tick de soin
  sleep-heal-ticks: 8         # nombre de soins administrés tant qu'il reste au lit
  sleep-heal-period: 60       # période entre deux soins (60 ticks = 3 s)
```

---

## 🐛 Correctifs techniques transverses

Cette release a aussi servi à aligner le projet sur les changements d'API introduits par Paper 1.21 (renommages et dépréciations), pour garantir une compilation propre sans warning bloquant.

- `Enchantment.LUCK` → `Enchantment.LUCK_OF_THE_SEA` (Paper 1.21)
- `Material.GOLD_STAINED_GLASS_PANE` → `Material.YELLOW_STAINED_GLASS_PANE` (Paper 1.21)
- `Material.BED` → `Material.RED_BED` (Paper 1.21)
- `PotionEffectType.INCREASE_DAMAGE` → `PotionEffectType.STRENGTH` (Paper 1.21)
- `PotionEffectType.DAMAGE_RESISTANCE` → `PotionEffectType.RESISTANCE` (Paper 1.21)
- `Particle.SPELL_WITCH` → `Particle.WITCH` (Paper 1.21)
- `Particle.VILLAGER_HAPPY` → `Particle.HAPPY_VILLAGER` (Paper 1.21)
- `Sound.ENTITY_PLAYER_EAT` → `Sound.ENTITY_GENERIC_EAT` (Paper 1.21)
- Import `org.bukkit.plugin.java.JavaPlugin` (sous‑package déplacé)
- Correction d'une **variable non‑effectivement‑finale** capturée par une lambda dans `FactionMapManager`
- Réordonnancement des appels `cmdItem(..., boolean, varargs)` pour respecter la nouvelle signature acceptant du lore supplémentaire
- Mise à jour de `plugin.yml` (description v5.9.4) et de `config.yml` (section `villager` exposant tous les paramètres nouveaux)

---

## 📦 Contenu de la release

- `FactionPlugin-5.9.4.jar` — binaire compilé pour Paper 1.21.4.
- `FactionPlugin-5.9.4-source.zip` — code source complet (Maven project, prêt à compiler avec `mvn clean package`).
- `CHANGELOG_v5_9_4.md` — ce fichier.

## 🔗 Liens

- 📥 **Téléchargement direct** : voir les *Assets* ci‑dessous.
- 📘 **README complet** : [README.md](./README.md)
- 🏷️ **Comparaison** : [v5.9.3…v5.9.4](../../compare/v5.9.3...v5.9.4)

---

_🤖 Cette version et ses notes de version ont été préparées par un agent IA (OpenHands) pour le compte de herocraftlol. Le code source reste sous licence MIT._
