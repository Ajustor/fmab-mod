package com.ajustor.fmab.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Un délai par joueur, tenu par le serveur : un client qui insiste n'y gagne rien. */
class ThrottleTest {
	private final UUID player = UUID.randomUUID();

	@Test
	void theFirstActionPasses() {
		assertTrue(new Throttle(10).pass(player, 100));
	}

	@Test
	void anActionWithinTheDelayIsRefused() {
		Throttle throttle = new Throttle(10);
		throttle.pass(player, 100);
		assertFalse(throttle.pass(player, 101));
		assertFalse(throttle.pass(player, 109));
	}

	@Test
	void aRefusedActionDoesNotPushTheDelayBack() {
		Throttle throttle = new Throttle(10);
		throttle.pass(player, 100);
		throttle.pass(player, 105);
		assertTrue(throttle.pass(player, 110));
	}

	@Test
	void eachPlayerHasTheirOwnDelay() {
		Throttle throttle = new Throttle(10);
		throttle.pass(player, 100);
		assertTrue(throttle.pass(UUID.randomUUID(), 101));
	}

	@Test
	void aClockThatWentBackCountsAsElapsed() {
		Throttle throttle = new Throttle(10);
		throttle.pass(player, 1_000_000);
		assertTrue(throttle.pass(player, 5), "un autre monde, en solo, dont l'horloge est en retard");
	}

	@Test
	void aLeavingPlayerIsForgotten() {
		Throttle throttle = new Throttle(10);
		throttle.pass(player, 100);
		PlayerMaps.forget(player);
		assertTrue(throttle.pass(player, 101));
	}
}
