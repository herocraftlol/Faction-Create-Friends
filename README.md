# FactionPlugin

Plugin Minecraft de gestion de factions pour serveur Spigot/Paper 1.20.4

## Description

FactionPlugin est un plugin Minecraft qui permet la gestion complète des factions sur votre serveur. Il inclut des fonctionnalités telles que la création de factions, la gestion des membres, et des notifications en jeu.

## Fonctionnalités

- **Création de factions** : Créez votre propre faction avec une commande simple
- **Gestion des membres** : Invitez et gérez les membres de votre faction
- **Système de chat** : Communication entre membres de faction
- **ActionBar notifications** : Notifications en jeu pour les événements importants
- **Protection de territoire** : Système de protection basique pour les territoires de faction

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

📦 **[Télécharger FactionPlugin-1.0.0.jar](../../releases/download/v1.0.0/FactionPlugin-1.0.0.jar)**

## License

Ce projet est sous licence MIT.