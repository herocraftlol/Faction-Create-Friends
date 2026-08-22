# CHANGELOG — FactionPlugin v5.0.0

## Nouveautés v5.0.0

### 🤝 Système d'Alliances (`/faction alliance`)
- **Alliances entre factions** : proposez, acceptez ou rompez des pactes entre factions
- **Bonus de puissance progressif** : plus vous avez d'alliés, plus votre faction gagne de puissance bonus
  - 1 allié → +500 power • 2 alliés → +1 200 • 3 alliés → +2 500 • +500 par allié supplémentaire
- **Interface graphique** de gestion des alliances (`/faction alliance gui`)
- Commandes : `/faction alliance inviter|accepter|refuser|rompre|liste <faction>`

### 🏠 Homes personnels (`/sethome`, `/home`)
- **Homes nommés** : plusieurs points de téléportation par joueur avec persistance (`homes.yml`)
- **Limites dynamiques** : 1 home sans faction, **3 homes si votre faction a des alliés**
- **Sécurité anti-abus** : distance minimale de 10 chunks (160 blocs) entre deux homes, warmup de 5 secondes
- Commandes : `/sethome [nom]`, `/home [nom]`, `/delhome <nom>`, `/homes` (aussi via `/faction`)

### 📦 Spawn de Faction (`/faction setspawn` / `/faction spawn`)
- Le chef définit un point de ralliement pour sa faction
- Tous les membres peuvent s'y téléporter avec `/faction spawn`

### 🚀 Téléportation entre joueurs (`/tpa`)
- **Demandes de téléportation** joueur-à-joueur avec acceptation/refus
- Expiration après 30 secondes, warmup de 3 secondes, anti-spam par cooldown
- Commandes : `/tpa <joueur>`, `/tpaccept`, `/tpdeny` (aussi via `/faction`)

### 🔒 Coffres Privés
- **Verrouillage instantané** : sneak + clic droit avec un panneau sur un coffre (normal ou piégé)
- Seuls le propriétaire (et les admins) peuvent ouvrir le coffre
- Protection contre la casse du bloc par les autres joueurs
- Persistance dans `private_chests.yml`
- Permission admin : `faction.admin` peut ouvrir/casser les coffres privés

### 🗺️ Claims & Alliés
- Nouvelle permission de claim `claimallies` : autorisez vos factions alliées dans vos territoires

## Correction de compilation
- Ajout de l'import `org.bukkit.Location` manquant dans `PlayerTeleportManager`

## Fichiers ajoutés
```
src/main/java/fr/faction/alliance/
  ├── AllianceManager.java        ← Alliances + bonus power + GUI
  ├── HomeManager.java            ← Homes nommés avec limites
  ├── PlayerTeleportManager.java  ← /tpa avec accept/refus/cooldown
  └── PrivateChestManager.java    ← Coffres verrouillables
```

## Commandes ajoutées
| Commande | Description |
|---|---|
| `/faction alliance [inviter\|accepter\|refuser\|rompre\|liste\|gui]` | Gérer les alliances |
| `/faction setspawn` / `/faction spawn` | Spawn de faction |
| `/sethome [nom]`, `/home [nom]`, `/delhome <nom>`, `/homes` | Homes personnels |
| `/tpa <joueur>`, `/tpaccept`, `/tpdeny` | Téléportation entre joueurs |
