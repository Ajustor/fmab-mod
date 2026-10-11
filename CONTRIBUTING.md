# Contribuer au mod

Tout se fait en français : le code, ses commentaires, les messages de commit. Les textes du jeu
passent par les fichiers de langue (`fr_fr.json` et `en_us.json`), jamais en dur.

## Construire et jouer

Il faut un **JDK 25** (ou plus récent).

```sh
./gradlew build        # compile, lance les tests unitaires, produit le jar dans build/libs/
./gradlew runClient    # lance le jeu de développement
```

## Les tests

- **Tests unitaires** (`src/test`, JUnit 5), lancés par `./gradlew build` ou `./gradlew test` :
  parseur et reconnaissance des cercles, savoir et apprentissage, règles du fauteuil roulant,
  disposition des données, clés de langue fr/en, exemples du Traité. Une règle de jeu testable
  s'écrit en fonction pure, que le code en jeu appelle (voir `Wheelchairs.canPropel`).
- **Cercles de test** (`testdata/circles`) : partagés avec l'éditeur web ; le Java fait référence,
  `./gradlew test -PwriteFixtures=true` les régénère.
- **Gametests client** (`src/gametest`) : le vrai jeu tourne, chaque scène vérifie un état et prend
  des captures dans `build/run/clientGameTest/screenshots`.

  ```sh
  ./gradlew runClientGameTest                                   # toutes les scènes
  ./gradlew runClientGameTest -PfmabGametest=gestures,figures   # quelques-unes
  ./gradlew runClientGameTest -PfmabGametest=towns -PfmabTowns=liore,dublith
  ```

  Scènes : `showcase`, `blocks`, `towns`, `figures`, `items`, `automail_blade`, `animations`,
  `gestures`, `gate`, `stonegate`, `wheelchair`, `recomposition`. Pour tout ce qui se voit en jeu
  (modèle, texture, animation, lieu), regarder les captures avant et après ; ajouter une scène si
  aucune ne montre ce qu'on a changé. Une `AssertionError` fait échouer le gametest : une scène
  vérifie ce qu'elle montre, en plus de le photographier.

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

Les générateurs sont déterministes : relancer `all.py` ne doit rien changer aux PNG commités. La
CI le vérifie (job `textures` de `build.yml`) ; un nouveau générateur posé dans le dossier est pris
en compte tout seul.

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
- `commits.yml` : la convention des titres et des lignes de changelog.
- `pages.yml` : le site GitHub Pages (éditeur de cercles, versions, changelog).
- `release.yml`, au tag `vX.Y.Z` : la version publiée (voir le README).
