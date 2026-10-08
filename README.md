# FMAB — Alchemy of Amestris

Mod Minecraft (Fabric, Java Edition 26.2) inspiré de *Fullmetal Alchemist: Brotherhood* : échange
équivalent, cercles de transmutation dessinés par le joueur, et à terme la Porte de la Vérité.
*English below.*

## Français

**État : v0.1 en développement (socle).** Le cœur du mod est la transmutation par cercles que le
joueur compose lui-même :

- **Carnet de cercles** : grille de 32 cases, outils ligne, cercle, polygone, arc et point,
  symétrie, pages nommées, import/export par code. Le carnet lit le cercle en direct : glyphes
  reconnus, effet, complexité, stabilité, coût et verdict.
- **Craie de transmutation** : trace au sol le cercle sélectionné dans le carnet (tenu dans l'autre
  main). On l'active paume contre le sol, main vide.
- **Traité d'alchimie** : principes, trois étapes, catalogue des glyphes (on les étudie pour les
  comprendre), règles de composition, cercles d'exemple, rebonds, tabous.
- **10 glyphes** (Terre, Eau, Fer, Cuivre, Fixer, Projeter, Réparer, Décomposer, Direction,
  Intensité) et **6 combinaisons** : mur et pique de pierre, lame de fer, plateforme de glace,
  décomposition, réparation.
- **Échange équivalent** : la matière transmutée vient du monde (un mur est fait de la terre
  creusée devant lui) ou des objets posés sur le cercle ; jamais de changement de famille, jamais
  plus de masse produite que donnée.
- Rangs, concentration (se recharge au repos), rebond proportionnel à l'erreur.

**v0.2 (Alchimiste), en cours :**

- **Cercles complexes** : plusieurs étages reliés par un trait simple (série : la matière produite
  passe à l'étage suivant), double (parallèle) ou brisé (conditionnel) ; satellites sur les sommets
  (infusion d'un élément ou effet propre) ; fusion de deux éléments dans un hexagramme ; polygones
  superposés.
- **Glyphes** Feu, Air et Recomposer : jet de flammes, rafale, fonte, combustion amplifiée, lance de
  pierre et lame-bras temporaires.
- **Savoir** : maîtrise par école gagnée en pratiquant, nœuds qui donnent des bonus, rang Alchimiste
  atteint par la pratique.
- **Supports** : sol, murs et plafonds ; craie, peinture alchimique, gravure au burin.
- **Gants** brodés ou gravés à la table d'alchimiste, lancés d'une touche (G, Maj+G pour joindre les
  mains, H pour l'écran des gants).
- **Izumi** : épreuves récompensées en savoir et combats d'entraînement.
- **Éditeur web** (`web/`) : même parseur, porté en TypeScript, vérifié contre le Java par des
  cercles de test communs (`testdata/circles`). `cd web && npm install && npm run dev`.

Tout ce qui peut être data-driven l'est : glyphes (`data/<ns>/glyph`), combinaisons
(`data/<ns>/combination`), valeurs d'échange (`data/<ns>/exchange`), éléments visés (tags
`fmab:element/*`), pages du Traité. Textes en français et en anglais.

### Compiler

JDK 25 ou plus récent, puis :

```sh
./gradlew build
```

Le jar est dans `build/libs/`. Dépendances à l'exécution : Fabric Loader ≥ 0.19.3 et Fabric API.

Commandes de test (opérateur) : `/fmab rank <rang>`, `/fmab learn_all`, `/fmab forget_all`,
`/fmab rest`, `/fmab mastery <école> <n>`, `/fmab grant <nœud>`.

Les cercles de test partagés se régénèrent depuis l'implémentation Java, qui fait référence :
`./gradlew test -PwriteFixtures=true`.

## English

**Status: v0.1 in development (foundation).** The heart of the mod is transmutation through
circles the player designs: a Circle Notebook to draw them on a 32-cell grid with live analysis,
Transmutation Chalk to draw them on the ground, and an Alchemy Treatise that teaches the glyphs and
the rules. 10 glyphs, 6 combinations, strict equivalent exchange, ranks, concentration and
rebounds. Everything that can be data-driven is. Texts in English and French.

**v0.2 (Alchemist), in progress:** multi-stage circles with links and satellites, Fire, Air and
Recompose glyphs, a practice-based knowledge tree, circles on walls and ceilings (chalk, paint,
engraving), alchemist gloves cast with a key, Izumi's trials and sparring, and a web circle editor
(`web/`) running a TypeScript port of the parser checked against the Java one on shared test circles.

Build with JDK 25+ (`./gradlew build`); runtime needs Fabric Loader ≥ 0.19.3 and Fabric API.

## Mention des ayants droit / Rights holders

**FR** — Fullmetal Alchemist, ses personnages, noms, lieux et éléments d'univers appartiennent à
leurs ayants droit : Hiromu Arakawa et Square Enix pour le manga ; Bones, Aniplex et leurs
partenaires pour l'anime *Fullmetal Alchemist: Brotherhood*. Ce mod est un projet de fan
indépendant, gratuit et non officiel, sans lien avec eux ni approbation de leur part. Les visuels
du mod sont des créations originales ; les glyphes reprennent le symbolisme alchimique historique,
qui est du domaine public.

**EN** — Fullmetal Alchemist, its characters, names, places and universe belong to their rights
holders: Hiromu Arakawa and Square Enix for the manga; Bones, Aniplex and their partners for the
anime *Fullmetal Alchemist: Brotherhood*. This mod is an independent, free and unofficial fan
project, not affiliated with or endorsed by them. The mod's visuals are original creations; the
glyphs draw on historical alchemical symbolism, which is in the public domain.
