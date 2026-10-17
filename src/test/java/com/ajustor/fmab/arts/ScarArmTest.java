package com.ajustor.fmab.arts;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le bras de Scar : un bloc coûte selon sa dureté, et la Destruction ne se cultive pas à la chaîne. */
class ScarArmTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void harderBlocksCostMore() {
		assertEquals(1, ScarArm.blockCost(0), 1e-6, "la terre meuble");
		assertEquals(1.15, ScarArm.blockCost(1.5f), 1e-6, "la pierre");
		assertEquals(6, ScarArm.blockCost(50), 1e-6, "l'obsidienne");
		assertTrue(ScarArm.blockCost(50) > 5 * ScarArm.blockCost(1.5f) - 1e-6);
	}

	@Test
	void destructionGrowsOnceEveryFiveUses() {
		int gained = 0;
		for (int uses = 1; uses <= 100; uses++) {
			if (ScarArm.masteryDue(uses)) {
				gained++;
			}
		}
		assertEquals(100 / ScarArm.USES_PER_MASTERY, gained);
	}
}
