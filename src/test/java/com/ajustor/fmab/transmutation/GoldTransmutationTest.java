package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** L'or interdit : pas plus d'un bloc par transmutation, et la loi frappe selon la quantité. */
class GoldTransmutationTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void noMoreThanABlockOfGoldAtOnce() {
		assertEquals(1, GoldTransmutation.convertible(64, 81, GoldTransmutation.MAX_NUGGETS), "un bloc sur soixante-quatre");
		assertEquals(9, GoldTransmutation.convertible(64, 9, GoldTransmutation.MAX_NUGGETS), "neuf lingots");
		assertEquals(5, GoldTransmutation.convertible(5, 9, GoldTransmutation.MAX_NUGGETS), "cinq lingots, tous");
		assertEquals(0, GoldTransmutation.convertible(3, 81, 80), "plus de place pour un bloc");
	}

	@Test
	void theLawStrikesHarderTheMoreGoldYouMake() {
		assertEquals(-10, GoldTransmutation.karma(1), "une pépite");
		assertEquals(-11, GoldTransmutation.karma(9), "un lingot");
		assertEquals(-19, GoldTransmutation.karma(81), "un bloc");
	}
}
