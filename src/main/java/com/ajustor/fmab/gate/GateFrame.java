package com.ajustor.fmab.gate;

/**
 * Le cadre de la Porte de la Vérité, dans l'Espace blanc : un socle, deux piliers, un linteau et une
 * corniche à gradins autour des battants (de {@code -halfWidth} à {@code halfWidth - 1}, hauts de
 * {@code height}), comme le monument de l'anime. Une fonction pure de la position, dans le plan de
 * la Porte.
 */
public final class GateFrame {
	/** L'épaisseur des piliers et du linteau. */
	public static final int THICKNESS = 2;

	private GateFrame() {
	}

	/** Le cadre passe-t-il par cette colonne et cette hauteur (y = 0 : le sol) ? */
	public static boolean at(int x, int y, int halfWidth, int height) {
		int left = -halfWidth - THICKNESS, right = halfWidth - 1 + THICKNESS;
		boolean doors = x >= -halfWidth && x < halfWidth && y >= 1 && y <= height;
		if (doors) {
			return false;
		}
		if (y == 0) {
			// Le socle déborde d'un bloc de chaque côté.
			return x >= left - 1 && x <= right + 1;
		}
		if (y <= height + THICKNESS) {
			// Les piliers, puis le linteau au-dessus des battants.
			return x >= left && x <= right;
		}
		if (y == height + THICKNESS + 1) {
			// La corniche, plus large.
			return x >= left - 1 && x <= right + 1;
		}
		// Le fronton, en retrait.
		return y == height + THICKNESS + 2 && x >= -halfWidth + 1 && x < halfWidth - 1;
	}

	/** La hauteur du cadre, fronton compris. */
	public static int top(int height) {
		return height + THICKNESS + 2;
	}

	/** La demi-largeur du cadre, socle compris. */
	public static int reach(int halfWidth) {
		return halfWidth + THICKNESS + 1;
	}
}
