package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.stone.Eclipse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L'éclipse revient tous les huit jours, vers midi. */
class EclipseTest {
	private static final long DAY = 24000;
	private static final long NEVER = Long.MIN_VALUE;

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

	@Test
	void theEclipseIsAnnouncedThatMorningThenWhenItBeginsAndEnds() {
		assertEquals(Eclipse.Announcement.TODAY, Eclipse.announcement(7 * DAY, NEVER));
		assertEquals(Eclipse.Announcement.BEGINS, Eclipse.announcement(7 * DAY + 5000, NEVER));
		assertEquals(Eclipse.Announcement.ENDS, Eclipse.announcement(7 * DAY + 8000, NEVER));
	}

	@Test
	void otherDaysAndHoursAnnounceNothing() {
		assertEquals(Eclipse.Announcement.NONE, Eclipse.announcement(6 * DAY, NEVER), "la veille");
		assertEquals(Eclipse.Announcement.NONE, Eclipse.announcement(6 * DAY + 5000, NEVER), "la veille à midi");
		assertEquals(Eclipse.Announcement.NONE, Eclipse.announcement(7 * DAY + 6000, NEVER), "en pleine éclipse");
	}

	@Test
	void aFrozenClockDoesNotRepeatTheAnnouncement() {
		assertEquals(Eclipse.Announcement.NONE, Eclipse.announcement(7 * DAY + 5000, 7 * DAY + 5000),
				"cycle jour/nuit figé pile au début de l'éclipse");
		assertEquals(Eclipse.Announcement.NONE, Eclipse.announcement(7 * DAY, 7 * DAY));
	}

	@Test
	void theNextEclipseIsAnnouncedAgain() {
		assertEquals(Eclipse.Announcement.BEGINS, Eclipse.announcement(15 * DAY + 5000, 7 * DAY + 5000));
	}
}
