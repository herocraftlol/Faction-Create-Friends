# FactionPlugin

Plugin Minecraft de gestion de factions pour serveur Spigot/Paper 1.20.4

## Description

FactionPlugin est un plugin Minecraft avancé qui permet la gestion complète des factions sur votre serveur. Il inclut un système unique de **Puissance de Faction** avec des rangs et des avantages passifs pour récompenser les factions les plus actives !

## Fonctionnalités

### ⚡ Système de Puissance (Power System)
- **Puissance Individuelle (PI)** : Chaque joueur génère de la puissance basée sur ses stats PvP, survie, progression et activité
- **Puissance Globale (PG)** : La somme des PI des membres + bonus de taille
- **7 Rangs de Faction** : Pierre, Bronze, Argent, Or, Diamant, Emeraude, Légendaire
- **Effets passifs** : Plus votre faction monte en rang, plus vous obtenez d'avantages (Speed, Haste, Force, Regen, etc.)
- **Aura de soin** : Au rang Légendaire, les alliés proches reçoivent Regeneration

### 📊 Classement & GUI
- **Classement en temps réel** : Voir le top 10 des factions par puissance
- **Interface de classement** : GUI interactif pour parcourir le classement
- **Breakdown des stats** : Détail de votre puissance par catégorie

### 🔧 Fonctionnalités de base
- **Création de factions** : Créez votre propre faction avec une commande simple
- **Gestion des membres** : Invitez, expulsez, nommez un nouveau chef
- **Système de téléportation** : TP vers les membres de votre faction
- **Inventaire partagé** : Un coffre commun pour tous les membres
- **Interface GUI** : Menu interactif complet pour gérer votre faction
- **ActionBar notifications** : Notifications en jeu pour les événements importants

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

📦 **[Télécharger FactionPlugin-2.0.0.jar](../../releases/download/v2.0.0/FactionPlugin-2.0.0.jar)**

## Version

- **v2.0.0** : Système de Puissance, Rangs et Classement
- **v1.1.0** : Ajout du GUI, téléportation et inventaire partagé
- **v1.0.0** : Version initiale avec les fonctionnalités de base

## License

Ce projet est sous licence MIT.