package com.ajustor.fmab.alchemy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fabric range les registres de données d'un mod sous {@code data/<ns>/<ns du registre>/<registre>/},
 * et non sous {@code data/<ns>/<registre>/} comme les registres de Minecraft. Un fichier rangé au
 * mauvais endroit est ignoré sans un mot : en jeu, le registre est vide et aucun glyphe n'est
 * reconnu, alors que les tests (qui lisent le dépôt) passent.
 */
class DataLayoutTest {
	@Test
	void modRegistriesLiveUnderTheirNamespaceFolder() throws IOException {
		Path data = TestGlyphs.resources().resolve("data/fmab");
		for (String registry : new String[]{"glyph", "combination", "knowledge", "exchange"}) {
			Path dir = data.resolve("fmab").resolve(registry);
			try (Stream<Path> files = Files.walk(dir)) {
				assertTrue(files.anyMatch(f -> f.toString().endsWith(".json")), "aucun JSON dans " + dir);
			}
			assertTrue(Files.notExists(data.resolve(registry)), "data/fmab/" + registry + " serait ignoré par le jeu");
		}
	}
}
