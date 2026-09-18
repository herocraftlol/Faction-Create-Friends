# Changelog v5.15.5 — *Compilation propre & stabilité* 🛠️⚡📦

Cette version **consolide** tout le travail des releases v5.15.x et apporte un ensemble de corrections qui rendent le plugin **immédiatement compilable et fonctionnel sur Paper 1.21.4**, sans rien casser de ce qui existait déjà.

> Si tu utilisais déjà la v5.15.4, lis au moins la partie **« Migration depuis v5.15.4 »** — il n'y a aucune action obligatoire, mais quelques notes utiles.

---

## 🚀 Nouveautés de la v5.15.5

### ✅ Compilation Paper 1.21.4 garantie dès le premier `mvn clean package`
La v5.15.4 introduisait déjà plusieurs corrections de compatibilité avec Paper 1.21.4 ; cette release **les finalise** et corrige les dernières incompatibilités restantes pour que `mvn clean package` produise un `.jar` fonctionnel du premier coup, sans patch manuel.

- ✅ `org.bukkit.plugin.JavaPlugin` → `org.bukkit.plugin.java.JavaPlugin` corrigé dans **`ShopCreateGUI`** et **`SortMenuGUI`**.
- ✅ Import `fr.faction.village.PostType` ajouté dans **`Contract.java`** (commerce inter-villes).
- ✅ Import `org.bukkit.Location` ajouté dans **`PlayerTeleportManager.java`**.

### 🌙 Soin en dormant — détection par polling (Paper 1.21.4)
`EntitySleepEvent` a été **supprimé en Paper 1.21.4**. La régénération des villageois recrutés dans leur lit est maintenant détectée par **polling de `Villager#isSleeping()`** à chaque tick d'IA : la transition *éveillé → endormi* déclenche le même soin périodique qu'auparavant.

- ✅ Comportement identique à la v5.15.0 — pas de régression côté joueur.
- ✅ Plus aucun appel à une classe/événement inexistant, plus d'`ClassNotFoundError` silencieux en console.

### 🛠 Corrections mineures du `MapRenderer` (mini-map de faction)
- ✅ `MapPalette.matchColor(Color)` a été supprimé en 1.21.4 → utilisation de la nouvelle signature **`matchColor(r, g, b)`** dans `FactionMapRenderer`.
- ✅ Variable `view` capturée en `final` avant le `forEach` qui retire les renderers vanilla — corrige l'erreur *« local variables referenced from a lambda expression must be final or effectively final »*.

### 🎨 Enrichissement du GUI principal
- ✅ Nouvelle surcharge **`cmdItem(Material, name, desc, extra, tip, enabled)`** dans `MainMenuGUI` pour les tooltips à plusieurs lignes (utilisée par les nouvelles entrées « command » de la v5.15.5).

### 🏷️ Version & message de démarrage
- ✅ Version exposée par `getDescription().getVersion()` et `/version` : **5.15.5**.
- ✅ Message de démarrage du plugin :
  > `FactionPlugin v5.15.5 — recrutement illimité, villageois niveau 100, rang Mythique, shop sécurisé et améliorations de stabilité`

### 📦 Aucun changement de format de données
Tous les fichiers générés dans `plugins/FactionPlugin/` restent **strictement rétro-compatibles** :

- `factions.yml`, `claims.yml`, `bank.yml`, `ranking.yml`, `map.yml`
- `stats.yml`, `villagers.yml`, `shop.yml`, `homes.yml`, `privatechests.yml`, `contracts.yml`

→ **Aucune migration à effectuer** : il suffit de remplacer le `.jar`.

---

## 🔄 Migration depuis v5.15.4

**Aucune action requise.** Il te suffit de :

1. Remplacer `FactionPlugin-5.15.4.jar` par `FactionPlugin-5.15.5.jar` dans le dossier `plugins/`.
2. Redémarrer le serveur.

Toutes les fonctionnalités de la v5.15.4 sont conservées à l'identique :

- Liaison site web sans SQL (`/lier` via HTTP + clé d'API).
- `FactionTabSync` (synchro du tab HeroTab via MySQL, si configuré).
- Compilation Paper 1.21.4 propre.
- Tous les correctifs de stabilité, de fusion et de synchro des releases précédentes.

---

## 📦 Téléchargements

- **JAR** : `FactionPlugin-5.15.5.jar` (≈ 484 KB)
- **Sources** : `FactionPlugin-5.15.5-source.zip` (≈ 260 KB)

---

## 🛠 Installation

1. Télécharge `FactionPlugin-5.15.5.jar`.
2. Place-le dans `plugins/` de ton serveur Paper 1.21.4 (en remplaçant l'ancien `.jar`).
3. Redémarre le serveur.
4. Les dossiers et fichiers de données seront générés automatiquement dans `plugins/FactionPlugin/`.

---

## 📜 Liens utiles

- 🏠 Dépôt : https://github.com/herocraftlol/Faction-Create-Friends
- 📦 Releases : https://github.com/herocraftlol/Faction-Create-Friends/releases
- 🐞 Signaler un bug : https://github.com/herocraftlol/Faction-Create-Friends/issues

---

_Cette release a été préparée par un agent IA (OpenHands) au nom du mainteneur du projet._