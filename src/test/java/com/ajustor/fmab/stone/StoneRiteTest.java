package com.ajustor.fmab.stone;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.entity.SoulEyeEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le rite de la Pierre, comme à Xerxès : la lumière court du bord vers le centre, puis les vies tombent,
 * des plus lointaines aux plus proches (un œil s'ouvre dans le sol sous chacune), avant que les âmes se
 * condensent.
 */
class StoneRiteTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void theLightRunsFromTheEdgeToTheCenter() {
		assertEquals(24, StoneRite.front(0, 24), 1e-9, "au premier instant, seul le bord s'allume");
		assertEquals(12, StoneRite.front(StoneRite.AWAKENING / 2, 24), 1e-9);
		assertEquals(0, StoneRite.front(StoneRite.AWAKENING, 24), 1e-9, "à la fin de l'éveil, tout le cercle est rouge");
		assertEquals(0, StoneRite.front(StoneRite.LENGTH, 24), 1e-9);
	}

	@Test
	void livesFallFromTheFarthestToTheNearestBeforeTheSoulsCondense() {
		int count = 40;
		assertEquals(StoneRite.AWAKENING, StoneRite.deathTick(0, count), "la plus lointaine tombe quand l'éveil s'achève");
		assertTrue(StoneRite.deathTick(count - 1, count) < StoneRite.HARVEST_END, "la dernière avant la colonne de lumière");
		for (int i = 1; i < count; i++) {
			assertTrue(StoneRite.deathTick(i, count) >= StoneRite.deathTick(i - 1, count), "des plus lointaines aux plus proches");
		}
		assertEquals(StoneRite.AWAKENING, StoneRite.deathTick(0, 1), "une seule vie : elle tombe d'emblée");
		assertTrue(StoneRite.HARVEST_END < StoneRite.LENGTH, "le temps de condenser les âmes");
	}

	@Test
	void theEyeUnderAVictimOpensWatchesAndCloses() {
		int shut = 40;
		assertEquals(0, SoulEyeEntity.openness(0, shut), 1e-6, "fermé à l'instant où il paraît");
		assertTrue(SoulEyeEntity.openness(SoulEyeEntity.OPEN / 2f, shut) > 0.5f, "il s'ouvre vite");
		assertEquals(1, SoulEyeEntity.openness(20, shut), 1e-6, "grand ouvert pendant que sa proie meurt");
		assertTrue(SoulEyeEntity.openness(shut - SoulEyeEntity.CLOSE / 2f, shut) < 1, "il se referme");
		assertEquals(0, SoulEyeEntity.openness(shut, shut), 1e-6);
	}
}
