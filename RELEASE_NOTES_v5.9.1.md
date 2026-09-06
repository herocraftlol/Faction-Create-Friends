# FactionPlugin v5.9.1 — Les Guerriers prennent vie ⚔️

> **Les villageois que vous avez recrutés en v5.9.0 prennent désormais les armes — ils patrouillent, gardent un poste, détectent vos ennemis et se mettent en formation sur votre ordre.**

Cette mise à jour transforme vos **villageois recrutés** en véritables **unités militaires autonomes**. Chaque guerrier peut maintenant être posté sur le terrain, configuré avec un périmètre de défense et une zone de patrouille, et combat intelligemment — y compris pendant les guerres inter-factions.

---

## ⚔️ Nouveautés de la v5.9.1

### 🛡️ Poste de garde — *où le guerrier se tient*
Définissez en un clic la position où votre guerrier doit rester. Il y **revient automatiquement** après chaque poursuite ou ronde. Parfait pour garder une porte, un portail ou un point stratégique.

### 📍 Rayon de défense — *combien de blocs autour du poste*
Chaque guerrier effectue des rondes dans un rayon configurable de **4 à 48 blocs** autour de son poste. S'il détecte une menace dans ou hors du périmètre, il quitte sa ronde, l'affronte, puis reprend sa position.

### 🗺️ Patrouille tracée dans le monde — *deux points suffisent*
Pour les bases complexes, tracez **deux points** dans le monde : votre guerrier y effectuera des **allers-retours autonomes** en restant à l'intérieur du périmètre défini. Sélection annulable à tout moment avec `/faction annuler` ou en tapant `annuler` dans le chat.

### 🧠 Combat intelligent — *qui doit-il attaquer ?*
Activez le mode combat et vos guerriers :
- repèrent automatiquement les **mobs hostiles** et les **joueurs ennemis** de votre faction (vérification via `FactionManager`),
- consultent l'état des **`WarSession`** : pendant une guerre, les factions belligérantes sont automatiquement hostiles,
- poursuivent leur cible jusqu'à **24 blocs** et l'attaquent avec leur arme équipée,
- se **soignent automatiquement** grâce à l'emplacement *Nourriture* du GUI tant qu'ils ne sont pas à pleine vie.

### 🪖 Formation militaire — *alignez vos troupes*
Tapez `/faction villageois formation` : **tous les villageois recrutés dans un rayon de 40 blocs** se mettent en formation devant vous, à 1,5 bloc d'espacement. Idéal pour les cérémonies, les entraînements et les embuscades.

### 🚶 Mode suivi — *le guerrier vous accompagne*
Activez le mode suivi depuis le GUI et votre guerrier devient un **compagnon loyal** qui vous suit dans vos déplacements. Re-cliquez pour annuler.

### 🩹 Correctif du double-clic
Les clics rapides qui exécutaient deux actions à la suite sont désormais **filtrés par un cooldown GUI de 150 ms**. Toutes les interactions sont enfin fluides et prévisibles.

---

## 📦 Téléchargements
- **`FactionPlugin-5.9.1.jar`** — le plugin prêt à l'emploi (drop dans `plugins/`)
- **`FactionPlugin-v5.9.1-source.zip`** — le code source complet de cette version

---

## 🔧 Installation & mise à jour

1. Téléchargez **FactionPlugin-5.9.1.jar** ci-dessus.
2. Déposez-le dans le dossier `plugins/` de votre serveur **Paper 1.21.4**.
3. Si vous mettez à jour depuis une v5.x antérieure :
   - votre configuration, vos factions, vos alliances, vos guerres et vos villageois recrutés **sont préservés** ;
   - seuls les fichiers `villagers.yml` peuvent être enrichis automatiquement (rôle, poste, rayon, patrouille, suivi).
4. Redémarrez le serveur.

> 💡 Si vous utilisez encore Paper < 1.21.4, restez sur la v5.9.0.

---

## ✅ Checklist technique
- Paper **1.21.4** (Spigot 1.21.4 compatible)
- Java **21+**
- Migration Bukkit/Paper API : `LUCK` → `LUCK_OF_THE_SEA`, `SPELL_WITCH` → `WITCH`, `VILLAGER_HAPPY` → `HAPPY_VILLAGER`, `ENTITY_PLAYER_EAT` → `ENTITY_GENERIC_EAT`, `DAMAGE_RESISTANCE` → `RESISTANCE`, `INCREASE_DAMAGE` → `STRENGTH`, `GOLD_STAINED_GLASS_PANE` → `YELLOW_STAINED_GLASS_PANE`, `BED` → `RED_BED`.

---

## 📚 Documentation complète

- 📘 **README.md** — description détaillée, captures, commandes
- 📜 **CHANGELOG_v5_9_1.md** — notes de version complètes
- 📜 **CHANGELOG_v4.md**, **CHANGELOG_v5.md**, **CHANGELOG_v5_3_0.md** — historique
- 🔀 **Branche** : `release/v5.9.1`

---

### 🔗 Liens rapides
- [📂 Code source sur la branche `release/v5.9.1`](https://github.com/herocraftlol/Faction-Create-Friends/tree/release/v5.9.1)
- [📜 CHANGELOG v5.9.1 détaillé](https://github.com/herocraftlol/Faction-Create-Friends/blob/release/v5.9.1/CHANGELOG_v5_9_1.md)
- [⬇️ Télécharger le JAR](https://github.com/herocraftlol/Faction-Create-Friends/releases/download/v5.9.1/FactionPlugin-5.9.1.jar)
- [⬇️ Télécharger le code source](https://github.com/herocraftlol/Faction-Create-Friends/releases/download/v5.9.1/FactionPlugin-v5.9.1-source.zip)

Bonne guerre ⚔️🛡️
