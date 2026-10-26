package com.ajustor.fmab.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Greed n'est en carbone qu'en combat, hors du moment où il frappe ; le carbone gagne le corps, et
 * s'en retire, en {@link GreedEntity#DEPLOY} ticks.
 */
class GreedShieldTest {
	@Test
	void theShieldIsUpOnlyInAFight() {
		assertFalse(GreedEntity.armored(false, 0), "hors combat, un homme");
		assertTrue(GreedEntity.armored(true, 0), "en combat, le bouclier");
		assertFalse(GreedEntity.armored(true, 12), "il le retire pour frapper");
	}

	@Test
	void theCarbonSpreadsAndRecedesOverTheDeployTime() {
		float cover = 0;
		for (int i = 0; i < GreedEntity.DEPLOY; i++) {
			cover = GreedEntity.spread(cover, true);
		}
		assertEquals(1, cover, 1e-4);
		assertEquals(1, GreedEntity.spread(cover, true), 1e-6, "il ne déborde pas");
		for (int i = 0; i < GreedEntity.DEPLOY; i++) {
			cover = GreedEntity.spread(cover, false);
		}
		assertEquals(0, cover, 1e-4);
	}
}
