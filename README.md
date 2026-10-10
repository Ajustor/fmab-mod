# FMAB — Alchemy of Amestris

Mod Minecraft (Fabric, Java Edition 26.2) inspiré de *Fullmetal Alchemist: Brotherhood* : échange
équivalent, cercles de transmutation dessinés par le joueur, la Porte de la Vérité, les homonculus
et le Jour promis. Site : <https://ajustor.github.io/fmab-mod/> (éditeur de cercles en ligne,
versions, changelog). *English below.*

## Français

**État : v1.0, tout le contenu prévu est en place.** La première version publiée sortira avec le
premier tag `v1.0.0`.

### Installation

Minecraft 26.2, [Fabric Loader](https://fabricmc.net/) ≥ 0.19.3, [Fabric API](https://modrinth.com/mod/fabric-api)
≥ 0.161.0 et Java 25. Posez le jar du mod dans `mods/`. Les jars de chaque version sont sur la page
[Releases](https://github.com/Ajustor/fmab-mod/releases) ; chaque build de `master` en produit un
aussi, dans les artefacts de l'onglet Actions.

### Touches

Toutes réglables dans Options → Commandes → Touches, catégorie « Alchimie ».

| Touche | Action |
|---|---|
| N | Ouvrir le Carnet de cercles |
| R (maintenue) | Roue des cercles : pointer un cercle et relâcher ; 1 à 9 pour choisir sans viser ; un clic gauche joint aussitôt les mains (Initiés de la Porte) |
| G / Maj+G | Lancer le cercle des gants / joindre les mains (les deux gants, ou sans cercle pour un Initié) |
| H | Écran des gants |
| — | Cercle suivant / précédent (sans touche par défaut) |

Dans l'inventaire de survie, l'onglet **Corps** montre les membres, les automails et les gants.

### Premiers pas

Chaque joueur commence avec le **Traité d'alchimie**, le tome des **Rudiments** et de la **craie** ;
son **carnet** contient déjà un mur, une pique et une plateforme de glace. Choisissez un cercle (N ou
R), tracez-le au sol à la craie (clic droit), puis posez la paume dessus (clic droit, main vide). Le
Traité explique le reste, et les progrès (touche L) tracent le chemin : Izumi à Dublith, l'examen
d'État à Central, la Porte, les homonculus, puis Père.

### Contenu

- **Cercles composés par le joueur** : grille de 32 cases, outils ligne, cercle, polygone, arc, point
  et tampon (glyphes compris), symétrie, pages nommées et rangées, import/export par code. Le carnet
  lit le cercle en direct : glyphes, effet, complexité, stabilité, coût, verdict.
- **15 glyphes et 19 combinaisons** : Terre, Eau, Fer, Cuivre, Feu, Air, Or, Humain ; Fixer,
  Projeter, Réparer, Décomposer, Recomposer ; Direction, Intensité. Étages reliés en série, en
  parallèle ou en condition, satellites, fusion de deux éléments, polygones superposés.
- **Échange équivalent** : la matière vient du monde ou des objets posés sur le cercle ; jamais de
  changement de famille, jamais plus de masse produite que donnée.
- **Supports** : sol, murs et plafonds ; craie, peinture, gravure au burin ; cercles de 1 à 7 blocs ;
  déclencheurs (retardement, piège, redstone) ; fusion à deux alchimistes. Accroupi, main vide, un
  clic droit sur un cercle inconnu le recopie dans le carnet.
- **Rangs et savoir** : Apprenti, Alchimiste (par la pratique), Alchimiste d'État (examen contre un
  golem), Initié de la Porte. La concentration grandit avec le rang. Maîtrise par école (Terre, Métal,
  Eau, Feu, Explosion, Destruction, Médecine, Vie) ; un glyphe non compris marche avec un risque de
  rebond, et s'apprend par l'usage ou par les tomes.
- **Cercles portés** : gants brodés ou gravés (dont les gants à silex de Mustang et les gantelets),
  vêtements brodés, tatouages par rituel, aux passifs permanents.
- **La Porte** : la transmutation humaine échoue toujours et happe l'alchimiste dans l'Espace blanc,
  où la Vérité prend son péage (un bras, une jambe, les organes, la vue... ou le corps entier : l'âme
  vit alors dans une armure scellée de son sang). En échange, l'Initié comprend tous les glyphes et transmute sans cercle. Le cercle
  n'est dans aucun traité : on le recompose soi-même, ou on trouve de très rares **Notes sur la
  transmutation humaine**.
- **Automail** (fer, Rush Valley, Briggs) sur les membres perdus : à l'établi ou dans l'onglet Corps,
  usure, réparation et retrait chez **Winry**.
- **Fauteuil roulant** (craft, ou chez un forgeron d'outils) : sans jambes, on rampe, ou l'on s'y assied.
  Il ne saute pas et ne franchit qu'une demi-hauteur (dalles, escaliers : il faut des rampes) ; il faut
  deux bras pour faire tourner ses roues, qui prennent alors les mains. Un autre joueur peut le pousser
  (clic droit dessus, accroupi pour lâcher), ou un villageois contre une émeraude.
- **Pierre philosophale** : devant la Porte, elle rachète à la Vérité ce qu'elle a pris ; elle anime
  l'être d'une transmutation humaine, amplifie les transmutations, se fabrique par sacrifice.
  **Karma**, **éclipse** tous les huit jours, **Pierre vivante**.
- **Homonculus** qui se reconstituent tant que leur Pierre a des âmes : Lust, Gluttony (et son Ventre),
  Envy le déguisé, Greed (allié possible), Sloth, Wrath, Pride ; enfin **Père** et sa zone
  anti-alchimie, au bout de la quête des sept **points de sang**. Barres de boss une fois le combat
  engagé.
- **Autres arts et armes** : alkahestry de Xing (kunaï, May Chang), bras de Scar, mines de Kimblee,
  pistolet et fusil, couteaux, sabre de Briggs, épée de Xing.
- **Villes habitées** : villageois (leur métier vient du poste de travail de leur maison), maisons
  meublées, gardes aux portes de Central ; les cartographes vendent les cartes des lieux du mod.
- **Finition** : cinématiques (la Porte, le Jour promis, la chute de Père), éclipse dans le ciel,
  sons propres (CC0), modèles des boss, textes en français et en anglais.

### Lieux

`/locate structure fmab:<id>` pour en trouver un.

| Lieu | `id` | Où |
|---|---|---|
| Central City (QG et examen d'État, bibliothèque, résidence Bradley, tunnel de Sloth, repaire de Père) | `central` | plaines, savanes, prairies |
| Resembool (Rockbell, maison des Elric) | `resembool` | plaines, prairies, forêts fleuries |
| Dublith (boucherie d'Izumi) | `dublith` | plaines, forêts, prairies |
| Rush Valley (Winry, ateliers d'automail) | `rush_valley` | désert, savanes, badlands |
| Liore (Cornello) | `liore` | désert |
| Ruines d'Ishval (Scar) | `ishval_ruins` | désert, badlands |
| Ruines de Xerxès (Hohenheim) | `xerxes_ruins` | désert, badlands |
| Dispensaire de Marcoh | `marcoh_clinic` | taïga, forêts, plaines |
| Laboratoire 5 (Barry, Lust, Gluttony) | `laboratory_5` | plaines, forêts, taïga, savanes |
| Devil's Nest (Greed) | `devils_nest` | savanes, plaines, désert |
| Pavillon de Xing (May Chang) | `xing` | jungles, cerisaies |
| Fort Briggs (Olivier Armstrong) | `fort_briggs` | montagnes et plaines enneigées |
| Points de sang | `blood_crest` | partout |
| Île de Yock (épreuve d'Izumi) | `yock_island` | océans |

### Données

Tout ce qui peut être data-driven l'est : glyphes (`data/<ns>/fmab/glyph`), combinaisons
(`data/<ns>/fmab/combination`), nœuds de savoir (`data/<ns>/fmab/knowledge`), valeurs d'échange
(`data/<ns>/fmab/exchange`), éléments visés (tags `fmab:element/*`), pages du Traité, lieux
(`data/fmab/worldgen`), Espace blanc et Ventre (`data/fmab/dimension`).

Configuration du serveur : `config/fmab.json`, créé au premier lancement.
`restart_wipes_progress` (par défaut `true`) : une âme qui repart de zéro devant la Vérité perd aussi
toute sa progression d'alchimiste ; à `false`, elle ne retrouve que son corps.

Commandes de test (opérateur) : `/fmab rank <rang>`, `/fmab learn_all`, `/fmab forget_all`,
`/fmab rest`, `/fmab mastery <école> <n>`, `/fmab grant <nœud>`.

### Développement

JDK 25 ou plus récent :

```sh
./gradlew build        # jar dans build/libs/, tests compris
./gradlew runClient    # lancer le jeu de développement
```

L'**éditeur de cercles** (`web/`) porte le même parseur en TypeScript, vérifié contre le Java sur
des cercles de test communs (`testdata/circles`) : `cd web && npm install && npm run dev`. Ces
cercles se régénèrent depuis le Java, qui fait référence : `./gradlew test -PwriteFixtures=true`.

Chaque pull request et chaque push sur `master` construisent le mod et l'éditeur
(`.github/workflows/build.yml`) ; le jar est téléchargeable dans les artefacts du build. Le travail
arrive sur `master` par petites pull requests. Les commits suivent la convention
`type(portée): sujet` ; chaque `feat:`, `fix:` ou `perf:` porte les lignes `Changelog-fr:` et
`Changelog-en:`, que le check `commits` vérifie.

### Publier une version

Pousser un tag `vX.Y.Z` (ou `vX.Y.Z-beta`, publié en préversion) lance
`.github/workflows/release.yml` : jar à la version du tag, GitHub Release avec le changelog français
et anglais (git-cliff, `cliff.fr.toml` et `cliff.toml`), `CHANGELOG.fr.md` et `CHANGELOG.md`
recommités sur `master`, puis le site (pages Versions, Changelog et Installation).

```sh
git tag v1.0.0 && git push origin v1.0.0
```

La version du mod suit les tags : sur `v1.2.3`, le jar est en `1.2.3` ; après lui, en
`1.2.4-dev.N` (N commits depuis le tag) ; sans tag, celle de `gradle.properties`.

## English

**Status: v1.0, all planned content is in.** The first published version comes with the first
`v1.0.0` tag. Website: <https://ajustor.github.io/fmab-mod/> (online circle editor, versions,
changelog).

### Installation

Minecraft 26.2, Fabric Loader ≥ 0.19.3, Fabric API ≥ 0.161.0 and Java 25. Drop the mod jar into
`mods/`. Jars are on the [Releases](https://github.com/Ajustor/fmab-mod/releases) page; every
`master` build also produces one in the Actions artifacts.

### Controls

All rebindable under Options → Controls → Key Binds, "Alchemy" category.

| Key | Action |
|---|---|
| N | Open the Circle Notebook |
| R (hold) | Circle wheel: point at a circle and release; 1 to 9 to pick without aiming; a left click joins the hands at once (Gate initiates) |
| G / Shift+G | Cast the gloves' circle / join hands (both gloves, or no circle for an initiate) |
| H | Gloves screen |
| — | Next / previous circle (unbound by default) |

In the survival inventory, the **Body** tab shows limbs, automail and gloves.

### Getting started

Every player starts with the **Alchemy Treatise**, the **Rudiments** tome and **chalk**; their
**notebook** already holds a wall, a spike and an ice platform. Pick a circle (N or R), draw it on
the ground with chalk (right-click), then lay your palm on it (right-click, empty hand). The Treatise
explains the rest, and advancements (L key) trace the path: Izumi in Dublith, the State exam in
Central, the Gate, the homunculi, then Father.

### Content

- **Player-designed circles**: a 32-cell grid with line, circle, polygon, arc, dot and stamp tools,
  symmetry, named and ordered pages, import/export codes, and live analysis (glyphs, effect,
  complexity, stability, cost, verdict).
- **15 glyphs and 19 combinations**, multi-stage circles linked in series, parallel or condition,
  satellites, two-element fusion, layered polygons.
- **Equivalent exchange**: matter comes from the world or from items laid on the circle; never a
  change of family, never more mass out than in.
- **Media**: floors, walls and ceilings; chalk, paint, chisel engraving; 1 to 7-block circles;
  triggers (delay, trap, redstone); two-alchemist fusion. Crouching empty-handed, right-click an
  unknown circle to copy it into the notebook.
- **Ranks and knowledge**: Apprentice, Alchemist (through practice), State Alchemist (exam against a
  golem), Gate initiate; concentration grows with rank; mastery per school (Earth, Metal, Water, Fire, Explosion, Destruction,
  Medicine, Life); unknown glyphs work with
  a rebound risk and are learned through use or tomes.
- **Worn circles**: embroidered or engraved gloves (including Mustang's spark gloves and
  gauntlets), embroidered clothes, ritual tattoos with permanent passives.
- **The Gate**: human transmutation always fails and drags the alchemist into the White Space, where
  Truth takes its toll (an arm, a leg, the organs, the sight... or the whole body, leaving the soul in
  a blood-sealed armor). In exchange, initiates transmute without a circle. The circle is in no
  treatise: work it out yourself, or find very rare **Notes on Human Transmutation**.
- **Automail** (iron, Rush Valley, Briggs) on lost limbs: fitted at the bench or in the Body tab,
  wears out, repaired and removed by **Winry**.
- **Wheelchair** (crafted, or bought from a toolsmith): without legs you crawl, or sit in one. It
  cannot jump and only climbs half a block (slabs, stairs: you need ramps); turning its wheels takes
  both arms, and keeps your hands busy. Another player can push it (right-click it, sneak to let go),
  or a villager for an emerald.
- **Philosopher's Stone**: restores what the Gate took, amplifies transmutations, made by sacrifice.
  **Karma**, an **eclipse** every eighth day, the **Living Stone**.
- **Homunculi** that reconstitute while their Stone holds souls: Lust, Gluttony (and his Belly),
  Envy in disguise, Greed (a possible ally), Sloth, Wrath, Pride; finally **Father** and his
  anti-alchemy zone, at the end of the quest for the seven **blood crests**. Boss bars only show once
  a fight has begun.
- **Other arts and weapons**: Xing alkahestry (kunai, May Chang), Scar's arm, Kimblee's mines,
  pistol and rifle, throwing knives, Briggs sabre, Xing sword.
- **Lived-in towns**: villagers whose trade comes from their house's workstation, furnished houses,
  guards at Central's gates; cartographers sell maps to the mod's places.
- **Polish**: cinematics (the Gate, the Promised Day, Father's fall), an eclipse in the sky, the mod's
  own sounds (CC0), boss models, texts in French and English.

### Places

Use `/locate structure fmab:<id>`: `central`, `resembool`, `dublith`, `rush_valley`, `liore`,
`ishval_ruins`, `xerxes_ruins`, `marcoh_clinic`, `laboratory_5`, `devils_nest`, `xing`,
`fort_briggs`, `blood_crest`, `yock_island` (see the French table above for who lives where).

### Development and releases

Build with JDK 25+ (`./gradlew build`, jar in `build/libs/`; `./gradlew runClient` to play). The web
circle editor (`web/`) ports the parser to TypeScript and is checked against the Java one on shared
test circles. Every pull request and push on `master` builds the mod and the editor; the jar is in
the build artifacts. Pushing a `vX.Y.Z` tag (`vX.Y.Z-beta` for a pre-release) publishes a GitHub
Release with the French and English changelog, and the mod version follows the tags (`1.2.3` on
`v1.2.3`, `1.2.4-dev.N` after it). Server settings live in `config/fmab.json`.

## Mention des ayants droit / Rights holders

**FR** — Fullmetal Alchemist, ses personnages, noms, lieux et éléments d'univers appartiennent à
leurs ayants droit : Hiromu Arakawa et Square Enix pour le manga ; Bones, Aniplex et leurs
partenaires pour l'anime *Fullmetal Alchemist: Brotherhood*. Ce mod est un projet de fan
indépendant, gratuit et non officiel, sans lien avec eux ni approbation de leur part. Les visuels
du mod sont des créations originales ; les glyphes reprennent le symbolisme alchimique historique,
qui est du domaine public. Les sons dérivent de banques du domaine public (CC0) : Kenney et OpenGameArt
(voir `assets/fmab/sounds/credits.txt`).

**EN** — Fullmetal Alchemist, its characters, names, places and universe belong to their rights
holders: Hiromu Arakawa and Square Enix for the manga; Bones, Aniplex and their partners for the
anime *Fullmetal Alchemist: Brotherhood*. This mod is an independent, free and unofficial fan
project, not affiliated with or endorsed by them. The mod's visuals are original creations; the
glyphs draw on historical alchemical symbolism, which is in the public domain. Sounds are derived from public
domain (CC0) packs by Kenney and from OpenGameArt (see `assets/fmab/sounds/credits.txt`).
