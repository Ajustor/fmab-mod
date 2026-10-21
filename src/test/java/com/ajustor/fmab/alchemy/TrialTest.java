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

/**
 * Les épreuves d'Izumi sont à la portée d'un Apprenti ; le cercle à deux étages, qu'il ne tient pas,
 * est une épreuve de l'examen d'État.
 */
class TrialTest {
	/**
	 * Un Apprenti ne tient pas deux étages : même le plus simple cercle du Traité qui en a deux
	 * (le plan B) rebondit entre ses mains, et coûte plus de concentration qu'il n'en a. Un Alchimiste,
	 * qui seul peut se présenter à l'examen d'État, le lance sans peine.
	 */
	@Test
	void twoStagesBelongToTheStateExam() {
		assertTrue(java.util.Arrays.stream(Trial.values()).noneMatch(t -> t.id().equals("two_stages")),
				"Izumi ne devrait plus demander de cercle à deux étages");
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

	/** L'examen garde la preuve des deux étages, même dans une sauvegarde d'avant. */
	@Test
	void theExamRemembersTheTwoStages() {
		var old = com.ajustor.fmab.data.ExamProgress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
				JsonParser.parseString("{\"items_given\": false}")).getOrThrow();
		assertTrue(!old.twoStages());
		assertTrue(old.withTwoStages().twoStages() && !old.withTwoStages().itemsGiven());
		assertTrue(old.withTwoStages().withItemsGiven().twoStages());
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
