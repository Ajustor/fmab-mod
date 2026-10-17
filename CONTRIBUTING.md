# Contribuer au mod

Tout se fait en français : le code, ses commentaires, les messages de commit. Les textes du jeu
passent par les fichiers de langue (`fr_fr.json` et `en_us.json`), jamais en dur.

## Construire et jouer

Il faut un **JDK 25** (ou plus récent).

```sh
./gradlew build                     # toutes les versions : compile, tests unitaires, jars dans build/libs/
./gradlew :fabric:26.2:runClient    # le jeu de développement, Fabric, Minecraft 26.2
./gradlew :26.2:test                # les tests unitaires du cœur, pour une version
```

Après avoir modifié des données (`src/main/resources/data`) : `python tools/data/migrate.py`, qui
régénère leurs versions pour les Minecraft plus récents (`src/overlays/`), à committer avec.

Le mod est découpé en un cœur sans loader (`src/`) et un adaptateur par loader (`fabric/`), construits
pour chaque version de Minecraft prise en charge : voir [ARCHITECTURE.md](ARCHITECTURE.md). Le code du
jeu va dans le cœur ; il ne parle au loader que par ses ports (`com.ajustor.fmab.platform`).

## Les tests

- **Tests unitaires** (`src/test`, JUnit 5), lancés par `./gradlew build` ou `./gradlew :26.2:test`, sur
  une plateforme de test sans loader (`TestPlatform`) :
  parseur et reconnaissance des cercles, savoir et apprentissage, règles du jeu (fauteuil roulant,
  karma, usure des automails), rotations des structures, balayage de tous les plans de bâtiments,
  disposition des données, clés de langue fr/en, exemples du Traité. Une règle de jeu testable
  s'écrit en fonction pure, que le code en jeu appelle (voir `Wheelchairs.canPropel`, `Karma.shifted`).
  `TestBootstrap` amorce Minecraft et les registres du mod pour les tests qui touchent aux blocs ; les
  piles d'objets restent hors de portée (leurs composants ne sont liés qu'au chargement des données).
- **Cercles de test** (`testdata/circles`) : partagés avec l'éditeur web ; le Java fait référence,
  `./gradlew test -PwriteFixtures=true` les régénère.
- **Gametests client** (`fabric/src/gametest`) : le vrai jeu tourne, chaque scène vérifie un état et
  prend des captures dans `fabric/versions/<minecraft>/build/run/clientGameTest/screenshots`.

  ```sh
  ./gradlew runClientGameTest                                               # toutes les scènes, toutes les versions
  ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=gestures,figures  # quelques-unes, une version
  ./gradlew :fabric:26.2:runClientGameTest -PfmabGametest=towns -PfmabTowns=liore,dublith
  ```

  Scènes : `showcase`, `blocks`, `towns`, `figures`, `items`, `automail_blade`, `animations`,
  `gestures`, `gate`, `stonegate`, `wheelchair`, `recomposition`. Pour tout ce qui se voit en jeu
  (modèle, texture, animation, lieu), regarder les captures avant et après ; ajouter une scène si
  aucune ne montre ce qu'on a changé. Chaque scène vérifie ce qu'elle montre, en plus de le
  photographier, avec `GametestAsserts.check` : une règle qui ne tient pas fait échouer le gametest.

## Les textures

Les textures du mod sont **dessinées en code** par les générateurs Python de `tools/skins/` ; on
modifie le générateur, jamais le PNG à la main.

```sh
pip install -r tools/skins/requirements.txt
python tools/skins/all.py              # tout régénérer
python tools/skins/items.py state_watch   # ou un générateur, quelques textures
```

| Générateur | Ce qu'il dessine |
| --- | --- |
| `characters.py` | les peaux des personnages (`textures/entity`) |
| `creatures.py` | les créatures et les homonculus (`textures/entity`) |
| `eggs.py` | les œufs d'apparition (`textures/item`) |
| `items.py` | les icônes d'objets (`textures/item`), et les textures animées de l'obscurité de la Porte (`textures/block/gate_darkness*`) |
| `blocks.py` | les autres textures de blocs (`textures/block`) |
| `cinematic.py` | les images des cinématiques |
| `skinlib.py` | la boîte à outils commune (couleurs, ombrage, patrons de peau) |
| `all.py`, `check.py` | tout régénérer ; vérifier que les images commitées en sont bien issues |

Les générateurs sont déterministes : relancer `all.py` ne doit rien changer aux images commitées.
`python tools/skins/check.py` le vérifie (pixel à pixel : la compression des PNG varie d'une machine
à l'autre), et la CI aussi (job `textures` de `build.yml`). Un nouveau générateur posé dans le
dossier est pris en compte tout seul.

## L'éditeur web

`web/` porte le parseur de cercles en TypeScript, vérifié contre le Java sur les cercles de test :

```sh
cd web && npm install && npm run dev   # npm test pour les tests
```

## Commits et pull requests

- Le travail arrive sur `master` par petites pull requests.
- Titres de commit conventionnels, en français : `type(portée): sujet`, avec `type` parmi `feat`,
  `fix`, `perf`, `refactor`, `docs`, `test`, `chore`, `build`, `ci`, `style`, `revert`.
- Chaque `feat`, `fix` ou `perf` porte en pied de message les deux lignes `Changelog-fr:` et
  `Changelog-en:` : ce sont elles qui font les notes de version.
- Vérifier avant de pousser : `python .github/scripts/check_commits.py origin/master HEAD` (le check
  `commits` refait cette vérification sur chaque pull request).
- Ne pas toucher aux `CHANGELOG*.md` : git-cliff les écrit à la sortie d'une version.

## La CI

- `build.yml`, à chaque pull request et push sur `master` : build et tests du mod, jar en artefact,
  tests et build de l'éditeur web, vérification des textures.
- `gametest.yml`, sur les pull requests qui touchent au rendu ou aux scènes, ou à la demande (avec un
  choix de scènes) : les gametests client sous un écran virtuel, captures en artefact.
- `commits.yml` : la convention des titres et des lignes de changelog.
- `pages.yml` : le site GitHub Pages (éditeur de cercles, versions, changelog).
- `release.yml`, au tag `vX.Y.Z` : la version publiée (voir le README).
