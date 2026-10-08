package com.ajustor.fmab.alchemy.glyph;

/**
 * Marge laissée au tracé du joueur.
 *
 * @param rotationDeg écart d'orientation accepté de part et d'autre de la position de référence ;
 *                    180 ou plus veut dire « n'importe quelle orientation »
 * @param position    écart moyen accepté entre le tracé et la référence, une fois les deux ramenés
 *                    à la même taille (unité : rayon moyen du glyphe)
 * @param minScale    rapport largeur/hauteur minimal du tracé par rapport à la référence
 * @param maxScale    rapport largeur/hauteur maximal
 */
public record Tolerance(double rotationDeg, double position, double minScale, double maxScale) {
	public static final Tolerance DEFAULT = new Tolerance(15, 0.1, 0.7, 1.4);

	public boolean anyRotation() {
		return rotationDeg >= 180;
	}
}
