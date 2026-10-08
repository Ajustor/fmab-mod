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

**v0.3 (État), en cours :**

- **Écoles** du Feu, de la Glace (piques et murs de glace) et de l'Explosion (Feu + Air +
  Décomposer), avec leurs arbres de savoir.
- **Examen d'Alchimiste d'État** : un examinateur de l'armée demande des objets, puis l'épreuve
  oppose le candidat à un golem de pierre lié à un cercle gravé. Récompense : le rang d'État et la
  montre d'Alchimiste d'État (+1 de portée, −20 % de concentration).
- **Cercles portés** : vêtements de cuir brodés, tatouages par rituel (encre alchimique sur un
  cercle tracé au sol). Ils donnent des passifs : renfort, protection contre le feu, vitesse,
  régénération, épines. Une paume tatouée se lance comme un gant.
- **Structures** : Central City, grande ville circulaire fortifiée (quartier général et arène,
  bibliothèque, maisons), et le hameau de Resembool (maison des Rockbell, fermes, champs).
  `/locate structure fmab:central`.

**v0.4 (Porte), en cours :**

- **Savoir** : un glyphe qu'on ne comprend pas fonctionne, avec 25 % de risque de rebond chacun ; on
  le comprend après cinq usages, ou en lisant un **tome d'alchimie** (Rudiments au départ, tomes
  vendus par les bibliothécaires des villages, offerts par Izumi et l'examen d'État, notes de
  Hohenheim à Resembool). Le Traité décrit sans enseigner. Le carnet pose au
  **tampon** les glyphes compris.
- **Transmutation humaine** : glyphe Humain et Recomposer, ingrédients d'un corps et sang de
  l'alchimiste. Elle échoue toujours : une créature difforme naît du cercle, l'alchimiste est happé
  dans l'**Espace blanc**, devant sa Porte et la Vérité.
- **Péage** selon l'ambition : un bras (la main ne tient plus rien), une jambe (lent, sans course),
  les organes (−4 cœurs, toux), la vue (voile sombre). La Vérité est une copie blanche de
  l'alchimiste : ce qu'elle prend, elle le porte, avec sa peau.
- **Corps entier** : la première fois, l'âme se réveille dans une armure de fer scellée de son sang
  (sans faim ni souffle). Les coups usent les pièces, qu'on refait ; si le plastron cède, l'âme erre
  devant la Porte jusqu'à ce qu'un **cercle d'âme** (Humain + Fixer) l'appelle dans une armure. Une
  âme prévoyante prépare des plastrons scellés sur des porte-armure, au-dessus de cercles d'âme, et
  demande à sa Vérité de l'y rappeler.
- **Transmutation sans cercle** pour les Initiés de la Porte : Maj+G joint les mains et lance le
  cercle sélectionné du carnet.
- **Automail** (fer, Rush Valley, Briggs) sur les membres perdus : établi d'automail, usure,
  réparation chez **Winry**, dans la ville de **Rush Valley** (`/locate structure fmab:rush_valley`).
  À Resembool, la maison des Elric garde les notes de Hohenheim.

Tout ce qui peut être data-driven l'est : glyphes (`data/<ns>/fmab/glyph`), combinaisons
(`data/<ns>/fmab/combination`), valeurs d'échange (`data/<ns>/fmab/exchange`), éléments visés (tags
`fmab:element/*`), pages du Traité, Espace blanc (`data/fmab/dimension`). Textes en français et en
anglais.

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

**v0.3 (State), in progress:** Fire, Ice and Explosion schools, the State Alchemist examination
against a stone golem bound to its circle, the State Alchemist's watch, embroidered garments and
ritual tattoos bearing passive circles, and two generated places: the walled Central City and the
hamlet of Resembool.

**v0.4 (Gate), in progress:** glyphs you do not understand work with a risk of rebound and are
learned through practice or alchemy tomes, and the notebook stamps the ones you know; human
transmutation drags the alchemist into the White Space before their Gate and Truth, which takes a
toll weighed by ambition (an arm, a leg, the organs, the sight), and Truth, a white copy of the
alchemist, wears what it takes; losing the whole body seals the soul into an iron armor whose pieces
wear out and can be remade, and a soul whose breastplate breaks wanders before the Gate until a soul
circle calls it into another armor (prepared seals let it call itself back); Gate initiates transmute without a circle by joining their hands; automail
(iron, Rush Valley, Briggs) replaces lost limbs and is repaired by Winry in the town of Rush Valley.

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
