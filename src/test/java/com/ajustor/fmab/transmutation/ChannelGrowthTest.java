package com.ajustor.fmab.transmutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Un ouvrage qu'on garde sous la main grandit d'un cran par geste ; celui d'une Pierre vivante, d'emblée. */
class ChannelGrowthTest {
	@Test
	void anAlchemistsWorkGrowsOneNotchPerProlongation() {
		assertEquals(1, Transmutation.growth(1, false));
		assertEquals(7, Transmutation.growth(7, false));
		assertEquals(Transmutation.MAX_GROWTH, Transmutation.growth(Transmutation.MAX_GROWTH, false));
	}

	@Test
	void aLivingStonesWorkIsFullGrownFromTheFirstProlongation() {
		assertEquals(Transmutation.MAX_GROWTH, Transmutation.growth(1, true));
		assertEquals(Transmutation.MAX_GROWTH, Transmutation.growth(5, true), "puis il ne grandit plus");
	}

	@Test
	void holdingTheCircleWithAStoneCostsASoulNowAndThen() {
		assertTrue(Transmutation.channelPaysASoul(1), "la première prolongation paie");
		int paid = 0;
		for (int prolonged = 1; prolonged <= Transmutation.MAX_GROWTH; prolonged++) {
			if (Transmutation.channelPaysASoul(prolonged)) {
				paid++;
			}
		}
		assertEquals(3, paid, "un ouvrage tenu jusqu'au bout : trois âmes");
	}

	/** La maîtrise fait l'Alchimiste, d'où qu'elle vienne (transmutation ou récompense d'Izumi). */
	@Test
	void anApprenticeIsPromotedAsSoonAsMasteryAllows() {
		int needed = com.ajustor.fmab.alchemy.knowledge.Knowledge.ALCHEMIST_MASTERY;
		assertTrue(Transmutation.promotes(com.ajustor.fmab.alchemy.glyph.Rank.APPRENTICE, needed));
		assertTrue(!Transmutation.promotes(com.ajustor.fmab.alchemy.glyph.Rank.APPRENTICE, needed - 1));
		assertTrue(!Transmutation.promotes(com.ajustor.fmab.alchemy.glyph.Rank.STATE, needed * 10), "on ne redescend pas");
	}
}
