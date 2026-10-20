package com.ajustor.fmab.alchemy.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Mise en page de l'arbre de savoir : les écoles en grille, autant de colonnes que la largeur de
 * l'écran en tient, et les rangées les unes sous les autres. Ce qui dépasse en hauteur défile.
 *
 * @param left        bord gauche du livre
 * @param width       largeur du livre
 * @param columnWidth largeur d'une école
 * @param cells       coin de chaque école, relatif au haut du contenu, dans l'ordre reçu
 * @param height      hauteur de tout le contenu
 */
public record KnowledgeLayout(int left, int width, int columnWidth, List<Cell> cells, int height) {
	/** Largeur d'une école quand la place ne manque pas. */
	public static final int COLUMN = 150;
	/** Hauteur d'une ligne de texte. */
	public static final int ROW = 12;
	/** Le livre garde cette marge de chaque côté de l'écran. */
	public static final int MARGIN = 8;
	/** Marge intérieure du livre, de chaque côté. */
	public static final int PADDING = 10;
	private static final int ROW_GAP = 8;

	/** Coin d'une école, relatif au haut du contenu. */
	public record Cell(int x, int y) {
	}

	/** Hauteur d'une école : son nom, sa maîtrise, puis un nœud par ligne. */
	public static int schoolHeight(int nodes) {
		return 2 * ROW + 4 + nodes * ROW;
	}

	/**
	 * @param screenWidth largeur de l'écran
	 * @param nodeCounts  nombre de nœuds de chaque école, dans l'ordre d'affichage
	 */
	public static KnowledgeLayout of(int screenWidth, List<Integer> nodeCounts) {
		int schools = nodeCounts.size();
		int room = Math.max(1, screenWidth - 2 * MARGIN - 2 * PADDING);
		int columns = Math.max(1, Math.min(Math.max(2, schools), room / COLUMN));
		int columnWidth = Math.min(COLUMN, room / columns);
		int width = columns * columnWidth + 2 * PADDING;
		int left = (screenWidth - width) / 2;

		List<Cell> cells = new ArrayList<>();
		int y = 0;
		for (int first = 0; first < schools; first += columns) {
			int rowHeight = 0;
			for (int i = first; i < Math.min(schools, first + columns); i++) {
				cells.add(new Cell(left + PADDING + (i - first) * columnWidth, y));
				rowHeight = Math.max(rowHeight, schoolHeight(nodeCounts.get(i)));
			}
			y += rowHeight + ROW_GAP;
		}
		return new KnowledgeLayout(left, width, columnWidth, List.copyOf(cells), Math.max(0, y - ROW_GAP));
	}

	/** Le décalage de défilement le plus grand, pour une zone visible de cette hauteur. */
	public int maxScroll(int visibleHeight) {
		return Math.max(0, height - visibleHeight);
	}
}
