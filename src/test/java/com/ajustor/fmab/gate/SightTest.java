package com.ajustor.fmab.gate;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.data.GateState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La vue perdue, comme le serveur la juge : des yeux pris, ou une armure d'âme sans heaume. */
class SightTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void sightTakenByTheTruthIsLostForTheServerToo() {
		assertFalse(Tolls.sightless(GateState.NONE, true), "un corps entier voit, heaume ou non");
		assertTrue(Tolls.sightless(GateState.NONE.pay(BodyPart.SIGHT), false), "des yeux pris, même casqué");
	}

	@Test
	void aSoulInArmourSeesThroughItsHelmet() {
		GateState al = GateState.NONE.pay(BodyPart.BODY);
		if (al.soulBound() && al.inArmor()) {
			assertTrue(Tolls.sightless(al, true), "sans heaume");
			assertFalse(Tolls.sightless(al, false), "avec son heaume");
		}
	}
}
