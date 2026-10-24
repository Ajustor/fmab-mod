package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'alchimie comme outil : chaque cercle du Traité qui abat, récolte, fait pousser, creuse, jette un
 * pont ou aplanit déclenche bien cet effet, au rang qu'il annonce.
 */
class ToolCirclesTest {
	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@Test
	void everyToolCircleOfTheTreatiseTriggersItsWork() {
		Map<String, String> tools = Map.of(
				"treatise.fmab.everyday.recycling", "fmab:fell",
				"treatise.fmab.everyday.tunnel", "fmab:decompose",
				"treatise.fmab.everyday.harvest", "fmab:harvest",
				"treatise.fmab.everyday.growth", "fmab:grow",
				"treatise.fmab.everyday.bridge", "fmab:bridge",
				"treatise.fmab.everyday.leveling", "fmab:level");
		tools.forEach((page, effect) -> {
			Analysis a = analyzer.analyze(parser.parse(example(page).drawing()), Rank.ALCHEMIST, null);
			assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> page + " : " + a.issues());
			assertEquals(effect, a.effects().getFirst().combination().effect(), page);
		});
	}

	@Test
	void theTunnelFollowsItsArrowAndApprenticesCanDigIt() {
		Analysis a = analyzer.analyze(parser.parse(example("treatise.fmab.everyday.tunnel").drawing()), Rank.APPRENTICE, null);
		assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertTrue(a.effects().getFirst().hasDirection(), "la flèche fait du puits un tunnel");
	}

	@Test
	void bridgesAndEarthworksAreAnAlchemistsWork() {
		for (String page : new String[]{"treatise.fmab.everyday.bridge", "treatise.fmab.everyday.leveling"}) {
			Rank rank = analyzer.analyze(parser.parse(example(page).drawing()), Rank.GATE, null).requiredRank();
			assertEquals(Rank.ALCHEMIST, rank, page + " : la fusion de deux éléments demande un hexagramme");
		}
	}

	/** Le cercle d'un joueur : le bûcheron dans un hexagone, deux points d'Intensité pour une zone de huit blocs. */
	@Test
	void anAmplifiedWoodcutterInAHexagonStillFellsTrees() {
		List<Primitive> out = new ArrayList<>(example("treatise.fmab.everyday.recycling").drawing().primitives());
		List<Vec2> hexagon = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			double a = Math.toRadians(-90 + 60 * i);
			hexagon.add(new Vec2(Math.round(16 + 14 * Math.cos(a)), Math.round(16 + 14 * Math.sin(a))));
		}
		out.set(1, new Primitive.Polygon(hexagon));
		out.add(new Primitive.Dot(new Vec2(2, 16)));
		out.add(new Primitive.Dot(new Vec2(30, 16)));
		Analysis a = analyzer.analyze(parser.parse(new Drawing(out)), Rank.ALCHEMIST, null);
		assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals("fmab:fell", a.effects().getFirst().combination().effect());
		assertEquals(8, a.effects().getFirst().range(), 1e-9, "deux points doublent la portée");
	}

	/**
	 * Le crochet de l'Objet sur l'anneau : le bûcheron ne recycle que les objets. Le glyphe pèse : il faut
	 * un pentagone, donc un Alchimiste.
	 */
	@Test
	void theItemGlyphRestrictsTheWoodcutterToItems() {
		Analysis a = analyzer.analyze(parser.parse(example("treatise.fmab.everyday.aims").drawing()), Rank.ALCHEMIST, null);
		assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		Analysis.StageEffect fell = a.effects().getFirst();
		assertEquals("fmab:fell", fell.combination().effect());
		assertEquals(java.util.Set.of("item"), fell.aims());
		assertTrue(fell.aimsItems() && !fell.aimsBlocks(), "les objets, pas les arbres");
		Analysis plain = analyzer.analyze(parser.parse(example("treatise.fmab.everyday.recycling").drawing()), Rank.APPRENTICE, null);
		assertTrue(plain.effects().getFirst().aimsItems() && plain.effects().getFirst().aimsBlocks(),
				"sans glyphe de cible, rien ne change : les deux");
	}

	private static Treatise.ExamplePage example(String key) {
		Optional<Treatise.ExamplePage> found = TreatiseTest.load().chapters().stream()
				.flatMap(c -> c.pages().stream())
				.filter(p -> p instanceof Treatise.ExamplePage e && e.textKey().equals(key))
				.map(p -> (Treatise.ExamplePage) p)
				.findFirst();
		return found.orElseThrow(() -> new AssertionError("pas d'exemple " + key));
	}
}
