package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.item.Tomes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** On comprend un glyphe grâce à un tome, ou à force de le tracer. */
class LearningTest {
	@Test
	void everyTomeTeachesExistingGlyphs() {
		for (Tome tome : Tomes.ALL) {
			assertFalse(tome.glyphs().isEmpty(), tome.titleKey());
			for (String id : tome.glyphs()) {
				assertTrue(TestGlyphs.all().containsKey(id), tome.titleKey() + " : glyphe inconnu " + id);
			}
		}
	}

	@Test
	void everyGlyphCanBeFoundInSomeTome() {
		Set<String> taught = new HashSet<>();
		Tomes.ALL.forEach(t -> taught.addAll(t.glyphs()));
		for (String id : TestGlyphs.all().keySet()) {
			assertTrue(taught.contains(id), id + " n'est enseigné par aucun tome");
		}
	}

	@Test
	void drawingAGlyphOftenEnoughTeachesIt() {
		AlchemistData data = AlchemistData.NEW;
		for (int i = 1; i < AlchemistData.USES_TO_LEARN; i++) {
			data = data.practiceGlyph("fmab:feu");
			assertEquals(i, data.uses("fmab:feu"));
			assertFalse(data.known().contains("fmab:feu"));
		}
		data = data.practiceGlyph("fmab:feu");
		assertTrue(data.known().contains("fmab:feu"));
		assertEquals(0, data.uses("fmab:feu"), "la familiarité d'un glyphe compris est oubliée");
		assertEquals(data, data.practiceGlyph("fmab:feu"), "un glyphe compris ne s'use plus");
	}
}
