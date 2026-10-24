package com.ajustor.fmab.stone;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.alchemy.ForbiddenCircles;
import com.ajustor.fmab.alchemy.TestGlyphs;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deux cercles interdits, deux usages : la transmutation humaine ouvre la Porte, le cercle de la Pierre
 * consume des vies sur le rayon qu'on lui donne et les condense en Pierres, ou dans un corps vivant.
 */
class StoneCircleTest {
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void theStoneAndTheGateHaveTheirOwnCircles() {
		Analysis stone = analyzer.analyze(parser.parse(ForbiddenCircles.PHILOSOPHER_STONE), Rank.ALCHEMIST, null);
		assertEquals(Analysis.Outcome.WORKS, stone.outcome(), () -> stone.issues().toString());
		assertEquals(ForbiddenCircles.PHILOSOPHER_STONE_EFFECT, stone.effects().getFirst().combination().effect());
		assertEquals(Rank.ALCHEMIST, stone.requiredRank(), "à la portée d'un Alchimiste");
		assertTrue(stone.concentration() <= Rank.ALCHEMIST.maxConcentration(), "et dans sa réserve de concentration : "
				+ stone.concentration());
		Analysis human = analyzer.analyze(parser.parse(ForbiddenCircles.HUMAN_TRANSMUTATION), Rank.STATE, null);
		assertEquals(ForbiddenCircles.HUMAN_TRANSMUTATION_EFFECT, human.effects().getFirst().combination().effect());
	}

	@Test
	void theRadiusGoesFromABlockToAWholeFarm() {
		assertEquals(1, StoneCircle.radius(0));
		assertEquals(24, StoneCircle.radius(24));
		assertEquals(StoneCircle.MAX_RADIUS, StoneCircle.radius(500));
	}

	@Test
	void livesBecomeSoulsAndSoulsBecomeStones() {
		assertEquals(10 * 12 + 4 * 3, Sacrifice.souls(10, 4), "douze par villageois, trois par bête");
		assertEquals(List.of(100, 32), Sacrifice.stones(132), "une Pierre pleine, et le reste");
		assertEquals(List.of(100, 100, 100), Sacrifice.stones(300));
	}
}
