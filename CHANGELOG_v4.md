# CHANGELOG — FactionPlugin v4.0.0

## v5.12.0 — Commerce inter-villes (ports, gares, contrats)

- **Nouveaux rôles Navigateur et Cheminot.** Un navigateur livre des
  marchandises en bateau entre les ports de deux villes ; un cheminot les
  livre en train entre leurs gares.
- **Port et gare** : `/faction port definir <village>` et `/faction gare
  definir <village>` (clic-droit dans les claims de la faction). Place un
  coffre à cet endroit : c'est l'entrepôt utilisé pour le commerce.
- **Contrats commerciaux** (`/faction contrat creer|liste|assigner|annuler`) :
  une livraison à sens unique d'une ressource entre deux villes — la tienne,
  ou celle d'une **faction alliée** avec qui tu commerces. Réservé aux
  villages ayant atteint le palier **Ville** (niveau 5+, cf. v5.11).
  Assigne un contrat en visant un navigateur/cheminot avec
  `/faction contrat assigner <id>` : il part avec la cargaison, effectue le
  trajet (bateau ou train, à vue, à petite vitesse), dépose la marchandise à
  l'arrivée, puis revient.
- **Fret protégé** : seuls les membres de la faction propriétaire et de ses
  alliées peuvent ouvrir le chargement ou monter dans le véhicule.
- Comme annoncé, ce système reste volontairement simplifié : un contrat = un
  aller simple (crée un second contrat pour un retour), trajet en ligne
  droite (prévoir un chenal/une voie dégagée), pas de couplage physique réel
  des wagons.

### Correctif critique en cours de session
Un système de commerce inter-villes précédemment développé avait par erreur
écrasé un fichier portant le même nom (`TradeManager`) appartenant à une
fonctionnalité totalement différente et déjà existante : le **troc
joueur-à-joueur** (`/faction troc`, `/faction accepter`). Le fichier de troc
a été reconstruit et le nouveau système de commerce déplacé dans son propre
package (`fr.faction.commerce`, classe `CommerceManager`) pour ne plus jamais
entrer en conflit. Les deux fonctionnalités sont maintenant strictement
indépendantes.

## v5.11.0 — Villages, entraide, suivi longue distance

- **Suivi longue distance** : le seuil de téléportation de rattrapage pour le
  suivi est passé à 100 blocs (au lieu de 50).
- **Système de village** :
  - `/faction village fonder <nom>` — fonde un village en sélectionnant 2
    coins dans le monde (doivent être dans des claims de ta faction), comme
    les autres zones du plugin.
  - `/faction village liste` — liste les villages de ta faction (niveau,
    population).
  - `/faction village dissoudre <nom>` — dissout un village (chef/sous-chef).
  - Les villageois recrutés à l'intérieur de la zone d'un village y sont
    automatiquement rattachés ("nés" dans ce village) ; réassignation
    manuelle possible via un nouveau bouton "Village" dans leur fiche.
  - Leur étiquette affiche désormais le nom de leur village (`<NomDuVillage>`).
  - **Base de repli** : un villageois sans poste/chantier/patrouille/
    rassemblement et rattaché à un village y retourne automatiquement — sa
    "sorte de base de repli" une fois qu'aucune tâche ne lui est plus
    attribuée (ex. une guerre qui se termine).
  - **Niveau du village** (dérivé de sa population, jamais désynchronisé) :
    1 à 4 = Village, 5+ = Ville. Visible dans `/faction village liste` et
    dans le nouveau GUI "Villages" (bouton dans `/fac villageois`), qui
    montre chaque village fondé et permet d'ouvrir la liste filtrée de ses
    habitants.
- **Entraide entre villageois** : un récolteur qui a de la nourriture en
  réserve en donne automatiquement (jusqu'à 4 à la fois) aux guerriers/
  constructeurs de sa faction à moins de 48 blocs qui n'en ont pas et sont
  sous 70% de vie, avec un message dans le chat de la faction.

**Hors scope pour cette version** (volontairement non commencé, projet à part
entière) : le commerce inter-villes/villes avec ports, gares, bateaux et
trains de marchandises, contrats, et les rôles de villageois navigateur/
marchand maritime/cheminot. À planifier séparément si souhaité.

## v5.10.4 — Annuler une patrouille, mode "vie normale"

- **Annuler la patrouille** : nouveau bouton dans la fiche du guerrier
  (visible uniquement si une patrouille est définie) pour tout retirer d'un
  coup.
- **Mode "vie normale"** (nouveau bouton, activé/désactivé) : un guerrier en
  mode vie normale vagabonde et se comporte comme un villageois classique
  (déplacements, commerce...) tant qu'il n'a rien de particulier à faire —
  tout en continuant à se défendre normalement si un ennemi entre dans son
  périmètre. Désactivé, il reste sur place comme avant.
- **Changement technique important** : l'ancien mécanisme "reste immobile
  par défaut" retirait carrément certains comportements vanille du
  villageois (déambulation...) via l'API de goals de Paper. Or cette API ne
  permet aucun moyen officiel de les restituer ensuite (limitation confirmée
  de Paper, ticket toujours ouvert) — un guerrier ainsi figé l'aurait donc
  été *pour toujours*, même en activant le nouveau mode "vie normale". Le
  mécanisme a été remplacé par une interruption active et réversible de son
  déplacement tant qu'il n'a rien à faire et que le mode vie normale est
  désactivé — sans jamais rien retirer de façon permanente.

## v5.10.3 — Correctif : couleur de faction dans le chat

- Le chat coloré (rôle/faction) ne s'affichait plus du tout depuis un moment :
  `AsyncPlayerChatEvent#setFormat()` (API Bukkit historique) n'a plus vraiment
  d'effet sur Paper 1.21 depuis le passage au chat signé par Component — le
  format pouvait être ignoré silencieusement.
- Migration vers la bonne API : `io.papermc.paper.event.player.AsyncChatEvent`
  + `event.renderer(...)`, qui construit un vrai Component. Le rendu
  `[rang][Faction] Pseudo: message`, coloré selon le rang, refonctionne
  normalement — c'est un vrai formatage serveur (jamais du texte brut
  réécrit), donc toujours aucun risque de kick anti-triche.
- HeroTab n'a rien à faire de plus ici : ce correctif est entièrement côté
  FactionPlugin, et s'applique déjà uniquement sur le serveur où tourne le
  plugin (donc "dans le faction" uniquement, comme avant).

## v5.10.2 — Verrou d'accès aux fiches de villageois

- Un seul joueur à la fois peut avoir la fiche de gestion d'un villageois
  recruté (guerrier, constructeur, récolteur) ouverte. Si un second joueur
  essaie de l'ouvrir pendant ce temps, il reçoit un message ("Ce villageois
  est déjà géré par ...") au lieu d'accéder en même temps aux mêmes
  emplacements — ça évite les bugs de duplication ou de perte d'objets liés
  à deux joueurs modifiant l'inventaire du même villageois simultanément.
- Le verrou se libère automatiquement à la fermeture de la fiche, et en
  filet de sécurité si le joueur se déconnecte sans fermer proprement.

## v5.10.1 — Indicateurs d'emplacement, correctifs anti-disparition, mains occupées

- **Indicateurs visuels** : chaque emplacement vide (arc, flèches, épée, casque,
  blocs, outil, nourriture, graines/butin…) affiche désormais une icône-repère
  grisée ("[Vide] ...") indiquant précisément quoi y déposer. Elle disparaît dès
  qu'un vrai objet est posé, et ne peut jamais être ramassée par erreur ni finir
  confondue avec un objet réel.
- **Correctif — objets qui disparaissaient parfois :**
  - Le glisser-déposer (drag) est désormais bloqué dans les GUI de villageois :
    il pouvait faire atterrir un objet dans un emplacement décoratif invisible,
    perdu au rafraîchissement suivant.
  - L'emplacement transitoire "type de bloc du prochain chantier" rendait
    l'objet posé s'il n'était jamais utilisé ni sorti ; il est maintenant rendu
    au joueur (ou déposé à ses pieds si l'inventaire est plein) à la fermeture
    de la fiche.
- **Les villageois tiennent maintenant ce qu'ils utilisent** : le constructeur
  tient en main le bloc de son chantier actif (ou, à défaut, le premier
  matériau de sa réserve) — le guerrier et le récolteur le faisaient déjà
  (arme/arc, outil).

## v5.10.0 — Rôle Récolteur, construction instantanée, fuite des guerriers

- **Nouveau rôle : Récolteur.** Donne-lui une pioche, une hache ou une pelle et
  définis-lui une zone de récolte : il mine les minerais, coupe le bois, ou
  creuse la terre/sable/gravier selon l'outil en main. Donne-lui aussi un champ
  et des graines (blé, carottes, pommes de terre, betteraves) : il plante,
  récolte à maturité, replante aussitôt, et sème les cases vides tout seul.
  Définis un coffre de dépôt (clic-droit dessus) : tout ce qu'il récolte y est
  déposé automatiquement, sauf les graines qu'il garde sur lui pour replanter.
  Comme le guerrier, il s'use et se refabrique son outil au niveau 4+.
- **Constructeur — construction instantanée.** Il se téléporte directement sur
  chaque bloc à poser ou à miner, peu importe la distance : plus d'attente.
  S'il doit atteindre une hauteur difficile, il pose de l'échafaudage en bambou
  sous lui pour ne pas tomber, puis le retire (sans le faire tomber au sol) une
  fois le chantier terminé.
- **Constructeur — récolte en un seul passage.** Il calcule combien il lui
  manque pour finir tout le chantier et récolte ce lot d'un coup, au lieu de
  faire un aller-retour à chaque bloc.
- **Guerrier — reste immobile par défaut.** Le vagabondage aléatoire vanille
  est désactivé (best-effort) : il ne bouge plus que sur ordre (poste,
  patrouille, suivi, rassemblement).
- **Guerrier — fuite et discrétion.** S'il tombe sous 25% de vie ou se retrouve
  face à 3 ennemis ou plus dans son périmètre, il fuit (loin de la menace, ou
  vers son poste) au lieu de continuer à se battre, jusqu'à retrouver plus de
  60% de vie et n'avoir plus aucune menace proche.
- **Formations en cercle.** En plus de la ligne, `/faction villageois ranger
  cercle` (ou le bouton dédié) les dispose en cercle autour du joueur.

## v5.9.8 — Tout tombe au sol à la mort

- Quand un villageois recruté (Constructeur ou Guerrier) meurt, il lâche
  désormais **tout ce qu'il avait sur lui** : arme, armure, arc, flèches,
  nourriture, et toute sa réserve (matériaux de construction ou butin de guerre
  ramassé) — plus rien n'est perdu silencieusement dans le vide.
- La mécanique vanille de "chance de drop d'équipement" est désactivée sur nos
  recrues pour éviter tout doublon : c'est désormais nous qui gérons 100% du
  drop à la mort.

## v5.9.7 — Clic-droit direct, butin de guerre, compteur de kills

- **Clic-droit sur un villageois recruté** ouvre directement sa fiche de gestion
  (au lieu du commerce vanille), pour les membres de sa faction uniquement —
  plus besoin de repasser par la liste `/faction villageois`.
- **Butin de guerre** : un guerrier ramasse maintenant automatiquement ce que
  lâchent ses victimes (armes, armures, objets…) dans sa réserve personnelle
  (8 emplacements visibles dans sa fiche). Si la réserve est pleine, le surplus
  reste au sol comme avant.
- **Compteur de kills individuel** : affiché dans la liste et la fiche de chaque
  guerrier ("Ennemis tués : X").

## v5.9.6 — Point de rassemblement

- Nouveau bouton **"Point de rassemblement"** dans la fiche de chaque villageois
  (Constructeur et Guerrier) : clique, puis clique-droit sur l'endroit voulu dans
  le monde. Bouton "Annuler le rassemblement" pour le retirer.
  - **Constructeur** : une fois sa file de chantiers vide, il s'y rend au lieu de
    rester sur place.
  - **Guerrier** : une fois libre (plus de cible, pas de patrouille, ne suit
    personne), il s'y rend en priorité sur son poste habituel.
- **Assignation en groupe** : en mode sélection dans `/faction villageois`, le
  bouton "Rassemblement commun" assigne le même point à tous les villageois
  actuellement sélectionnés (guerriers et constructeurs mélangés, en un seul clic).
- Fonctionne avec le même système de rattrapage longue distance que le reste
  (poste, patrouille, chantiers) : le point de rassemblement peut être n'importe
  où sur la carte.

## v5.9.5 — Niveaux d'expérience, tag de faction

- Les villageois recrutés (Constructeur et Guerrier) ont désormais **5 niveaux**,
  chacun de plus en plus dur à atteindre. Une montée de niveau soigne entièrement
  le villageois et prévient toute la faction dans le chat ("Je suis maintenant
  niveau X !").
- **Constructeur** : gagne de l'XP en posant des blocs, en récoltant, et un gros
  bonus en terminant un chantier.
  - Aux niveaux supérieurs, il pose/récolte **plus vite** (jusqu'à 3 blocs par
    passage d'IA au lieu d'1) et se déplace un peu plus vite en chantier.
  - Plus de vie max à chaque niveau.
  - Au niveau 5, s'il n'a pas de nourriture donnée, il se régénère quand même
    tout seul (petite régénération passive).
- **Guerrier** : gagne de l'XP à chaque coup porté (mêlée ou flèche) et un gros
  bonus par ennemi achevé.
  - Plus de dégâts et plus de vie max à chaque niveau.
  - Les armes (épée/arc) s'usent maintenant réellement à l'usage ; à partir du
    niveau 4, il se fabrique lui-même une arme neuve dès que la sienne casse
    (sinon elle est perdue comme avant, il faut lui en redonner une).
- **Tag de faction** : le nom affiché au-dessus de chaque villageois recruté
  inclut désormais le niveau et le nom de sa faction, coloré avec l'icône du
  rang de cette faction (même code couleur que partout ailleurs dans le plugin).
- La fiche (`/faction villageois`) affiche le niveau et l'XP de chaque villageois.

## v5.9.4 — Soin par le sommeil, guerriers archers

- **Sommeil réparateur** : un villageois recruté qui va dormir dans un lit (IA
  vanille) regagne progressivement de la vie pendant qu'il dort.
- **Mode archerie** pour les guerriers : nouveaux emplacements Arc et Flèches dans
  sa fiche, plus un bouton "Mode archerie" (ON/OFF).
  - En mode archerie avec des flèches en stock, il tire à distance sur ses cibles
    et **garde ses distances** (recule s'il se fait approcher de trop près).
  - Dès qu'il n'a plus de flèches, ou que l'ennemi est déjà au corps à corps, il
    repasse automatiquement en mode mêlée : épée si équipée, sinon à mains nues.
  - Aucun changement s'il n'a pas de mode archerie activé : il reste au corps à
    corps comme avant.

## v5.9.3 — Longue distance, assignation de groupe

- **Suivi longue distance** : un guerrier qui te suit continue de te rejoindre même
  à 50+ blocs (rattrapage par téléportation s'il est trop loin pour marcher, ex. si
  tu voles ou te téléportes toi-même) au lieu de rester bloqué.
- **Retour au poste n'importe où sur la carte** : même logique pour rejoindre son
  poste de défense ou son chantier — la distance ou même un autre monde n'est plus
  un obstacle. Il continue de se défendre en chemin et ne "n'oublie" jamais sa tâche
  de poste : dès qu'il n'a plus de cible, il reprend automatiquement la route vers
  son poste.
- **Chantiers n'importe où** : même chose pour les constructeurs — un chantier ou
  une zone de récolte très éloignée n'empêche plus le trajet.
- **Assignation de groupe** dans `/faction villageois` : un nouveau "mode sélection"
  permet de cocher plusieurs villageois dans la liste, puis :
  - "Poste commun" : assigne la même position de défense à tous les guerriers
    sélectionnés en un seul clic dans le monde.
  - "Chantier commun" : tiens le bloc voulu en main, clique, puis délimite une zone —
    elle est ajoutée à la file de TOUS les constructeurs sélectionnés à la fois,
    pour qu'ils bâtissent en parallèle et aillent plus vite.

## v5.9.2 — Constructeur : file de chantiers, récolte autonome, zones libres

- Les zones de chantier, de récolte et les points de patrouille n'ont plus besoin
  d'être dans un chunk claimé par la faction — le guerrier et le constructeur
  peuvent désormais opérer n'importe où dans le monde.
- **File de chantiers** : on peut désormais assigner plusieurs chantiers d'affilée
  à un même constructeur (GUI "Chantier actuel" + compteur de file d'attente) ;
  il les traite dans l'ordre, un par un.
- **Type de bloc par chantier** : chaque chantier a maintenant son propre type de
  bloc (choisi en plaçant un bloc dans un emplacement dédié avant de définir la
  zone), plutôt que de piocher n'importe quoi dans sa réserve.
- **Annuler un chantier** : bouton "Annuler le chantier actuel" (passe au suivant
  dans la file) et "Vider toute la file" (annule tout).
- **Récolte autonome** : bouton "Zone de récolte" — si le constructeur n'a plus
  assez du bloc voulu pour son chantier en cours, il va miner lui-même ce type de
  bloc dans cette zone (s'il y en a) avant de reprendre la construction.
- **Notification de fin de chantier** : à la fin d'un chantier, le constructeur
  "parle" dans le chat du joueur qui le lui a assigné (avec son nom personnalisé),
  ex : `[Bob] Chantier terminé ! (cobblestone)`.

## v5.9.1 — Guerriers : ennemis, périmètre, patrouille, suivi, formation

- Correctif : la sélection de coin de chantier ne se validait plus toute seule en 1
  clic (l'event de clic-droit se déclenche 2 fois par clic, main + off-hand — désormais filtré).
- Les **Guerriers** attaquent désormais aussi les **joueurs des factions actuellement en
  guerre** contre la leur (pas juste les mobs hostiles), en plus de se défendre eux-mêmes
  et de défendre les autres villageois recrutés / joueurs de leur faction attaqués à
  proximité (riposte automatique, même hors période de guerre, sauf contre un attaquant
  de leur propre faction).
- **Poste & périmètre** : bouton "Définir le poste ici" (position actuelle du guerrier)
  et "Rayon de défense" (4-48 blocs, réglable par chat) — il n'engage le combat que
  dans ce rayon autour du poste (ou du joueur suivi), et abandonne une cible qui fuit
  trop loin.
- **Patrouille** : bouton "Définir une patrouille" — clic-droit pour ajouter des points
  dans le monde, shift+clic-droit pour terminer ; il fait la ronde entre ces points
  quand il n'a ni cible ni joueur à suivre.
- **Cesser/reprendre le combat** : bouton toggle — le guerrier arrête d'attaquer (et de
  riposter) tant qu'on ne le réactive pas.
- **Suivre** : bouton toggle — le guerrier suit le joueur qui clique et le défend en
  priorité (son périmètre de défense se recentre sur ce joueur).
- **Formation** : `/faction villageois ranger` (ou bouton dans la liste) aligne tous
  les villageois recrutés à moins de 40 blocs en rang devant le joueur.

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
