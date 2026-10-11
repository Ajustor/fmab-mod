package com.ajustor.fmab.gate;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** L'armure d'une âme après que la Vérité a arraché son sceau : cabossée, jamais brisée. */
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
}
