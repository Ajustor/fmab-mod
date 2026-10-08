package com.ajustor.fmab.alchemy.drawing;

import java.util.List;

/**
 * Tracé d'un cercle tel que le joueur l'a dessiné dans le carnet : une suite de primitives en
 * coordonnées de grille, de 0 à {@link #GRID} sur chaque axe, centre en ({@link #CENTER},
 * {@link #CENTER}).
 *
 * <p>On garde le tracé vectoriel plutôt qu'une matrice de pixels : c'est ce que les outils
 * produisent, c'est compact à stocker dans un composant d'item, et le parseur compare des traits
 * à des traits. La matrice ne sert qu'au rendu.
 */
public record Drawing(List<Primitive> primitives) {
	public static final int GRID = 32;
	public static final double CENTER = GRID / 2.0;
	public static final Vec2 CENTER_POINT = new Vec2(CENTER, CENTER);
	/** Au-delà, un dessin est refusé : c'est la limite du réseau, pas celle du parseur. */
	public static final int MAX_PRIMITIVES = 256;

	public static final Drawing EMPTY = new Drawing(List.of());

	public Drawing {
		primitives = List.copyOf(primitives);
	}

	public boolean isEmpty() {
		return primitives.isEmpty();
	}
}
