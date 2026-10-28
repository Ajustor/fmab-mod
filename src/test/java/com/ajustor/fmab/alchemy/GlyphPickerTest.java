package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphPicker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le sélecteur de glyphes du carnet : on retrouve un glyphe sans faire défiler tous les autres. */
class GlyphPickerTest {
	private final List<Glyph> all = List.copyOf(TestGlyphs.all().values());
	/** Les noms français, accents compris, comme les affiche le carnet. */
	private final Map<String, String> names = Map.of("fmab:etain", "Étain", "fmab:decomposer", "Décomposer",
			"fmab:reparer", "Réparer", "fmab:netherite", "Nethérite");
	private final Function<Glyph, String> name = g -> names.getOrDefault(g.id(), g.id().substring(5));

	@Test
	void everyGlyphShowsWithoutFilter() {
		assertEquals(all.size(), GlyphPicker.filter(all, null, "", name, List.of()).size());
	}

	@Test
	void layerKeepsOnlyItsGlyphs() {
		List<Glyph> actions = GlyphPicker.filter(all, GlyphLayer.ACTION, "", name, List.of());
		assertTrue(!actions.isEmpty() && actions.size() < all.size());
		assertTrue(actions.stream().allMatch(g -> g.layer() == GlyphLayer.ACTION));
	}

	@Test
	void searchIgnoresAccentsAndCase() {
		assertEquals(List.of("fmab:etain"), ids(GlyphPicker.filter(all, null, "ÉTA", name, List.of())));
		assertEquals(List.of("fmab:decomposer"), ids(GlyphPicker.filter(all, null, " decomp ", name, List.of())));
	}

	@Test
	void namespaceDoesNotMatchEverything() {
		assertTrue(GlyphPicker.filter(all, null, "fmab", name, List.of()).isEmpty());
	}

	@Test
	void recentComeFirstThenByLayer() {
		List<Glyph> out = GlyphPicker.filter(all, null, "", name, List.of("fmab:fixer", "fmab:terre"));
		assertEquals(List.of("fmab:fixer", "fmab:terre"), ids(out.subList(0, 2)));
		List<GlyphLayer> layers = out.subList(2, out.size()).stream().map(Glyph::layer).toList();
		List<GlyphLayer> sorted = new ArrayList<>(layers);
		sorted.sort(null);
		assertEquals(sorted, layers);
	}

	@Test
	void rememberMovesToFrontAndStaysBounded() {
		List<String> recent = List.of();
		for (int i = 0; i < GlyphPicker.MAX_RECENT + 3; i++) {
			recent = GlyphPicker.remember(recent, "g" + i);
		}
		assertEquals(GlyphPicker.MAX_RECENT, recent.size());
		recent = GlyphPicker.remember(recent, "g5");
		assertEquals("g5", recent.getFirst());
		assertEquals(recent.size(), recent.stream().distinct().count());
	}

	private static List<String> ids(List<Glyph> glyphs) {
		return glyphs.stream().map(Glyph::id).toList();
	}
}
