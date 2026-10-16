package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.AlchemistData;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Les données d'alchimiste : la concentration n'en fait plus partie, les anciennes sauvegardes se lisent encore. */
class AlchemistDataTest {
	@Test
	void anOldSaveWithItsConcentrationStillLoads() {
		String old = """
				{"rank": "state", "known": ["earth", "circle"], "concentration": 12.5,
				 "mastery": {"earth": 7}, "granted": [], "familiarity": {"fire": 2}}""";
		AlchemistData data = AlchemistData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(old)).getOrThrow();
		assertEquals(Rank.STATE, data.rank());
		assertEquals(Set.of("earth", "circle"), data.known());
		assertEquals(Map.of("earth", 7), data.mastery());
		assertEquals(2, data.uses("fire"));
	}

	@Test
	void theSaveNoLongerCarriesTheConcentration() {
		var json = AlchemistData.CODEC.encodeStart(JsonOps.INSTANCE, AlchemistData.NEW.learn("earth")).getOrThrow();
		assertEquals(false, json.getAsJsonObject().has("concentration"));
	}
}
