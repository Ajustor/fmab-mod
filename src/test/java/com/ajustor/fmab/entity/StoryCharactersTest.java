package com.ajustor.fmab.entity;

import com.ajustor.fmab.alchemy.TestGlyphs;
import com.ajustor.fmab.entity.character.StoryCharacter;
import com.ajustor.fmab.entity.character.StoryCharacters;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chaque personnage de l'histoire a sa peau, son œuf, son nom et toutes ses répliques, en deux langues. */
class StoryCharactersTest {
	@Test
	void everyCharacterIsComplete() throws IOException {
		Path assets = TestGlyphs.resources().resolve("assets/fmab");
		for (String lang : new String[]{"fr_fr", "en_us"}) {
			JsonObject texts = JsonParser.parseString(Files.readString(assets.resolve("lang/" + lang + ".json")))
					.getAsJsonObject();
			for (StoryCharacter c : StoryCharacters.ALL) {
				assertTrue(texts.has("entity.fmab." + c.id()), lang + " : nom de " + c.id());
				assertTrue(texts.has("item.fmab." + c.id() + "_spawn_egg"), lang + " : œuf de " + c.id());
				for (int line = 0; line < c.lines(); line++) {
					assertTrue(texts.has(c.lineKey(line)), lang + " : " + c.lineKey(line));
				}
				assertFalse(texts.has(c.lineKey(c.lines())), lang + " : " + c.id() + " a plus de répliques que déclaré");
			}
		}
		for (StoryCharacter c : StoryCharacters.ALL) {
			assertTrue(Files.exists(assets.resolve("textures/entity/" + c.id() + ".png")), "peau de " + c.id());
			assertTrue(Files.exists(assets.resolve("textures/item/" + c.id() + "_spawn_egg.png")), "œuf de " + c.id());
			assertTrue(Files.exists(assets.resolve("items/" + c.id() + "_spawn_egg.json")), "modèle d'œuf de " + c.id());
		}
	}

	@Test
	void giantsAndLittleOnesHaveTheirSize() {
		assertTrue(StoryCharacters.scale("alphonse") > 1.2f, "Alphonse et sa grande armure");
		assertTrue(StoryCharacters.scale("armstrong") > 1, "le colosse Armstrong");
		assertTrue(StoryCharacters.scale("pinako") < 0.7f, "la toute petite Pinako");
		assertTrue(StoryCharacters.scale("hughes") == 1, "les autres font la taille d'un homme");
	}
}
