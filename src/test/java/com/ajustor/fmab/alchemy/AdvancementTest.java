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
	/** Les étapes que le mod déclenche (voir Milestones, NationalCircle, FatherSealBlock, Transmutation). */
	private static final Set<String> MILESTONES = Set.of("transmuted", "rank_alchemist", "rank_state", "rank_gate",
			"izumi_met", "izumi_done", "soul_armor", "automail", "living_stone", "alkahestry", "briggs_ally",
			"seven_sins", "seal_opened", "crest_sealed", "circle_broken", "slain_lust", "slain_gluttony",
			"slain_envy", "slain_greed", "slain_sloth", "slain_wrath", "slain_pride", "slain_father", "fused",
			"greed_pact", "scar_tattoo", "cornello_exposed", "marcoh_notes", "hohenheim_met", "notebook_circle",
			"tome_read", "first_node", "gloves_worn", "tattoo", "chimera", "gold", "trial_first_wall",
			"trial_surface", "trial_two_stages", "trial_gloves", "trial_spar", "trial_island", "epilogue");

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

	/**
	 * L'arbre suit l'ordre du jeu : un progrès ne descend pas d'un autre qu'on peut ne jamais obtenir
	 * avant lui (sans quoi le jeu l'affiche hors de portée alors qu'on vient de l'obtenir).
	 */
	@Test
	void orderFollowsTheGame() throws IOException {
		Map<String, JsonObject> all = load();
		// La carte du Laboratoire 5 est la récompense de l'examen d'État ; Barry y garde l'entrée.
		assertTrue(descends(all, "visit_laboratory_5", "rank_state"));
		assertTrue(descends(all, "slain_barry", "visit_laboratory_5"));
		// Une Pierre se fait par le sacrifice, avec le sang des points de sang : nul besoin de Lust.
		assertTrue(descends(all, "philosopher_stone", "visit_blood_crest"));
		assertTrue(!descends(all, "philosopher_stone", "slain_lust"));
		// La Porte s'ouvre à qui tente la transmutation humaine, quel que soit son rang.
		assertTrue(!descends(all, "rank_gate", "rank_alchemist"));
		// L'examen se passe au QG de Central, où l'on peut se rendre dès le début ; Envy rôde partout.
		assertTrue(descends(all, "rank_state", "visit_central"));
		for (String anyTime : new String[]{"visit_central", "visit_devils_nest", "slain_envy"}) {
			assertTrue(!descends(all, anyTime, "rank_alchemist") && !descends(all, anyTime, "rank_state"), anyTime);
		}
	}

	/** {@code ancestor} est-il un parent, direct ou non, de {@code name} ? */
	private static boolean descends(Map<String, JsonObject> all, String name, String ancestor) {
		JsonObject adv = all.get("fmab:alchemist/" + name);
		while (adv != null && adv.has("parent")) {
			String parent = adv.get("parent").getAsString();
			if (parent.equals("fmab:alchemist/" + ancestor)) {
				return true;
			}
			adv = all.get(parent);
		}
		return false;
	}
}
