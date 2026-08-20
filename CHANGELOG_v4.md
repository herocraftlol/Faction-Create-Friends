# CHANGELOG — FactionPlugin v4.0.0

## Nouveautés v4.0.0

### 🛒 Shop Global (`/faction shop`)
- GUI paginé (5 rangées × 9 = 45 items/page)
- Recherche par mot-clé : clic sur le panneau dans le GUI, puis saisie dans le chat
- Tri par prix croissant (`↑`) ou décroissant (`↓`)
- Monnaies acceptées : Lingot de fer, Lingot d'or, Diamant, Émeraude
- Paiement automatique au vendeur dès la vente (ou livré à la reconnexion si hors-ligne)
- Drop à tes pieds si l'inventaire est plein (acheteur ET vendeur)
- Vue "Mes annonces" depuis le GUI ou `/faction mesannonces`

### Commandes shop
| Commande | Description |
|---|---|
| `/faction shop` | Ouvrir le shop (GUI) |
| `/faction vendre <prix> <monnaie>` | Mettre l'item en main en vente |
| `/faction acheter <ID>` | Acheter directement par ID |
| `/faction recuperer [ID]` | Récupérer une annonce non vendue (sans ID = liste) |
| `/faction mesannonces` | Voir ses annonces (GUI) |

Monnaies : `fer`, `or`, `diamant`, `emeraude`

### 👁️ InvSee (`/faction invsee <joueur>`)
- Permission requise : `faction.admin`
- Affiche l'inventaire complet du joueur (36 slots + hotbar + armure + offhand)
- Lecture seule : aucun item ne peut être pris ou déplacé
- Message d'état dans le chat (joueur ciblé + confirmation lecture seule)

## Fichiers ajoutés
```
src/main/java/fr/faction/shop/
  ├── ShopListing.java     ← Modèle d'annonce
  ├── ShopManager.java     ← Logique métier + persistance (shop.yml)
  ├── ShopGUI.java         ← GUI paginé avec recherche + Listener
  └── InvSeeGUI.java       ← GUI InvSee admin + Listener
```

## Modifications
- `FactionPlugin.java` : intégration des nouveaux managers
- `FactionCommand.java` : +8 nouvelles sous-commandes
- `PlayerListener.java` : hook recherche chat + livraison paiements en attente
- `plugin.yml` : version 4.0.0

## Fichier de données
`plugins/FactionPlugin/shop.yml` — auto-créé au premier `/faction vendre`

---

## Historique
- v3.2.4 : Claim, Banque émeraudes, Troc, Stats, Classements, Puissance
- v4.0.0 : Shop Global paginé + InvSee admin
