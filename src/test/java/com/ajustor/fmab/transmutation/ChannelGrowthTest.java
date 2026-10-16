package com.ajustor.fmab.transmutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
