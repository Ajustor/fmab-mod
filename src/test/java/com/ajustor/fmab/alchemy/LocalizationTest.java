package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.KnowledgeNode;
import com.ajustor.fmab.alchemy.rules.Combination;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Aucun texte en dur : chaque clé utilisée existe, en français comme en anglais. */
class LocalizationTest {
	private static Set<String> keys(String lang) throws IOException {
		JsonObject json = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("assets/fmab/lang/" + lang + ".json"))).getAsJsonObject();
		return new TreeSet<>(json.keySet());
	}

	@Test
	void frenchAndEnglishHaveTheSameKeys() throws IOException {
		assertEquals(keys("fr_fr"), keys("en_us"));
	}

	@Test
	void everyKeyTheModUsesIsTranslated() throws IOException {
		Set<String> keys = keys("fr_fr");
		List<String> needed = new ArrayList<>();
		for (Glyph g : TestGlyphs.all().values()) {
			needed.add(g.nameKey());
			needed.add(g.descriptionKey());
		}
		for (Rank r : Rank.values()) {
			needed.add(r.translationKey());
		}
		for (CircleIssue.Kind k : CircleIssue.Kind.values()) {
			needed.add(k.translationKey());
		}
		for (Combination c : TestGlyphs.combinations().all()) {
			needed.add("effect." + c.effect().replace(':', '.'));
		}
		for (Analysis.Outcome o : Analysis.Outcome.values()) {
			needed.add("notebook.fmab.outcome." + o.name().toLowerCase(Locale.ROOT));
		}
		for (String kind : new String[]{"items", "blockstates"}) {
			String prefix = kind.equals("items") ? "item.fmab." : "block.fmab.";
			try (var files = Files.list(TestGlyphs.resources().resolve("assets/fmab/" + kind))) {
				files.forEach(f -> needed.add(prefix + f.getFileName().toString().replace(".json", "")));
			}
		}
		for (KnowledgeNode n : TestGlyphs.knowledgeNodes()) {
			needed.add(n.nameKey());
			needed.add(n.descriptionKey());
			needed.add("knowledge.fmab.school." + n.school());
		}
		for (Combination c : TestGlyphs.combinations().all()) {
			needed.add("knowledge.fmab.school." + c.school());
		}
		for (LinkKind k : LinkKind.values()) {
			needed.add("notebook.fmab.link." + k.name().toLowerCase(Locale.ROOT));
		}
		for (GlyphLayer l : GlyphLayer.values()) {
			needed.add("treatise.fmab.layer." + l.name().toLowerCase(Locale.ROOT));
		}
		Treatise treatise = TreatiseTest.load();
		for (Treatise.Chapter c : treatise.chapters()) {
			needed.add(c.titleKey());
			for (Treatise.Page p : c.pages()) {
				switch (p) {
					case Treatise.TextPage t -> needed.add(t.textKey());
					case Treatise.ExamplePage e -> needed.add(e.textKey());
					case Treatise.GlyphCatalogue g -> {
					}
				}
			}
		}
		List<String> missing = needed.stream().filter(k -> !keys.contains(k)).toList();
		assertTrue(missing.isEmpty(), () -> "clés manquantes : " + missing);
	}
}
