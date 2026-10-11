package com.ajustor.fmab.homunculus;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les homonculus ne pullulent pas : Envy vaincu ne revient plus, les rôdeurs s'en vont, un noyau par sorte. */
class HomunculusRulesTest {
	@Test
	void envyNoLongerHauntsWhoSawHimFall() {
		assertTrue(HomunculusRules.envyHaunts(Set.of()));
		assertTrue(HomunculusRules.envyHaunts(Set.of("lust")));
		assertFalse(HomunculusRules.envyHaunts(Set.of("envy")));
	}

	@Test
	void onlyTheFirstOfEachKindLeavesACore() {
		assertTrue(HomunculusRules.dropsCore(Set.of(), "lust"));
		assertTrue(HomunculusRules.dropsCore(Set.of("envy"), "lust"));
		assertFalse(HomunculusRules.dropsCore(Set.of("lust", "envy"), "lust"));
	}

	@Test
	void aWandererLeftAloneFiveMinutesLeaves() {
		int alone = 0;
		for (int second = 0; second < 60 * 5 - 1; second++) {
			alone = HomunculusRules.alone(alone, false, 20);
		}
		assertFalse(HomunculusRules.vanishes(alone));
		alone = HomunculusRules.alone(alone, false, 20);
		assertTrue(HomunculusRules.vanishes(alone));
		// Un joueur qui passe remet le compte à zéro.
		assertEquals(0, HomunculusRules.alone(alone, true, 20));
	}
}
