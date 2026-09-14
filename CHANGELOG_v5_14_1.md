# 🆕 CHANGELOG — FactionPlugin v5.14.1

## 🎯 Résumé

Cette version fait **le ménage des factions fantômes** : les factions qui
n'ont plus aucun membre (à cause de bugs anciens où la dissolution ne
libéait pas toujours les claims, le coffre, la banque et le classement)
sont maintenant **automatiquement et entièrement supprimées** dès qu'elles
se retrouvent vides. Plus d'entrées qui traînent pour toujours dans
`/faction topbanque`, `/faction classement`, ou qui bloquent des claims
« à personne ».

La v5.14.0 avait introduit une dissolution différée d'une heure (pour
laisser le temps de récupérer ses affaires) ; la v5.14.1 s'attaque au cas
où il n'y a déjà plus personne pour récupérer quoi que ce soit — la
suppression est alors **immédiate et complète**.

---

## 🧹 Nettoyage automatique des factions fantômes

### Le problème que ça corrige

Au fil des versions, plusieurs bugs historiques ont laissé des factions
dans un état semi-vivant :

- **Banque** : un compte en banque de faction n'était jamais supprimé
  au disband (corrigé partiellement pour le renommage en v5.13.0, jamais
  pour la dissolution). Résultat : des soldes orphelins apparaissent
  pour toujours dans `/faction topbanque`.
- **Claims** : libérés correctement seulement depuis la v5.14.0 ; avant,
  les chunks « appartenaient » à une faction qui n'existait plus.
- **Coffre partagé** : supprimé seulement depuis la v5.14.0.
- **Classement** : les caches de puissance/classement n'étaient jamais
  purgés au disband ou au renommage — d'où des entrées fantômes
  immortelles dans `/faction classement`.

Toutes ces entrées « zombies » empilaient silencieusement de la donnée
morte et un peu de CPU à chaque rechargement du plugin.

### Ce qui change

- **Toute faction dont la liste de membres est vide** est désormais
  considérée comme **fantôme** et est **automatiquement et entièrement
  supprimée** :
  - claims libérés (`ClaimManager.removeAllClaims`)
  - coffre partagé de faction supprimé
  - compte en banque de faction supprimé
  - entrée retirée du classement de puissance
  - faction réellement supprimée (elle n'existe plus nulle part)
- **Pas de délai d'une heure** dans ce cas (à la différence d'un disband
  « normal » de la v5.14.0) : il n'y a personne pour venir récupérer
  quoi que ce soit, autant faire le ménage tout de suite.
- **Nettoyage initial au démarrage** : un passage est exécuté **une
  première fois au démarrage du serveur**, spécifiquement pour rattraper
  les factions fantômes déjà présentes (héritées des versions
  précédentes).
- **Filet de sécurité** : la même purge est répétée **toutes les 30
  minutes** au cas où une faction se retrouverait un jour sans membre
  par un autre chemin que `/faction disband`.

### Pourquoi deux rythmes ?

- **Démarrage** : nécessaire parce que la donnée fantôme peut déjà
  être présente dans les fichiers de sauvegarde avant la mise à jour.
- **Toutes les 30 min** : rustine de sécurité pour ne pas avoir à
  prouver une seule et unique porte d'entrée vers « faction vide » —
  si une telle situation apparaît par un futur bug, elle sera nettoyée
  d'elle-même peu après.

---

## 🔧 Correctifs de compilation Paper API 1.21.4

Plusieurs symboles de l'API Bukkit/Paper utilisés par le plugin avaient
changé d'emplacement ou de nom entre Paper 1.21.0 et 1.21.4 (déjà
partiellement signalés en v5.13.1 ; derniers ajustements ici) :

| Symbole | Avant | Après |
|---------|-------|-------|
| `org.bukkit.plugin.JavaPlugin` | en `org.bukkit.plugin` | déplacé en `org.bukkit.plugin.java` (Paper 1.21+) |
| `Particle.SPELL_WITCH` | retiré en 1.21 | `Particle.WITCH` |
| `Particle.VILLAGER_HAPPY` | retiré en 1.21 | `Particle.HAPPY_VILLAGER` |
| `Sound.ENTITY_PLAYER_EAT` | retiré en 1.21 | `Sound.ENTITY_GENERIC_EAT` |
| `Material.GOLD_STAINED_GLASS_PANE` | retiré en 1.21 | `Material.YELLOW_STAINED_GLASS_PANE` |

À noter également :

- L'événement **`org.bukkit.event.entity.EntitySleepEvent`** n'existe
  plus en Paper 1.21+ (seuls restent `BatToggleSleepEvent`,
  `PlayerBedEnterEvent`, `PlayerBedLeaveEvent`, `PlayerDeepSleepEvent`).
  Le soin accordé aux villageois recrutés en dormant a donc été
  désactivé — voir la note ci-dessous.
- Le **typage de `MapPalette.matchColor(...)`** a changé : la signature
  publique utilise `java.awt.Color` (au lieu de `org.bukkit.Color`).
  Le `FactionMapRenderer` convertit maintenant explicitement.

### Soin des recrues en dormant

Paper 1.21 a supprimé l'événement **`EntitySleepEvent`** pour les
entités non-joueur, ce qui rend impossible la détection fiable du
moment exact où un villageois recruté se couche. La mécanique de
**soin périodique pendant le sommeil** (introduite en v5.9.4, à l'époque
où cette fonctionnalité existait encore côté API) est **désactivée**
dans cette version. Les autres soins (auto-régénération naturelle,
potion de soin donnée par un Récolteur, etc.) continuent de
fonctionner normalement.

---

## 📦 Migration

- **Aucune action requise côté admin** : la purge des factions fantômes
  s'exécute toute seule au démarrage et toutes les 30 minutes.
- **Si vous voulez forcer un nettoyage tout de suite** : il suffit de
  redémarrer le serveur — la purge initiale est garantie.
- **Les factions encore actives ne sont pas affectées** : le critère est
  strictement « 0 membre », pas « basse puissance » ou « inactif depuis
  longtemps ».

---

## ℹ️ Note sur la numérotation

- **v5.14.0** = dissolution différée d'une heure, avec bugs historiques
  corrigés au passage.
- **v5.14.1** = nettoyage automatique des factions qui n'ont déjà plus
  aucun membre (cas pathologique laissé par d'anciens bugs).

Les deux releases se complètent : la 5.14.0 traite le **cas normal**
(dissolution volontaire par un chef, qui doit avoir le temps de
récupérer), la 5.14.1 traite le **cas pathologique** (faction déjà
vide, plus rien à récupérer).