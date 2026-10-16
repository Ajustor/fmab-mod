package com.ajustor.fmab.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les maps inscrites oublient un joueur qui part, et tout quand le serveur s'arrête. */
class PlayerMapsTest {
	@Test
	void aLeavingPlayerIsForgottenEverywhere() {
		Map<UUID, Long> cooldowns = PlayerMaps.track(new HashMap<>());
		Map<UUID, String> combos = PlayerMaps.track(new HashMap<>());
		UUID leaving = UUID.randomUUID(), staying = UUID.randomUUID();
		cooldowns.put(leaving, 1_000_000L);
		cooldowns.put(staying, 5L);
		combos.put(leaving, "combo");
		PlayerMaps.forget(leaving);
		assertEquals(Map.of(staying, 5L), cooldowns);
		assertTrue(combos.isEmpty());
	}

	@Test
	void stoppingTheServerEmptiesEverything() {
		Map<UUID, Long> cooldowns = PlayerMaps.track(new HashMap<>());
		cooldowns.put(UUID.randomUUID(), 1_000_000L);
		PlayerMaps.clear();
		assertTrue(cooldowns.isEmpty());
	}
}
