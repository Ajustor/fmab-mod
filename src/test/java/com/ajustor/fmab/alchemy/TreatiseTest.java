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
import java.util.List;
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

	/**
	 * Le Traité se dévoile au fil des progrès : un nouveau venu lit les bases (dont le catalogue et la
	 * conception des cercles), les chapitres d'Alchimiste et d'Alchimiste d'État attendent leur rang.
	 */
	@Test
	void theChaptersUnlockAsTheReaderProgresses() {
		List<Treatise.Chapter> chapters = load().chapters();
		List<String> open = chapters.stream()
				.filter(c -> c.unlock().met(Rank.APPRENTICE, 0))
				.map(Treatise.Chapter::titleKey)
				.toList();
		for (String chapter : new String[]{"treatise.fmab.ch1.title", "treatise.fmab.ch2.title",
				"treatise.fmab.ch3.title", "treatise.fmab.ch4.title", "treatise.fmab.ch5.title",
				"treatise.fmab.ch6.title", "treatise.fmab.design.title"}) {
			assertTrue(open.contains(chapter), chapter + " devrait s'ouvrir au nouveau venu");
		}
		assertTrue(open.size() < chapters.size(), "certains chapitres doivent attendre les progrès du lecteur");
		// Les débuts du Traité sont d'un seul tenant : rien de scellé avant le dernier chapitre ouvert des bases.
		assertTrue(chapters.subList(0, chapters.indexOf(chapter(chapters, "treatise.fmab.design.title")) + 1)
				.stream().allMatch(c -> c.unlock().equals(Treatise.Unlock.ALWAYS)), "les bases doivent rester ouvertes");

		assertEquals(Rank.ALCHEMIST, chapter(chapters, "treatise.fmab.stages.title").unlock().rank());
		assertEquals(Rank.STATE, chapter(chapters, "treatise.fmab.mastery.title").unlock().rank());
		// Qui a franchi la Porte en devient Initié : tout lui est ouvert, même sans maîtrise.
		Treatise.Unlock gate = chapter(chapters, "treatise.fmab.ch8.title").unlock();
		assertTrue(gate.met(Rank.GATE, gate.mastery()), "la Porte se lit au retour de la Porte");
		assertTrue(chapters.stream().allMatch(c -> c.unlock().met(Rank.GATE, Integer.MAX_VALUE)), "tout s'ouvre");
		// La maîtrise ouvre aussi : un Apprenti qui pratique lit les Tabous avant d'être Alchimiste.
		Treatise.Unlock taboos = chapter(chapters, "treatise.fmab.ch7.title").unlock();
		assertTrue(!taboos.met(Rank.APPRENTICE, taboos.mastery() - 1) && taboos.met(Rank.APPRENTICE, taboos.mastery()));
		assertTrue(taboos.mastery() < com.ajustor.fmab.alchemy.knowledge.Knowledge.ALCHEMIST_MASTERY);
	}

	/** Un chapitre conçu pour un rang ne montre pas d'exemple qu'il ne tienne pas. */
	@Test
	void aSealedChapterOnlyShowsCirclesItsReaderCanHold() {
		CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
		CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());
		for (Treatise.Chapter chapter : load().chapters()) {
			for (Treatise.Page page : chapter.pages()) {
				if (page instanceof Treatise.ExamplePage example && !chapter.unlock().equals(Treatise.Unlock.ALWAYS)) {
					Rank rank = analyzer.analyze(parser.parse(example.drawing()), Rank.GATE, null).requiredRank();
					assertTrue(chapter.unlock().rank().atLeast(rank), () -> example.textKey() + " demande " + rank);
				}
			}
		}
	}

	private static Treatise.Chapter chapter(List<Treatise.Chapter> chapters, String titleKey) {
		return chapters.stream().filter(c -> c.titleKey().equals(titleKey)).findFirst().orElseThrow();
	}

	@Test
	void theCatalogueIsInTheTreatise() {
		assertTrue(load().chapters().stream()
				.flatMap(c -> c.pages().stream())
				.anyMatch(p -> p instanceof Treatise.GlyphCatalogue));
	}

	/** Le catalogue se complète au fil du savoir : appris ou tracé une fois, sinon une page blanche. */
	@Test
	void theCatalogueFillsInAsYouLearn() throws IOException {
		assertTrue(Treatise.revealed("fmab:terre", Set.of("fmab:terre"), 0), "appris");
		assertTrue(Treatise.revealed("fmab:humain", Set.of(), 1), "tracé une fois");
		assertTrue(!Treatise.revealed("fmab:humain", Set.of("fmab:terre"), 0), "ni l'un ni l'autre");
		// Chaque page blanche dit où l'apprendre : un tome, et où le trouver.
		var fr = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("assets/fmab/lang/fr_fr.json"))).getAsJsonObject();
		var en = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("assets/fmab/lang/en_us.json"))).getAsJsonObject();
		for (String glyph : TestGlyphs.all().keySet()) {
			var tome = com.ajustor.fmab.item.Tomes.teaching(glyph);
			assertTrue(tome.isPresent(), glyph + " : aucun tome ne l'enseigne");
			String where = com.ajustor.fmab.item.Tomes.HIDDEN.contains(tome.get())
					? "treatise.fmab.unknown.where." + tome.get().titleKey().substring("tome.fmab.".length())
					: "treatise.fmab.unknown.where.library";
			assertTrue(fr.has(where) && en.has(where), glyph + " : " + where);
		}
	}

	/**
	 * Un nouveau venu sait quoi faire dès le premier chapitre : devenir Alchimiste (et à combien de
	 * maîtrise), aller tôt chez Izumi, passer l'examen, trouver le sang cristallisé.
	 */
	@Test
	void theFirstChapterShowsTheWay() throws IOException {
		assertTrue(load().chapters().getFirst().pages().contains(new Treatise.TextPage("treatise.fmab.ch1.path")),
				"les premiers pas doivent être au chapitre I");
		String mastery = Integer.toString(com.ajustor.fmab.alchemy.knowledge.Knowledge.ALCHEMIST_MASTERY);
		for (String lang : new String[]{"fr_fr", "en_us"}) {
			var texts = JsonParser.parseString(Files.readString(
					TestGlyphs.resources().resolve("assets/fmab/lang/" + lang + ".json"))).getAsJsonObject();
			String path = texts.get("treatise.fmab.ch1.path").getAsString();
			assertTrue(path.contains(mastery), lang + " : la maîtrise qui fait l'Alchimiste");
			assertTrue(path.contains("Izumi") && path.contains("Dublith"), lang + " : Izumi, tôt");
			assertTrue(path.contains("Central"), lang + " : l'examen");
			assertTrue(texts.get("exam.fmab.not_eligible").getAsString().contains(mastery), lang);
			for (String key : new String[]{"welcome.fmab.greeting", "trial.fmab.not_student",
					"item.fmab.crystallized_blood.where"}) {
				assertTrue(texts.has(key), lang + " : " + key);
			}
		}
	}
}
