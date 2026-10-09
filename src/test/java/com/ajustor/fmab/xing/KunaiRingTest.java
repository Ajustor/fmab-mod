package com.ajustor.fmab.xing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cinq kunaï plantés en rond referment un cercle ; en ligne, en tas ou trop loin, non. */
class KunaiRingTest {
	private static List<double[]> around(int n, double r, double fromDeg, double toDeg) {
		List<double[]> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / n);
			out.add(new double[]{Math.cos(a) * r, Math.sin(a) * r});
		}
		return out;
	}

	@Test
	void fiveAroundCloseACircle() {
		assertEquals(4, KunaiRing.radius(around(5, 4, 0, 360), 0, 0), 1e-9);
	}

	@Test
	void fourAreNotEnough() {
		assertTrue(KunaiRing.radius(around(4, 4, 0, 360), 0, 0) < 0);
	}

	@Test
	void aHalfCircleDoesNotClose() {
		assertTrue(KunaiRing.radius(around(5, 4, 0, 180), 0, 0) < 0);
	}

	@Test
	void tooSmallOrTooWideFails() {
		assertTrue(KunaiRing.radius(around(5, 1, 0, 360), 0, 0) < 0, "en tas");
		assertTrue(KunaiRing.radius(around(5, 12, 0, 360), 0, 0) < 0, "trop loin");
	}
}
