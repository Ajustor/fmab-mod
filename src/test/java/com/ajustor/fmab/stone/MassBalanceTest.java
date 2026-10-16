package com.ajustor.fmab.stone;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La matière que la Pierre vivante tire des projectiles, au prorata : les fractions s'additionnent. */
class MassBalanceTest {
	/** Un silex, un bâton et une plume font quatre flèches. */
	private static final Map<String, Double> ARROW = Map.of("earth", 0.25, "wood", 0.25, "fiber", 0.25);

	@Test
	void fourArrowsGiveBackAFlintAStickAndAFeather() {
		MassBalance balance = new MassBalance();
		for (int i = 0; i < 3; i++) {
			balance.add(ARROW, 1);
			assertTrue(balance.takeWhole().isEmpty());
		}
		balance.add(ARROW, 1);
		assertEquals(Map.of("earth", 1, "wood", 1, "fiber", 1), balance.takeWhole());
	}

	@Test
	void aStackCountsAsMuchAsItsItems() {
		MassBalance balance = new MassBalance();
		balance.add(ARROW, 8);
		assertEquals(Map.of("earth", 2, "wood", 2, "fiber", 2), balance.takeWhole());
	}

	@Test
	void theFractionWaitsForTheNextOne() {
		MassBalance balance = new MassBalance();
		balance.add(Map.of("iron", 1.5), 1);
		assertEquals(Map.of("iron", 1), balance.takeWhole());
		assertEquals(0.5, balance.of("iron"), 1e-9);
		balance.add(Map.of("iron", 1.5), 1);
		assertEquals(Map.of("iron", 2), balance.takeWhole());
		assertEquals(0, balance.of("iron"), 1e-9);
	}

	@Test
	void threeThirdsMakeAWhole() {
		MassBalance balance = new MassBalance();
		for (int i = 0; i < 2; i++) {
			balance.add(Map.of("wood", 1.0 / 3), 1);
			assertTrue(balance.takeWhole().isEmpty());
		}
		balance.add(Map.of("wood", 1.0 / 3), 1);
		assertEquals(Map.of("wood", 1), balance.takeWhole(), "les arrondis du calcul ne coûtent pas une planche");
		assertTrue(balance.of("wood") >= 0);
	}

	@Test
	void eachElementIsCountedOnItsOwn() {
		MassBalance balance = new MassBalance();
		balance.add(Map.of("iron", 0.5), 1);
		balance.add(Map.of("gold", 0.5), 1);
		assertTrue(balance.takeWhole().isEmpty());
		balance.add(Map.of("iron", 0.5), 1);
		assertEquals(Map.of("iron", 1), balance.takeWhole());
		assertEquals(0.5, balance.of("gold"), 1e-9);
	}

	@Test
	void whatCouldNotBeGivenIsTakenAgainLater() {
		// Une planche pèse deux : une masse de bois seule ne fait pas de pièce, elle attend la suivante.
		MassBalance balance = new MassBalance();
		balance.add(Map.of("wood", 1.0), 1);
		assertEquals(Map.of("wood", 1), balance.takeWhole());
		balance.add("wood", 1);
		balance.add(Map.of("wood", 1.0), 1);
		assertEquals(Map.of("wood", 2), balance.takeWhole());
	}

	@Test
	void anEmptyBalanceGivesNothing() {
		MassBalance balance = new MassBalance();
		assertTrue(balance.takeWhole().isEmpty());
		assertEquals(0, balance.of("iron"), 1e-9);
	}
}
