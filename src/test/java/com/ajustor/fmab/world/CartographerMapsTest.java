package com.ajustor.fmab.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les cartographes vendent des cartes vers les lieux du mod. Elles passent par {@code fmab:site_map} :
 * la carte d'exploration du jeu cherche un lieu unique chunk par chunk, en lisant le disque, et un
 * villageois qui préparait ses offres figeait le serveur de longues minutes.
 */
class CartographerMapsTest {
	private static final Path TRADES = Path.of("src/main/resources/data/fmab/villager_trade/cartographer");

	@Test
	void everyMapTradeOfTheModUsesTheSiteMap() throws IOException {
		List<Path> files;
		try (Stream<Path> listed = Files.list(TRADES)) {
			files = listed.sorted().toList();
		}
		assertFalse(files.isEmpty());
		for (Path file : files) {
			String text = Files.readString(file);
			assertFalse(text.contains("minecraft:exploration_map"), file + " : pas de recherche dans le monde");
			assertFalse(text.contains("search_radius"), file + " : pas de rayon de recherche");
			JsonObject map = null;
			for (JsonElement modifier : JsonParser.parseString(text).getAsJsonObject().getAsJsonArray("given_item_modifiers")) {
				if (modifier.getAsJsonObject().get("function").getAsString().equals("fmab:site_map")) {
					map = modifier.getAsJsonObject();
				}
			}
			assertTrue(map != null, file + " : une carte fmab:site_map");
			String name = file.getFileName().toString().replace("map_", "").replace(".json", "");
			assertEquals("fmab:on_" + name + "_maps", map.get("destination").getAsString(), file.toString());
		}
	}
}
