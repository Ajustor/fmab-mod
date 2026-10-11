package com.ajustor.fmab.gate;

import com.ajustor.fmab.alchemy.TestGlyphs;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** L'infobulle du noyau dit combien il en faut : le chiffre doit être celui de la recette. */
class StoneRecipeTest {
	@Test
	void theRecipeUsesAsManyCoresAsTheTooltipSays() throws IOException {
		JsonObject recipe = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("data/fmab/recipe/philosopher_stone.json"))).getAsJsonObject();
		String core = recipe.getAsJsonObject("key").entrySet().stream()
				.filter(e -> e.getValue().getAsString().equals("fmab:philosopher_stone_core"))
				.map(java.util.Map.Entry::getKey).findFirst().orElseThrow();
		long cores = recipe.getAsJsonArray("pattern").asList().stream()
				.mapToLong(row -> row.getAsString().chars().filter(c -> c == core.charAt(0)).count()).sum();
		assertEquals(Restoration.CORES_PER_STONE, cores);
	}
}
