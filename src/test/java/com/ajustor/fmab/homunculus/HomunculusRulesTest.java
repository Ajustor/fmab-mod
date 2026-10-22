package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.stone.Recruitment;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les homonculus ne s'en prennent qu'aux menaces, et ne pullulent pas : Envy vaincu ne revient plus,
 * les rôdeurs s'en vont, un noyau par sorte.
 */
class HomunculusRulesTest {
	@Test
	void anOrdinaryHumanIsNoThreat() {
		// Même une âme noire : tant qu'elle n'a pas répondu, elle attend l'offre de Lust, pas ses lances.
		assertFalse(HomunculusRules.threatens(null, 0, Set.of(), false, false));
	}

	@Test
	void whoThwartsFatherIsAThreat() {
		assertTrue(HomunculusRules.threatens(null, 3, Set.of(), false, false), "une Pierre vivante");
		assertTrue(HomunculusRules.threatens(Recruitment.REFUSED, 0, Set.of(), false, false), "l'offre refusée");
		assertTrue(HomunculusRules.threatens(null, 0, Set.of("lust"), false, false), "il a vu tomber Lust");
		assertTrue(HomunculusRules.threatens(null, 0, Set.of(), true, false), "un point de sang scellé");
	}

	@Test
	void whoStrikesIsStruckBack() {
		assertTrue(HomunculusRules.threatens(null, 0, Set.of(), false, true));
		assertTrue(HomunculusRules.threatens(Recruitment.ACCEPTED, 0, Set.of(), false, true), "même une recrue");
	}

	@Test
	void aRecruitIsSparedWhateverItCarries() {
		assertFalse(HomunculusRules.threatens(Recruitment.ACCEPTED, 5, Set.of("envy"), true, false));
	}

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
