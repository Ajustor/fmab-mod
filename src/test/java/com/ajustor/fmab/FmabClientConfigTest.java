package com.ajustor.fmab;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Les réglages du joueur : leur fichier, et la langue du mod posée sur celle du jeu. */
class FmabClientConfigTest {
	@Test
	void theModFollowsTheGameLanguageByDefault() {
		assertEquals(List.of(), FmabClientConfig.DEFAULT.overlay());
	}

	@Test
	void anotherLanguageSitsOnTheReferenceOne() {
		assertEquals(List.of("en_us", "fr_fr"), FmabClientConfig.DEFAULT.withLanguage("fr_fr").overlay(),
				"l'anglais d'abord : une clé pas encore traduite reste lisible");
		assertEquals(List.of("en_us"), FmabClientConfig.DEFAULT.withLanguage("en_us").overlay());
	}

	@Test
	void theFileRoundTrips() {
		FmabClientConfig config = new FmabClientConfig("fr_fr", false, true);
		assertEquals(config, FmabClientConfig.fromJson(config.toJson()));
	}

	@Test
	void missingKeysKeepTheirDefaults() {
		FmabClientConfig config = FmabClientConfig.fromJson(JsonParser.parseString("{\"item_hints\": false}").getAsJsonObject());
		assertEquals(FmabClientConfig.DEFAULT.withItemHints(false), config);
		assertEquals("", new FmabClientConfig(null, true, true).language());
	}
}
