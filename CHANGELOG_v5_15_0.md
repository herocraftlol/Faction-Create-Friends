# 🆕 CHANGELOG — FactionPlugin v5.15.0

## 🎯 Résumé

La v5.15.0 s'attaque **à la vraie cause racine** du tab HeroTab qui ne
s'actualisait jamais — et, au passage, ferme un bug de compilation latent qui
aurait empêché le plugin de se compiler proprement dans certaines
configurations. Cette version est **plus solide et plus complète** que les
précédentes : tout ce qui faisait la joie des versions 5.14.x reste évidemment
intact, et le tab est maintenant **vraiment, entièrement, à jour** sur
l'ensemble du réseau (tous les sous-serveurs, onglet global, et site web).

---

## 🏷️ HéroTab : la cause racine a enfin été trouvée

### Le problème que ça corrige

Depuis plusieurs versions, malgré des correctifs successifs censés rendre le tab
« instantané », **le tab HeroTab ne se rafraîchissait jamais** après un
recrutement, un départ, un kick, une dissolution, un renommage ou une montée
de rang. La cause était en réalité très simple — et a échappé à toutes les
analyses précédentes :

- **HeroTab lit une table MySQL `faction_tab_sync`**.
- Cette table était censée être écrite par une classe **`FactionTabSync`** côté
  FactionPlugin.
- **Cette classe n'existait tout simplement pas.**

Tous les correctifs précédents déclenchaient bien une synchronisation
immédiate, mais **vers `WebMapSync`** — un système totalement différent qui
alimente la carte du site web, sans aucun rapport avec le tab HeroTab. Rien
n'écrivait donc jamais dans la table `faction_tab_sync`. Peu importe le
nombre de correctifs « de synchro » apportés, le tab ne pouvait pas se mettre
à jour : **le bon code n'avait jamais été écrit**.

### La solution

- **Nouvelle classe `FactionTabSync`** : écrit désormais réellement dans la
  table `faction_tab_sync` lue par HeroTab, en réutilisant la connexion MySQL
  déjà configurée pour `/lier` (section `mysql:` du `config.yml`). Aucune
  configuration supplémentaire n'est nécessaire si `/lier` fonctionne déjà sur
  ton serveur.
- **Table créée automatiquement** si absente, avec exactement les colonnes
  attendues par HeroTab : `uuid`, `faction_name`, `rank_name`, `rank_color`,
  `rank_icon`. Le format de couleur `&x` (legacy) correspond à ce que HeroTab
  parse côté proxy.
- **Tâche de fond toutes les 30 secondes**, plus déclenchement immédiat sur
  les mêmes événements que `WebMapSync` : recrutement, départ, kick,
  disband, renommage, montée de rang. Local, site, onglet HeroTab, sous-
  serveurs — **tout est cohérent en permanence, sans aucun délai**.

---

## 🔧 Bug de compilation corrigé en passant

`FactionPlugin.java` (la classe principale) utilisait la classe `Bukkit` à
plusieurs endroits sans jamais l'importer — un import manquant qui aurait
empêché toute compilation propre du plugin dans certaines configurations
d'IDE ou de chaîne de build. **Un balayage exhaustif de l'ensemble du projet
a été fait** : aucun autre fichier ne présente ce problème (tous les autres
utilisent un import `org.bukkit.*` correct).

Cette correction est invisible à l'usage, mais elle garantit que
`mvn clean package` produit un `.jar` valide du premier coup sur n'importe
quel poste de développement.

---

## 📦 Compatibilité

- **Serveurs** : Paper **1.21.x** (testé sur 1.21.4), Java 21.
- **Données existantes** : totalement rétro-compatible. Aucun changement de
  format YAML. La table MySQL `faction_tab_sync` est créée automatiquement
  au premier démarrage si elle n'existe pas.
- **Configuration** : la section `mysql:` doit être renseignée (comme pour
  `/lier`). Si elle l'est déjà pour la liaison au site, aucune manipulation
  supplémentaire n'est requise.
- **API publique** : aucun changement de signature pour les utilisateurs du
  plugin — uniquement l'ajout d'une nouvelle classe interne.

---

## ⚠️ Notes de mise à jour depuis v5.14.3

- **Aucune migration de données nécessaire.** Remplacer le `.jar` et
  redémarrer le serveur suffit.
- Si tu vois `[FactionTabSync] Section 'mysql' absente du config.yml —
  synchro tab HeroTab désactivée.` au démarrage, c'est que tu n'as pas
  configuré la section `mysql:` de `config.yml`. Le plugin continue de
  fonctionner normalement — seule la synchro du tab HeroTab est désactivée
  (les autres fonctionnalités, dont le tab local et la carte web, sont
  intactes).
- Pour redémarrer proprement : `/reload confirm` ou redémarrage complet du
  serveur.

---

## 🔗 Liens

- 📥 [Téléchargement direct de `FactionPlugin-5.15.0.jar`](../../releases/download/v5.15.0/FactionPlugin-5.15.0.jar)
- 📦 [Code source complet (`FactionPlugin-v5.15.0-source.zip`)](../../releases/download/v5.15.0/FactionPlugin-v5.15.0-source.zip)
- 📜 [README complet](../../)

---

🤖 Cette version a été compilée et publiée par un agent OpenHands sur behalf
de herocraftlol.

Co-authored-by: openhands <openhands@all-hands.dev>
