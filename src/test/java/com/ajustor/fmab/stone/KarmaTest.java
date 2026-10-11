package com.ajustor.fmab.stone;

import net.minecraft.world.entity.ai.gossip.GossipType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le karma : ses bornes, ses paliers, ce qu'en disent les villageois. */
class KarmaTest {
	@Test
	void staysBetweenItsBounds() {
		assertEquals(10, Karma.shifted(0, 10));
		assertEquals(Karma.MAX, Karma.shifted(95, 20));
		assertEquals(Karma.MIN, Karma.shifted(-95, -20));
		assertEquals(-3, Karma.shifted(5, -8));
	}

	@Test
	void onlyTiersAndSignChangesAreFelt() {
		assertFalse(Karma.crossesTier(3, 10));
		assertTrue(Karma.crossesTier(20, 26));
		assertTrue(Karma.crossesTier(-24, -25));
		// De part et d'autre de zéro, même palier entier : on le sent quand même.
		assertTrue(Karma.crossesTier(4, -4));
		assertTrue(Karma.crossesTier(0, -1));
		assertFalse(Karma.crossesTier(-30, -40));
	}

	@Test
	void villagersTalkPastTwentyFive() {
		assertNull(Karma.reputation(0));
		assertNull(Karma.reputation(24));
		assertNull(Karma.reputation(-24));
		assertEquals(GossipType.MINOR_POSITIVE, Karma.reputation(25));
		assertEquals(GossipType.MINOR_NEGATIVE, Karma.reputation(-25));
		assertEquals(GossipType.MINOR_NEGATIVE, Karma.reputation(-49));
		assertEquals(GossipType.MAJOR_NEGATIVE, Karma.reputation(-50));
		assertEquals(GossipType.MAJOR_NEGATIVE, Karma.reputation(Karma.MIN));
	}

	@Test
	void homunculiNoticeYouBeforeTheBottom() {
		assertTrue(Karma.RECRUITABLE > Karma.MIN && Karma.RECRUITABLE < 0);
	}
}
