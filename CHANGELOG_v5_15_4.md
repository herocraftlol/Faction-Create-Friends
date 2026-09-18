# Changelog v5.15.4 — *Liaison site web sans SQL* 🔌🌐✨

Cette version **supprime la dépendance à MySQL pour la commande `/lier`**. Vous n'avez plus besoin de configurer une base de données partagée entre le plugin et le backend du site web : tout passe maintenant par une simple **API HTTP** entre FactionPlugin et le serveur Node.js.

---

## 🌐 Liaison site web sans SQL (`/lier`)

**Avant la v5.15.4 :**
- Obligation d'installer et configurer une **base MySQL partagée** entre le plugin et le backend Node.js du site (table `web_link_codes`).
- Synchronisation manuelle entre les deux configurations (mêmes `host`/`user`/`password` côté plugin et côté site).
- Maintenance lourde : droits à授, migrations, pannes silencieuses en cas de désynchronisation.
- Impossible de faire fonctionner `/lier` en mode «单机 » (solo, sans backend MySQL).

**Depuis la v5.15.4 :**
- Le plugin **génère le code** de liaison côté Minecraft, puis l'envoie au site via `POST /api/faction/push/link-code`.
- L'authentification est une simple clé d'API partagée (header `X-Faction-Key`), déjà configurée côté site dans `.env`.
- Le site stocke le code dans `data/local-game.json` (pas de SQL).
- Le joueur saisit le code sur le site, qui crée la liaison `accountLinks` dans le même fichier JSON.
- **`FactionTabSync` reste optionnel** : si vous voulez la synchronisation du tab HeroTab, vous configurez la section `mysql:` comme avant. Sinon, `/lier` fonctionne quand même.

### Configuration minimale (côté plugin)

```yaml
# plugins/FactionPlugin/config.yml
site-url: "http://192.168.1.196:3000"
faction-api-key: "la-même-clé-que-dans-le-.env-du-site"

# Section mysql: OPTIONNELLE — uniquement si vous utilisez FactionTabSync
# mysql:
#   host: 127.0.0.1
#   port: 3306
#   database: herocraft
#   user: herocraft_user
#   password: mot-de-passe
```

### Configuration minimale (côté site)

```env
# .env du backend Node.js
FACTION_API_KEY=la-même-clé-que-dans-le-config.yml-du-plugin
GAME_PUSH_PATH=/api/faction/push/link-code
```

### Flux complet

1. Le joueur tape `/lier` en jeu.
2. Le plugin génère un code à 6 chiffres valable 10 minutes.
3. Le plugin POST le code au site via `POST /api/faction/push/link-code` (header `X-Faction-Key`).
4. Le site stocke le code dans `data/local-game.json` côté serveur.
5. Le joueur va sur le site, se connecte, saisit le code.
6. Le site crée la liaison `accountLinks` dans le même fichier JSON.

---

## 🛠️ Compilation Paper 1.21.4 propre

Cette release inclut également plusieurs correctifs de compatibilité avec la dernière API Paper 1.21.4, afin que `mvn clean package` produise un `.jar` fonctionnel du premier coup :

- ✅ `org.bukkit.plugin.JavaPlugin` → `org.bukkit.plugin.java.JavaPlugin` (`ShopCreateGUI`, `SortMenuGUI`).
- ✅ Imports `Location`, `PostType`, `Material`, etc. ajoutés/corrigés.
- ✅ `Material.BED` → `RED_BED`, `GOLD_STAINED_GLASS_PANE` → `YELLOW_STAINED_GLASS_PANE`.
- ✅ `Enchantment.LUCK` → `LUCK_OF_THE_SEA`.
- ✅ `PotionEffectType.INCREASE_DAMAGE` → `STRENGTH`, `DAMAGE_RESISTANCE` → `RESISTANCE`.
- ✅ `Sound.ENTITY_PLAYER_EAT` → `ENTITY_GENERIC_EAT` (supprimé en 1.21.4).
- ✅ `Particle.SPELL_WITCH` → `WITCH`, `VILLAGER_HAPPY` → `HAPPY_VILLAGER`.
- ✅ `MapPalette.matchColor(Color)` → `matchColor(java.awt.Color)` (signature changée en 1.21.4).
- ✅ `EntitySleepEvent` (supprimé en 1.21.4) → **polling** de `Villager#isSleeping()` avec transition endormi → éveillé.
- ✅ Variable `view` rendue `final` avant `forEach(r -> view.removeRenderer(r))` dans `FactionMapManager`.
- ✅ Nouvelle Surcharge `cmdItem(Material, name, l1, l2, l3, enabled)` dans `MainMenuGUI` pour les tooltips à plusieurs lignes.

---

## ✅ Migration depuis v5.15.3 (ou v5.15.x)

**Aucune migration de données** : tous les fichiers `data/` (factions, contrats, villages, alliances, shops, rangs) sont **rétro-compatibles**.

### Si vous utilisiez `/lier` en MySQL avant

Vous avez deux options :

**Option A — Basculer sur le nouveau mode sans SQL (recommandé) :**
1. Supprimez la section `mysql:` du `config.yml` du plugin (gardez-la uniquement si vous voulez `FactionTabSync`).
2. Ajoutez `site-url` et `faction-api-key` (clés partagées avec `.env` du site).
3. Mettez à jour le backend du site pour qu'il accepte les requêtes `POST /api/faction/push/link-code`.
4. Redémarrez.

**Option B — Garder MySQL pour `/lier` (legacy) :**
- La section `mysql:` reste valide si vous l'utilisez pour `FactionTabSync` ou un backend site qui lit encore la table `web_link_codes`.
- Dans ce cas, le plugin utilise MySQL pour le tab, et `/lier` utilise le nouveau mode HTTP.

### Si vous utilisiez `FactionTabSync` (v5.15.0+)

Aucune action requise : la section `mysql:` reste utilisée pour le tab HeroTab. La nouvelle méthode HTTP pour `/lier` n'entre pas en conflict avec MySQL — les deux fonctionnent en parallèle.

---

## 📦 Téléchargements

- **JAR** : `FactionPlugin-5.15.4.jar` (≈ 480 KB).
- **Sources** : `FactionPlugin-5.15.4-source.zip` (≈ 270 KB).

---

## 🛠 Installation

1. Téléchargez `FactionPlugin-5.15.4.jar`.
2. Placez-le dans `plugins/` de votre serveur Paper 1.21.4.
3. Redémarrez le serveur.
4. Le fichier `plugins/FactionPlugin/` et ses sous-dossiers seront générés automatiquement.

---

## 📜 Liens utiles

- 🏠 Dépôt : https://github.com/herocraftlol/Faction-Create-Friends
- 📦 Releases : https://github.com/herocraftlol/Faction-Create-Friends/releases
- 🐞 Signaler un bug : https://github.com/herocraftlol/Faction-Create-Friends/issues

---

_Cette release a été préparée par un agent IA (OpenHands) au nom du mainteneur du projet._