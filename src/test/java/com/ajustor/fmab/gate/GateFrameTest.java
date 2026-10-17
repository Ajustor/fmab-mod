package com.ajustor.fmab.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le cadre de la Porte entoure les battants sans jamais les recouvrir. */
class GateFrameTest {
	private static final int HALF = 4;
	private static final int HEIGHT = 12;

	@Test
	void theFrameNeverCoversTheDoors() {
		for (int x = -HALF; x < HALF; x++) {
			for (int y = 1; y <= HEIGHT; y++) {
				assertFalse(GateFrame.at(x, y, HALF, HEIGHT), "battant en " + x + "," + y);
			}
		}
	}

	@Test
	void pillarsLintelAndPlinthSurroundThem() {
		assertTrue(GateFrame.at(-HALF - 1, 5, HALF, HEIGHT), "pilier gauche");
		assertTrue(GateFrame.at(HALF, 5, HALF, HEIGHT), "pilier droit");
		assertTrue(GateFrame.at(0, HEIGHT + 1, HALF, HEIGHT), "linteau");
		assertTrue(GateFrame.at(0, 0, HALF, HEIGHT), "socle");
		assertTrue(GateFrame.at(0, GateFrame.top(HEIGHT), HALF, HEIGHT), "fronton");
		assertFalse(GateFrame.at(-GateFrame.reach(HALF) - 1, 0, HALF, HEIGHT), "rien au-delà du socle");
	}
}
