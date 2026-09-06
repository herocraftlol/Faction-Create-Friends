# CHANGELOG — FactionPlugin v5.9.1

## ⚔️ Nouveautés v5.9.1 — *Les Guerriers prennent vie*

Cette version enrichit considérablement le système de villageois recrutés
introduit en v5.9.0. Les **Guerriers** obtiennent un véritable comportement
autonome : poste de garde, périmètre de défense, patrouille tracée,
formation militaire, et détection intelligente des ennemis pendant les
guerres inter-factions. Un correctif est également inclus sur l'ergonomie
des GUIs.

---

### 🛡️ Poste de garde (slot 36 du GUI)
- En un clic, le poste d'un guerrier est défini à sa **position actuelle**.
- Le guerrier y **revient automatiquement** s'il s'en éloigne (entre deux
  combats, ou après une poursuite).
- Idéal pour garder un portail, une porte de base, un pont-levis ou un point
  sensible.

### 📍 Rayon de défense (slot 37)
- Définit un **périmètre circulaire** autour du poste, de **4 à 48 blocs**.
- Le guerrier y effectue des **rondes automatiques**.
- En cas de menace détectée dans ou hors du périmètre, il sort de sa ronde
  pour l'affronter (jusqu'à 24 blocs de poursuite) puis reprend sa position.
- Configurable directement via le GUI ou via le chat (`<rayon> | annuler`).

### 🗺️ Patrouille tracée dans le monde (slot 38)
- Tracez **deux points** dans le monde : le guerrier y effectuera **des
  allers-retours** autonomes en restant à l'intérieur du périmètre défini.
- Sélection annulable à tout moment par `/faction annuler` ou en tapant
  `annuler` dans le chat.
- Parfait pour patrouiller un couloir, un hall, ou le tour d'un bâtiment
  complexe.

### 🧠 Combat intelligent (slot 39)
- Active / désactive le mode combat du guerrier.
- Quand actif, le guerrier :
  1. **Détecte automatiquement** les mobs hostiles **et** les joueurs
     ennemis de votre faction (vérification via `FactionManager`).
  2. **Consulte `WarSession`** : pendant une guerre, les factions
     belligérantes sont automatiquement hostiles, même si elles ne se
     connaissent pas.
  3. **Poursuit** la cible jusqu'à 24 blocs et l'attaque avec son arme
     équipée.
  4. **Revient** à son poste / sa patrouille une fois la cible vaincue ou
     hors de portée.
- **Soin automatique** grâce à l'emplacement *Nourriture* (coche dans le
  GUI) tant que le guerrier n'est pas à pleine vie.

### 🪖 Formation militaire — `/faction villageois formation`
- Aligne **tous les villageois recrutés dans un rayon de 40 blocs** devant
  vous, à 1,5 bloc d'espacement.
- Idéal pour les cérémonies, les entraînements, les embuscades ou tout
  simplement pour faire impression.
- Re-cliquez la commande pour réorganiser la formation après déplacement.

### 🚶 Mode suivi du chef (slot 41)
- Activez ou désactivez le mode « **suit le joueur** » : votre guerrier vous
  accompagnera comme un compagnon loyal jusqu'à annulation.
- Très utile pour escorter un Constructeur jusqu'à un nouveau chantier,
  ou pour vous déplacer avec votre garde personnelle.

### 🩹 Correctif du double-clic sur les GUIs
- Symptôme : un double-clic rapide pouvait exécuter deux actions — la
  première ouverture de menu rouvre le menu parent immédiatement, ce qui
  déclenchait un second traitement avant la fermeture effective.
- Correction : un **cooldown GUI de 150 ms** côté logique de clic ignore
  les clics trop rapprochés, rendant toutes les interactions fluides et
  prévisibles.

### 📚 Commande utilitaire : `/faction annuler`
- Annule **toute sélection en cours** (patrouille tracée, chantier, rayon
  entré via chat) pour le joueur courant.

---

### 🔧 Compatibilité Paper 1.21.4
- Mise à jour des références Bukkit pour Paper **1.21.4** :
  `Enchantment.LUCK` → `LUCK_OF_THE_SEA`, `Particle.SPELL_WITCH` →
  `Particle.WITCH`, `Particle.VILLAGER_HAPPY` →
  `Particle.HAPPY_VILLAGER`, `Sound.ENTITY_PLAYER_EAT` →
  `Sound.ENTITY_GENERIC_EAT`, `PotionEffectType.DAMAGE_RESISTANCE` →
  `PotionEffectType.RESISTANCE`, `Material.GOLD_STAINED_GLASS_PANE` →
  `YELLOW_STAINED_GLASS_PANE`, etc.
- Ajout des imports manquants (`org.bukkit.Bukkit`, `org.bukkit.Location`,
  `org.bukkit.plugin.java.JavaPlugin`) et résolution de l'ambiguïté
  `java.awt.Color` vs `org.bukkit.Color` pour `MapPalette.matchColor`.
- Le GUI principal accepte désormais un nombre variable de lignes de lore
  supplémentaires pour les commandes de faction, ce qui supprime l'erreur
  de signature observée en v5.9.0.

---

## Fichiers modifiés
```
src/main/java/fr/faction/FactionPlugin.java           ← import Bukkit ajouté
src/main/java/fr/faction/FactionPlugin.java           ← Guerre/villageois branchés
src/main/java/fr/faction/villager/
  ├── RecruitedVillager.java                          ← poste + rayon + formation
  ├── VillagerGUI.java                                ← slots 36-41 (poste, rayon, patrol, combat, chantier, suivi)
  └── VillagerManager.java                            ← IA combattant, formation, isEnemyFaction, WarSession
src/main/java/fr/faction/gui/MainMenuGUI.java         ← cmdItem arity, BED → RED_BED
src/main/java/fr/faction/map/FactionMapRenderer.java  ← java.awt.Color explicite (matchColor(int,int,int))
src/main/java/fr/faction/map/FactionMapManager.java   ← lambda MapView::removeRenderer
src/main/java/fr/faction/power/FactionPowerManager.java  ← INCREASE_DAMAGE → STRENGTH, DAMAGE_RESISTANCE → RESISTANCE
src/main/java/fr/faction/gui/FactionGUI.java, FactionRankingGUI.java ← LUCK → LUCK_OF_THE_SEA
src/main/java/fr/faction/shop/ShopGUI.java           ← enum NOT_ENOUGH_PAYMENT
src/main/java/fr/faction/shop/ShopCreateGUI.java      ← GOLD_STAINED_GLASS_PANE → YELLOW_STAINED_GLASS_PANE, LUCK → LUCK_OF_THE_SEA
src/main/java/fr/faction/sort/SortMenuGUI.java        ← SPELL_WITCH → WITCH
src/main/java/fr/faction/alliance/PrivateChestManager.java ← VILLAGER_HAPPY → HAPPY_VILLAGER
src/main/java/fr/faction/alliance/PlayerTeleportManager.java ← import Location
src/main/java/fr/faction/commands/FactionCommand.java ← handleVillageois + formation, handleAnnuler
src/main/java/fr/faction/web/WebMapSync.java         ← échappement JSON (cx/cz/world)
src/main/resources/plugin.yml                        ← version 5.9.1, descriptions enrichies
pom.xml                                              ← version 5.9.1, paper-api 1.21.4-R0.1-SNAPSHOT
```

---

## Commandes ajoutées / modifiées
| Commande | Effet |
|---|---|
| `/faction villageois formation` (alias `ranger`) | Met tous les villageois à 40 blocs en formation militaire devant vous 🆕 |
| `/faction annuler` | Annule toute sélection en cours 🆕 |
| `/faction recruter` | Inchangé (déjà présent en v5.9.0) |

## Slots GUI ajoutés dans `VillagerGUI.openDetail` (guerriers)
| Slot | Action |
|---|---|
| 36 | Définir le **poste** à la position actuelle du villageois |
| 37 | Définir le **rayon** de défense (4 – 48 blocs) |
| 38 | Démarrer la **patrouille** (clics sur 2 points du monde) |
| 39 | Activer / désactiver le **combat intelligent** |
| 41 | Activer / désactiver le **mode suivi du joueur** |

---

## Version
- `pom.xml` : 5.9.1
- `plugin.yml` : 5.9.1

---

## Historique récent
- v5.9.0 : Villageois recrutés (Constructeur / Guerrier de base, chantier, équipement, nourriture)
- v5.8.4 : Sous-chefs
- v5.3.0 : Tri de coffre & d'inventaire
- v5.2.0 : Comptoir d'échange
- v5.1.1 : Guerre inter-factions, nouveau menu principal
- v5.0.0 : Alliances, homes, spawn faction, /tpa, coffres privés
