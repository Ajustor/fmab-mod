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

	@Test
	void everyExampleWorksForAnApprenticeWhoStudiedTheTreatise() {
		CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
		CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());
		Set<String> apprenticeGlyphs = TestGlyphs.all().values().stream()
				.filter(g -> g.rank() == Rank.APPRENTICE)
				.map(Glyph::id)
				.collect(Collectors.toSet());
		int examples = 0;
		for (Treatise.Chapter chapter : load().chapters()) {
			for (Treatise.Page page : chapter.pages()) {
				if (page instanceof Treatise.ExamplePage example) {
					Analysis a = analyzer.analyze(parser.parse(example.drawing()), Rank.APPRENTICE, apprenticeGlyphs);
					assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> example.textKey() + " : " + a.issues());
					examples++;
				}
			}
		}
		assertTrue(examples >= 6, "le Traité doit montrer chaque combinaison de départ");
	}

	@Test
	void theCatalogueIsInTheTreatise() {
		assertTrue(load().chapters().stream()
				.flatMap(c -> c.pages().stream())
				.anyMatch(p -> p instanceof Treatise.GlyphCatalogue));
	}
}
