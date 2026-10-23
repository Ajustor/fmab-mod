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
	void theMoonCrossesTheSkyTowardTheSunAllDayLong() {
		float dawn = Eclipse.separation(7 * DAY);
		float morning = Eclipse.separation(7 * DAY + 3000);
		assertEquals(Eclipse.DAWN_SEPARATION, dawn, 1e-6, "au lever, la lune attend le soleil sur sa route");
		assertTrue(morning > 0 && morning < dawn, "elle s'en approche toute la matinée");
		assertEquals(0, Eclipse.separation(7 * DAY + 6000), "elle le couvre à midi");
		float afternoon = Eclipse.separation(7 * DAY + 10000);
		assertTrue(afternoon < 0 && afternoon > -Eclipse.DAWN_SEPARATION, "elle ressort de l'autre côté");
	}

	@Test
	void theEclipseMoonIsOnlyThereByDayOnTheEclipseDay() {
		assertTrue(Float.isNaN(Eclipse.separation(6 * DAY + 3000)), "la veille");
		assertTrue(Float.isNaN(Eclipse.separation(7 * DAY + 18000)), "la nuit de l'éclipse");
		assertEquals(0, Eclipse.coverage(6 * DAY + 6000), "la veille à midi, le soleil est entier");
	}

	@Test
	void theSunIsBittenThenCoveredThenFreed() {
		assertEquals(0, Eclipse.coverage(7 * DAY + 2000), "à huit heures, la lune est encore loin");
		float bitten = Eclipse.coverage(7 * DAY + 4500);
		assertTrue(bitten > 0 && bitten < 1, "vers dix heures et demie, elle le mord : " + bitten);
		assertEquals(1, Eclipse.coverage(7 * DAY + 5000), "la totalité commence avec l'éclipse");
		assertEquals(1, Eclipse.coverage(7 * DAY + 7999), "et dure jusqu'à sa fin");
		float freed = Eclipse.coverage(7 * DAY + 8400);
		assertTrue(freed > 0 && freed < 1, "elle ressort peu à peu : " + freed);
		assertEquals(0, Eclipse.coverage(7 * DAY + 10000), "l'après-midi, le soleil est libre");
	}

	@Test
	void theCoverageNeverGoesBackWhileTheMoonApproaches() {
		float last = 0;
		for (long hour = 0; hour <= 5000; hour += 50) {
			float now = Eclipse.coverage(7 * DAY + hour);
			assertTrue(now >= last, "à " + hour);
			last = now;
		}
	}

	@Test
	void theNextEclipseIsAnnouncedAgain() {
		assertEquals(Eclipse.Announcement.BEGINS, Eclipse.announcement(15 * DAY + 5000, 7 * DAY + 5000));
	}
}
