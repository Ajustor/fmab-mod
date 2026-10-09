package com.ajustor.fmab.alchemy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'arbre des progrès se tient : chaque parent existe, chaque étape nommée est une que le mod
 * déclenche, chaque icône du mod existe, chaque titre et description est traduit.
 */
class AdvancementTest {
	/** Les étapes que le mod déclenche (voir Milestones, NationalCircle, FatherSealBlock). */
	private static final Set<String> MILESTONES = Set.of("transmuted", "rank_alchemist", "rank_state", "rank_gate",
			"izumi_met", "izumi_done", "soul_armor", "automail", "living_stone", "alkahestry", "briggs_ally",
			"seven_sins", "seal_opened", "crest_sealed", "circle_broken", "slain_lust", "slain_gluttony",
			"slain_envy", "slain_greed", "slain_sloth", "slain_wrath", "slain_pride", "slain_father");

	private static Map<String, JsonObject> load() throws IOException {
		Path dir = TestGlyphs.resources().resolve("data/fmab/advancement/alchemist");
		Map<String, JsonObject> out = new HashMap<>();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path f : files.toList()) {
				out.put("fmab:alchemist/" + f.getFileName().toString().replace(".json", ""),
						JsonParser.parseString(Files.readString(f)).getAsJsonObject());
			}
		}
		return out;
	}

	@Test
	void treeHolds() throws IOException {
		Map<String, JsonObject> all = load();
		JsonObject fr = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("assets/fmab/lang/fr_fr.json"))).getAsJsonObject();
		JsonObject en = JsonParser.parseString(Files.readString(
				TestGlyphs.resources().resolve("assets/fmab/lang/en_us.json"))).getAsJsonObject();
		for (Map.Entry<String, JsonObject> e : all.entrySet()) {
			JsonObject adv = e.getValue();
			if (adv.has("parent")) {
				assertTrue(all.containsKey(adv.get("parent").getAsString()), e.getKey() + " : parent inconnu");
			}
			for (Map.Entry<String, com.google.gson.JsonElement> c : adv.getAsJsonObject("criteria").entrySet()) {
				JsonObject crit = c.getValue().getAsJsonObject();
				if (crit.get("trigger").getAsString().equals("fmab:milestone")) {
					String m = crit.getAsJsonObject("conditions").get("milestone").getAsString();
					assertTrue(MILESTONES.contains(m), e.getKey() + " : étape inconnue " + m);
				}
			}
			JsonObject display = adv.getAsJsonObject("display");
			String icon = display.getAsJsonObject("icon").get("id").getAsString();
			if (icon.startsWith("fmab:")) {
				assertTrue(Files.exists(TestGlyphs.resources().resolve("assets/fmab/items/" + icon.substring(5) + ".json")),
						e.getKey() + " : icône inconnue " + icon);
			}
			for (String part : new String[]{"title", "description"}) {
				String key = display.getAsJsonObject(part).get("translate").getAsString();
				assertTrue(fr.has(key) && en.has(key), e.getKey() + " : clé non traduite " + key);
			}
		}
	}
}
