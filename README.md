# FactionPlugin

Plugin Minecraft de gestion de factions pour serveur Spigot/Paper 1.20.4

## Description

FactionPlugin est un plugin Minecraft qui permet la gestion complète des factions sur votre serveur. Il inclut des fonctionnalités avancées telles que la création de factions, la gestion des membres, les interfaces graphiques (GUI), la téléportation et la gestion d'inventaire partagé.

## Fonctionnalités

### 🔧 Fonctionnalités de base
- **Création de factions** : Créez votre propre faction avec une commande simple
- **Gestion des membres** : Invitez et gérez les membres de votre faction
- **Système de chat** : Communication entre membres de faction
- **ActionBar notifications** : Notifications en jeu pour les événements importants

### ✨ Nouveautés v1.1.0
- **Interface Graphique (GUI)** : Menu interactif complet pour gérer votre faction
  - Menu principal avec toutes les options
  - Gestion des membres (voir, expulser, nommer chef)
  - Menu de téléportation vers les membres
- **Système de Téléportation** : Téléportez-vous vers n'importe quel membre de votre faction
  - Téléportation vers un membre spécifique
  - Téléportation vers le membre le plus proche
- **Inventaire Partagé** : Un coffre commun pour tous les membres de la faction
  - Accès via le GUI ou commande
  - Tous les membres peuvent accéder au même inventaire
- **Confirmation de sécurité** : Confirmation avant les actions irréversibles (dissoudre la faction, quitter)

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

📦 **[Télécharger FactionPlugin-1.1.0.jar](../../releases/download/v1.1.0/FactionPlugin-1.1.0.jar)**

## Version

- **v1.1.0** : Ajout du GUI, téléportation et inventaire partagé
- **v1.0.0** : Version initiale avec les fonctionnalités de base

## License

Ce projet est sous licence MIT.