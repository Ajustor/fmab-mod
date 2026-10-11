# Journal des modifications

## 1.2.0 — 2026-10-11

### Nouveautés

- HUD redessiné : le compteur d'âmes a l'icône de la Pierre, la concentration une jauge encadrée, la vue perdue un voile doux
- Les chimères de Tucker ont leurs propres corps : lion, loup et serpent pour la grande, six membres cousus pour la rampante.
- Liore devient une vraie ville du désert : enceinte, allée du temple, place à fontaine, marché couvert et une vingtaine de maisons à terrasse.
- Les gestes de transmutation se voient aussi à la première personne : les mains claquent devant soi, plongent sur le cercle ou se tendent vers une âme.
- Des chemins relient les maisons de Resembool, Dublith et Rush Valley à leur place, bordés de réverbères.
- Faire de Greed un allié suffit pour « Les sept péchés », et de nouveaux progrès récompensent le pacte avec Greed, le tatouage d'Ishval, Cornello démasqué, les notes de Marcoh, Hohenheim et le dispensaire de Marcoh.
- La montre d'État, le noyau de Pierre, le sang cristallisé, le fil, l'encre et la cartouche ont une infobulle qui dit à quoi ils servent.
- Avec un karma très bas, Lust vient d'abord vous faire l'offre de Père : l'accepter met fin aux traques, la refuser les déclenche.
- Barry le Boucher a sa propre mécanique : coup de hachoir en arc annoncé, ruée une fois blessé, et un sceau qui tient au premier effondrement.
- Commandes de test /fmab karma [valeur] et /fmab offer (opérateurs).

### Corrections

- Après un changement de monde en solo, le bras de Scar et le marché de la Pierre ne restent plus bloqués ; les états par joueur ne s'accumulent plus sur un serveur
- Le sommaire du Traité ne coupe plus les titres des chapitres ; Winry montre l'usure de chaque automail en barre ; l'écran des gants est plus lisible
- Les cercles portés (broderies, tatouages) ne cumulent plus un même effet au-delà de trois, et se taisent dans la zone de Père.
- Envy ne revient plus hanter qui l'a vaincu, les homonculus errants repartent quand personne ne les approche, et seul le premier de chaque sorte laisse un noyau de Pierre.
- La difficulté des boss suit l'ordre de progression : Gluttony et Sloth s'adoucissent, Pride, Wrath et surtout Père deviennent les plus coriaces.
- Les cercles ne disparaissent plus pendant la cinématique de la Porte, et beaucoup de cercles à l'écran ne font plus saccader le jeu.
- Le pistolet tire moins vite, ses balles faiblissent avec la distance et ne font guère que ralentir les homonculus.
- Pride démasqué vous traque et fuit vers la pénombre quand ses ombres meurent ; ses ombres ne traversent plus les murs et épargnent vos chimères, vos bêtes et les villageois.
- L'arme transmutée de l'examen d'État ne s'effrite plus près de l'examinateur, et lancer une arme sans le savoir requis dit lequel manque et comment l'obtenir.
- Une âme qui rouvre la Porte revient dans sa propre armure, cabossée, au lieu de la perdre et d'avoir à laisser une armure sans défense en attente.

### Performances

- Moins de trafic réseau pour les joueurs amputés, appareillés ou portant des passifs : leurs attributs ne sont plus renvoyés à chaque tick
- Le rendu des joueurs est plus léger : plus de recherche du fauteuil poussé ni de reconstruction du corps à chaque image.
- La recharge de la concentration n'envoie plus toutes les données d'alchimiste au client chaque seconde.
## 1.1.0 — 2026-10-11

### Nouveautés

- Fauteuil roulant : sans jambes on rampe ou l'on roule, deux bras pour avancer, des rampes pour monter, et l'on peut se faire pousser par un joueur ou un villageois
- Fabriquer des objets par alchimie : décomposez un objet pour le comprendre, puis recomposez-le d'après modèle avec la matière qu'il contient
- Page de conception (touche J) : pensez à l'objet à créer, sans modèle ; la Pierre philosophale crée sans matière ; un surplus de matière rend l'objet plus solide
- Glyphes Bois et Fibre : recomposez coffres, bateaux, lits, arcs… (tome « Du bois et de la fibre » chez les bibliothécaires)
- 14 nouveaux glyphes (Carbone, Cristal, Soufre, Mercure, Chaux, Plante, Chair, Argent, Étain, Plomb, Zinc, Nickel, Aluminium, Nethérite) : tout se transmute, y compris les matières des autres mods
- Nouveau cercle de transmutation humaine, à six satellites (Alchimiste d'État) ; la chimère demande Humain + Chair, la détonation Feu + Soufre
- Le rang ne bloque plus un cercle : au-delà, il perd en stabilité et risque de rebondir (une Pierre philosophale le tient)
- Transmutation humaine avec une Pierre philosophale : ouvrir la Porte et racheter ses parties avec les âmes de la Pierre, ou créer un être en lui donnant une âme ; un Initié de la Porte peut joindre les mains une Pierre en main
- La forme de l'être créé avec une Pierre se choisit (humain, chien, chat, cheval, perroquet) ; la Pierre ne rend plus les parties perdues que devant la Porte
- Nouvelles textures pour toutes les créatures (personnages, homoncules, chimères, golem, automails), œufs d'apparition en portraits et armes redessinées
- La Porte enseigne tous les glyphes ; elle s'ouvre et se referme en deux battants, ses bras ondulent et ont de vraies mains ; nouvelle cinématique (bras d'ombre, œil, tunnel de cercles et de glyphes) avec les répliques de la Vérité en sous-titres
- Les blocs du mod sont redessinés (briques, chair de Gluttony, sang cristallisé, pierre rouge, antre de Père, sceaux, table d'alchimiste, établi d'automail, pierre de la Porte)
- Les villes sont plus denses et vivantes : maisons avec avant-toits, volets, jardinières et cheminées, arbres, marchés, pré à moutons à Resembool, palmiers et auvents à Liore et Rush Valley
- La montre d'Alchimiste d'État, les gants de cuir et l'éclat de pierre rouge sont redessinés
- Table d'alchimiste, établi d'automail et tuyaux de Père modelés en 3D ; les sceaux et la pierre rouge luisent dans le noir, les yeux de la Porte clignent
- Les personnages ont des silhouettes reconnaissables : poitrine et hanches de Lust, poitrine d'Izumi, de Winry et d'Olivier, queues de cheval, chignons et nattes de May Chang, visières des soldats
- Cornello porte son médaillon et sa robe de prêtre, Marcoh sa sacoche, Olivier sa chevelure dans le dos ; les soldats de Briggs ont leur col de fourrure et leur sac, ceux de Drachma leur chapka et leur capote
- Scar a des épaules de lutteur ; Pride a une tête d'enfant et, révélé, un masque d'ombre et plus de lames
- Icônes redessinées (encres, livres, craie, gants, cercles écarlates, Pierre philosophale) ; le plastron d'âme ressemble à l'armure d'Alphonse, la lance de pierre a sa propre apparence ; œuf d'apparition du golem de pierre
- Un bras d'automail peut être transmuté en lame (Lame-bras) comme celui d'Ed, puis retrouver sa forme ; une lame large sort d'un manchon riveté, se voit et tranche à mains nues
- Les personnages respirent, marchent à leur manière et prennent des poses (bras croisés, mains dans le dos, poings sur les hanches) puis une garde au combat ; Lust tend la main et lance ses doigts-lames, le ventre de Gluttony ballotte
- Les créatures du mod ont une voix (au repos, touchées, à la mort) et certaines un bruit de pas propre
- Les soldats de Drachma, les immortels, les chimères, les armures vides, Barry et le golem de pierre laissent un butin
- Fort Briggs (contreforts, meurtrières, bannière, porte à herse, caserne éclairée) et le Laboratoire 5 (complexe abandonné, enceinte à barbelés, bâtiment de briques) sont reconstruits
- Les gestes de transmutation du joueur (mains jointes, paumes sur le cercle, âme arrachée) s'animent avec élan et retombée
## 1.0.1 — 2026-10-09

### Nouveautés

- Les automails et les membres perdus se voient sur le corps du joueur, pour lui comme pour les autres

### Corrections

- À la première personne, le bras perdu n'est plus dessiné et l'automail remplace la peau
- Les mains jointes se rejoignent bien, et le corps penché sur un cercle reste d'un seul tenant
- Le sommaire du Traité tient dans le livre ; chez Winry, le nom et l'usure des automails ne sont plus cachés par les boutons
- Père a le même corps que Hohenheim, qu'il a copié
## 1.0.0 — 2026-10-09

### Nouveautés

- Cercles de transmutation dessinés dans le Carnet, tracés à la craie et expliqués par le Traité d'alchimie
- Cercles à plusieurs étages avec liaisons et satellites, glyphes Feu et Air, jet de flammes, rafale, fonte et combustion amplifiée
- Arbre de savoir des écoles Terre et Métal, passage au rang Alchimiste par la pratique, glyphe Recomposer, lance de pierre et lame-bras temporaires
- Cercles inscrits au sol, sur les murs et au plafond ; peinture alchimique durable et burin pour graver la pierre et le métal
- Gants d'alchimiste brodés ou gravés à la table d'alchimiste, lancés d'une touche, à distance (silex) ou en frappant (gantelets)
- Izumi Curtis, ses épreuves récompensées en savoir et ses combats d'entraînement
- Éditeur de cercles web, avec le même parseur que le jeu, pour concevoir ses cercles et les importer dans le carnet
- L'éditeur de cercles propose les cercles d'exemple du Traité, prêts à ouvrir et à modifier
- Écoles du Feu, de la Glace et de l'Explosion : piques et murs de glace, détonations
- Examen d'Alchimiste d'État contre un golem de pierre lié à son cercle, et montre d'Alchimiste d'État
- Vêtements brodés et tatouages par rituel, qui portent des cercles passifs ; paumes tatouées lancées d'une touche
- Central City, grande ville fortifiée avec son quartier général et l'examinateur d'État, et le hameau de Resembool
- Le carnet de départ contient des cercles simples, et le Traité recopie ses exemples dans le carnet d'un clic
- Le carnet affiche un verdict en couleur et dit à quel glyphe ressemble un trait illisible
- Les glyphes non compris fonctionnent avec un risque de rebond et s'apprennent par la pratique ou les tomes d'alchimie ; le carnet pose les glyphes compris au tampon
- La transmutation humaine ouvre la Porte de la Vérité : péages, armure d'âme, transmutation sans cercle, automail, Winry et Rush Valley
- Les bibliothécaires des villages vendent les tomes d'alchimie, comme des livres enchantés
- La Vérité porte les parties qu'elle vole ; une âme sans armure erre devant la Porte jusqu'à ce qu'un cercle d'âme l'appelle, et peut préparer ses propres sceaux
- L'âme habite une armure vide, la répare en y transmutant son matériau, peut changer d'armure, et chacun peut tracer son sceau à l'encre ou de son sang
- On choisit la taille des cercles tracés (1, 3, 5 ou 7 blocs) : plus grand, plus puissant et plus coûteux ; plus petit, plus discret
- Chaque joueur a son propre sceau de sang, montré par le Traité et donné à l'âme avec le cercle qui répare son armure
- Les transmutations font jaillir les éclairs bleus de la réaction alchimique
- Lust et Gluttony gardent le Laboratoire 5 ; les homonculus se reconstituent tant que leur Pierre a des âmes, et Gluttony avale dans son Ventre
- Envy erre déguisé, se révèle, devient un monstre géant puis un petit lézard
- Greed tient le Devil's Nest ; son Bouclier ultime ne s'ouvre que lorsqu'il frappe, et un pacte en fait un allié payé à la journée
- La Pierre philosophale, fondue de quatre noyaux d'homonculus, rend son corps à une âme fixée dans une armure
- La Pierre philosophale rend tout ce que la Porte a pris, chaque partie coûtant des âmes selon sa gravité
- Sloth charge dans son tunnel sous Central, Wrath esquive de son Œil ultime, et les ombres de Pride ne vivent qu'avec la lumière
- La Pierre philosophale amplifie les transmutations et se fabrique par sacrifice ; le karma, l'éclipse et la Pierre vivante arrivent
- La Pierre vivante absorbe les âmes sans limite ; garder la main sur un cercle fait grandir l'effet, et les gestes de transmutation s'animent
- La Vérité devient familière au fil des visites, se moque des âmes sans corps et leur propose de repartir de zéro (réglable dans config/fmab.json)
- Gestes de transmutation sur le modèle du joueur : paumes sur le cercle, mains tenues, mains jointes, bras tendu vers une âme
- Père, l'Homonculus originel, sous Central : boss en trois formes, zone anti-alchimie et sceau d'Ouroboros
- Alkahestry de Xing : cinq kunaï en cercle soignent ou piègent, même chez Père ; May Chang l'enseigne
- Fort Briggs, la générale Olivier Armstrong, le sabre de Briggs (×1,5 contre les homonculus) et les soldats de Drachma
- Progrès du chemin de l'alchimiste, cartes des lieux chez les cartographes, et quête des points de sang du cercle national
- Barry le Boucher, soldats d'Amestris et de Briggs, armures habitées, soldats immortels et chimères
- Bras de Scar, mines de Kimblee, pistolet et fusil de Hawkeye, couteaux de Hughes, épée de Xing ; les gants de Mustang craignent la pluie
- Liore et Cornello, les ruines d'Ishval et Scar, les ruines de Xerxès et Hohenheim, l'île de Yock et sa dernière épreuve d'Izumi
- Cinématiques de la Porte (bras noirs, œil et torrent du savoir), du Jour promis et de la chute de Père ; sons propres au mod ; absorption accroupie
- Dublith et la boucherie des Curtis, où vit Izumi ; elle donne la carte de l'île de Yock
- Glyphe Or : changer le fer en or, interdit par la loi d'État (karma, soldats aux trousses)
- Déclencheurs des cercles inscrits : à retardement, en piège ou sur signal de redstone
- Écoles de la Destruction, de la Médecine (alkahestry) et de la Vie ; fabriquer des chimères qui vous obéissent
- Briques amestriennes, briques de Xing et neige compacte de Briggs, dans les structures et à fabriquer
- Le docteur Marcoh et son dispensaire ; ses notes chiffrées, déguisées en recettes, qu'il rend en tomes d'alchimie
- Les bras noirs ramènent les âmes errantes devant leur Porte ; deux alchimistes peuvent fusionner leurs cercles
- L'éclipse se voit dans le ciel : la lune devant le soleil, un ciel rouge sombre et des étoiles à midi
- Silhouettes propres pour Gluttony (ventre), Sloth (carrure) et Père (barbe et chevelure)
- Silhouettes de Lust (chevelure), Envy (touffe en palmier) et Greed (col de fourrure)
- Silhouettes de Wrath (vareuse), Pride (ombres) et Barry (cimier) ; Fort Briggs et l'île de Yock mieux posés
- Carnet de cercles à la touche N, roue des cercles (R) pour changer de cercle en combat, relevé des cercles inconnus, Notes sur la transmutation humaine très rares
- Les villes sont habitées et meublées, des gardes aux portes de Central ; les barres de boss n'apparaissent qu'au combat ; plus de lieu posé dans Central
- Panneau en haut à gauche : cercle sélectionné et état du corps (automails et leur usure, membres perdus)
- Onglet Corps dans l'inventaire (membres, automails, gants, organes, vue) ; le cercle sélectionné ne s'affiche que quand il sert

### Corrections

- Les glyphes et les combinaisons sont enfin chargés en jeu : les cercles du Traité et du carnet fonctionnent
- Nombreuses corrections de la v1 : bras noirs, cinématiques, Porte, Fort Briggs, recettes, Scar, crédits des homonculus, équilibrage, génération des lieux
- Corrections : on entre enfin dans les maisons, combat d'Izumi, éclipse qui assombrit le monde, chimères liées, pièges et cercles armés
- Corrections : Izumi et Marcoh restent chez eux, Fort Briggs bien posé, éclipse progressive, Envy déguisé fidèle à sa victime
- Avant les tests : bras droit perdu jouable, sceaux dans les plastrons, concentration par rang, lieux accessibles, soleil de Père esquivable, cartes offertes, recettes connues
- Un carnet bien rempli s'enregistre sans déconnecter ; les points de sang scellés survivent à la mise à jour
- Le jeu ne plante plus au lancement ; le plastron d'âme s'enchante comme le fer

