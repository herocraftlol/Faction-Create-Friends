# CHANGELOG — FactionPlugin v5.10.2

## Nouveautés v5.10.2

### 🔒 Verrouillage d'accès aux GUIs de villageois (`verrou-gui`)
- **Un seul joueur à la fois** peut ouvrir la fiche détaillée d'un villageois recruté. Si un autre joueur essaie d'y accéder en même temps, il reçoit un message clair : *« Ce villageois est déjà géré par Pseudo en ce moment. Réessaie dans un instant. »*
- **Libération automatique du verrou** dans tous les cas :
  - Lors de la fermeture normale de la GUI (par le joueur qui la possède)
  - Sur déconnexion du joueur (filet de sécurité) — le verrou est nettoyé proprement, aucun villageois ne reste « bloqué »
  - Au rechargement du plugin (`/reload`)
- **Aucune action perdue** : pendant qu'un joueur gère un villageois, ses clics s'exécutent normalement ; les autres joueurs reçoivent simplement un refus poli. Plus de modifications de meta ou d'équipement en conflit entre deux personnes !
- **Aucun risque de duplication de données** : évite les conflits quand deux joueurs cliquent en même temps pour modifier le même villageois (équipement, chantier, rôle…).

### 🔧 Correctifs de compilation Paper API 1.21.4
Cette release met à jour en interne les références à l'API moderne de Bukkit/Paper :
- `Enchantment.LUCK` → `Enchantment.LUCK_OF_THE_SEA` (API 1.21)
- `Particle.SPELL_WITCH` → `Particle.WITCH`
- `Particle.SPELL` / `Particle.REDSTONE` → `Particle.HAPPY_VILLAGER` / `Particle.ROSE_RED`
- `Material.GOLD_STAINED_GLASS_PANE` → `Material.YELLOW_STAINED_GLASS_PANE`
- `Material.BED` → `Material.WHITE_BED`
- `Material.GRAY_STAINED_GLASS_PANE` → conservé (toujours valide)
- `PotionEffectType.DAMAGE_RESISTANCE` → `PotionEffectType.RESISTANCE`
- `PotionEffectType.INCREASE_DAMAGE` → `PotionEffectType.STRENGTH`
- `Particle.VILLAGER_ANGRY` → `Particle.ANGRY_VILLAGER`
- `Sound.ENTITY_PLAYER_EAT` → `Sound.ENTITY_PLAYER_BURP`
- `VanillaGoal.MOVE_TO_VILLAGE` → `VanillaGoal.MOVE_BACK_TO_VILLAGE`
- `VanillaGoal.RANDOM_STROLL_LAND` → `VanillaGoal.WATER_AVOIDING_RANDOM_STROLL`
- `MapView` / `MapRenderer` : rendu compatible via `MapPalette.matchColor(java.awt.Color)` + `Color.fromARGB`/`fromRGB`
- Lambda `final MapView finalView` pour autoriser la capture dans `getRenderers().forEach(...)`
- `EconomyResponse.ResponseType.NOT_ENOUGH_MONEY` → `NOT_ENOUGH_PAYMENT`

### 📦 Migration de données
**Aucune.** Remplacez simplement le `.jar` et redémarrez. Les fichiers `villagers.yml`, `factions.yml`, `stats.yml`, etc. restent compatibles.

---

## Résumé
La v5.10.2 consolide le système de villageois recrutés introduit en v5.9.0 et étoffé en v5.10.0/v5.10.1. La principale amélioration visible pour les joueurs est le **verrou-gui** : un seul joueur peut désormais gérer un villageois à la fois, sans risque de conflit. En parallèle, toute la base de code a été modernisée pour compiler proprement sur Paper 1.21.4 sans warnings bloquants, garantissant un binaire stable et un démarrage sans erreur.

Pour toute question ou bug, ouvre une **Issue** sur le dépôt GitHub.
