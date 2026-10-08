package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTest {
	private final List<KnowledgeNode> nodes = TestGlyphs.knowledgeNodes();
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@Test
	void nodesUnlockAtTheirMasteryOnceTheirParentIsThere() {
		Knowledge k = new Knowledge(nodes, Map.of("earth", 30), Set.of());
		assertTrue(k.has("fmab:earth/docile_stone"));
		assertTrue(k.has("fmab:earth/thick_wall"));
		assertFalse(k.has("fmab:earth/stone_lance"));
		assertFalse(k.has("fmab:metal/docile_iron"));
		assertEquals(1, k.perk("earth", "wall_thickness"));
		assertEquals(0, k.perk("metal", "wall_thickness"));
	}

	@Test
	void aNodeGrantedByABookDoesNotWaitForMastery() {
		Knowledge k = new Knowledge(nodes, Map.of(), Set.of("fmab:earth/stone_lance"));
		assertTrue(k.has("fmab:earth/stone_lance"));
	}

	@Test
	void everyParentExists() {
		Set<String> ids = new java.util.HashSet<>();
		nodes.forEach(n -> ids.add(n.id()));
		for (KnowledgeNode n : nodes) {
			n.parent().ifPresent(p -> assertTrue(ids.contains(p), () -> n.id() + " : parent inconnu " + p));
		}
		for (var c : TestGlyphs.combinations().all()) {
			c.requires().ifPresent(r -> assertTrue(ids.contains(r), () -> c.id() + " demande " + r));
		}
	}

	@Test
	void aStoneLanceNeedsItsNode() {
		Drawing lance = square("terre", "recomposer");
		Analysis without = analyzer.analyze(parser.parse(lance), Rank.ALCHEMIST, null,
				new Knowledge(nodes, Map.of(), Set.of()));
		assertEquals(Analysis.Outcome.INERT, without.outcome());
		assertTrue(without.issues().stream().anyMatch(i -> i.kind() == CircleIssue.Kind.KNOWLEDGE_MISSING));

		Analysis with = analyzer.analyze(parser.parse(lance), Rank.ALCHEMIST, null,
				new Knowledge(nodes, Map.of("earth", 45), Set.of()));
		assertEquals(Analysis.Outcome.WORKS, with.outcome(), () -> with.issues().toString());
		assertEquals("fmab:stone_lance", with.effects().getFirst().combination().effect());
	}

	@Test
	void practiceMakesCirclesCheaperAndLonger() {
		Drawing wall = square("terre", "fixer");
		Analysis novice = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null, Knowledge.NONE);
		Analysis master = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null,
				new Knowledge(nodes, Map.of("earth", 70), Set.of()));
		assertEquals(novice.concentration() - 1, master.concentration());
		assertEquals(novice.effects().getFirst().range() + 1, master.effects().getFirst().range(), 1e-9);
	}

	@Test
	void anApprenticeBecomesAnAlchemistAfterEnoughPractice() {
		int effects = (int) Math.ceil((double) Knowledge.ALCHEMIST_MASTERY / Knowledge.MASTERY_PER_EFFECT);
		assertTrue(effects >= 10 && effects <= 40, "ni trop vite ni trop lentement : " + effects + " transmutations");
	}

	/** Un élément en haut, une action en bas, dans un carré. */
	private static Drawing square(String element, String action) {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		all.add(new Primitive.Polygon(List.of(new Vec2(16, 2), new Vec2(30, 16), new Vec2(16, 30), new Vec2(2, 16))));
		all.addAll(TestGlyphs.drawn(element, new Vec2(16, 11), 3, 0));
		all.addAll(TestGlyphs.drawn(action, new Vec2(16, 20), 3, 0));
		return new Drawing(all);
	}
}
