package com.ajustor.fmab.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La vue lointaine des dimensions du mod se décrit sans les générer : elle doit dire vrai, donc suivre
 * leurs fichiers (un plateau d'une couche, et quel bloc).
 */
class FlatDimensionsTest {
	private static final Path DIMENSIONS = Path.of("src/main/resources/data/fmab/dimension");

	@Test
	void everyDimensionOfTheModIsAFlatFloorOfTheListedBlock() throws IOException {
		Map<String, String> floors = new HashMap<>();
		try (Stream<Path> files = Files.list(DIMENSIONS)) {
			for (Path file : files.toList()) {
				JsonObject generator = JsonParser.parseString(Files.readString(file)).getAsJsonObject()
						.getAsJsonObject("generator");
				String id = "fmab:" + file.getFileName().toString().replace(".json", "");
				assertEquals("minecraft:flat", generator.get("type").getAsString(), id);
				JsonObject settings = generator.getAsJsonObject("settings");
				assertEquals(1, settings.getAsJsonArray("layers").size(), id + " : une seule couche");
				JsonObject layer = settings.getAsJsonArray("layers").get(0).getAsJsonObject();
				assertEquals(1, layer.get("height").getAsInt(), id + " : une couche d'un bloc");
				assertTrue(settings.getAsJsonArray("structure_overrides").isEmpty(), id + " : sans structure");
				floors.put(id, layer.get("block").getAsString());
			}
		}
		assertEquals(floors, FlatDimensions.all());
	}

	@Test
	void theOverworldIsNotOneOfThem() {
		assertTrue(FlatDimensions.floor("minecraft:overworld").isEmpty());
		assertEquals("fmab:belly_flesh", FlatDimensions.floor("fmab:gluttony_belly").orElseThrow());
	}
}
