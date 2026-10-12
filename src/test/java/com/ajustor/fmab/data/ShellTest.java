package com.ajustor.fmab.data;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'armure d'une âme comme coquille : se figer, être revêtue, emporter son occupant, le relâcher. */
class ShellTest {
	private static final UUID WINRY = UUID.randomUUID();
	private static final UUID ED = UUID.randomUUID();

	@Test
	void theKeyFreezesThenWakesAnEmptyArmour() {
		Shell still = Shell.NONE.press();
		assertTrue(still.still());
		assertEquals(Shell.NONE, still.press());
	}

	@Test
	void onlyAStillEmptyArmourCanBePutOn() {
		assertFalse(Shell.NONE.canEnter(), "une âme éveillée ne se revêt pas");
		Shell worn = Shell.NONE.press().enter(WINRY);
		assertEquals(Optional.of(WINRY), worn.occupant());
		assertFalse(worn.canEnter(), "une seule place");
		assertEquals(worn, worn.enter(ED), "le second reste dehors");
	}

	@Test
	void whoeverWearsAStillArmourDrivesItAndMayLeave() {
		Shell worn = Shell.NONE.press().enter(WINRY);
		assertTrue(worn.worn());
		assertFalse(worn.trapped());
		Shell left = worn.leave();
		assertTrue(left.still() && left.occupant().isEmpty(), "sortie d'elle-même, l'armure reste figée et vide");
	}

	@Test
	void whenTheSoulMovesTheOccupantIsTrapped() {
		Shell trapped = Shell.NONE.press().enter(WINRY).move();
		assertTrue(trapped.trapped());
		assertFalse(trapped.canLeave());
		assertEquals(trapped, trapped.leave(), "prisonnier, on ne sort pas");
		assertEquals(trapped, trapped.move());
	}

	@Test
	void theKeyReleasesWhoeverIsInside() {
		assertEquals(Shell.NONE, Shell.NONE.press().enter(WINRY).move().press());
		assertEquals(Shell.NONE, Shell.NONE.press().enter(WINRY).press());
	}
}
