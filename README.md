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
- **Craie de transmutation** : trace au sol le cercle sélectionné dans le carnet (touche N). On
  l'active paume contre le sol, main vide.
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
  devant la Porte jusqu'à ce qu'un **cercle d'âme** (Humain + Fixer) l'appelle dans une armure.
  Chaque joueur a son propre **sceau de sang** (Traité, chapitre VIII) : on le trace dans un plastron
  à l'encre alchimique ou de son sang, pour soi ou pour un ami. Une âme prévoyante prépare des
  plastrons scellés sur des porte-armure au-dessus de cercles d'âme, et demande à sa Vérité de l'y
  rappeler ; elle peut aussi changer d'armure, et se répare en y transmutant son matériau.
- **Taille des cercles** : 1, 3, 5 ou 7 blocs, de plus en plus puissants et coûteux.
- **Éclairs bleus** de la réaction alchimique à chaque transmutation.

**v0.5 (Homonculus), en cours :**

- Les **homonculus** se reconstituent sur place tant que leur Pierre philosophale a des âmes ; à la
  dernière, ils laissent un noyau de Pierre.
- **Lust** (doigts-lames qui ignorent les boucliers, craint le feu) et **Gluttony** (aspire et avale
  dans son **Ventre**, une dimension noyée de sang) gardent le **Laboratoire 5**
  (`/locate structure fmab:laboratory_5`).
- **Envy** erre déguisé (un joueur, un habitant), se révèle, devient un monstre géant puis un lézard.
- **Greed** et son Bouclier ultime tiennent le **Devil's Nest** ; un pacte (or, émeraudes, diamants,
  ou le battre) en fait un allié payé à la journée.
- La **Pierre philosophale** (quatre noyaux autour de sang cristallisé, cent âmes) rend ce que la
  Porte a pris, chaque partie coûtant des âmes selon sa gravité.

**v0.6 (Derniers homonculus), en cours :**

- **Sloth** creuse le grand tunnel sous Central : charges dévastatrices en ligne droite à travers la
  roche, puis une pause épuisée où il encaisse davantage.
- **Wrath** (King Bradley), dans la résidence Bradley à Central : une seule vie, mais son Œil
  ultime esquive les flèches et une bonne part des coups de face ; on le touche dans le dos ou
  pendant qu'il frappe.
- **Pride** (Selim) : ses ombres tranchent, dévorent et parent les coups, mais ne vivent qu'avec de
  la lumière ; dans le noir complet ou une lumière intense, il n'est qu'un enfant vulnérable.

**v0.7 (Pierre), en cours :**

- La **Pierre philosophale** tenue en main amplifie les transmutations (×1,5, sans concentration ni
  rebond d'instabilité) au prix d'âmes, et attire les monstres. On la fabrique par **sacrifice** :
  sang cristallisé sur un cercle de transmutation humaine, qui consume les vies de son aire.
- Le **karma** (−100 à +100) suit vos actes ; les villageois en parlent (prix, golems), et les
  homonculus recrutent les âmes noires.
- Tous les huit jours, l'**éclipse** ; c'est là qu'un Initié, une Pierre pleine dans chaque main,
  devient une **Pierre vivante** : plus de péage, résurrection sur place tant qu'il reste des âmes.
- **Transmutation sans cercle** pour les Initiés de la Porte : Maj+G joint les mains et lance le
  cercle sélectionné du carnet.
- **Automail** (fer, Rush Valley, Briggs) sur les membres perdus : établi d'automail, usure,
  réparation chez **Winry**, dans la ville de **Rush Valley** (`/locate structure fmab:rush_valley`).
  À Resembool, la maison des Elric garde les notes de Hohenheim.

**v1.0 (Jour promis), en cours :**

- **Père**, l'Homonculus originel, sous Central : un sceau d'Ouroboros au fond du tunnel de Sloth ne
  cède qu'à qui a vu tomber Sloth, Wrath et Pride. Trois formes (le vieillard, la forme sans visage
  et son petit soleil, la forme divine instable qui dévore les âmes). Autour de lui, une **zone
  anti-alchimie** : aucune transmutation d'Amestris ne s'allume.
- L'**alkahestry de Xing** marche partout, même chez Père : cinq **kunaï** plantés autour d'une
  zone y dessinent un cercle de soin ou de piège. **May Chang** l'enseigne dans son pavillon des
  jungles et cerisaies (`/locate structure fmab:xing`).
- **Fort Briggs** dans les montagnes enneigées (`/locate structure fmab:fort_briggs`) : la générale
  **Olivier Armstrong** confie aux alliés de bon karma un **sabre de Briggs** (×1,5 contre les
  homonculus) ; les **soldats de Drachma** rôdent la nuit au pied du mur.
- **Fil conducteur** : un arbre de progrès « Fullmetal Alchemist », des cartes de tous les lieux chez
  les cartographes, et la quête des **points de sang** du cercle national (sept à sceller avant que
  l'éclipse ne devienne le Jour promis).
- **Lieux** : **Dublith** et la boucherie d'Izumi (elle donne la carte de l'île de Yock), **Liore** et
  Cornello, les ruines d'**Ishval** (Scar) et de **Xerxès** (fresques, Hohenheim), l'**île de Yock**,
  le dispensaire du docteur **Marcoh** (il déchiffre ses notes de recherche). Les villes sont
  **habitées** : des villageois dans les maisons de Central, Resembool, Dublith, Liore et dans les
  ateliers de Rush Valley (leur métier vient du poste de travail de la maison, cartographes
  compris), des maisons **meublées** (lits, table, fourneau, lanternes) et des gardes aux portes de
  Central. Aucun lieu ne se pose dans Central.
- **Créatures** : Barry le Boucher, armures habitées, soldats immortels, chimères, soldats
  d'Amestris et de Briggs. Les barres de boss n'apparaissent qu'une fois le combat engagé, aux
  joueurs présents.
- **Styles de jeu** : bras de Scar, mines de Kimblee, pistolet et fusil, couteaux de Hughes, épée de
  Xing ; le glyphe **Or** (interdit par la loi d'État) ; les écoles de la Destruction, de la
  Médecine et de la Vie (chimères qui vous obéissent).
- **Cercles** : déclencheurs (retardement, piège, redstone) et fusion à deux alchimistes. Le
  **carnet** n'est plus un objet : l'alchimiste le garde en tête (touche N, gardé à la mort). La
  **roue des cercles** (R maintenue) change de cercle d'un geste, et un clic y joint aussitôt les
  mains pour qui a vu la Porte ; accroupi, main vide, un clic droit sur un cercle inconnu le recopie
  dans le carnet. Un panneau en haut à gauche montre le cercle sélectionné et, dès qu'un péage est
  payé, une silhouette du corps (automails et leur usure, membres manquants). Le cercle de la
  transmutation humaine ne figure plus dans le Traité : de très
  rares **Notes sur la transmutation humaine** le donnent (Laboratoire 5, ruines de Xerxès,
  bibliothèques des forts, cités antiques).
- **Finition** : cinématiques (la Porte, le Jour promis, la chute de Père), éclipse dans le ciel,
  sons propres (CC0), silhouettes des boss, briques amestriennes et de Xing, neige de Briggs.

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

Configuration du serveur : `config/fmab.json`, créé au premier lancement.
`restart_wipes_progress` (par défaut `true`) : une âme qui choisit de repartir de zéro devant la
Vérité perd aussi toute sa progression d'alchimiste (rang, glyphes, maîtrise, épreuves, tatouages,
karma) ; à `false`, elle ne retrouve que son corps.

Commandes de test (opérateur) : `/fmab rank <rang>`, `/fmab learn_all`, `/fmab forget_all`,
`/fmab rest`, `/fmab mastery <école> <n>`, `/fmab grant <nœud>`.

Les cercles de test partagés se régénèrent depuis l'implémentation Java, qui fait référence :
`./gradlew test -PwriteFixtures=true`.

### Publier une version

Pousser un tag `vX.Y.Z` (ou `vX.Y.Z-beta`, publié en préversion) lance `.github/workflows/release.yml` :
jar à la version du tag, GitHub Release avec le changelog français et anglais (git-cliff,
`cliff.fr.toml` et `cliff.toml`), `CHANGELOG.fr.md` et `CHANGELOG.md` recommités sur `master`, puis
le site (pages Versions, Changelog et Installation, générées par `site/build.py`). Chaque commit
`feat:`, `fix:` ou `perf:` porte les lignes `Changelog-fr:` et `Changelog-en:` ; le check `commits`
des pull requests refuse ceux qui les oublient.

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

**v0.5 (Homunculi), in progress:** homunculi reconstitute while their Philosopher's Stone holds
souls; Lust and Gluttony (who swallows into his Belly) guard Laboratory 5; Envy roams in disguise,
then turns into a giant monster and a lizard; Greed and his Ultimate Shield run the Devil's Nest and
can be hired as an ally; the Philosopher's Stone gives back what the Gate took, for souls.

**v0.6 (Last homunculi), in progress:** Sloth charges through the rock of his tunnel under Central,
then rests; Wrath (King Bradley) dodges with his Ultimate Eye unless struck from behind or while
attacking; Pride's shadows only live with light, so total darkness or intense light leaves him a
vulnerable child.

**v0.7 (Stone), in progress:** the Philosopher's Stone amplifies transmutations and draws monsters,
and is made by sacrificing lives on a human transmutation circle; karma tracks your deeds; every
eighth day an eclipse lets a Gate initiate holding two full Stones become a Living Stone.

**v1.0 (Promised Day), in progress:** Father waits beneath Central behind an Ouroboros seal that
yields only to those who saw Sloth, Wrath and Pride fall; he fights in three forms and blocks all
Amestrian alchemy around him. Xing's alkahestry (five kunai planted in a circle, taught by May
Chang) still works there. Fort Briggs, Olivier Armstrong and her Briggs sabre hold the snowy north
against Drachma's soldiers.
An advancement tree, cartographer maps and the national circle's blood crests guide the way; Dublith
(Izumi), Liore, the Ishval and Xerxes ruins, Yock Island and Marcoh's clinic fill the map, and the
towns are lived in (villagers whose trade comes from their house's workstation, furnished houses,
guards at Central's gates); Barry, haunted armors, immortal soldiers and chimeras fill it with fights,
and boss bars only show once a fight has begun. New playstyles (Scar's arm,
Kimblee's mines, firearms, throwing knives, the Xing sword), the forbidden Gold glyph, the schools of
Destruction, Medicine and Life, circle triggers and two-alchemist fusion, cinematics, an eclipse in
the sky and the mod's own sounds round it out. The notebook is no longer an item: it opens with a key
(N) and survives death; the circle wheel (hold R) switches circles in one gesture and, for Gate
initiates, a click on it joins the hands at once; crouching empty-handed and right-clicking an unknown
circle copies it into the notebook. A panel at the top left shows the selected circle and, once a
toll is paid, a body silhouette (automail and its wear, missing limbs). The human transmutation
circle is no longer in the Treatise:
very rare Notes on Human Transmutation hand it out.

Build with JDK 25+ (`./gradlew build`); runtime needs Fabric Loader ≥ 0.19.3 and Fabric API.

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
