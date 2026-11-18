package com.ajustor.fmab.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les boutons de la souris n'ont pas le même numéro d'une version à l'autre (le clic gauche vaut 0 en
 * 26.2, 1 en 26.3) : l'interface les compare toujours aux constantes d'{@code InputConstants}, jamais à
 * un nombre en dur.
 */
class MouseButtonsTest {
	private static final Pattern RAW_BUTTON = Pattern.compile(
			"(\\.button\\(\\)|\\.input\\(\\))\\s*[!=<>]=?\\s*\\d|GLFW_MOUSE_BUTTON");

	@Test
	void noScreenComparesAMouseButtonToARawNumber() throws IOException {
		List<String> offenders = new ArrayList<>();
		for (Path root : List.of(Path.of("src/client/java"), Path.of("fabric/src/client/java"))) {
			if (!Files.isDirectory(root)) {
				continue;
			}
			try (Stream<Path> files = Files.walk(root)) {
				for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
					List<String> lines = Files.readAllLines(file);
					for (int i = 0; i < lines.size(); i++) {
						if (RAW_BUTTON.matcher(lines.get(i)).find()) {
							offenders.add(root.relativize(file) + ":" + (i + 1) + " " + lines.get(i).strip());
						}
					}
				}
			}
		}
		assertEquals(List.of(), offenders, "un bouton de souris se compare à InputConstants.MOUSE_BUTTON_*");
	}
}
