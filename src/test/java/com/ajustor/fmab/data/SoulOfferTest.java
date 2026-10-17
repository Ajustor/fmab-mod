package com.ajustor.fmab.data;

import com.ajustor.fmab.gate.BodyPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sacrifier un membre pour lier l'âme d'un autre : le consentement de chacun, et un seul paiement. */
class SoulOfferTest {
	private static final UUID AL = UUID.randomUUID();
	private static final UUID NINA = UUID.randomUUID();

	@Test
	void onlyRemainingFleshCanBeOffered() {
		GateState oneArm = GateState.NONE.pay(BodyPart.RIGHT_ARM);
		assertFalse(SoulOffer.offerable(oneArm).contains(BodyPart.RIGHT_ARM), "un bras déjà pris ne s'offre plus");
		assertTrue(SoulOffer.offerable(oneArm).contains(BodyPart.LEFT_ARM));
		assertTrue(SoulOffer.offerable(GateState.NONE).containsAll(List.of(BodyPart.SIGHT, BodyPart.ORGANS)));
		assertEquals(List.of(), SoulOffer.offerable(GateState.NONE.pay(BodyPart.BODY)), "une âme n'a plus de chair à offrir");
		assertEquals(SoulOffer.start(), SoulOffer.start().choose(BodyPart.RIGHT_ARM, oneArm));
	}

	@Test
	void theOldestSoulIsAskedFirstAndARefusalPassesToTheNext() {
		SoulOffer offer = SoulOffer.start().choose(BodyPart.LEFT_ARM, GateState.NONE);
		assertEquals(Optional.of(NINA), offer.next(List.of(NINA, AL)));
		offer = offer.ask(NINA).answer(NINA, false);
		assertEquals(SoulOffer.Stage.ASKING, offer.stage());
		assertEquals(Optional.of(AL), offer.next(List.of(NINA, AL)), "on ne redemande pas une âme qui a refusé");
		assertEquals(Optional.empty(), offer.ask(AL).answer(AL, false).next(List.of(NINA, AL)));
	}

	@Test
	void onlyTheAskedSoulCanAccept() {
		SoulOffer asked = SoulOffer.start().choose(BodyPart.LEFT_ARM, GateState.NONE).ask(AL);
		assertEquals(asked, asked.answer(NINA, true));
		assertEquals(SoulOffer.Stage.CONFIRMING, asked.answer(AL, true).stage());
	}

	@Test
	void theDonorConfirmsLastAndPaysOnce() {
		SoulOffer accepted = SoulOffer.start().choose(BodyPart.LEFT_ARM, GateState.NONE).ask(AL).answer(AL, true);
		SoulOffer done = accepted.confirm();
		assertEquals(SoulOffer.Stage.DONE, done.stage());
		assertEquals(done, done.confirm(), "une réussite ne se rejoue pas");
		assertEquals(done, done.cancel(), "ni ne s'annule");
	}

	@Test
	void cancellingBeforeTheEndCostsNothing() {
		assertEquals(SoulOffer.Stage.CANCELLED, SoulOffer.start().cancel().stage());
		SoulOffer cancelled = SoulOffer.start().choose(BodyPart.LEFT_ARM, GateState.NONE).ask(AL).answer(AL, true).cancel();
		assertEquals(SoulOffer.Stage.CANCELLED, cancelled.stage());
		assertEquals(cancelled, cancelled.confirm(), "annulé, on ne paie plus");
	}
}
