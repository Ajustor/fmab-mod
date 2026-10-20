package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.knowledge.KnowledgeLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'arbre de savoir tient dans la largeur de l'écran, quel que soit le nombre d'écoles. */
class KnowledgeLayoutTest {
	/** Les huit écoles du mod et leurs nœuds. */
	private static final List<Integer> SCHOOLS = List.of(2, 4, 2, 2, 2, 3, 3, 3);

	@Test
	void eightSchoolsFitOnASmallScreen() {
		// 854×480 en échelle d'interface 2.
		assertFits(427, SCHOOLS);
	}

	@Test
	void evenAVeryNarrowScreenKeepsTheBookInside() {
		assertFits(160, SCHOOLS);
		assertFits(320, SCHOOLS);
	}

	@Test
	void aWideScreenShowsEverySchoolSideBySide() {
		KnowledgeLayout layout = KnowledgeLayout.of(1920, SCHOOLS);
		assertTrue(layout.cells().stream().allMatch(c -> c.y() == 0), "une seule rangée sur un écran large");
		assertEquals(KnowledgeLayout.COLUMN, layout.columnWidth());
	}

	@Test
	void schoolsThatDoNotFitWrapOntoNewRows() {
		KnowledgeLayout layout = KnowledgeLayout.of(427, SCHOOLS);
		assertEquals(2, layout.cells().stream().filter(c -> c.y() == 0).count());
		assertTrue(layout.height() > KnowledgeLayout.schoolHeight(4), "plusieurs rangées");
		assertTrue(layout.maxScroll(120) > 0, "le contenu trop haut défile");
		assertEquals(0, layout.maxScroll(layout.height()));
	}

	@Test
	void aFewSchoolsKeepTwoColumnsAtLeast() {
		assertEquals(2 * KnowledgeLayout.COLUMN + 2 * KnowledgeLayout.PADDING,
				KnowledgeLayout.of(800, List.of(3)).width());
	}

	private static void assertFits(int screenWidth, List<Integer> schools) {
		KnowledgeLayout layout = KnowledgeLayout.of(screenWidth, schools);
		assertEquals(schools.size(), layout.cells().size());
		assertTrue(layout.left() - 3 >= 0, "le bord gauche du livre sort de l'écran");
		assertTrue(layout.left() + layout.width() + 3 <= screenWidth, "le bord droit du livre sort de l'écran");
		for (KnowledgeLayout.Cell cell : layout.cells()) {
			assertTrue(cell.x() >= layout.left(), "une école déborde à gauche");
			assertTrue(cell.x() + layout.columnWidth() <= layout.left() + layout.width(), "une école déborde à droite");
		}
	}
}
