# FactionPlugin

Plugin Minecraft de gestion de factions pour serveur Spigot/Paper 1.20.4

## Description

FactionPlugin est un plugin Minecraft tout-en-un qui combine gestion de factions, statistiques joueurs, système de puissance, **banque d'émeraudes**, **système de claims** et **commerce entre joueurs** !

## Fonctionnalités

### 🏦 Banque de Faction (Emerald Bank)
- **Coffre partagé** : Déposez et retirez des émeraudes dans la banque de votre faction
- **Interface GUI intuitive** pour gérer les dépôts/retraits
- **Accès réservé aux membres autorisés**

### 🗺️ Système de Claims
- **Réclamez des chunks** pour votre faction
- **Permissions par joueur** : Configurez qui peut construire/casser dans les zones réclamées
- **Protection du territoire** : Interdiction d'accès aux non-membres
- **GUI de gestion des permissions**

### 💱 Commerce Entre Joueurs (Trade)
- **Échange sécurisé** d'items entre deux joueurs
- **Interface GUI** avec slots pour proposer des items et des émeraudes
- **Confirmation des deux parties** requise pour finaliser l'échange
- **Annulation possible** à tout moment

### ⚡ Système de Puissance (Power System)
- **Puissance Individuelle (PI)** : Chaque joueur génère de la puissance basée sur ses stats PvP, survie, progression et activité
- **Puissance Globale (PG)** : La somme des PI des membres + bonus de taille
- **7 Rangs de Faction** : Pierre, Bronze, Argent, Or, Diamant, Emeraude, Légendaire
- **Effets passifs** : Plus votre faction monte en rang, plus vous obtenez d'avantages

### 📊 Statistiques Joueurs Intégrées
- **`/faction stats [joueur]`** : Stats complètes avec kills, mobs, dégâts, blocs, temps de jeu, K/D ratio
- **`/faction classementjoueurs`** : Classement des top 10 joueurs par catégorie

### 🔧 Fonctionnalités de base
- **Création de factions** : `/faction create`
- **Gestion des membres** : Invitez, expulsez, nommez un nouveau chef
- **Système de téléportation** : TP vers les membres
- **Inventaire partagé** : Un coffre commun
- **Interface GUI** : Menu interactif complet

## Installation

1. Téléchargez la dernière version depuis la [page des releases](../../releases)
2. Placez le fichier `FactionPlugin-1.0.0.jar` dans le dossier `plugins` de votre serveur
3. Redémarrez votre serveur
4. Le fichier de configuration sera généré automatiquement dans `plugins/FactionPlugin/`

## Configuration

Le fichier `config.yml` vous permet de personnaliser :
- Les messages du plugin
- Les permissions
- Les paramètres de faction

## Commandes

| Commande | Description |
|----------|-------------|
| `/faction create <nom>` | Créer une nouvelle faction |
| `/faction info` | Voir les informations de votre faction |
| `/faction invite <joueur>` | Inviter un joueur dans votre faction |
| `/faction leave` | Quitter votre faction |

## Permissions

- `faction.create` - Créer une faction
- `faction.join` - Rejoindre une faction
- `faction.leave` - Quitter une faction

## Développement

### Compilation

```bash
mvn clean package
```

Le JAR compilée sera dans `target/FactionPlugin-1.0.0.jar`

## Télécharger

📦 **[Télécharger FactionPlugin-3.2.1.jar](../../releases/download/v3.2.1/FactionPlugin-3.2.1.jar)**

## Version

- **v3.2.1** : Corrections et améliorations du système de Trade
- **v3.2.0** : Banque d'émeraudes, Système de Claims et Commerce entre joueurs
- **v3.1.0** : Fusion FactionPlugin + FactionStats - Stats joueurs intégrées + Classement
- **v2.0.0** : Système de Puissance, Rangs et Classement Factions
- **v1.1.0** : Ajout du GUI, téléportation et inventaire partagé
- **v1.0.0** : Version initiale avec les fonctionnalités de base

## License

Ce projet est sous licence MIT.