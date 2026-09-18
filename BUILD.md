# FactionPlugin 5.15.5 — compilation

## Prérequis
- Java 21
- Maven 3.9+
- Accès réseau à Maven Central et au dépôt PaperMC

## Compilation
Depuis ce dossier :

```bash
mvn clean package
```

Le JAR est généré dans `target/FactionPlugin-5.15.5.jar`.

## Installation
Copier le JAR dans `plugins/` de ton serveur Paper 1.21.x puis redémarrer le serveur.

## Modifications consolidées
- recrutement de villageois sans limite par faction ;
- villageois jusqu'au niveau 100 ;
- rang Mythique à 1 000 000 de puissance ;
- bonus exclusifs du rang Mythique et préfixe dédié ;
- 6 homes au rang Mythique ;
- GUI des rangs mis à jour ;
- correction des chemins de duplication dans `/fac shop` (dépôt curseur, Shift-clic, annulation et drag).
