package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Critère de la v0.1 : un joueur doit pouvoir faire marcher un cercle avec le seul Traité. Chaque
 * cercle d'exemple doit donc fonctionner pour un Apprenti qui a étudié les glyphes de son rang.
 */
class TreatiseTest {
	static Treatise load() {
		try {
			return Treatise.parse(JsonParser.parseString(Files.readString(
					TestGlyphs.resources().resolve("assets/fmab/treatise/treatise.json"))).getAsJsonObject());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Chaque exemple fonctionne pour le rang qu'il demande, avec les seuls glyphes de ce rang ; et
	 * il y a au moins six exemples à la portée d'un Apprenti.
	 */
	@Test
	void everyExampleWorksForTheRankItNeedsOnceTheGlyphsAreStudied() {
		CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
		CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());
		int apprentice = 0;
		for (Treatise.Chapter chapter : load().chapters()) {
			for (Treatise.Page page : chapter.pages()) {
				if (page instanceof Treatise.ExamplePage example) {
					Rank rank = analyzer.analyze(parser.parse(example.drawing()), Rank.GATE, null).requiredRank();
					assertTrue(Rank.ALCHEMIST.atLeast(rank), () -> example.textKey() + " demande " + rank);
					Set<String> studied = TestGlyphs.all().values().stream()
							.filter(g -> rank.atLeast(g.rank()))
							.map(Glyph::id)
							.collect(Collectors.toSet());
					Analysis a = analyzer.analyze(parser.parse(example.drawing()), rank, studied);
					assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> example.textKey() + " : " + a.issues());
					if (rank == Rank.APPRENTICE) {
						apprentice++;
					}
				}
			}
		}
		assertTrue(apprentice >= 6, "le Traité doit montrer chaque combinaison de départ");
	}

	@Test
	void theCatalogueIsInTheTreatise() {
		assertTrue(load().chapters().stream()
				.flatMap(c -> c.pages().stream())
				.anyMatch(p -> p instanceof Treatise.GlyphCatalogue));
	}
}
