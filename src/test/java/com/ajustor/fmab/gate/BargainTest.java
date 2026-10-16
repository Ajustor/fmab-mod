package com.ajustor.fmab.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Le marché de la Vérité dans la visite : proposé une fois, il la retient, puis la laisse reprendre. */
class BargainTest {
	@Test
	void theTruthOffersWhenTheVisitReachesTheBargain() {
		assertEquals(GateOfTruth.AtBargain.OFFER, GateOfTruth.atBargain(false, false, true));
	}

	@Test
	void anOpenBargainHoldsTheVisitWithoutBeingOfferedAgain() {
		assertEquals(GateOfTruth.AtBargain.WAIT, GateOfTruth.atBargain(true, false, true));
	}

	@Test
	void aSettledBargainLetsTheVisitGoOn() {
		assertEquals(GateOfTruth.AtBargain.GO, GateOfTruth.atBargain(false, true, true),
				"s'en aller ne rouvre pas le marché");
	}

	@Test
	void backInTheMiddleOfABargainTheTruthWaitsForTheScreenToBeReady() {
		assertEquals(GateOfTruth.AtBargain.WAIT, GateOfTruth.atBargain(false, false, false));
	}
}
