# Architecture

Le mod tourne sur plusieurs versions de Minecraft (26.2, 26.3) depuis les mêmes sources, et il est
construit pour accueillir d'autres loaders que Fabric. Trois idées le permettent : un cœur qui ne
connaît aucun loader, des ports que chaque loader implémente, et une couche de compatibilité pour
ce qui change d'une version du jeu à l'autre.

## Les modules

```
src/                 le cœur : tout le jeu, sans loader (compile contre Minecraft et Mixin seuls)
  main/              logique, contenu, données, mixins, ports (com.ajustor.fmab.platform)
  client/            rendu, écrans, interface (ports du client : platform.client)
  test/              tests unitaires, avec TestPlatform (aucun loader)
fabric/src/          l'adaptateur Fabric (com.ajustor.fmab.fabric)
  main/, client/     implémentations des ports, relais des événements, points d'entrée
  gametest/          les scènes jouées dans le vrai jeu (Fabric Client GameTest API)
versions/<mc>/       les nœuds du cœur, un par version de Minecraft (générés par Stonecutter)
fabric/versions/<mc>/  les nœuds Fabric
```

Chaque nœud Fabric compile contre le cœur de la **même** version (son « frère » dans l'arbre de
Stonecutter) et l'embarque dans son jar : un jar par loader et par version,
`build/libs/fmab-fabric-<version>+<mc>.jar`.

Le cœur ne peut pas importer `net.fabricmc` : il ne l'a pas dans son classpath. Une erreur de
compilation le rappelle à qui l'oublierait.

## Les ports (`com.ajustor.fmab.platform`)

Ce que le mod attend d'un loader, en interfaces à lui, dans son vocabulaire :

| Port | Rôle |
| --- | --- |
| `Platform` | dossier de configuration, mods présents, accès aux autres ports |
| `data.DataSlot` | une donnée portée par une entité : sauvegardée, gardée à la mort, synchronisée |
| `network.Network` | déclarer les paquets, recevoir ceux du client, envoyer les siens |
| `registry.Content` | inscrire le contenu (registres, registres de données, attributs, apparitions, onglet créatif) |
| `event.*` | les événements du jeu que le mod écoute (serveur, joueurs, êtres vivants, interactions, contenu) |
| `client.ClientPlatform` | côté client : réseau, touches, rendus, couches, interface |
| `client.ClientEvents` | les événements du client |

Le loader fournit son implémentation par `ServiceLoader`
(`META-INF/services/com.ajustor.fmab.platform.Platform`, et `…client.ClientPlatform`), et relaie ses
événements vers ceux du mod une fois, au démarrage (`FabricEvents`). Le mod n'écoute jamais
directement un événement de loader.

Ce qui peut se faire sans loader se fait sans : un mixin plutôt qu'une API de loader quand le
besoin est petit (les onglets de l'inventaire, `InventoryScreenMixin`).

## Les versions de Minecraft (Stonecutter)

[Stonecutter](https://stonecutter.kikugie.dev) construit chaque version depuis les mêmes sources.
Les sources commitées sont celles de la version active (`stonecutter active` dans
`stonecutter.gradle.kts`, 26.2). Les dépendances de chaque version sont dans
`stonecutter.properties.toml`.

Ce qui change d'une version à l'autre, du plus simple au plus rare :

1. **Un renommage** (classe, champ, constante) : une règle de remplacement dans
   `stonecutter.gradle.kts`. Le code reste écrit pour la version active, sans commentaire.
2. **Un appel dont la forme change** : une fonction de `com.ajustor.fmab.compat`, seul endroit du
   code où vivent les conditions de version (`//? if >=26.3 {`). Le reste du mod appelle la
   fonction, sans savoir quelle version tourne.
3. **Une signature qu'on surcharge ou qu'un mixin vise** (une méthode de Minecraft redéfinie par une
   classe du mod, ou ciblée par un mixin) : la condition de version directement dans la classe, à
   défaut d'autre moyen. Le compilateur ne vérifie pas les cibles des mixins : c'est le jeu qui refuse
   de démarrer, d'où les gametests sur chaque version.

### Les données

Les JSON de `src/main/resources/data` sont écrits au format de la version active. Pour les versions
suivantes, `tools/data/migrate.py` en dérive les fichiers dont le format change (progrès, butin,
échanges, biomes…) dans `src/overlays/<version>/`. Le build les pose par-dessus les sources pour cette
version et les suivantes, comme les « overlays » des packs du jeu. On n'écrit donc les données qu'une
fois : après avoir touché à `data/`, `python tools/data/migrate.py`, puis committer les overlays (la CI
vérifie qu'ils sont à jour). Une nouvelle version de Minecraft qui change un format : une fonction de
migration de plus dans le script, ses règles tirées des données du jeu lui-même, comparées d'une
version à l'autre.

La scène de gametest `data` vérifie, sur chaque version, que tous les progrès, recettes, tables de
butin et échanges du mod sont bien chargés par le jeu.

Pour travailler sur une autre version dans l'IDE : `./gradlew "Set active project to 26.3"` (les
sources sont réécrites pour elle), puis `./gradlew "Reset active project"` avant de committer.

## Ajouter un loader (NeoForge…)

1. Une branche dans `settings.gradle.kts` (`branch("neoforge")`), avec son `neoforge/build.gradle.kts`.
2. `neoforge/src/main/java/com/ajustor/fmab/neoforge/` : une implémentation de chaque port, le relais
   des événements de NeoForge vers ceux du mod, le point d'entrée (`@Mod`) qui appelle `Fmab.init()`.
3. `neoforge.mods.toml` et les fichiers `META-INF/services` des ports.

Points d'attention connus pour NeoForge : les registres du jeu n'acceptent d'inscriptions que
pendant `RegisterEvent` (l'implémentation de `Content.register` les garde jusque-là) ; les
apparitions de créatures passent par des modificateurs de biomes ; Mod Menu y est remplacé par
l'écran de configuration de NeoForge.

## Ajouter une version de Minecraft

1. Sa section dans `stonecutter.properties.toml` et son nom dans `settings.gradle.kts`.
2. `./gradlew build` : les erreurs de compilation donnent la liste de ce qui a changé ; le corriger
   selon les trois cas plus haut.
3. Les formats de données : une migration dans `tools/data/migrate.py` si le jeu en a changé.
4. Les gametests sur cette version : `./gradlew :fabric:<mc>:runClientGameTest`, la scène `data` en
   premier. Ajouter la version à la matrice de `.github/workflows/gametest.yml`.
