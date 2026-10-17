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
import com.ajustor.fmab.training.Trial;
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
		for (Trial trial : Trial.values()) {
			trial.nodes().forEach(n -> assertTrue(ids.contains(n), () -> trial + " accorde " + n));
		}
	}

	@Test
	void recomposingEarthNeedsNoNodeOnlyTheLanceDoes() {
		// Terre + Recomposer copie un modèle posé sur le cercle, sans savoir particulier : le cercle
		// tient. C'est la lance, sans modèle, qui demande son nœud (vérifié par l'effet).
		Drawing lance = square("terre", "recomposer");
		Analysis without = analyzer.analyze(parser.parse(lance), Rank.ALCHEMIST, null,
				new Knowledge(nodes, Map.of(), Set.of()));
		assertEquals(Analysis.Outcome.WORKS, without.outcome(), () -> without.issues().toString());
		assertTrue(without.issues().stream().noneMatch(i -> i.kind() == CircleIssue.Kind.KNOWLEDGE_MISSING));
		assertEquals("fmab:stone_lance", without.effects().getFirst().combination().effect());
	}

	@Test
	void practiceMakesCirclesCheaperAndLonger() {
		Drawing wall = square("terre", "fixer");
		Analysis novice = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null, Knowledge.NONE);
		Analysis master = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null,
				new Knowledge(nodes, Map.of("earth", 70), Set.of()));
		// La main exercée (70 points de Terre), puis le nœud qui retire un point.
		assertEquals(Math.round(novice.concentration() * (1 - 70 * Knowledge.SAVING_PER_MASTERY)) - 1,
				master.concentration());
		assertEquals(novice.effects().getFirst().range() + 1, master.effects().getFirst().range(), 1e-9);
	}

	@Test
	void everyUseMakesTheNextOneLessTiring() {
		Knowledge fresh = new Knowledge(List.of(), Map.of(), Set.of());
		assertEquals(1, fresh.practice("earth"), 1e-9);
		double previous = 1;
		for (int uses = 1; uses <= 100; uses++) {
			double now = new Knowledge(List.of(), Map.of("earth", uses * Knowledge.MASTERY_PER_EFFECT), Set.of())
					.practice("earth");
			assertTrue(now < previous, "le " + uses + "e usage devrait coûter moins que le précédent");
			previous = now;
		}
		assertEquals(0.5, previous, 1e-9, "cent transmutations : moitié prix");
		assertEquals(0.5, new Knowledge(List.of(), Map.of("earth", 10_000), Set.of()).practice("earth"), 1e-9,
				"jamais moins de la moitié");
		assertEquals(1, new Knowledge(List.of(), Map.of("earth", 300), Set.of()).practice("fire"), 1e-9,
				"chaque école s'apprend pour elle-même");
	}

	@Test
	void aPractisedHandHalvesTheCostOfACircle() {
		Drawing wall = square("terre", "fixer");
		Analysis novice = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null,
				new Knowledge(List.of(), Map.of(), Set.of()));
		Analysis master = analyzer.analyze(parser.parse(wall), Rank.APPRENTICE, null,
				new Knowledge(List.of(), Map.of("earth", 300), Set.of()));
		assertEquals(Math.max(1, Math.round(novice.concentration() * 0.5)), master.concentration());
		assertTrue(master.concentration() < novice.concentration());
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
