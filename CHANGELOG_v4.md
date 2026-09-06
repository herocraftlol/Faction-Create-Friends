# CHANGELOG — FactionPlugin v4.0.0

## v5.9.0 — Villageois recrutés (Constructeur / Guerrier)

- `/faction recruter` — vise un villageois (8 blocs max) et convertis-le en unité
  de faction (chef/sous-chef uniquement, limite configurable par faction — 5 par défaut).
- `/faction villageois` — GUI listant les villageois recrutés de ta faction :
  nom, rôle, vie, statut (chunk chargé ou non).
- Fiche détaillée par villageois (clic sur un villageois dans la liste) :
  - Renommage (clic → tape le nom dans le chat).
  - Attribution du rôle : Aucun / Constructeur / Guerrier (chef/sous-chef uniquement).
  - Inventaire restreint selon le rôle : impossible d'insérer un objet qui n'a pas
    de sens pour le rôle (seuls des blocs pour le Constructeur ; seuls arme+armure
    aux bons emplacements pour le Guerrier).
  - Emplacement "Nourriture" commun aux deux rôles : le villageois se soigne
    automatiquement en consommant la nourriture donnée, tant qu'il n'est pas à pleine vie.
  - "Libérer" : retire le rôle et l'équipement (chef/sous-chef uniquement).
- **Constructeur** : le chef lui définit un chantier (clic-droit sur 2 coins dans
  le monde, doit être dans un chunk claimé par la faction, volume plafonné) ;
  il comble ensuite tout vide (AIR) dans cette zone avec les blocs qu'on lui donne —
  ça sert aussi bien à construire qu'à réparer, puisqu'un trou qui réapparaît dans
  la zone sera automatiquement recomblé au prochain passage.
- **Guerrier** : équipé d'une arme + armure, il repère seul les mobs hostiles à
  proximité (rayon configurable), se déplace vers eux et les attaque — ce qui le
  fait aussi défendre les villageois alentour. Les dégâts et la résistance
  dépendent du matériel donné (bois < pierre/or < fer < diamant < netherite).
- Le villageois meurt normalement (mob hostile, joueur…) ; sa faction est alors
  notifiée et il est retiré des données.
- Persistance complète dans `villagers.yml` (rôle, équipement, ressources, chantier).

## v5.8.4 — Sous-chefs

- Nouveau rôle **Sous-chef** : le chef peut promouvoir jusqu'à **2** membres au rang de sous-chef
  via `/faction souschef promouvoir <joueur>` (et les rétrograder avec `retirer`).
- Le chef peut régler la limite de sous-chefs autorisés (`/faction souschef limite <0-2>`, plafond absolu = 2).
- `/faction souschef liste` — voir les sous-chefs actuels.
- Un sous-chef peut désormais :
  - Inviter des joueurs (`/faction invite`)
  - Expulser des membres, **sauf le chef** (`/faction kick`)
  - Proposer, accepter, refuser et rompre des alliances (`/faction alliance ...`)
  - Déclarer, accepter et refuser des guerres (`/faction guerre declarer/accepter/refuser`)
  - Définir les spawns de faction (`/faction setspawn`)
  - Claimer / retirer des claims (`/faction claim`, `/faction unclaim`)
- Restent réservés au **chef uniquement** : `setchef`, `rename`, `disband`, `claimallow`/`claimdeny`,
  `perms`, et la capitulation en guerre (`/faction guerre capituler`).
- GUI (`MainMenuGUI` et `FactionGUI`) mis à jour pour refléter ces nouvelles permissions.
- Persistance des sous-chefs et de la limite dans `factions.yml`.

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
