package com.ajustor.fmab.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Barry le Boucher : son sceau, son arc de hachoir, sa ruée. */
class ButcherTest {
	@Test
	void theSealHoldsTheFirstTime() {
		assertFalse(Butcher.sealExposed(1));
		assertTrue(Butcher.sealExposed(2));
		assertTrue(Butcher.sealExposed(3));
		assertEquals(Butcher.FIRST_REFORM, Butcher.reformTicks(1, 300));
		assertEquals(300, Butcher.reformTicks(2, 300));
	}

	@Test
	void theSweepCoversTheArcInFront() {
		// Tourné vers +z (yaw 0) : devant, en biais, derrière.
		assertTrue(Butcher.inSweep(0, 3, 0));
		assertTrue(Butcher.inSweep(2, 2, 0));
		assertTrue(Butcher.inSweep(-2, 2, 0));
		assertFalse(Butcher.inSweep(0, -3, 0));
		assertFalse(Butcher.inSweep(3, -0.5, 0));
		// Hors de portée.
		assertFalse(Butcher.inSweep(0, Butcher.SWEEP_REACH + 0.1, 0));
		// Tourné vers −x (yaw 90).
		assertTrue(Butcher.inSweep(-3, 0, 90));
		assertFalse(Butcher.inSweep(3, 0, 90));
		// De part et d'autre de ±180°.
		assertTrue(Butcher.inSweep(0.5, -3, 179));
		assertTrue(Butcher.inSweep(-0.5, -3, -179));
	}

	@Test
	void heRushesOnceWhenWounded() {
		assertFalse(Butcher.rushes(70, 70, false, 10));
		assertTrue(Butcher.rushes(35, 70, false, 10));
		assertFalse(Butcher.rushes(35, 70, true, 10));
		assertFalse(Butcher.rushes(35, 70, false, 3));
		assertFalse(Butcher.rushes(35, 70, false, 25));
	}
}
