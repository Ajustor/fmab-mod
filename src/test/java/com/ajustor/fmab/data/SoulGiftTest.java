package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ajustor.fmab.data.SoulGift.Payment.GATE;
import static com.ajustor.fmab.data.SoulGift.Payment.OWN_SOUL;
import static com.ajustor.fmab.data.SoulGift.Payment.STONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offrir une âme pour rendre à un allié ce que la Porte lui a pris : la logique des Elric. */
class SoulGiftTest {
	/** Un allié de chair, qui a laissé une jambe et un bras à la Porte. */
	private static final GateState ALLY = GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.RIGHT_ARM);
	/** Alphonse : une jambe perdue avant le corps, puis le corps ; son âme habite une armure. */
	private static final GateState AL = GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.BODY);
	private static final GateState DONOR = GateState.NONE;

	@Test
	void aFleshAllyGetsBackLimbsSightOrOrgans() {
		assertEquals(List.of(BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG), SoulGift.restorable(ALLY));
		assertEquals(SoulGift.start(), SoulGift.start().choose(BodyPart.SIGHT, OWN_SOUL, ALLY, DONOR, 0, false),
				"ce qui n'a pas été pris ne se rend pas");
	}

	@Test
	void aSoulInArmourOnlyGetsBackItsBody() {
		assertEquals(List.of(BodyPart.BODY), SoulGift.restorable(AL), "ni sa jambe : elle n'a plus de chair");
		assertEquals(List.of(), SoulGift.restorable(AL.withAdrift(true)), "une âme errante n'est pas dans le cercle");
	}

	@Test
	void onlyAWholeBodyIsWorthAGate() {
		assertTrue(SoulGift.affordable(GATE, BodyPart.BODY, DONOR, 0, false), "le prix d'Edward pour Alphonse");
		assertFalse(SoulGift.affordable(GATE, BodyPart.LEFT_LEG, DONOR, 0, false), "on ne donne pas sa Porte pour une jambe");
		assertFalse(SoulGift.affordable(GATE, BodyPart.BODY, DONOR, 0, true), "une Porte déjà donnée ne se redonne pas");
	}

	@Test
	void theStoneMustHoldEnoughSouls() {
		assertFalse(SoulGift.affordable(STONE, BodyPart.LEFT_LEG, DONOR, 19, false));
		assertTrue(SoulGift.affordable(STONE, BodyPart.LEFT_LEG, DONOR, 20, false), "une jambe : vingt âmes");
		assertFalse(SoulGift.affordable(STONE, BodyPart.BODY, DONOR, 59, false));
		assertTrue(SoulGift.affordable(STONE, BodyPart.BODY, DONOR, 60, false), "un corps : soixante âmes");
		assertFalse(SoulGift.affordable(OWN_SOUL, BodyPart.LEFT_LEG, AL, 0, false), "une âme sans corps n'a plus d'âme à donner");
	}

	@Test
	void withTheStoneTheAllysConsentIsEnough() {
		SoulGift asked = SoulGift.start().choose(BodyPart.LEFT_LEG, STONE, ALLY, DONOR, 100, false);
		assertEquals(SoulGift.Stage.ASKING, asked.stage());
		assertEquals(SoulGift.Stage.DONE, asked.answer(true).stage());
		assertEquals(SoulGift.Stage.CANCELLED, asked.answer(false).stage(), "l'allié refuse : rien n'est payé");
	}

	@Test
	void givingOnesSoulOrGateIsConfirmedTwiceAndPaidOnce() {
		for (SoulGift.Payment by : List.of(OWN_SOUL, GATE)) {
			SoulGift accepted = SoulGift.start().choose(BodyPart.BODY, by, AL, DONOR, 0, false).answer(true);
			assertEquals(SoulGift.Stage.CONFIRMING, accepted.stage(), "le donneur confirme une seconde fois");
			SoulGift done = accepted.confirm();
			assertEquals(SoulGift.Stage.DONE, done.stage());
			assertEquals(done, done.confirm());
			assertEquals(done, done.cancel());
			assertEquals(SoulGift.Stage.CANCELLED, accepted.cancel().stage(), "renoncer avant la fin ne coûte rien");
		}
	}
}
