package com.ajustor.fmab.gate;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'armure d'une âme : cabossée, jamais brisée, après que la Vérité a arraché son sceau ; sans
 * jambières, sans jambes.
 */
class SoulArmorTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void aFreshPieceComesBackHalfWorn() {
		assertEquals(120, SoulArmor.battered(0, 240));
	}

	@Test
	void aPieceAlreadyWornerKeepsItsWear() {
		assertEquals(200, SoulArmor.battered(200, 240));
	}

	@Test
	void theTollNeverBreaksAPiece() {
		assertEquals(239, SoulArmor.battered(239, 240));
		assertEquals(0, SoulArmor.battered(0, 1));
	}

	@Test
	void withoutLeggingsASoulHasNoLegs() {
		assertTrue(SoulArmor.legsMissing(true, false));
		assertFalse(SoulArmor.legsMissing(true, true), "des jambières valides lui rendent ses jambes");
	}

	@Test
	void onlyAnInhabitedArmourLosesItsLegs() {
		assertFalse(SoulArmor.legsMissing(false, false), "une âme errante, ou un joueur de chair, n'est pas concerné");
	}
}
