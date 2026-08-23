# v5.2 — Comptoir d'échange (dépôt de monnaie contre un item)

## Nouveau système
En plus du shop classique, un joueur peut maintenant créer un **ordre d'échange** :
il dépose une réserve de monnaie (fer, or, diamant ou émeraude) et demande en retour
un item précis, à un taux fixé par "lot". N'importe quel joueur peut alors fournir
cet item pour recevoir la monnaie correspondante, jusqu'à épuisement du stock déposé.

Exemple concret demandé :
- Le créateur tient **60 fer** en main et fait `/faction deposer pierre 32 5`.
- Cela crée un ordre : "32 pierre → 5 fer par lot", avec 60 fer en réserve (12 lots).
- N'importe quel joueur fait `/faction fournir <ID>` : le jeu vérifie son inventaire,
  lui retire 32 pierre (ou un multiple, s'il en a assez pour plusieurs lots d'un coup),
  et lui donne 5 fer par lot fourni.
- La pierre reçue s'accumule dans l'ordre ; le créateur la récupère avec
  `/faction collecter <ID>` (sans fermer l'ordre s'il reste du fer à distribuer).
- L'ordre reste actif jusqu'à ce que les 60 fer soient entièrement distribués.
- Le créateur peut annuler à tout moment avec `/faction retirerordre <ID>` :
  il récupère alors la monnaie restante ET la pierre déjà reçue.

## GUI
- `/faction echange` ouvre un GUI paginé listant tous les ordres actifs
  (clic sur un ordre = le fournir avec les items de ton inventaire).
- Bouton **➕ Créer un ordre** dans ce GUI : ferme l'inventaire, tiens ta
  monnaie en main, puis tape dans le chat `<item> <quantité_par_lot> <prix_par_lot>`
  (ex: `pierre 32 5`) — l'ordre est créé et le GUI se rouvre automatiquement.
- Bouton **📦 Mes ordres** : ouvre la liste de tes propres ordres, avec
  clic gauche pour collecter les items reçus, et clic droit pour annuler
  l'ordre (rembourse monnaie restante + items reçus).

## Nouvelles commandes
- `/faction echange` (alias `comptoir`) — ouvrir le comptoir d'échange (GUI paginé)
- `/faction deposer <item> <quantité_par_lot> <prix_par_lot>` (alias `depot`) —
  crée un ordre en déposant tout le stack de monnaie tenu en main
- `/faction fournir <ID>` (alias `livrer`) — fournir l'item demandé depuis
  l'inventaire (honore automatiquement plusieurs lots si possible)
- `/faction collecter [ID]` — récupérer les items reçus en attente ;
  sans ID, liste tes ordres actifs
- `/faction retirerordre <ID>` (alias `annulerordre`) — annuler un ordre
  (rembourse monnaie restante + items reçus)
- `/faction mesordres` — GUI listant tes ordres, avec collecte (clic gauche)
  et annulation (clic droit)

## Nouveaux fichiers
- `fr.faction.shop.ExchangeOrder` — modèle d'un ordre d'échange
- `fr.faction.shop.ExchangeManager` — logique + persistance (`exchange.yml`)
- `fr.faction.shop.ExchangeGUI` — interface graphique paginée
- `fr.faction.shop.ItemAliasUtil` — reconnaît les noms d'items en français
  courant (pierre, bois, charbon, etc.) ou le nom technique Bukkit,
  pour permettre de demander n'importe quel item

## Notes
- La monnaie déposée doit être l'une des 4 monnaies déjà utilisées par le shop
  (fer, or, diamant, émeraude), tenue en main au moment du dépôt.
- L'item demandé peut être n'importe quel item du jeu.
- Aucune limite du nombre d'ordres actifs par joueur pour l'instant.
