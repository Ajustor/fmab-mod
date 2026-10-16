package com.ajustor.fmab.stone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'offre de Père : qui la reçoit, qui est traqué, qui est laissé tranquille, combien de temps Lust attend. */
class RecruitmentTest {
	@Test
	void aDarkSoulFirstGetsAnOfferNotAFight() {
		assertEquals(Recruitment.Visit.OFFER, Recruitment.visit(Karma.RECRUITABLE, 0, null));
		assertEquals(Recruitment.Visit.OFFER, Recruitment.visit(-100, 0, null));
		// Même porteur d'une Pierre vivante : l'offre passe avant la traque.
		assertEquals(Recruitment.Visit.OFFER, Recruitment.visit(-80, 50, null));
	}

	@Test
	void refusingMeansTheHunt() {
		assertEquals(Recruitment.Visit.HUNT, Recruitment.visit(-80, 0, Recruitment.REFUSED));
		// Le karma remonté, plus de traque, sauf pour qui porte une Pierre vivante.
		assertEquals(Recruitment.Visit.NONE, Recruitment.visit(-20, 0, Recruitment.REFUSED));
		assertEquals(Recruitment.Visit.HUNT, Recruitment.visit(-20, 5, Recruitment.REFUSED));
	}

	@Test
	void aRecruitIsLeftAlone() {
		assertEquals(Recruitment.Visit.NONE, Recruitment.visit(-100, 0, Recruitment.ACCEPTED));
		assertEquals(Recruitment.Visit.NONE, Recruitment.visit(-100, 80, Recruitment.ACCEPTED));
	}

	@Test
	void othersAreHuntedOnlyForTheirStone() {
		assertEquals(Recruitment.Visit.NONE, Recruitment.visit(Karma.RECRUITABLE + 1, 0, null));
		assertEquals(Recruitment.Visit.HUNT, Recruitment.visit(0, 1, null));
	}

	@Test
	void silenceAndDistanceMeanNo() {
		assertTrue(Recruitment.stillWaiting(0, 5));
		assertTrue(Recruitment.stillWaiting(Recruitment.PATIENCE - 1, Recruitment.WALK_AWAY));
		assertFalse(Recruitment.stillWaiting(Recruitment.PATIENCE, 5));
		assertFalse(Recruitment.stillWaiting(10, Recruitment.WALK_AWAY + 1));
	}
}
