# CHANGELOG — FactionPlugin v5.3.0

## Nouveautés v5.3.0

### 🧹 Tri de coffre & d'inventaire

Nouveau moteur de tri complet pour le **coffre partagé de faction** et
l'**inventaire personnel**, accessible via un GUI dédié.

- `/faction ranger` (alias `trier`, `organiser`, `sort`) — ouvrir le menu de
  tri pour le coffre partagé de la faction
- `/faction ranger perso` (alias `inventaire`) — trier son propre inventaire
  (slots 9–35, hotbar exclue)

#### 🎛️ 6 modes de tri

| Mode | Description |
|---|---|
| **⬡ Similaires regroupés** | Fusionne les stacks identiques puis compacte |
| **☰ Par catégorie** | Regroupe par type : blocs, outils, armes, armures, nourriture, potions, minéraux, plantes, redstone, livres, matériaux, divers |
| **🔤 Alphabétique A→Z** | Trie par nom d'affichage (ou clé Bukkit) |
| **📦 Quantité ↓** | Du plus grand au plus petit stack |
| **📦 Quantité ↑** | Du plus petit au plus grand stack |
| **✦ Par rareté** | Items enchantés et rares en premier |

- **Aperçu avant confirmation** : le menu affiche le nombre d'items présents
  et les types distincts, puis propose un écran de confirmation avec le mode
  choisi avant d'appliquer le tri
- **Bouton « Organiser le coffre »** : ajouté dans le menu principal
  (MainMenuGUI) et dans le coffre partagé (slot 53) — un clic ouvre le menu
  de tri
- Dans tous les modes, les stacks identiques sont d'abord fusionnés
  (stack-merge) avant d'être triés et disposés

### 🔧 Corrections
- **`/tpa`** : correction du message d'acceptation (affichage correct du nom
  du demandeur)
- Tri des items endommagés géré proprement (compacité des stacks selon la
  durabilité)
- Nettoyage général du code du trieur

> ⚠️ **Note** : cette version du code source est basée sur le zip fourni
> (`FactionPlugin-v5.2.0.zip`). Elle **remplace** le contenu de la branche
> par la variante « Tri de coffre » : le système **Comptoir d'échange**
> (`ExchangeGUI`, `ExchangeManager`, `ExchangeOrder`, `ItemAliasUtil`,
> commandes `/faction echange`, `deposer`, `fournir`, `collecter`,
> `retirerordre`, `mesordres`) de la v5.2.0 publiée précédemment n'est pas
> présent dans cette branche. Pour le réintégrer, fusionnez-le à nouveau
> depuis le tag `v5.2.0`.

## Fichiers ajoutés
```
src/main/java/fr/faction/sort/
  ├── ChestSorter.java   ← Moteur de tri (6 modes, fusion de stacks, catégories)
  └── SortMenuGUI.java   ← GUI de tri (coffre partagé + inventaire perso)

src/main/java/fr/faction/commands/FactionCommand.java — sous-commandes ranger/trier
src/main/java/fr/faction/gui/MainMenuGUI.java         — bouton « Organiser le coffre »
src/main/java/fr/faction/managers/SharedInventoryManager.java — slot 53 réservé au bouton de tri
```

## Version
- `pom.xml` : 5.3.0
- `plugin.yml` : 5.3.0

---

## Historique
- v5.2.0 : Comptoir d'échange (dépôt de monnaie contre items), GUI paginé
- v5.1.1 : Guerre inter-factions, nouveau menu principal, fix coffres privés
- v5.0.0 : Alliances, homes, spawn faction, /tpa, coffres privés
- v4.0.0 : Shop global paginé + InvSee admin
- v3.x : Stats, puissance, banque, claims, troc