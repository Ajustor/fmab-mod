package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.training.Trial;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les épreuves d'Izumi : chacune à la portée du rang qu'elle annonce, et expliquée. */
class TrialTest {
	/**
	 * Un Apprenti ne tient pas deux étages : même le plus simple cercle du Traité qui en a deux
	 * (le plan B) rebondit entre ses mains, et coûte plus de concentration qu'il n'en a. L'épreuve
	 * attend donc l'Alchimiste, qui le lance sans peine.
	 */
	@Test
	void twoStagesWaitForTheAlchemist() {
		assertEquals(Rank.ALCHEMIST, Trial.TWO_STAGES.rank());
		var drawing = TreatiseTest.load().chapters().stream().flatMap(c -> c.pages().stream())
				.filter(p -> p instanceof Treatise.ExamplePage e && e.textKey().equals("treatise.fmab.ch5.fallback_wall"))
				.map(p -> ((Treatise.ExamplePage) p).drawing()).findFirst().orElseThrow();
		CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
		CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());
		Analysis apprentice = analyzer.analyze(parser.parse(drawing), Rank.APPRENTICE, null);
		assertEquals(2, apprentice.parsed().stages().size());
		assertTrue(apprentice.outcome() != Analysis.Outcome.WORKS
				|| apprentice.concentration() > Rank.APPRENTICE.maxConcentration(), "un Apprenti ne devrait pas le tenir");
		Analysis alchemist = analyzer.analyze(parser.parse(drawing), Rank.ALCHEMIST, null);
		assertEquals(Analysis.Outcome.WORKS, alchemist.outcome());
		assertTrue(alchemist.concentration() <= Rank.ALCHEMIST.maxConcentration());
	}

	/** Toutes les autres épreuves sont pour l'Apprenti, qui vient voir Izumi tôt. */
	@Test
	void theOtherTrialsAreForTheApprentice() {
		for (Trial trial : Trial.values()) {
			if (trial != Trial.TWO_STAGES) {
				assertEquals(Rank.APPRENTICE, trial.rank(), trial.id());
			}
		}
	}

	/** Chaque épreuve a son nom et ce qu'Izumi en dit au survol, en français et en anglais. */
	@Test
	void everyTrialIsExplained() throws IOException {
		for (String lang : new String[]{"fr_fr", "en_us"}) {
			var texts = JsonParser.parseString(Files.readString(
					TestGlyphs.resources().resolve("assets/fmab/lang/" + lang + ".json"))).getAsJsonObject();
			assertTrue(texts.has("entity.fmab.izumi.hover"), lang);
			for (Trial trial : Trial.values()) {
				assertTrue(texts.has(trial.translationKey()), lang + " : " + trial.translationKey());
				assertTrue(texts.has(trial.hintKey()), lang + " : " + trial.hintKey());
			}
		}
	}
}
