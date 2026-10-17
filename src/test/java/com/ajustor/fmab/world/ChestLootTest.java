package com.ajustor.fmab.world;

import com.ajustor.fmab.TestBootstrap;
import com.ajustor.fmab.alchemy.TestGlyphs;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chaque lieu récompense l'effort par une table de butin qu'un pack peut changer : chaque coffre de
 * chaque plan en a une qui existe, et se pose dans le vide, sur du plein, sans percer un mur.
 */
class ChestLootTest {
	/** Les lieux qui n'avaient pas de coffre avant FMAB-58. */
	private static final Set<String> NEWLY_REWARDED = Set.of("central_library", "bradley_residence", "sloth_tunnel",
			"father_lair", "yock_island", "blood_crest", "resembool_house", "dublith_house");
	/** Les boss, et leur table de butin. */
	private static final List<String> BOSSES = List.of("lust", "gluttony", "envy", "greed", "sloth", "wrath", "pride",
			"father", "barry", "kimblee", "slicer");

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void everyChestHasALootTableAndAPlace() throws java.io.IOException {
		Set<String> rewarded = new TreeSet<>();
		for (Map.Entry<String, List<Blueprint.Plot>> entry : BlueprintsTest.plots().entrySet()) {
			Blueprint plan = Blueprints.get(entry.getKey());
			for (Blueprint.Plot plot : entry.getValue()) {
				for (Blueprint.Chest chest : plan.chests(plot)) {
					rewarded.add(entry.getKey());
					String path = chest.table().identifier().getPath();
					assertTrue(Files.exists(TestGlyphs.resources().resolve("data/fmab/loot_table/" + path + ".json")),
							entry.getKey() + " : table inconnue " + path);
					var at = chest.local();
					var here = plan.at(at.getX(), at.getY(), at.getZ(), plot);
					var below = plan.at(at.getX(), at.getY() - 1, at.getZ(), plot);
					assertTrue(below != null && below.isSolid(), entry.getKey() + " : coffre dans le vide en " + at);
					// Un coffre peut prendre la place d'un meuble (le tonneau du Devil's Nest), jamais d'un mur.
					assertTrue(here == null || !here.isSolid() || here.is(Blocks.BARREL),
							entry.getKey() + " : coffre dans un mur en " + at + " (" + here + ")");
				}
			}
		}
		for (String place : NEWLY_REWARDED) {
			assertTrue(rewarded.contains(place) || rewarded.contains(place + "_dressed"), place + " n'a pas de coffre");
		}
	}

	@Test
	void everyBossHasALootTable() {
		for (String boss : BOSSES) {
			assertTrue(Files.exists(TestGlyphs.resources().resolve("data/fmab/loot_table/entities/" + boss + ".json")),
					boss + " n'a pas de table de butin");
		}
	}
}
