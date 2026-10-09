package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.data.AlchemistData;
import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.item.Tomes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

	/** Les bibliothécaires vendent les tomes comme des livres enchantés : les mêmes que dans le code. */
	@Test
	void librariansSellTheModsTomes() throws IOException {
		Path trades = TestGlyphs.resources().resolve("data/fmab/villager_trade/librarian");
		List<Path> files;
		try (Stream<Path> list = Files.list(trades)) {
			files = list.sorted().toList();
		}
		assertFalse(files.isEmpty());
		for (Path f : files) {
			JsonObject tome = JsonParser.parseString(Files.readString(f)).getAsJsonObject()
					.getAsJsonObject("gives").getAsJsonObject("components").getAsJsonObject("fmab:tome");
			Tome parsed = Tome.CODEC.parse(JsonOps.INSTANCE, tome).getOrThrow();
			assertTrue(Tomes.ALL.contains(parsed), f.getFileName() + " ne correspond à aucun tome du mod");
			String id = "fmab:librarian/" + f.getFileName().toString().replace(".json", "");
			boolean listed = false;
			for (int level = 1; level <= 5; level++) {
				Path tag = TestGlyphs.resources().resolve("data/minecraft/tags/villager_trade/librarian/level_" + level + ".json");
				listed |= Files.exists(tag) && Files.readString(tag).contains("\"" + id + "\"");
			}
			assertTrue(listed, id + " n'est proposé à aucun niveau de bibliothécaire");
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
