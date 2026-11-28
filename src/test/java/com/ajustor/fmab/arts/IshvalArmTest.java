package com.ajustor.fmab.arts;

import com.ajustor.fmab.alchemy.TestGlyphs;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Le motif d'Ishval se lit dans les cercles tatoués autour d'un bras : un cercle qui décompose fait la
 * moitié d'Amestris ; un cercle qui recompose, la moitié de Xing, mais seulement pour qui connaît
 * l'alkahestry. Les autres cercles n'y font rien.
 */
class IshvalArmTest {
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	private Analysis circle(String element, String action) {
		Glyph e = TestGlyphs.get(element);
		Glyph a = TestGlyphs.get(action);
		return analyzer.analyze(parser.parse(SimpleCircles.of(e, a, SimpleCircles.sidesFor(e, a))), Rank.GATE, null);
	}

	@Test
	void aDecomposingCircleIsScarsHalf() {
		assertEquals(Set.of(IshvalArm.Half.DECOMPOSE), IshvalArm.halves(List.of(circle("terre", "decomposer")), false));
		// N'importe quel élément : le bras de Scar défait tout.
		assertEquals(Set.of(IshvalArm.Half.DECOMPOSE), IshvalArm.halves(List.of(circle("fer", "decomposer")), false));
	}

	@Test
	void theRecomposingHalfOnlyReadsWithAlkahestry() {
		List<Analysis> xing = List.of(circle("terre", "recomposer"));
		assertEquals(Set.of(), IshvalArm.halves(xing, false));
		assertEquals(Set.of(IshvalArm.Half.RECOMPOSE), IshvalArm.halves(xing, true));
	}

	@Test
	void theWholeMotifHoldsBothHalves() {
		List<Analysis> arm = List.of(circle("terre", "decomposer"), circle("terre", "recomposer"));
		assertEquals(Set.of(IshvalArm.Half.DECOMPOSE, IshvalArm.Half.RECOMPOSE), IshvalArm.halves(arm, true));
	}

	@Test
	void otherCirclesAreOnlyCircles() {
		assertEquals(Set.of(), IshvalArm.halves(List.of(circle("terre", "fixer")), true));
		assertEquals(Set.of(), IshvalArm.halves(List.of(), true));
	}
}
