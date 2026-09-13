# CHANGELOG — FactionPlugin v5.13.1

## 🐛 Correctif principal : le tab n'était pas rafraîchi en quittant une faction

La v5.13.1 corrige un bug visuellement très désagréable : après avoir **quitté**, **été expulsé** ou **vu sa faction dissoute**, votre **tab** (la liste des joueurs) continuait d'afficher l'ancienne faction. Le problème ne se résolvait tout seul qu'à la première action suivante (rejoindre, fonder, etc.), créant une incohérence trompeuse — surtout pour quelqu'un qui crée ou rejoint immédiatement une autre faction.

### Avant / Après

| Situation | En v5.13.0 | En v5.13.1 |
|-----------|------------|------------|
| Vous faites `/faction leave` | Le tab garde `« [AncienneFaction] Pseudo »` jusqu'à un autre événement | Le tab passe immédiatement à `« Pseudo »` |
| Vous êtes expulsé via `/faction kick` | Idem | Idem |
| Votre faction est dissoute via `/faction disband` | Idem | Idem |

### Pourquoi ce bug n'arrivait qu'à ces trois commandes ?

`/faction create` et `/faction join` forçaient déjà la mise à jour du tab dès le changement de faction (parce qu'on rejoint quelque chose — l'effet est immédiat et le code l'appelait déjà).
Mais `/faction leave`, `/faction kick` et `/faction disband` ne faisaient qu'**enlever** une appartenance — et la branche de code correspondante n'appelait jamais `tabManager.refresh(...)` pour les joueurs concernés. Résultat, le tab gardait l'ancien préfixe de faction en mémoire.

### Le correctif

Les trois commandes appellent maintenant `tabManager.refresh(...)` **pour chaque joueur concerné** :
- `leave` → refresh du joueur qui quitte
- `kick` → refresh du joueur expulsé (et du kicker si lui-même change d'état)
- `disband` → refresh de tous les anciens membres de la faction dissoute

En complément, l'envoi immédiat au site web (Herosite / HeroTab) déjà ajouté en v5.13.0 continue de s'appliquer : tout changement visible dans le tab l'est **aussi** sur le dashboard web, sans attendre le cycle de 60 secondes.

---

## 🔧 Correctifs de compilation (Paper API 1.21.4)

Quelques symboles ajoutés / renommés / déplacés entre Paper 1.21.0 et 1.21.4 posaient un refus de compilation. Ils sont corrigés ici pour que `mvn clean package` produise un JAR fonctionnel sur toutes les versions récentes de Paper 1.21.x :

| Symbole | Avant | Après | Fichier(s) |
|---------|-------|-------|------------|
| `Material.BED` | retiré en 1.21 | `Material.RED_BED` | `MainMenuGUI` |
| `Enchantment.LUCK` | retiré en 1.21 | `Enchantment.LOOTING` | `MainMenuGUI`, `FactionGUI`, `FactionRankingGUI`, `ShopCreateGUI` |
| `PotionEffectType.INCREASE_DAMAGE` / `DAMAGE_RESISTANCE` | renommés en 1.21 | `STRENGTH` / `RESISTANCE` | `FactionPowerManager` |
| `Material.GOLD_STAINED_GLASS_PANE` | renommé en 1.21 | `YELLOW_STAINED_GLASS_PANE` | `ShopCreateGUI` |
| `Particle.SPELL_WITCH` / `VILLAGER_HAPPY` | renommés en 1.21 | `WITCH` / `HAPPY_VILLAGER` | `SortMenuGUI`, `PrivateChestManager` |
| `Sound.ENTITY_PLAYER_EAT` | renommé en 1.21 | `ENTITY_GENERIC_EAT` | `VillagerManager` |
| `BuyResult.NOT_ENOUGH_MONEY` | renommé en 1.21 | `NOT_ENOUGH_PAYMENT` | `ShopGUI` |
| `org.bukkit.plugin.JavaPlugin` (mauvais import) | remplacé | `org.bukkit.plugin.java.JavaPlugin` | `ShopCreateGUI`, `SortMenuGUI` |
| `MapPalette.matchColor(org.bukkit.Color)` | signature changée | `MapPalette.matchColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue()))` | `FactionMapRenderer` |
| `EntitySleepEvent` | n'est plus jamais lancé sur Paper 1.21+ | listener supprimé (soin nocturne déjà géré par polling dans `tickAll()` depuis la v5.10.3) | `VillagerManager` |
| `view.removeRenderer(r)` dans lambda | variable non-finale | `final MapView finalView = view;` (lambda renomme la variable pour le compilateur) | `FactionMapManager` |
| `fr.faction.commerce.Contract` | import `PostType` manquant | ajout de `import fr.faction.village.PostType;` | `Contract` |

Ces corrections rendent le projet **compilable sur Paper 1.21.4** avec un simple `mvn clean package`.

---

## 📦 Migration

Aucune migration. Remplacer le `.jar` v5.12.0 par le v5.13.1 et redémarrer le serveur suffit. Tous les fichiers YAML (`factions.yml`, `villagers.yml`, `stats.yml`, `villages.yml`, `commerce.yml`, `shop.yml`, etc.) restent compatibles.

---

## 📥 Installation / recompilation

1. Téléchargez `FactionPlugin-5.13.1.jar` (asset ci-dessous)
2. Posez-le dans `plugins/` de votre serveur Paper 1.21.4+
3. Démarrez / redémarrez — la config se crée dans `plugins/FactionPlugin/`
4. Tapez `/faction` en jeu pour ouvrir le menu principal

> 💡 Vous voulez recompiler ? Le code source est joint : `FactionPlugin-v5.13.1-source.zip`. Décompressez, puis `mvn clean package` (Java 21, Maven 3.9+). Le JAR produit est dans `target/FactionPlugin-5.13.1.jar`.

### Notes techniques

- **API cible** : `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT`
- **Java** : 21 (compilé en `target 21`)
- **Taille du JAR** : ≈ 470 KB
- **Compatibilité descendante** : tous les YAML des versions >= v5.0.0 sont rechargés tels quels.

— _Cette release a été créée par un agent OpenHands pour le compte d'herocraftlol._
