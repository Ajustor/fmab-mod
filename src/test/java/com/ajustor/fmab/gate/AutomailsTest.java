package com.ajustor.fmab.gate;

import com.ajustor.fmab.item.AutomailItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'usure des automails : jusqu'où elle va, ce qu'use la marche, ce que coûte la réparation. */
class AutomailsTest {
	@Test
	void wearStopsAtTheLastPoint() {
		assertEquals(5, Automails.worn(3, 100, 2));
		assertEquals(99, Automails.worn(98, 100, 5));
		assertEquals(99, Automails.worn(99, 100, 1));
	}

	@Test
	void rushValleyLegsWearFasterBriggsSlower() {
		assertEquals(1, Automails.legWear(AutomailItem.Model.IRON, false));
		assertEquals(1, Automails.legWear(AutomailItem.Model.IRON, true));
		assertEquals(2, Automails.legWear(AutomailItem.Model.RUSH_VALLEY, false));
		assertEquals(1, Automails.legWear(AutomailItem.Model.BRIGGS, true));
		assertEquals(0, Automails.legWear(AutomailItem.Model.BRIGGS, false));
	}

	@Test
	void aWornOutPieceIsBrokenButStillThere() {
		int max = 400;
		assertFalse(Automails.broken(0, max));
		assertFalse(Automails.broken(max - 2, max));
		int worn = Automails.worn(0, max, max * 2);
		assertTrue(Automails.broken(worn, max));
		assertTrue(worn < max, "la pièce lâche mais ne disparaît pas");
	}

	@Test
	void repairCostsAnIngotPerQuarterOfWear() {
		int max = 400;
		assertEquals(0, Automails.repairCost(0, max));
		assertEquals(1, Automails.repairCost(1, max));
		assertEquals(1, Automails.repairCost(max / 4, max));
		assertEquals(2, Automails.repairCost(max / 2, max));
		assertEquals(Automails.REPAIR_STEPS, Automails.repairCost(max - 1, max));
	}
}
