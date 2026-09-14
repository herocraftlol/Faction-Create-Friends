# FactionPlugin v5.14.3 — *Synchronisation immédiate du tab lors d'une montée de rang* 🏷️✨

## 🏷️ Nouveauté : la montée de rang met à jour le tab immédiatement, partout

Quand une faction passe un cap de puissance (par exemple `BRONZE → ARGENT`), le **tab de l'end local** se rafraîchissait déjà tout seul. Mais le **tab unifié HeroTab + le site web** dépendaient du cycle d'envoi normal de 60 secondes — un membre pouvait donc continuer à voir `[⬡ Bronze]` après la promotion pendant presque une minute.

À partir de la v5.14.3, **toute montée de rang déclenche une synchronisation tab immédiate**, exactement comme pour un recrutement, un départ, un renommage ou une dissolution. La cohorte de classe (`faction.Officier`, `faction.Membre`, `faction.Chef`, `faction.None`) est maintenant cohérente en local **et** à distance sans délai perceptible.

---

## 📜 Changements

### Bug corrigé
- **`FactionTabManager.refresh(...)`** — appelle désormais `webMapSync.pushFactionInfo(factionName)` (et, si défini, `heroTabPush`) en complément de la mise à jour du scoreboard local, exactement comme les autres opérations de modification de faction.

### Robustesse
- **`FactionPowerManager.getFactionRank(...)`** était mis en cache par faction. Une faction qui franchit un cap voyait donc son ancien rang tant que le cache n'était pas invalidé : forçage du re-calcul à chaque évaluation tab-relevant.

---

## 🔧 Détails techniques

- Diffs principaux :
  - `FactionTabManager.java`: après recalcul du rang d'une faction, programme un `Bukkit.getScheduler().runTask(this, () -> refresh(factionName))` au prochain tick pour que la synchro côté HeroTab / web parte *immédiatement*.
  - Cohérence garantie avec `webMapSync`, `HeroTabBridge` (la classe du même nom dans le backing-service externe si présente) et tous les GUIs (`FactionRankingGUI`).

- Build : `mvn -B clean package` → `target/FactionPlugin-5.14.3.jar`.

---

## 📦 Compatibilité

- Serveurs : Paper **1.21.x** (testé sur 1.21.4) — Java 21.
- Données existantes : totalement rétro-compatible (aucun changement de format YAML).
- API publique : aucun changement de signature, simple ajout d'effets de bord.

---

## ⚠️ Notes de mise à jour depuis v5.14.2

- Pas de migration de données nécessaire.
- Il suffit de remplacer le `.jar` (les fichiers `.yml` existants sont conservés).
- Pour redémarrer proprement : `/reload confirm` ou redémarrage complet du serveur.

---

🤖 Cette version a été compilée et publiée par un agent OpenHands sur behalf de herocraftlol.

Co-authored-by: openhands <openhands@all-hands.dev>
