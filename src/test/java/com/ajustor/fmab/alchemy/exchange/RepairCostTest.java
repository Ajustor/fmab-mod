package com.ajustor.fmab.alchemy.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Comme à l'enclume, une unité de la matière qui répare rend un quart de la durabilité. */
class RepairCostTest {
	/** Un lingot de fer pèse 9, un diamant 64, une planche 2. */
	private static final int INGOT = 9;
	private static final int DIAMOND = 64;
	private static final int PLANK = 2;

	@Test
	void aQuarterOfWearCostsOneUnit() {
		assertEquals(INGOT, RepairCost.massFor(250, 1000, INGOT));
		assertEquals(4 * DIAMOND, RepairCost.massFor(1561, 1561, DIAMOND));
		assertEquals(PLANK, RepairCost.massFor(10, 59, PLANK));
	}

	@Test
	void theCostIsRoundedUpAndTheRepairDown() {
		assertEquals(1, RepairCost.massFor(1, 1000, INGOT), "la moindre usure coûte de la matière");
		assertEquals(250, RepairCost.restored(INGOT, 1000, INGOT));
		assertEquals(0, RepairCost.restored(1, 1000, INGOT * 1000), "trop peu de matière ne répare rien");
	}

	@Test
	void whatIsPaidRepairsAtLeastTheWear() {
		for (int unit : new int[]{1, PLANK, 4, INGOT, DIAMOND, 72}) {
			for (int max : new int[]{59, 131, 250, 1561, 2031}) {
				for (int damage = 1; damage < max; damage += 7) {
					int mass = RepairCost.massFor(damage, max, unit);
					assertEquals(true, RepairCost.restored(mass, max, unit) >= damage,
							"unité " + unit + ", usure " + damage + " sur " + max);
				}
			}
		}
	}

	@Test
	void nothingWornCostsNothing() {
		assertEquals(0, RepairCost.massFor(0, 1000, INGOT));
		assertEquals(0, RepairCost.restored(0, 1000, INGOT));
	}
}
