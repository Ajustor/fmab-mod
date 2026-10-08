# Brief Mod Minecraft — Fullmetal Alchemist: Brotherhood

Oct 8, 2026 · @Alexandre Gambier

## Vue d'ensemble de l'univers

FMA:B (anime de 2009-2010, 64 épisodes, fidèle au manga d'Hiromu Arakawa) se déroule à Amestris, une nation militaire d'inspiration européenne début XXe siècle, où l'alchimie est une science encadrée par l'État. Le mod doit retranscrire un principe central : tout a un prix, rien ne se crée gratuitement.

**Intrigue en bref.** Les frères Edward et Alphonse Elric tentent de ressusciter leur mère par transmutation humaine. Ed perd une jambe, Al tout son corps ; Ed sacrifie son bras pour fixer l'âme d'Al dans une armure. Devenu Alchimiste d'État, Ed cherche la Pierre philosophale pour réparer leurs corps. Ils découvrent que la pierre est faite de vies humaines et qu'un être, Père, et ses homonculus manipulent Amestris depuis sa fondation pour transformer tout le pays en un gigantesque cercle de transmutation.

**Piliers à retranscrire dans le jeu :**

- Échange équivalent : chaque transmutation consomme de la matière de valeur égale.
- Coût et risque : l'alchimie interdite (humaine, pierre) donne un pouvoir énorme mais punit le joueur.
- Progression par le savoir : on apprend des cercles, on ne les fabrique pas comme des outils ordinaires.
- Ennemis immortels mais pas invincibles : les homonculus se régénèrent jusqu'à épuisement de leur pierre.
- Ton : aventure, militaire, enquête, avec un fond sombre (génocide d'Ishval, chimères humaines).

## Règles de l'alchimie

L'alchimie d'Amestris suit trois étapes et une loi absolue ; le mod doit en faire ses règles de calcul.

**Les trois étapes** (à traduire en trois phases d'interaction) :

1. Compréhension : connaître la structure de la matière (dans le jeu : avoir appris la recette ou le « savoir » du matériau).
2. Décomposition : briser la matière en éléments (consommer les blocs ou items source).
3. Recomposition : la reconstruire sous une nouvelle forme (produire le résultat).

**L'échange équivalent.** On ne peut obtenir qu'une masse et une nature compatibles avec ce qu'on donne : de la pierre donne de la pierre, du fer donne du fer. Transmuter de l'or est interdit par la loi d'État (pas impossible). Seule la Pierre philosophale permet de contourner cette loi.

**Le cercle de transmutation.** Il canalise l'énergie (qui vient en réalité des âmes de Xerxès sous Amestris, cf. Père). Un cercle a une géométrie liée à l'effet : triangles, carrés, symboles élémentaires. Les alchimistes avancés gravent leur cercle sur un gant ou un tatouage.

**Transmutation sans cercle.** Seuls ceux qui ont vu la Porte de la Vérité (Ed, Al, Izumi, Mustang plus tard) font un cercle avec leurs mains en joignant les paumes. C'est l'objectif de progression naturel du mod.

**Tabous.** Transmutation humaine et fabrication d'or sont interdites. Les chimères (fusion d'êtres vivants) sont tolérées par l'armée mais moralement condamnées (Shou Tucker).

## Pierre philosophale et Porte de la Vérité

Ces deux éléments forment le « endgame » du mod : la Pierre est la ressource de puissance ultime, la Porte est la dimension où l'on paie le prix.

**La Pierre philosophale.** Pierre rouge, solide ou liquide, faite d'âmes humaines condensées. Elle amplifie l'alchimie, ignore l'échange équivalent et sert de réserve de vie aux homonculus. Elle s'épuise à l'usage. Les ébauches impures s'appellent « pierres rouges » (épisode de Liore). Fabrication : un cercle géant activé lors d'un massacre (Ishval, Xerxès).

Traduction gameplay : un item à durabilité (« âmes restantes »), fabriqué par un rituel coûteux impliquant des villageois ou mobs ; il accorde des transmutations sans cercle ni coût matériel, mais inflige un malus de réputation ou de « karma ».

**Devenir une Pierre philosophale vivante.** Comme Van Hohenheim, un joueur peut, par un rituel ultime, fusionner les âmes d'une pierre avec son propre corps. Effets : plus aucun péage à la Porte, membres et organes perdus régénérés, transmutation sans cercle et sans coût matériel. Contreparties : un compteur d'âmes visible qui baisse à chaque transmutation majeure et à chaque mort (le joueur ressuscite sur place tant qu'il reste des âmes) ; à zéro, le joueur meurt et perd ce statut. Le karma tombe au minimum, et Père et les homonculus le traquent comme une ressource à absorber.

Rituel proposé : cercle de rang maximal, une Pierre philosophale pleine dans chaque main, Porte déjà ouverte, réalisé pendant une éclipse (nouvel événement céleste du mod, rappel du Jour promis).

**La Porte de la Vérité.** Chaque être possède une Porte dans un espace blanc infini, gardée par la Vérité, une silhouette blanche sans visage. Derrière : un savoir alchimique absolu, des yeux et des bras noirs qui happent le visiteur. On y accède par transmutation humaine. La Vérité prend un « péage » proportionnel à ce qui a été tenté :

| Personnage | Péage |
| --- | --- |
| Edward | Jambe gauche, puis bras droit |
| Alphonse | Corps entier |
| Izumi Curtis | Organes internes |
| Roy Mustang | La vue (passage forcé) |
| Edward (finale) | Sa propre Porte, donc son alchimie |

**Transmutation humaine.** Échoue toujours : le résultat est une créature difforme. En échange, le survivant gagne la transmutation sans cercle.

**Règle du mod : toute ouverture de la Porte a un prix.** Le joueur atteint la dimension « Espace blanc » par transmutation humaine. La Vérité (PNJ non combattable) prélève un péage, puis le joueur gagne définitivement la transmutation sans cercle : il joint les mains (touche dédiée) et transmute directement. Chaque nouvelle ouverture coûte un nouveau péage.

| Péage | Effet en jeu | Réparation |
| --- | --- | --- |
| Bras gauche ou droit | Main désactivée : pas d'objet, pas d'attaque de ce côté | Automail de bras |
| Jambe gauche ou droite | Vitesse −30 %, pas de sprint, saut réduit | Automail de jambe |
| Organes internes | PV max −4 cœurs, toux aléatoire (dégâts légers) | Aucune, sauf Pierre philosophale |
| Vue | Champ de vision assombri, rayon de rendu réduit | Pierre philosophale (comme Mustang) |
| Corps entier (échec grave) | Âme fixée dans une armure (voir Armure d'Al) | Quête finale de la Porte |

Le péage dépend de ce qui a été tenté : plus la transmutation est ambitieuse (nombre de mobs ou PNJ visés, matériaux humains), plus le prix est lourd. L'automail est donc indispensable à la progression : sans lui, un joueur amputé reste handicapé.

## Les homonculus et Père

Les sept homonculus sont nés des péchés que Père a expulsés de lui-même ; chacun porte l'Ouroboros tatoué et un noyau de Pierre philosophale. Mécanique commune : régénération à chaque mort tant que la pierre contient des âmes (un compteur de vies caché, à vider).

| Homonculus | Pouvoir | Faiblesse | Boss Minecraft |
| --- | --- | --- | --- |
| Lust (Luxure) | Doigts-lames extensibles à longue portée | Feu de Mustang, régénération épuisable | Attaques en ligne droite qui percent les boucliers |
| Gluttony (Gourmandise) | Dévore tout ; faux Œil de la Vérité dans le ventre | Lent, peu intelligent | Aspire les blocs et items, téléporte le joueur dans une mini-dimension sombre |
| Envy (Envie) | Métamorphose ; vraie forme : petit lézard vert | Forme réelle très fragile | Copie l'apparence d'un joueur ou PNJ, phase 2 en monstre géant fait de visages |
| Greed (Avarice) | Bouclier ultime (carbone durci sur la peau) | Le bouclier doit se retirer pour attaquer | Invulnérable sauf après ses attaques ; peut devenir allié (Ling) |
| Sloth (Paresse) | Force et vitesse extrêmes | Paresseux, s'arrête souvent | Charges ultra rapides avec temps de pause |
| Wrath (Colère), King Bradley | Œil ultime : voit les mouvements à l'avance ; maître sabreur | Vieillit, ne se régénère pas | Duel au sabre, esquive les flèches, pas de régénération |
| Pride (Orgueil), Selim Bradley | Ombres tranchantes et dévorantes | Inutile sans lumière, vulnérable en vrai corps (embryon) | Boss dans le noir : les ombres ne vivent qu'avec une source de lumière ; une lumière intense ou le noir total le neutralise |

**Père** (le « Nain dans la fiole », Homonculus originel). Né du sang de Van Hohenheim à Xerxès, il a sacrifié tout le pays pour obtenir un corps, puis fondé Amestris. Son but : absorber Dieu (la Vérité) lors de « Jour promis » (éclipse) via un cercle national. Il peut bloquer l'alchimie amestrienne dans un rayon (son réseau de pierres capte l'énergie tectonique) ; l'alkahestry de Xing, elle, continue de marcher.

Traduction gameplay : boss final en trois phases (vieillard, forme sans visage avec mini-soleil, forme divine instable). Gimmick : zone « anti-alchimie » où seules les techniques d'alkahestry ou les armes classiques fonctionnent.

**Mobs mineurs liés :** chimères (Tucker et militaires), soldats immortels (corps sans âme animés par du sang), armures habitées (Barry le Boucher, Slicer frères).

## Personnages et alchimistes d'État

Chaque alchimiste a une spécialité nette : c'est la base idéale pour des « écoles » de transmutation débloquables par le joueur.

| Personnage | Titre / rôle | Spécialité | Capacité de mod |
| --- | --- | --- | --- |
| Edward Elric | Fullmetal | Transmutation de la terre et du métal, armes improvisées | Lame sur l'automail, murs et piques de pierre |
| Alphonse Elric | Âme dans une armure | Polyvalent, sans cercle | Mode armure : pas besoin de manger ni de dormir, mais pas de toucher |
| Roy Mustang | Flame | Gant d'ignition + contrôle de l'oxygène | Explosions de feu à distance, inutile sous la pluie |
| Riza Hawkeye | Lieutenant, tireuse d'élite | Armes à feu | Fusils et pistolets militaires |
| Alex Louis Armstrong | Strong Arm | Coups de poing qui transmutent la pierre en projectiles | Gantelets cloutés, projectiles de pierre |
| Olivier Armstrong | Générale de Briggs | Escrime, défense | Sabre de Briggs |
| Izumi Curtis | Professeure des Elric | Alchimie de combat sans cercle | PNJ maître d'entraînement |
| Van Hohenheim | Pierre philosophale vivante | Immense, sans cercle | PNJ de quête finale |
| Scar | Ishvalien, bras tatoué | Destruction (décomposition seule) puis reconstruction | Bras droit qui désintègre les blocs |
| Zolf J. Kimblee | Crimson | Transmute la matière en explosifs | Cercles sur les paumes, mines alchimiques |
| Basque Grand | Iron Blood | Armes à feu transmutées | Canons générés depuis le sol |
| Isaac McDougal | Freezing | Eau et glace | Murs et pics de glace |
| Shou Tucker | Sewing-Life | Chimères | Laboratoire de chimères (contenu sombre, optionnel) |
| Ling Yao / Greed | Prince de Xing | Sabre, perception du qi | Détection des mobs à travers les murs |
| Lan Fan | Garde de Ling | Ninja, kunaï | Mouvements rapides, automail |
| May Chang | Alkahestrist | Alkahestry à distance par kunaï | Soins, transmutation à distance |
| Winry Rockbell | Mécanicienne | Automail | PNJ forgeronne qui fabrique et répare l'automail |
| Maes Hughes | Enquêteur | Couteaux de lancer | Couteaux de lancer en item |

## Lieux, nations et autres arts

Amestris est un pays circulaire entouré de voisins hostiles ; sa forme même est le cercle de transmutation de Père. Les lieux se prêtent à des structures générées.

| Lieu | Description | Structure / biome de mod |
| --- | --- | --- |
| Central City | Capitale, QG militaire, laboratoires | Ville structure avec Quartier général et bibliothèque |
| Souterrains de Central | Tunnels, laboratoire de Père | Donjon final, salle du trône aux tuyaux |
| Resembool | Village rural des Elric et Rockbell | Village de départ, atelier d'automail |
| Rush Valley | Ville des mécaniciens d'automail | Village marchand d'automail |
| Dublith | Boucherie d'Izumi, île de Yock | Lieu d'entraînement (survie sur une île) |
| Liore | Ville du faux prophète Cornello | Ville désertique, pierre rouge impure |
| Ishval | Région désertique, ruines du génocide | Biome désertique avec ruines |
| Fort Briggs | Forteresse de montagne enneigée au nord | Forteresse géante dans les montagnes enneigées |
| Laboratoire 5 | Lieu de fabrication des pierres | Donjon intermédiaire, armures gardiennes |
| Xerxès | Ruines d'un empire disparu | Ruines dans le désert, fresques de lore |
| Xing | Empire oriental, 50 clans | Biome asiatique, PNJ alkahestry |
| Drachma | Empire du Nord | Mobs soldats ennemis près de Briggs |

**L'alkahestry (Xing).** Utilise le « flux du dragon », l'énergie qui circule dans la terre, plutôt que l'énergie tectonique d'Amestris. Spécialisée dans la médecine, elle peut agir à distance via des kunaï plantés en cercle. Elle fonctionne dans la zone anti-alchimie de Père.

**Ishval.** Peuple religieux pour qui l'alchimie est un sacrilège. Le génocide (Guerre d'extermination d'Ishval) sert de cercle de sang pour Père. Le tatouage de Scar est un savoir combiné des deux arts.

**Cercles de sang et Jour promis.** Chaque massacre historique d'Amestris marque un point du cercle national. Idée de quête : retrouver ces points sur une carte et les désactiver.

## Armes, objets et automail

Les armes se divisent en armes conventionnelles (militaires), armes alchimiques (créées ou alimentées par transmutation) et automail.

| Item | Type | Effet proposé |
| --- | --- | --- |
| Lame d'automail (Ed) | Alchimique | Bras automail transformé en lame, dégâts de type épée |
| Lance de pierre / piques | Alchimique, temporaire | Arme créée depuis le sol, se dégrade vite |
| Gant de Mustang | Alchimique | Clic pour déclencher une explosion de feu à distance ; désactivé s'il est mouillé |
| Gantelets d'Armstrong | Alchimique | Coup de poing qui projette des blocs de pierre |
| Bras de Scar | Alchimique | Désintègre les blocs ou les mobs touchés |
| Cercles de Kimblee | Alchimique | Transforme un bloc en explosif |
| Kunaï d'alkahestry | Alkahestry | Lancer 5 kunaï forme un cercle : soin ou piège à distance |
| Sabre militaire / de Briggs | Conventionnel | Épée lourde, bonus contre les homonculus |
| Épée de Xing (Ling) | Conventionnel | Épée rapide, combo |
| Fusils et pistolets amestriens | Conventionnel | Projectiles, munitions craftables |
| Couteaux de lancer (Hughes) | Conventionnel | Projectile rapide, faible coût |
| Pierre philosophale | Endgame | Amplifie toute transmutation, durabilité en âmes |
| Montre d'Alchimiste d'État | Accessoire | Débloque les recettes de rang « État », bonus de puissance |
| Craie de transmutation | Outil | Dessiner un cercle au sol |
| Livres et notes de recherche | Savoir | Débloquent des recettes de cercle (chiffrées, à décoder comme Marcoh) |

**Automail.** Prothèses mécaniques fixées sur des ports nerveux. Proposition : emplacements d'équipement dédiés (bras gauche, bras droit, jambe gauche, jambe droite) qui remplacent un membre perdu à la Porte. Matériaux : fer standard, acier léger « de Rush Valley » (vitesse), modèle d'hiver « de Briggs » (résiste au froid). Elles s'usent, se réparent chez Winry et cassent si on les néglige.

**Armure d'Al.** Item spécial : l'âme du joueur liée à une armure par un sceau de sang. Avantages : pas de faim, pas de noyade ; inconvénients : pas de soin par la nourriture, mort si le sceau est effacé.

## Systèmes de jeu

Cinq systèmes forment le cœur du mod ; tout le reste (items, boss) s'y branche.

**1. Cercles de transmutation créés par les joueurs.** Pas de liste fermée : le joueur compose son cercle à partir de glyphes répartis en couches, et le jeu lit le dessin pour en déduire l'effet. Pour permettre l'alchimie complexe, un cercle n'est pas plat : il s'organise en étages concentriques reliés entre eux et peut porter des cercles satellites.

| Couche | Rôle | Exemples |
| --- | --- | --- |
| Anneaux concentriques (étages) | Chaque anneau est un étage ; lus du centre vers l'extérieur, la sortie d'un étage alimente le suivant | 1 étage (Apprenti) à 5 étages (Initié de la Porte) |
| Polygone par étage | Puissance et stabilité de l'étage | Triangle à dodécagone ; hexagramme (deux triangles) pour fusionner deux éléments |
| Glyphes d'élément | Matière visée ; deux éléments dans un même étage se combinent | Terre + Métal = alliage, Feu + Air = combustion amplifiée, Eau + Air = brume ou glace |
| Glyphe d'action | Effet de l'étage | Mur, pique, lame, réparer, fondre, décomposer, recomposer, projeter, lier |
| Modificateurs | Forme et paramètres | Forme (ligne, cône, sphère, dôme), direction, durée, intensité, nombre de cibles |
| Déclencheurs | Moment d'activation | Contact de la paume, claquement des mains, étincelle, pression (piège), délai, signal redstone, entité qui entre |
| Liaisons | Relient étages et satellites, définissent le flux | Trait simple = en série, trait double = en parallèle, trait brisé = conditionnel |
| Cercles satellites | Mini-cercles complets sur les sommets du polygone, effets ajoutés | Satellite Feu et satellite Eau autour d'un cercle Terre |
| Sceaux (interdits) | Âme, sang, vie | Liaison d'âme dans une armure, transmutation humaine, Pierre |

**Règles de calcul.** Chaque glyphe a un score de complexité ; le total doit rester sous le plafond du rang. La stabilité dépend du rapport entre côtés du polygone et complexité, et la symétrie (miroir ou rotation) la renforce. Coût = complexité × portée × nombre d'étages, payé en concentration et en matière. Un cercle instable provoque un rebond proportionnel : de quelques dégâts à une explosion qui détruit le support.

**Exemple d'alchimie complexe.** Étage 1 (centre) : Terre + Décomposer, rayon 5 blocs. Étage 2 : Métal + Recomposer en Lame, relié en série. Satellite Feu en cône sur un sommet. Résultat : une forêt de lames chauffées au rouge jaillit du sol.

**Jeu de glyphes de départ.** 13 glyphes, tous issus du symbolisme alchimique historique ou de géométrie originale ; les 10 marqués v0.1 forment le socle. Coordonnées dans un carré unité de −1 à 1, axe y vers le bas. Plafond de complexité par rang : Apprenti 6, Alchimiste 14, Alchimiste d'État 25, Initié de la Porte 40.

| Glyphe | Couche | Tracé | Origine | Complexité | Concentration | Rang | Effet | Version |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Terre | Élément | Triangle pointe en bas, barre horizontale à y = 0,2 qui dépasse des côtés | Élément terre historique | 1 | 2 | Apprenti | Vise pierre, terre, sable, gravier | v0.1 |
| Eau | Élément | Triangle pointe en bas, sans barre | Élément eau historique | 1 | 2 | Apprenti | Vise eau, glace, neige | v0.1 |
| Feu | Élément | Triangle pointe en haut, sans barre | Élément feu historique | 3 | 5 | Alchimiste | Chaleur, combustion, fonte | v0.2 |
| Air | Élément | Triangle pointe en haut, barre horizontale à y = −0,2 | Élément air historique | 3 | 5 | Alchimiste | Souffle, oxygène, poussée | v0.2 |
| Fer | Élément | Cercle r = 0,5 + flèche vers le haut à droite partant du bord | Symbole de Mars (♂) | 2 | 3 | Apprenti | Vise fer, acier, armes et outils en fer | v0.1 |
| Cuivre | Élément | Cercle r = 0,45 en haut + croix sous le cercle | Symbole de Vénus (♀) | 2 | 3 | Apprenti | Vise cuivre ; conduit la redstone | v0.1 |
| Or | Élément | Cercle r = 0,7 + point central | Symbole du Soleil (☉) | 8 | 12 | Alchimiste d'État | Vise l'or ; transmutation interdite par la loi d'État : karma −10 et soldats hostiles | v0.3 |
| Fixer (Mur) | Action | Cercle r = 0,7 coupé par un diamètre horizontal | Symbole du sel | 1 | 3 | Apprenti | Fait monter un mur ou une plateforme | v0.1 |
| Projeter (Pique, Lame) | Action | Triangle pointe en haut posé sur une croix | Symbole du soufre | 2 | 4 | Apprenti | Fait jaillir une pique ; lame si combiné à Fer | v0.1 |
| Réparer | Action | Cercle presque fermé dont une extrémité s'élargit en tête, l'autre pénètre dedans | Ouroboros | 2 | 4 | Apprenti | Restaure la durabilité d'un objet ou d'un bloc cassé | v0.1 |
| Décomposer | Action | Croix dont les quatre branches se terminent par un petit cercle | Création originale | 2 | 4 | Apprenti | Brise les blocs visés en ressources | v0.1 |
| Direction | Modificateur | Petite flèche tangente placée sur l'anneau | Création originale | 1 | 1 | Apprenti | Oriente l'effet vers la position de la flèche | v0.1 |
| Intensité | Modificateur | 1 à 3 points alignés sur l'anneau | Création originale | 1 par point | 2 par point | Apprenti | +50 % de portée ou de force par point | v0.1 |

**Combinaisons de départ.** Terre + Fixer = mur de pierre. Terre + Projeter = pique de pierre. Fer + Projeter = lame de fer (consomme du fer). Eau + Fixer = plateforme de glace. Terre + Décomposer = minage de zone. Fer + Réparer = réparation d'outil. Une combinaison absente de la table produit un rebond.

**Format JSON d'un glyphe** (fichier `data/fmab/glyph/terre.json`, partagé avec l'éditeur web) :

```json
{
  "id": "fmab:terre",
  "layer": "element",
  "element": "earth",
  "primitives": [
    { "type": "polygon", "points": [[-0.8, -0.6], [0.8, -0.6], [0.0, 0.8]] },
    { "type": "line", "from": [-0.55, 0.2], "to": [0.55, 0.2] }
  ],
  "complexity": 1,
  "concentration": 2,
  "rank": "apprentice",
  "tolerance": { "rotation_deg": 15, "position": 0.1, "scale": [0.7, 1.4] },
  "name_key": "glyph.fmab.terre",
  "description_key": "glyph.fmab.terre.desc"
}
```

Les noms et descriptions passent par les clés de traduction (fr\_fr et en\_us). Le parseur compare chaque primitive dessinée aux définitions dans la tolérance indiquée ; un glyphe à symétrie (Eau, Feu, Fixer) n'est testé que sur les rotations utiles.

**Dessin et enregistrement.** Le joueur dessine dans le Carnet de cercles (grille 32×32, outils compas, polygone, symétrie) ou importe un tracé conçu dans l'éditeur web. Le parseur identifie les glyphes avec une tolérance (rotation, léger décalage) et décrit l'effet avant validation. Les joueurs ayant ouvert la Porte lancent directement un cercle du carnet, sans le tracer.

**Supports d'inscription.** Un cercle enregistré peut être posé sur cinq types de support ; le support fixe la taille maximale et les déclencheurs possibles.

| Support | Inscription | Comportement | Limites |
| --- | --- | --- | --- |
| Surfaces (sol, murs, plafonds, tout bloc) | Craie (temporaire), peinture alchimique (durable), gravure au burin (permanente, pierre et métal) | Cercle fixe, idéal pour les grands cercles et les pièges | Craie effacée par la pluie et le passage |
| Gants (nouvel emplacement d'équipement) | Brodés ou gravés à la table d'alchimiste | Un cercle par gant ; joindre les mains combine les deux | Usure ; main libre requise |
| Vêtements | Brodés sur manteau, veste, brassards | Cercles passifs (renfort, résistance au feu) ou à activation | Taille limitée par la pièce |
| Corps (tatouages) | PNJ tatoueur ou rituel ; paumes, avant-bras, dos, torse | Permanent, conservé à la mort, impossible à voler | Retrait coûteux ; emplacements limités |
| Armes, outils, automail | Gravure | Effet au coup ou à l'usage (lame d'automail) | Consomme la durabilité |

**Gants proposés.** Gants de tissu (1 étage), gants de cuir (2 étages), gants ignifugés avec silex intégré (déclencheur étincelle, style Mustang), gantelets métalliques (cercles de frappe, style Armstrong), gants d'Alchimiste d'État (5 étages, rang État requis). Craftables et teintables.

**Le Traité d'alchimie (livre en jeu).** Livre obtenu dès le départ à Resembool, qui se complète au fil du savoir appris. Chapitres : 1) principes et échange équivalent, 2) les trois étapes, 3) catalogue des glyphes (débloqués un à un), 4) règles de composition et calcul du coût, 5) exemples de cercles à recopier, 6) erreurs et rebonds, 7) tabous (transmutation humaine, Pierre), avec avertissements. Les pages sont data-driven et traduites en français et en anglais.

**2. Valeur d'échange (EMC-like mais strict).** Chaque item a une masse et une famille (minéral, métal, organique, cristal). Une transmutation ne change jamais de famille sans Pierre philosophale, et la masse produite est toujours inférieure ou égale à la masse donnée. Ce point distingue le mod d'un simple clone d'Equivalent Exchange.

**3. Énergie alchimique et savoir.** Une barre de « concentration » (mana) se recharge au repos. Le savoir est un arbre de compétences par école : Terre, Métal, Feu (oxygène), Eau/Glace, Explosion, Destruction, Médecine (alkahestry), Vie (chimères, interdit). On débloque les nœuds via livres, PNJ maîtres et l'examen d'Alchimiste d'État.

**4. Progression en rangs.**

1. Apprenti : anneau simple, triangle et carré, glyphes Terre et Métal.
2. Alchimiste : polygones jusqu'à l'hexagone, armes temporaires, murs.
3. Alchimiste d'État (examen = épreuve en arène) : montre, anneau triple, cercles gravés sur gants.
4. Initié de la Porte (après transmutation humaine) : transmutation sans cercle à partir du carnet, péage payé, automail nécessaire.
5. Pierre philosophale vivante : plus de péage, plus de coût matériel, mais compteur d'âmes et traque par les homonculus.

**5. Karma et conséquences.** Une jauge morale : fabriquer des pierres ou des chimères la baisse. Karma bas = PNJ hostiles, apparitions d'homonculus « recruteurs ». Karma haut = alliés de Briggs, accès à Xing.

**Bonus : multijoueur.** Un joueur peut retrouver son membre perdu en le récupérant à la Porte (quête de fin) ; deux joueurs peuvent fusionner leurs cercles pour un effet plus grand.

## Éditeur de cercles web

Un site gratuit, à côté du mod, pour concevoir ses tracés hors du jeu avec la fiche de chaque symbole, puis les importer en jeu par un code.

**Fonctions.**

- Canevas : même grille 32×32 que le Carnet, outils compas, polygone, ligne, symétrie, un calque par étage (masquer, verrouiller).
- Palette de glyphes filtrée par couche et par rang ; fiche au survol : nom, couche, effet, complexité, coût, rang requis, combinaisons connues.
- Analyse en direct : étages et satellites détectés, description de l'effet, jauges de complexité, stabilité et coût, aperçu de la zone d'effet.
- Erreurs signalées sur le dessin : glyphe inconnu, liaison orpheline, rang insuffisant, support trop petit.
- Choix du support (sol, gant, vêtement, tatouage, arme) : taille maximale et déclencheurs disponibles s'ajustent.
- Export : code texte (JSON compressé en base64) à coller dans le Carnet via « Importer », fichier JSON, image PNG, lien de partage. Import d'un code copié depuis le jeu pour retoucher un tracé.
- Interface en français et en anglais, avec les mêmes fichiers de langue que le mod.

**Règles de cohérence avec le mod.**

- Les glyphes viennent des mêmes JSON que le mod, publiés à chaque version ; l'éditeur affiche la version ciblée.
- Le parseur existe en Java (mod) et en JavaScript (web) ; un jeu de cercles de test commun, lancé dans les deux CI, garantit des résultats identiques. Variante : compiler le parseur Java en JavaScript avec TeaVM pour n'avoir qu'un seul code.
- Le serveur reste juge : à l'import, un glyphe que le joueur n'a pas encore appris est refusé. L'éditeur aide à concevoir, il ne donne aucun avantage.
- Hébergement retenu : GitHub Pages, en site statique sans compte ni serveur. Déploiement automatique par GitHub Actions à chaque tag de version du mod (voir Release au tag), avec les JSON de glyphes et les fichiers de langue copiés depuis le dépôt du mod.

## Site GitHub Pages du mod

Le site GitHub Pages regroupe la présentation du mod, les releases, les changelogs et l'éditeur de cercles, en français et en anglais, avec un sélecteur de langue sur chaque page.

| Page | Contenu | Mise à jour |
| --- | --- | --- |
| Accueil | Présentation, captures, fonctionnalités, avertissement « fan-made », lien vers la dernière version | Manuelle (textes fr et en) |
| Releases | Liste des versions : numéro, date, version de Minecraft (26.2), loader (Fabric), dépendances (Fabric API, Java 25), lien de téléchargement du jar, badge stable ou pre-release | Automatique à chaque tag |
| Changelog | Historique complet par version, regroupé en Nouveautés, Corrections, Performances | Automatique à chaque tag |
| Installation | Étapes Fabric Loader + Fabric API + jar, problèmes fréquents | Manuelle |
| Éditeur de cercles | L'outil web décrit plus haut | À chaque tag |

**Structure.** Les pages sont générées depuis le dépôt : `site/fr/` et `site/en/`, la langue par défaut suivant celle du navigateur. La page Releases est construite pendant le workflow à partir des données de la GitHub Release qui vient d'être créée (pas d'appel à l'API GitHub depuis le navigateur). Tout est publié dans le même job que la Release, à partir du même tag.

## Catalogue de contenu

Première estimation : environ 75 items, 20 blocs, 15 mobs, 9 boss et 10 structures pour une version complète.

**Blocs.** Cercle de transmutation (bloc-entité multi-tailles), table d'alchimiste, établi d'automail, minerai de « pierre rouge » (impur, Liore), bloc de sang cristallisé, tuyaux de Père (décor), sol de l'Espace blanc, Porte de la Vérité (bloc géant multi-structure), bibliothèque de recherche, briques amestriennes, briques de Xing, neige compacte de Briggs.

**Items d'alchimie et d'inscription.**

| Item | Rôle |
| --- | --- |
| Carnet de cercles | Dessiner, enregistrer, importer et lancer ses cercles |
| Traité d'alchimie | Livre explicatif, chapitres débloqués avec le savoir |
| Craie de transmutation | Tracé temporaire sur toute surface |
| Peinture alchimique | Tracé durable sur toute surface |
| Burin d'alchimiste | Gravure permanente sur pierre, métal, armes et automail |
| Gants de tissu | 1 étage |
| Gants de cuir | 2 étages |
| Gants ignifugés à silex | Déclencheur étincelle (style Mustang) |
| Gantelets métalliques | Cercles de frappe (style Armstrong) |
| Gants d'Alchimiste d'État | 5 étages, rang État requis |
| Fil alchimique | Broderie de cercles sur gants et vêtements |
| Automail de bras et de jambe (gauche, droite) | Remplace un membre perdu à la Porte ; variantes fer, Rush Valley, Briggs |
| Montre d'Alchimiste d'État | Rang État, bonus de puissance |
| Pierre philosophale | Endgame, durabilité en âmes |

**Mobs.**

| Mob | Comportement |
| --- | --- |
| Soldat amestrien | Neutre ; hostile si karma bas |
| Soldat de Drachma | Hostile près de Briggs |
| Chimère (plusieurs variantes) | Hostile, fusion d'animaux vanilla |
| Soldat immortel | Hostile, régénération, apparaît au Laboratoire 5 et sous Central |
| Armure habitée | Hostile, tombe en morceaux, renaît si le sceau reste |
| Bras noirs de la Porte | Piège dans l'Espace blanc |
| PNJ Izumi, Winry, Marcoh, May Chang, tatoueur alchimiste | Marchands et maîtres de quêtes |

**Boss** (ordre de progression suggéré) : Barry le Boucher, Lust, Gluttony, Greed, Envy, Sloth, Pride, Wrath (King Bradley), Père.

**Structures.** Resembool, Central City, Laboratoire 5, Fort Briggs, ruines de Xerxès, ruines d'Ishval, temple de Liore, île de Yock, Rush Valley, laboratoire de Père (non généré, accès par quête).

**Dimensions.** L'Espace blanc (Porte de la Vérité) et le Ventre de Gluttony (vide sombre et rempli de sang).

## Aspects techniques Minecraft 26.2

Choix retenu : un mod Fabric uniquement, d'identifiant « fmab » (Fabric Loader + Fabric API), en Java 25, ciblant Java Edition 26.2 « Chaos Cubed » (sortie le 16 juin 2026).

| Élément | Version minimale | Note |
| --- | --- | --- |
| Java (JDK) | 25 | Requis depuis Minecraft 26.1 |
| Fabric Loader | 0.19.3 | Loader unique du mod |
| Fabric API | 0.152.0 | Dépendance obligatoire (réseau, rendu, événements, attachements de données) |
| Fabric Loom | Variante no-remap | Le jeu n'est plus obfusqué depuis 26.1 |
| Data pack / resource pack | 107.1 / 88.0 | Formats de la 26.2 |

Sources : [docs Architectury 26.2](https://docs.architectury.dev/api/getting-started/setup), [notes techniques 26.2 (Teramont)](https://teramont.net/blog/minecraft-26-2-notes-technical-changes-recommended-hosting).

**Architecture du code proposée.**

- `registry/` : registres (items, blocs, entités, types de recette), via les helpers de Fabric API.
- `alchemy/`, `karma/`, `knowledge/` : logique de transmutation, karma et savoir, côté serveur.
- `client/` : point d'entrée client (`ClientModInitializer`), rendus, HUD de concentration, shaders.
- Données joueur (rang, savoir, membres perdus, karma) : Data Attachments de Fabric API, synchronisés par paquets réseau Fabric.
- Entrées déclarées dans `fabric.mod.json` (main, client, datagen) ; génération des JSON via Fabric Data Generation.

**Tout ce qui peut être data-driven doit l'être.** Recettes de transmutation sous forme de type de recette JSON personnalisé (motif du cercle, entrées, sortie, coût, rang requis) : ajouter du contenu ne demande alors pas de recompiler, et les packs de la communauté peuvent étendre le mod. Idem pour la valeur d'échange (tags de famille + masse en JSON), les structures (Jigsaw) et la dimension Espace blanc (dimension type + noise settings en JSON).

**Cercles personnalisés.** Chaque glyphe est un fichier JSON (motif sur la grille, élément ou action, coût, rang requis). Le cercle dessiné est stocké comme une matrice dans le Carnet (composant d'item) et dans le bloc-entité au sol ; le parseur le découpe en couches, identifie les glyphes avec une tolérance (rotation, léger décalage) et assemble l'effet. Les cercles enregistrés sont synchronisés via Data Attachments pour la transmutation sans cercle.

**Supports et rendu.** Emplacement gants : vérifier si Trinkets ou Accessories existe pour Fabric 26.2, sinon un emplacement maison via Data Attachments et un onglet d'inventaire. Le rendu repose sur une texture dynamique générée depuis la matrice du cercle. Elle s'applique en décalcomanie sur les faces de blocs (bloc-entité), en couche de rendu sur le modèle du joueur (tatouages, gants, vêtements) et sur les items gravés. Les tatouages et vêtements brodés sont stockés dans les Data Attachments du joueur, les gants et armes dans un composant d'item.

**Livre (Traité d'alchimie).** Patchouli ou un équivalent n'est pas garanti sur Fabric 26.2 : à vérifier au démarrage, sinon prévoir un écran de livre maison qui lit des pages JSON. Pages débloquées par avancements (advancements) liés au savoir.

**Localisation français et anglais dès la v0.1.** Aucun texte en dur : tout passe par des clés de traduction dans `assets/fmab/lang/fr_fr.json` et `en_us.json` (items, blocs, mobs, messages, interface du carnet, pages du Traité, dialogues de la Vérité et des PNJ). Terminologie fixée dans un glossaire : Luxure / Lust, Gourmandise / Gluttony, Envie / Envy, Avarice / Greed, Paresse / Sloth, Colère / Wrath, Orgueil / Pride, Pierre philosophale / Philosopher's Stone, Porte de la Vérité / Gate of Truth, automail / automail. La langue suit le réglage du client ; un test CI peut vérifier que les deux fichiers ont les mêmes clés.

**Release au tag.** Chaque tag de version poussé sur GitHub (format `v1.0.0`, avec un suffixe `-alpha` ou `-beta` pour les préversions) déclenche un workflow GitHub Actions :

1. Installation du JDK 25 et du cache Gradle.
2. Build du mod (`./gradlew build`) et lancement des tests, dont la vérification des clés fr\_fr et en\_us et les cercles de test du parseur.
3. Création d'une GitHub Release au nom du tag, avec le jar du mod en pièce jointe (`fmab-<version>+26.2.jar`) et le changelog de la version en description (voir Changelog automatique).
4. Publication de l'éditeur web sur GitHub Pages à partir du même tag, pour que le site et le mod restent sur la même version.

La version du jar est lue depuis le tag, pas écrite à la main dans `gradle.properties`. Un tag avec un suffixe crée une Release marquée « pre-release ». Les builds sans tag restent de simples artefacts de CI.

**Changelog automatique.** Le changelog est généré à chaque tag, sans rédaction manuelle, à partir de l'historique Git.

- Convention de commits : Conventional Commits (`feat:`, `fix:`, `perf:`, `refactor:`, `docs:`, `chore:`), avec une portée facultative par système, par exemple `feat(cercles): ajout des satellites` ou `fix(porte): péage de jambe`.
- Outil : git-cliff, configuré par un fichier `cliff.toml` dans le dépôt. Il regroupe les commits par catégorie (Nouveautés, Corrections, Performances, Divers) et ignore `chore:` et `docs:` dans la version joueur.
- Sorties : la section de la version dans la description de la GitHub Release, et le fichier `CHANGELOG.md` complet mis à jour et recommité sur la branche principale par le workflow.
- Site : les pages Changelog française et anglaise sont régénérées à chaque tag (voir Site GitHub Pages du mod).
- Contrôle : un check sur chaque pull request refuse les titres de commit qui ne respectent pas la convention, sinon le changelog serait incomplet.
- Bilingue : chaque commit \`feat:\`, \`fix:\` ou \`perf:\` porte deux lignes en pied de message, \`Changelog-fr:\` et \`Changelog-en:\`, lues par git-cliff. Deux modèles produisent \`CHANGELOG.fr.md\` et \`CHANGELOG.md\` (anglais) ; la description de la GitHub Release contient les deux langues. Le check de pull request refuse un commit de ce type s'il manque l'une des deux lignes.

**Points délicats.**

- Détection du motif de cercle : stocker le cercle dessiné comme un bloc-entité qui mémorise sa grille plutôt que de scanner le sol à chaque clic.
- Emplacements d'automail : interface d'équipement custom ; vérifier la compatibilité avec Accessories/Trinkets.
- Boss à régénération : un composant « âmes restantes » côté serveur, synchronisé pour la barre de boss.
- Rendu : la 26.2 ajoute un rendu Vulkan expérimental ; tester les shaders custom (Espace blanc, Pierre) sous OpenGL et Vulkan.

## Roadmap et points d'attention

Commencer par le système de transmutation : s'il n'est pas agréable à jouer, aucun boss ne sauvera le mod.

1. **Socle (v0.1)** : craie, Carnet de cercles avec dessin sur grille, parseur, 10 glyphes, valeur d'échange, Traité (premiers chapitres), fichiers fr\_fr et en\_us. Validé quand un joueur crée un cercle qui marche grâce au seul Traité.
2. **Alchimiste (v0.2)** : arbre de savoir, étages multiples, liaisons et satellites, gants, peinture et gravure sur surfaces, première version de l'éditeur web, armes temporaires, écoles Terre et Métal, PNJ Izumi.
3. **État (v0.3)** : examen d'Alchimiste d'État, montre, gants d'État, vêtements brodés, tatouages, écoles Feu, Glace, Explosion, structures Resembool et Central.
4. **Porte (v0.4)** : transmutation humaine, dimension Espace blanc, péage à chaque ouverture, transmutation sans cercle, automail de bras et de jambes, Winry, Rush Valley.
5. **Homonculus (v0.5)** : Lust, Gluttony, Envy, Greed avec régénération par pierre ; Laboratoire 5.
6. **Endgame (v1.0)** : Pierre philosophale, rituel de Pierre vivante et éclipse, karma, Sloth, Pride, Wrath, Père et zone anti-alchimie, alkahestry de Xing, Briggs.

**Équilibrage.** La Pierre philosophale casse forcément l'économie : limiter sa durabilité et la rendre visible aux mobs hostiles. Le péage doit rester réversible en endgame (quête pour récupérer son membre), sinon les joueurs éviteront la Porte.

**Droits et noms.** Mod de fan gratuit, non monétisé. Il utilise les noms officiels de l'œuvre (personnages, homonculus, lieux, titres d'alchimistes) dans les deux langues, avec un avertissement « fan-made, non affilié à Hiromu Arakawa, Square Enix ni Aniplex » sur le site et dans la description du mod. Les visuels restent des créations originales : aucune image, logo, musique ou voix officiels.

**Mention des ayants droit.** Fullmetal Alchemist, ses personnages, noms, lieux et éléments d'univers appartiennent à leurs ayants droit : Hiromu Arakawa et Square Enix pour le manga, Bones, Aniplex et leurs partenaires pour l'anime Fullmetal Alchemist: Brotherhood. Ce mod est un projet de fan indépendant, gratuit et non officiel, sans lien avec eux ni approbation de leur part. Cette mention figure en français et en anglais sur le site GitHub Pages, dans le README du dépôt, dans la description de chaque release et dans le fichier `fabric.mod.json`.

**Cercles et glyphes : sources visuelles.** Les glyphes sont dessinés à partir du symbolisme alchimique historique, qui est du domaine public : triangles des quatre éléments, symboles des sept métaux planétaires (or ☉, argent ☽, fer ♂, cuivre ♀, étain ♃, plomb ♄, mercure ☿), soufre, sel, Ouroboros, croix et serpent de Flamel, géométrie sacrée (cercles concentriques, hexagramme, heptagramme). Les cercles des personnages (Ed, Mustang, Armstrong, Scar, Kimblee) sont des créations originales qui reprennent leur logique (feu et oxygène, explosion, décomposition) sans copier les tracés de l'anime ou du manga.

**Contenu sensible.** Chimères humaines (Nina Tucker) et génocide d'Ishval : les traiter par le lore (livres, ruines) plutôt que par des mécaniques jouables directes.
