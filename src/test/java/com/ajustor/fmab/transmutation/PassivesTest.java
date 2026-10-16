package com.ajustor.fmab.transmutation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Les passifs des cercles portés : un même effet ne s'additionne pas sans fin. */
class PassivesTest {
	@Test
	void fourteenMendingCirclesHealNoMoreThanThree() {
		assertEquals(Passives.mending(Passives.MAX_PER_PASSIVE), Passives.mending(14));
		assertEquals(0.5f, Passives.mending(1));
		assertEquals(1.5f, Passives.mending(14));
	}

	@Test
	void eachPassiveIsCappedOnItsOwn() {
		Map<String, Integer> worn = Map.of("fmab:mending", 14, "fmab:swiftness", 2, "fmab:reinforce", 3);
		assertEquals(Map.of("fmab:mending", 3, "fmab:swiftness", 2, "fmab:reinforce", 3), Passives.capped(worn));
	}
}
