package com.ajustor.fmab.world;

import com.ajustor.fmab.TestBootstrap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Chaque plan enregistré, balayé sur toute sa boîte aux tailles où les lieux le posent : aucune
 * exception, et ses coffres et ses créatures tombent dans la boîte. (Le contenu des coffres n'est pas
 * tiré : les objets n'ont leurs composants qu'une fois les données du serveur chargées.) Un plan qu'on ajoute sans lui
 * donner sa taille ici fait échouer le test.
 */
class BlueprintsTest {
	/** Les graines essayées : de quoi passer par plusieurs variantes de chaque plan. */
	private static final long[] SEEDS = {0, 1, 42, -7_331_208_841L};
	/** Les bourgs posent leurs pièces avec trois couches de terrassement. */
	private static final int HAMLET_GROUND = 3;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	/** Les tailles où chaque sorte est posée (voir les lieux qui la posent). */
	static Map<String, List<Blueprint.Plot>> plots() throws IOException {
		Map<String, List<Blueprint.Plot>> out = new HashMap<>();
		int city = 2 * Blueprints.CITY_RADIUS + 1;
		put(out, "central_ground", city, 24, city, 6);
		put(out, "central_roads", city, 4, city, 1);
		put(out, "central_walls", city, 16, city, 0);
		put(out, "central_fountain", 9, 5, 9, 1);
		put(out, "central_hq", 31, 14, 23, 1);
		put(out, "central_library", 21, 11, 15, 1);
		put(out, "bradley_residence", 15, Blueprints.houseHeight(2, 11), 11, 1);
		put(out, "bradley_residence_dressed", 15 + 2 * Dressing.MARGIN, Blueprints.houseHeight(2, 11),
				11 + 2 * Dressing.MARGIN, 1);
		int depth = -SlothTunnel.FLOOR;
		put(out, "sloth_tunnel", city, depth + 1, 7, depth);
		put(out, "father_lair", 33, 1, 41, -FatherLair.ROOM_FLOOR);
		put(out, "laboratory_5", Laboratory5Structure.SIZE_X, Laboratory5Structure.DEPTH + Laboratory5Structure.ABOVE,
				Laboratory5Structure.SIZE_Z, Laboratory5Structure.DEPTH);
		put(out, "devils_nest", DevilsNestStructure.SIZE_X, Blueprints.houseHeight(1, DevilsNestStructure.SIZE_Z),
				DevilsNestStructure.SIZE_Z, 0);
		put(out, "fort_briggs", FortBriggs.SIZE_X, FortBriggs.SIZE_Y, FortBriggs.SIZE_Z, 0);
		put(out, "blood_crest", BloodCrestStructure.SIZE, 6, BloodCrestStructure.SIZE, 0);
		put(out, "xing_pavilion", XingStructure.SIZE, 12, XingStructure.SIZE, 0);
		put(out, "xing_grounds", XingStructure.GROUNDS, 1, XingStructure.GROUNDS, 0);
		put(out, "xing_shrine", XingStructure.HALL, 8, XingStructure.HALL, 1);
		put(out, "xing_house", XingStructure.HALL, 8, XingStructure.HALL, 1);
		put(out, "yock_island", YockIslandStructure.SIZE, 10, YockIslandStructure.SIZE, 4);
		for (String stall : List.of("market_stall", "desert_stall")) {
			put(out, stall, 3, 5, 3, 0);
		}
		for (String tree : List.of("town_tree", "birch_tree", "acacia_tree", "palm_tree")) {
			put(out, tree, 5, 10, 5, HAMLET_GROUND);
		}
		for (String square : List.of("rush_valley_square", "dublith_square")) {
			put(out, square, 13, 5, 13, HAMLET_GROUND);
		}
		put(out, "resembool_field", 11, 5, 9, HAMLET_GROUND);
		put(out, "resembool_field", 9, 5, 11, HAMLET_GROUND);
		put(out, "resembool_pasture", 11, 5, 9, HAMLET_GROUND);
		// Les maisons, d'un ou deux étages ; habillées, elles prennent un bloc de marge tout autour.
		for (String house : List.of("central_house", "resembool_house", "rockbell_house", "elric_house",
				"rush_valley_workshop", "rush_valley_shop", "dublith_house", "curtis_butcher")) {
			for (int[] walls : new int[][]{{9, 7}, {8, 7}, {11, 9}}) {
				for (int floors = 1; floors <= 2; floors++) {
					int sy = Blueprints.houseHeight(floors, walls[1]);
					put(out, house, walls[0], sy, walls[1], HAMLET_GROUND);
					put(out, house + "_dressed", walls[0] + 2 * Dressing.MARGIN, sy, walls[1] + 2 * Dressing.MARGIN,
							HAMLET_GROUND);
				}
			}
		}
		// Les lieux d'un seul tenant : leur taille est dans le JSON de la structure.
		try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/fmab/worldgen/structure"))) {
			for (Path file : files.toList()) {
				JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				if (json.has("piece")) {
					int size = json.get("size").getAsInt();
					put(out, json.get("piece").getAsString(), size, json.get("height").getAsInt(), size,
							json.has("ground") ? json.get("ground").getAsInt() : 0);
				}
			}
		}
		return out;
	}

	private static void put(Map<String, List<Blueprint.Plot>> out, String kind, int sx, int sy, int sz, int ground) {
		for (long seed : SEEDS) {
			out.computeIfAbsent(kind, k -> new ArrayList<>()).add(new Blueprint.Plot(sx, sy, sz, ground, seed));
		}
	}

	@Test
	void everyKindHasItsSize() throws IOException {
		Set<String> missing = new TreeSet<>(Blueprints.kinds());
		missing.removeAll(plots().keySet());
		assertEquals(Set.of(), missing, "des plans sans taille dans ce test");
	}

	@Test
	void everyPlanSweepsItsBoxWithoutError() throws IOException {
		for (Map.Entry<String, List<Blueprint.Plot>> entry : plots().entrySet()) {
			Blueprint plan = Blueprints.get(entry.getKey());
			assertTrue(plan != null, "plan inconnu : " + entry.getKey());
			for (Blueprint.Plot plot : entry.getValue()) {
				try {
					for (int x = 0; x < plot.sizeX(); x++) {
						for (int z = 0; z < plot.sizeZ(); z++) {
							for (int y = -plot.ground(); y < plot.sizeY(); y++) {
								plan.at(x, y, z, plot);
							}
						}
					}
				} catch (RuntimeException e) {
					fail(entry.getKey() + " sur " + plot + " : " + e, e);
				}
			}
		}
	}

	@Test
	void chestsAndSpawnsFallInsideTheBox() throws IOException {
		for (Map.Entry<String, List<Blueprint.Plot>> entry : plots().entrySet()) {
			Blueprint plan = Blueprints.get(entry.getKey());
			for (Blueprint.Plot plot : entry.getValue()) {
				for (Blueprint.Chest chest : plan.chests(plot)) {
					assertTrue(inside(chest.local().getX(), chest.local().getY(), chest.local().getZ(), plot),
							entry.getKey() + " : coffre hors de la boîte en " + chest.local() + " sur " + plot);
				}
				for (Blueprint.Spawn spawn : plan.spawns(plot)) {
					assertTrue(inside(spawn.local().getX(), spawn.local().getY(), spawn.local().getZ(), plot),
							entry.getKey() + " : " + spawn.type() + " hors de la boîte en " + spawn.local() + " sur " + plot);
				}
			}
		}
	}

	private static boolean inside(int x, int y, int z, Blueprint.Plot plot) {
		return x >= 0 && x < plot.sizeX() && z >= 0 && z < plot.sizeZ() && y >= -plot.ground() && y < plot.sizeY();
	}

	/**
	 * Le plancher d'une maison est un bloc au-dessus du sol : devant chaque porte qui s'ouvre sur lui,
	 * une marche, tournée vers la maison, pour qu'on (et les villageois) y entrent sans sauter.
	 */
	@Test
	void everyRaisedDoorHasAStep() throws IOException {
		int doors = 0;
		for (Map.Entry<String, List<Blueprint.Plot>> entry : plots().entrySet()) {
			if (!entry.getKey().endsWith("_dressed")) {
				continue;
			}
			Blueprint plan = Blueprints.get(entry.getKey());
			for (Blueprint.Plot plot : entry.getValue()) {
				for (int x = 0; x < plot.sizeX(); x++) {
					if (plan.at(x, 1, Dressing.MARGIN, plot) instanceof net.minecraft.world.level.block.state.BlockState d
							&& d.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
						var step = plan.at(x, 0, Dressing.MARGIN - 1, plot);
						assertTrue(step != null && step.getBlock() instanceof net.minecraft.world.level.block.StairBlock
								&& step.getValue(net.minecraft.world.level.block.StairBlock.FACING)
								== net.minecraft.core.Direction.SOUTH, entry.getKey() + " : pas de marche devant la porte");
						doors++;
					}
				}
			}
		}
		assertTrue(doors > 0, "aucune porte trouvée");
	}

	/** À Liore, les tonneaux des maisons et des étals ont leurs provisions. */
	@Test
	void lioreBarrelsAreStocked() {
		assertEquals(java.util.Optional.of(Blueprint.loot("liore_barrel")), Blueprints.get("liore").barrels());
		assertTrue(Files.exists(Path.of("src/main/resources/data/fmab/loot_table/chests/liore_barrel.json")));
	}

	/** Un bourg aéré : maisons et arbres s'écartent du centre, la place et ses étals restent au milieu. */
	@Test
	void hamletsSpreadAroundTheirSquare() {
		var square = new Hamlet.Spot(-6, -6, "dublith_square", 13, 13, 0, net.minecraft.world.level.block.Rotation.NONE);
		assertEquals(square, Hamlet.spread(square, 1.5));
		var house = new Hamlet.Spot(10, -7, "dublith_house_dressed", 9, 7, 2,
				net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
		var spread = Hamlet.spread(house, 1.5);
		// Tournée d'un quart, elle occupe 7 sur 9 : son milieu (13,5 ; −2,5) passe à (20,25 ; −3,75).
		assertEquals(17, spread.dx());
		assertEquals(-8, spread.dz());
		assertEquals(house.kind(), spread.kind());
	}
}
