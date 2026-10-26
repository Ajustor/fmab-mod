package com.ajustor.fmab.stone;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Une Pierre vivante se nourrit de ses âmes : sa barre de faim reste pleine, celle des autres ne bouge pas,
 * et ses plaies se referment tant qu'il lui reste des âmes.
 */
class LivingStoneTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void aLivingStoneIsNeverHungry() {
		assertEquals(20, LivingStone.fed(36, 2));
		assertEquals(20, LivingStone.fed(1, 0));
	}

	@Test
	void aLivingStoneHealsAHeartASecondWhileItHasSouls() {
		assertEquals(2, LivingStone.regeneration(1));
		assertEquals(2, LivingStone.regeneration(500));
		assertEquals(0, LivingStone.regeneration(0), "sans âmes, plus de régénération");
	}

	@Test
	void othersKeepTheirHunger() {
		assertEquals(2, LivingStone.fed(0, 2));
		assertEquals(17, LivingStone.fed(0, 17));
	}
}
