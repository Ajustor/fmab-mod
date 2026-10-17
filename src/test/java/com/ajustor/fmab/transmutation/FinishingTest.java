package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.alchemy.TestGlyphs;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les finitions : chaque école sonne à sa façon, et le livre de recettes connaît toutes les recettes. */
class FinishingTest {
	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void eachSchoolSoundsLikeItsMatter() {
		Set<Object> sounds = new HashSet<>();
		for (String school : new String[]{"earth", "metal", "water", "fire", "air", "life"}) {
			sounds.add(Transmutation.schoolSound(school));
		}
		assertEquals(6, sounds.size(), "six écoles, six sons");
		assertNotEquals(Transmutation.schoolSound("earth"), Transmutation.schoolSound("unknown"));
	}

	@Test
	void theRecipeBookKnowsEveryRecipe() throws IOException {
		Path data = TestGlyphs.resources().resolve("data/fmab");
		var rewards = JsonParser.parseString(Files.readString(data.resolve("advancement/recipes/all.json")))
				.getAsJsonObject().getAsJsonObject("rewards").getAsJsonArray("recipes");
		Set<String> book = new HashSet<>();
		rewards.forEach(r -> book.add(r.getAsString()));
		try (Stream<Path> recipes = Files.list(data.resolve("recipe"))) {
			for (Path r : recipes.toList()) {
				String id = "fmab:" + r.getFileName().toString().replace(".json", "");
				assertTrue(book.contains(id), id + " manque au livre de recettes");
			}
		}
	}
}
