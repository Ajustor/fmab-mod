package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.alchemy.TestGlyphs;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Élément + Décomposer brise aussi les minerais de cet élément : un cercle qui sait défaire le zinc
 * défait le minerai de zinc. Chaque élément qui a son minerai le déclare dans sa table d'échange.
 */
class OreDismantlingTest {
	/** Les éléments dont le monde (vanilla ou modé) a des minerais. */
	private static final Set<String> WITH_ORES = Set.of("iron", "copper", "gold", "silver", "tin", "lead", "zinc",
			"nickel", "aluminum", "netherite", "carbon", "crystal", "sulfur", "mercury");

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void anOreBreaksUnderACircleOfItsElement() {
		assertTrue(Recomposition.minable(Optional.of("zinc"), Set.of("zinc")));
		assertFalse(Recomposition.minable(Optional.of("zinc"), Set.of("iron")), "le fer ne touche pas au zinc");
		assertFalse(Recomposition.minable(Optional.empty(), Set.of("zinc")), "un minerai sans composition reste");
	}

	@Test
	void everyElementWithOresDismantlesThem() throws IOException {
		for (String element : WITH_ORES) {
			boolean decomposes = TestGlyphs.combinations().all().stream()
					.anyMatch(c -> c.elements().contains(element) && c.action().equals("decompose")
							&& c.effect().equals("fmab:dismantle"));
			assertTrue(decomposes, element + " + Décomposer devrait défaire ses minerais");
			var values = JsonParser.parseString(Files.readString(TestGlyphs.resources()
					.resolve("data/fmab/fmab/exchange/" + element + ".json"))).getAsJsonObject().getAsJsonObject("values");
			assertTrue(values.keySet().stream().anyMatch(k -> k.startsWith("#c:ores/") || k.endsWith("_ore")
					|| k.equals("minecraft:ancient_debris")), "la table d'échange de " + element + " devrait nommer ses minerais");
		}
	}
}
