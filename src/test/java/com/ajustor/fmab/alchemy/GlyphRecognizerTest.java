package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlyphRecognizerTest {
	private final GlyphRecognizer recognizer = new GlyphRecognizer(TestGlyphs.all().values());

	@TestFactory
	List<DynamicTest> everyShapedGlyphIsRecognizedAtUsualSizesAndSlightTilts() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Glyph g : TestGlyphs.all().values()) {
			if (g.role().equals("intensity")) {
				continue;
			}
			String name = g.id().substring("fmab:".length());
			for (double size : new double[]{2.5, 3, 4, 5}) {
				for (double rot : new double[]{-10, 0, 10}) {
					tests.add(DynamicTest.dynamicTest(name + " taille " + size + " rotation " + rot, () -> {
						Optional<GlyphRecognizer.Match> m = recognizer.recognize(
								TestGlyphs.drawn(name, new Vec2(16, 16), size, rot));
						assertTrue(m.isPresent(), "non reconnu");
						assertEquals(g.id(), m.get().glyph().id());
					}));
				}
			}
		}
		return tests;
	}

	@Test
	void earthNeedsItsBarAndWaterHasNone() {
		assertEquals("fmab:eau", recognize(TestGlyphs.drawn("eau", new Vec2(10, 10), 3, 0)));
		assertEquals("fmab:terre", recognize(TestGlyphs.drawn("terre", new Vec2(10, 10), 3, 0)));
	}

	@Test
	void anUpsideDownWaterIsNotWater() {
		// Un triangle pointe en haut, c'est le Feu : il n'est pas encore dans le jeu.
		assertEquals("", recognize(TestGlyphs.drawn("eau", new Vec2(10, 10), 3, 180)));
	}

	@Test
	void aHandDrawnEarthOnTheGridIsRecognized() {
		List<Primitive> earth = List.of(
				new Primitive.Polygon(List.of(new Vec2(7, 8), new Vec2(13, 8), new Vec2(10, 13))),
				new Primitive.Line(new Vec2(8, 11), new Vec2(12, 11)));
		assertEquals("fmab:terre", recognize(earth));
	}

	@Test
	void aSquareOrALoneLineIsNoGlyph() {
		assertEquals("", recognize(List.of(new Primitive.Polygon(List.of(
				new Vec2(4, 4), new Vec2(8, 4), new Vec2(8, 8), new Vec2(4, 8))))));
		assertEquals("", recognize(List.of(new Primitive.Line(new Vec2(4, 4), new Vec2(9, 6)))));
	}

	@Test
	void anExtraStrokeSpoilsTheGlyph() {
		List<Primitive> salt = new ArrayList<>(TestGlyphs.drawn("fixer", new Vec2(16, 16), 4, 0));
		salt.add(new Primitive.Line(new Vec2(16, 12), new Vec2(16, 20)));
		assertEquals("", recognize(salt));
	}

	@Test
	void theArrowMayPointAnywhere() {
		assertEquals("fmab:direction", recognize(TestGlyphs.drawn("direction", new Vec2(16, 3), 1.5, 135)));
	}

	private String recognize(List<Primitive> strokes) {
		return recognizer.recognize(strokes).map(m -> m.glyph().id()).orElse("");
	}
}
