package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.stone.Eclipse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'éclipse revient tous les huit jours, vers midi. */
class EclipseTest {
	private static final long DAY = 24000;

	@Test
	void theEighthDayAtNoonIsAnEclipse() {
		assertTrue(Eclipse.at(7 * DAY + 6000));
		assertTrue(Eclipse.at(15 * DAY + 5000));
	}

	@Test
	void otherDaysAndHoursAreNot() {
		assertFalse(Eclipse.at(6000), "le premier jour");
		assertFalse(Eclipse.at(6 * DAY + 6000), "la veille");
		assertFalse(Eclipse.at(7 * DAY + 4999), "trop tôt");
		assertFalse(Eclipse.at(7 * DAY + 8000), "trop tard");
		assertFalse(Eclipse.at(7 * DAY + 18000), "la nuit");
	}
}
