package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.SimpleCircles;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.CombinationTable;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le moteur ouvert aux données : des jokers donnent un effet à tout élément, et un effet peut se
 * décrire en JSON ; le glyphe Créature ajoute ce qu'un cercle fait aux créatures.
 */
class DataEngineTest {
	private final CombinationTable table = TestGlyphs.combinations();
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(table, TestGlyphs.all().values());

	@Test
	void aWildcardGivesAnEffectToEveryElementWithoutItsOwnCombination() {
		Combination wood = table.find(Set.of("wood"), "fix").orElseThrow();
		assertEquals("fmab:matter_wall", wood.effect());
		assertEquals(Set.of("wood"), wood.elements(), "le joker appliqué à l'élément du cercle");
		assertTrue(wood.spec().isPresent());
		assertEquals("earth", wood.school());
		assertEquals("fmab:wall", table.find(Set.of("earth"), "fix").orElseThrow().effect(), "la combinaison propre l'emporte");
		assertTrue(table.find(Set.of("gold"), "fix").isEmpty(), "un élément écarté par except");
		assertTrue(table.find(Set.of("water", "earth"), "project").isEmpty(), "un joker ne vaut que pour un élément seul");
		assertEquals("fmab:repair", table.find(Set.of("tin"), "repair").orElseThrow().effect());
	}

	/** Avant les jokers, ces cercles rebondissaient : ils marchent tous, au rang qui tient leurs glyphes. */
	@TestFactory
	List<DynamicTest> everyMatterElementCanBeFixedProjectedAndRepaired() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Glyph element : TestGlyphs.all().values()) {
			if (element.layer() != GlyphLayer.ELEMENT || Set.of("air", "fire", "gold", "human", "water").contains(element.role())) {
				continue;
			}
			for (String action : new String[]{"fixer", "projeter", "reparer"}) {
				Glyph glyph = TestGlyphs.all().get("fmab:" + action);
				tests.add(DynamicTest.dynamicTest(element.role() + "+" + action, () -> {
					Drawing d = SimpleCircles.of(element, glyph, Math.max(SimpleCircles.sidesFor(element, glyph), 6));
					Analysis a = analyzer.analyze(parser.parse(d), Rank.GATE, null);
					assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
				}));
			}
		}
		return tests;
	}

	@Test
	void aDataEffectSurvivesTheTripToTheClient() {
		Combination wild = table.all().stream().filter(c -> c.id().equals("fmab:any_fix")).findFirst().orElseThrow();
		Combination back = CombinationTable.parse("", JsonParser.parseString(CombinationTable.toJson(wild).toString()).getAsJsonObject());
		assertEquals(wild.spec(), back.spec());
		assertEquals(wild.except(), back.except());
		assertTrue(back.wildcard());
		assertEquals(table.find(Set.of("wood"), "fix").orElseThrow().school(),
				back.forElement("wood", "earth").school(), "l'école du joker se décide à l'usage");
	}

	@Test
	void theCreatureGlyphAddsWithoutTakingAway() {
		Glyph water = TestGlyphs.all().get("fmab:eau");
		Glyph fix = TestGlyphs.all().get("fmab:fixer");
		List<Primitive> strokes = new ArrayList<>(SimpleCircles.of(water, fix, 4).primitives());
		strokes.addAll(SimpleCircles.place(TestGlyphs.all().get("fmab:creature"), new Vec2(30, 16), 2, 0));
		Analysis a = analyzer.analyze(parser.parse(new Drawing(strokes)), Rank.APPRENTICE, null);
		assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		Analysis.StageEffect ice = a.effects().getFirst();
		assertEquals("fmab:ice_platform", ice.combination().effect());
		assertTrue(ice.aimsEntities(), "l'œil est lu");
		assertTrue(ice.aimsBlocks() && ice.aimsItems(), "Créature n'ôte rien à la glace");
	}
}
