package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.entity.WheelchairEntity;
import com.ajustor.fmab.gate.Wheelchairs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les règles du fauteuil roulant : deux bras pour rouler, ramper sans jambes, des rampes. */
class WheelchairTest {
	@Test
	void turningTheWheelsTakesBothArms() {
		assertTrue(Wheelchairs.canPropel(true, true));
		assertFalse(Wheelchairs.canPropel(true, false));
		assertFalse(Wheelchairs.canPropel(false, true));
		assertFalse(Wheelchairs.canPropel(false, false));
	}

	@Test
	void withoutLegsOneCrawlsUnlessSomethingCarriesYou() {
		assertTrue(Wheelchairs.mustCrawl(true, false, false, false));
		// Un fauteuil, un cheval : on est porté.
		assertFalse(Wheelchairs.mustCrawl(true, true, false, false));
		// En vol (créatif, élytres, spectateur) ou au lit, on ne rampe pas.
		assertFalse(Wheelchairs.mustCrawl(true, false, true, false));
		assertFalse(Wheelchairs.mustCrawl(true, false, false, true));
		// Avec au moins une jambe, on marche.
		assertFalse(Wheelchairs.mustCrawl(false, false, false, false));
	}

	@Test
	void aWheelchairClimbsSlabsButNotFullBlocks() {
		assertEquals(0.5f, WheelchairEntity.STEP);
	}
}
