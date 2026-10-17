package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ajustor.fmab.data.SoulGift.Payment.OWN_SOUL;
import static com.ajustor.fmab.data.SoulGift.Payment.STONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offrir une âme pour rendre à un allié ce que la Porte lui a pris. */
class SoulGiftTest {
	private static final GateState ALLY = GateState.NONE.pay(BodyPart.LEFT_LEG).pay(BodyPart.BODY);
	private static final GateState DONOR = GateState.NONE;

	@Test
	void onlyWhatWasTakenCanBeGivenBack() {
		assertEquals(List.of(BodyPart.LEFT_LEG), SoulGift.restorable(ALLY), "pas le corps entier : une partie");
		assertEquals(SoulGift.start(), SoulGift.start().choose(BodyPart.RIGHT_ARM, OWN_SOUL, ALLY, DONOR, 0));
	}

	@Test
	void theStoneMustHoldEnoughSouls() {
		assertFalse(SoulGift.affordable(STONE, BodyPart.LEFT_LEG, DONOR, 19));
		assertTrue(SoulGift.affordable(STONE, BodyPart.LEFT_LEG, DONOR, 20), "une jambe : vingt âmes");
		assertFalse(SoulGift.affordable(OWN_SOUL, BodyPart.LEFT_LEG, GateState.NONE.pay(BodyPart.BODY), 0),
				"une âme sans corps n'a plus d'âme à donner");
	}

	@Test
	void withTheStoneTheAllysConsentIsEnough() {
		SoulGift asked = SoulGift.start().choose(BodyPart.LEFT_LEG, STONE, ALLY, DONOR, 100);
		assertEquals(SoulGift.Stage.ASKING, asked.stage());
		assertEquals(SoulGift.Stage.DONE, asked.answer(true).stage());
		assertEquals(SoulGift.Stage.CANCELLED, asked.answer(false).stage(), "l'allié refuse : rien n'est payé");
	}

	@Test
	void givingOnesOwnSoulIsConfirmedTwiceAndPaidOnce() {
		SoulGift accepted = SoulGift.start().choose(BodyPart.LEFT_LEG, OWN_SOUL, ALLY, DONOR, 0).answer(true);
		assertEquals(SoulGift.Stage.CONFIRMING, accepted.stage(), "le donneur confirme une seconde fois");
		SoulGift done = accepted.confirm();
		assertEquals(SoulGift.Stage.DONE, done.stage());
		assertEquals(done, done.confirm());
		assertEquals(done, done.cancel());
		assertEquals(SoulGift.Stage.CANCELLED, accepted.cancel().stage(), "renoncer avant la fin ne coûte rien");
	}
}
