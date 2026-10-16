package com.ajustor.fmab.progress;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** « Les sept péchés » : chacun vu tomber, ou Greed soumis par un pacte. */
class MilestonesTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void sevenFallenSinsAreSubdued() {
		assertTrue(Milestones.sevenSins(Set.copyOf(Milestones.SINS), false));
	}

	@Test
	void aLoyalAllyOfGreedNeedNotBetrayHim() {
		Set<String> withoutGreed = new HashSet<>(Milestones.SINS);
		withoutGreed.remove("greed");
		assertFalse(Milestones.sevenSins(withoutGreed, false));
		assertTrue(Milestones.sevenSins(withoutGreed, true));
	}

	@Test
	void thePactWithGreedDoesNotStandInForTheOthers() {
		Set<String> missingPride = new HashSet<>(Milestones.SINS);
		missingPride.remove("pride");
		missingPride.remove("greed");
		assertFalse(Milestones.sevenSins(missingPride, true));
	}
}
