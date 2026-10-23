package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.Test;

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

	private static Treatise.ExamplePage example(String key) {
		Optional<Treatise.ExamplePage> found = TreatiseTest.load().chapters().stream()
				.flatMap(c -> c.pages().stream())
				.filter(p -> p instanceof Treatise.ExamplePage e && e.textKey().equals(key))
				.map(p -> (Treatise.ExamplePage) p)
				.findFirst();
		return found.orElseThrow(() -> new AssertionError("pas d'exemple " + key));
	}
}
