package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Vec2;

/**
 * La page d'un emplacement du corps, en cases de grille (celles du carnet). Les emplacements carrés
 * ont la page du carnet ({@link #SQUARE}) ; le tour d'un bras se déplie en une page deux fois plus
 * large que haute ({@link #SLEEVE}), à la même échelle : un cercle du carnet y garde sa taille.
 *
 * @param width  largeur en cases
 * @param height hauteur en cases
 */
public record Canvas(int width, int height) {
	public static final Canvas SQUARE = new Canvas(Drawing.GRID, Drawing.GRID);
	/** Le tour du bras, déplié : quatre faces de seize cases, de l'épaule au poignet. */
	public static final Canvas SLEEVE = new Canvas(4 * Drawing.GRID / 2, Drawing.GRID);
	/** Pixels de texture par case : le trait d'un dessin a la même épaisseur partout. */
	public static final int TEXELS_PER_CELL = 8;

	public Vec2 center() {
		return new Vec2(width / 2.0, height / 2.0);
	}

	/** Ce point est-il sur la page (bords compris) ? */
	public boolean contains(Vec2 v) {
		return Double.isFinite(v.x()) && Double.isFinite(v.y())
				&& v.x() >= 0 && v.y() >= 0 && v.x() <= width && v.y() <= height;
	}

	/** Le point ramené sur la page. */
	public Vec2 clamp(Vec2 v) {
		return new Vec2(Math.clamp(v.x(), 0, width), Math.clamp(v.y(), 0, height));
	}

	/** Le plus grand tampon que la page porte : sa demi-largeur tient dans la hauteur. */
	public double maxStampHalf() {
		return Math.min(width, height) / 2.0;
	}

	public int textureWidth() {
		return width * TEXELS_PER_CELL;
	}

	public int textureHeight() {
		return height * TEXELS_PER_CELL;
	}
}
