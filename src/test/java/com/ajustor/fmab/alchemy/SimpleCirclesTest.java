package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.alchemy.rules.Combination;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Chaque combinaison à un seul élément a un cercle simple qui fonctionne : c'est lui que montrent
 * le carnet de départ et les fiches de glyphe du Traité.
 */
class SimpleCirclesTest {
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@TestFactory
	List<DynamicTest> everySingleElementCombinationHasAWorkingSimpleCircle() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Combination c : TestGlyphs.combinations().all()) {
			if (c.elements().size() != 1) {
				continue;
			}
			Glyph element = find(GlyphLayer.ELEMENT, c.elements().iterator().next()).orElseThrow();
			Glyph action = find(GlyphLayer.ACTION, c.action()).orElseThrow();
			tests.add(DynamicTest.dynamicTest(c.id(), () -> {
				var drawing = SimpleCircles.of(element, action, SimpleCircles.sidesFor(element, action));
				Analysis a = analyzer.analyze(parser.parse(drawing), Rank.GATE, null);
				assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> c.id() + " : " + a.issues());
				assertEquals(c.effect(), a.effects().getFirst().combination().effect());
			}));
		}
		return tests;
	}

	private static Optional<Glyph> find(GlyphLayer layer, String role) {
		return TestGlyphs.all().values().stream().filter(g -> g.is(layer, role)).findFirst();
	}
}
