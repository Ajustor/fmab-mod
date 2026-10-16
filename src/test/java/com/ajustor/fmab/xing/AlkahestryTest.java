package com.ajustor.fmab.xing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le battement d'un cercle d'alkahestry : sur l'horloge du monde, ou dès son tracé pour une Pierre vivante. */
class AlkahestryTest {
	private static final int PULSE = Alkahestry.PULSE;
	/** Un cercle tracé entre deux battements de l'horloge. */
	private static final long DRAWN = 3 * PULSE + 5;

	@Test
	void anOrdinaryCircleBeatsOnTheWorldClock() {
		assertFalse(Alkahestry.pulseDue(DRAWN, DRAWN, false), "il attend le prochain battement");
		assertTrue(Alkahestry.pulseDue(4 * PULSE, DRAWN, false));
		assertTrue(Alkahestry.pulseDue(5 * PULSE, DRAWN, false));
		assertFalse(Alkahestry.pulseDue(DRAWN + PULSE, DRAWN, false));
	}

	@Test
	void aLivingStonesCircleBeatsAsSoonAsItIsDrawn() {
		assertTrue(Alkahestry.pulseDue(DRAWN, DRAWN, true));
	}

	@Test
	void aLivingStonesCircleThenKeepsItsOwnBeat() {
		assertFalse(Alkahestry.pulseDue(4 * PULSE, DRAWN, true));
		assertTrue(Alkahestry.pulseDue(DRAWN + PULSE, DRAWN, true));
		assertTrue(Alkahestry.pulseDue(DRAWN + 2 * PULSE, DRAWN, true));
	}
}
