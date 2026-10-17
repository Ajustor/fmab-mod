package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.entity.character.StoryCharacters;
import com.ajustor.fmab.registry.FmabBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.Map;

/**
 * Les plans des structures du mod, par sorte. Central : terrassement, muraille, voirie, fontaine,
 * quartier général (avec l'arène de l'examen), bibliothèque, maisons de ville. Resembool : fermes,
 * maison des Rockbell, champs.
 */
public final class Blueprints {
	/** Rayon de Central, du centre à l'extérieur de la muraille. */
	public static final int CITY_RADIUS = 74;
	static final int WALL_INNER = 68;
	static final int WALL_OUTER = 71;
	static final int WALL_HEIGHT = 10;
	static final int TOWER_HEIGHT = 14;
	static final int AVENUE = 3;
	static final int RING_INNER = 46;
	static final int RING_OUTER = 51;
	public static final int PLAZA = 14;

	/**
	 * Construit au premier usage : les plans s'appuient sur les palettes, déclarées plus bas. La
	 * génération du monde est parallèle, d'où la publication par une variable volatile.
	 */
	static volatile Map<String, Blueprint> plans;

	static Map<String, Blueprint> plans() {
		if (plans != null) {
			return plans;
		}
		Map<String, Blueprint> out = new HashMap<>();
		out.put("central_ground", CityPlans::cityGround);
		out.put("central_walls", new CityWalls());
		out.put("central_roads", CityPlans::cityRoads);
		out.put("central_fountain", CityPlans::fountain);
		out.put("central_house", Blueprint.withChest(new Home(CITY_PALETTES, true), 1, "house"));
		// Shou Tucker y fait ses recherches, entre les rayonnages.
		out.put("central_library", Blueprint.withSpawns(Blueprint.withChest(CityPlans::library, 0, "central_library"),
				p -> java.util.List.of(new Blueprint.Spawn(FmabEntities.character(StoryCharacters.TUCKER),
						new net.minecraft.core.BlockPos(p.sizeX() / 2, 0, p.sizeZ() - 5)))));
		out.put("central_hq", new Headquarters());
		out.put("resembool_house", Blueprint.withChest(new Home(RURAL_PALETTES, false), 1, "house"));
		out.put("rockbell_house", new RockbellHouse());
		out.put("resembool_field", Blueprints::field);
		out.put("elric_house", new ElricHouse());
		out.put("rush_valley_workshop", new Workshop(false));
		out.put("rush_valley_shop", new Workshop(true));
		out.put("rush_valley_square", Blueprints::square);
		out.put("laboratory_5", new Laboratory5());
		out.put("devils_nest", new DevilsNest());
		out.put("bradley_residence", Blueprint.withChest(new BradleyResidence(), 1, "bradley_residence"));
		out.put("sloth_tunnel", Blueprint.withChest(new SlothTunnel(), SlothTunnel.FLOOR + 1, "sloth_tunnel"));
		out.put("father_lair", Blueprint.withChest(new FatherLair(), FatherLair.ROOM_FLOOR + 1, "father_lair"));
		out.put("xing_pavilion", new XingPavilion());
		out.put("fort_briggs", new FortBriggs());
		out.put("blood_crest", Blueprint.withChest(Blueprints::bloodCrest, 1, "blood_crest"));
		out.put("liore", new Liore());
		out.put("dublith_square", Blueprints::dublithSquare);
		out.put("dublith_house", Blueprint.withChest(new Home(DUBLITH_PALETTES, true), 1, "house"));
		out.put("curtis_butcher", new CurtisButcher());
		out.put("marcoh_clinic", new MarcohClinic());
		out.put("ishval_ruins", new IshvalRuins());
		out.put("xerxes_ruins", new XerxesRuins());
		out.put("yock_island", Blueprint.withChest(new YockIsland(), 2, "yock_island"));
		// Les mêmes maisons, avec leurs abords (avant-toits, volets, jardinières, massifs) : les lieux
		// les posent sur une parcelle plus large de deux blocs.
		out.put("central_house_dressed", new Dressing(out.get("central_house"), Dressing.TOWN));
		out.put("resembool_house_dressed", new Dressing(out.get("resembool_house"), Dressing.RURAL));
		out.put("rockbell_house_dressed", new Dressing(out.get("rockbell_house"), Dressing.RURAL));
		out.put("elric_house_dressed", new Dressing(out.get("elric_house"), Dressing.RURAL));
		out.put("rush_valley_workshop_dressed", new Dressing(out.get("rush_valley_workshop"), Dressing.DESERT));
		out.put("rush_valley_shop_dressed", new Dressing(out.get("rush_valley_shop"), Dressing.DESERT));
		out.put("dublith_house_dressed", new Dressing(out.get("dublith_house"), Dressing.TOWN));
		out.put("curtis_butcher_dressed", new Dressing(out.get("curtis_butcher"), Dressing.TOWN));
		out.put("town_tree", Townscape.tree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.GRASS_BLOCK));
		out.put("birch_tree", Townscape.tree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.GRASS_BLOCK));
		out.put("acacia_tree", Townscape.tree(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.COARSE_DIRT));
		out.put("palm_tree", Townscape.palm(Blocks.SAND));
		out.put("market_stall", Townscape.stall(Blocks.DARK_OAK_FENCE, Blocks.SPRUCE_SLAB));
		out.put("desert_stall", Townscape.stall(Blocks.JUNGLE_FENCE, Blocks.SMOOTH_SANDSTONE_SLAB));
		out.put("resembool_pasture", new Townscape.Pasture());
		plans = out;
		return plans;
	}

	private Blueprints() {
	}

	public static Blueprint get(String kind) {
		return plans().get(kind);
	}

	/** Toutes les sortes de plans enregistrées. */
	static java.util.Set<String> kinds() {
		return java.util.Set.copyOf(plans().keySet());
	}

	static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	static final BlockState AIR = b(Blocks.AIR);

	// ---- Maisons ---------------------------------------------------------------------------------------

	/** Matériaux d'une maison. */
	record Palette(BlockState wall, BlockState corner, BlockState floor, BlockState foundation,
			Block roof, BlockState gable, Block door) {
	}

	static final Palette[] CITY_PALETTES = {
			new Palette(b(FmabBlocks.AMESTRIAN_BRICKS), b(Blocks.STONE_BRICKS), b(Blocks.OAK_PLANKS), b(Blocks.STONE_BRICKS),
					Blocks.DEEPSLATE_TILE_STAIRS, b(FmabBlocks.AMESTRIAN_BRICKS), Blocks.OAK_DOOR),
			new Palette(b(Blocks.BRICKS), b(Blocks.STONE_BRICKS), b(Blocks.SPRUCE_PLANKS), b(Blocks.STONE_BRICKS),
					Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.BRICKS), Blocks.SPRUCE_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), b(Blocks.STONE_BRICKS), b(Blocks.OAK_PLANKS), b(Blocks.STONE_BRICKS),
					Blocks.DARK_OAK_STAIRS, b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), Blocks.DARK_OAK_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY)), b(Blocks.POLISHED_ANDESITE), b(Blocks.SPRUCE_PLANKS),
					b(Blocks.STONE_BRICKS), Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY)),
					Blocks.SPRUCE_DOOR),
			new Palette(b(Blocks.STONE_BRICKS), b(Blocks.CHISELED_STONE_BRICKS), b(Blocks.OAK_PLANKS),
					b(Blocks.STONE_BRICKS), Blocks.BRICK_STAIRS, b(Blocks.STONE_BRICKS), Blocks.OAK_DOOR),
	};

	static final Palette[] RURAL_PALETTES = {
			new Palette(b(Blocks.BIRCH_PLANKS), b(Blocks.OAK_LOG), b(Blocks.OAK_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.SPRUCE_STAIRS, b(Blocks.BIRCH_PLANKS), Blocks.OAK_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), b(Blocks.SPRUCE_LOG), b(Blocks.SPRUCE_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.DARK_OAK_STAIRS, b(Blocks.SPRUCE_PLANKS), Blocks.SPRUCE_DOOR),
	};

	/** Hauteur d'une pièce de maison : murs d'un ou deux étages, puis le toit. */
	public static int houseHeight(int floors, int depth) {
		return 4 * floors + (depth + 1) / 2 + 1;
	}

	/** Hauteur des murs d'une maison : 4 pour un étage, 8 pour deux. */
	static int wallHeight(Blueprint.Plot p) {
		// La pièce mesure les murs, plus le toit ((sz + 1) / 2 rangées) et une marge.
		return p.sizeY() - (p.sizeZ() + 1) / 2 - 1 <= 4 ? 4 : 8;
	}

	/**
	 * Une maison : fondations, murs à pans, fenêtres, porte au milieu de la façade (z = 0), toit à
	 * deux pans le long de x.
	 */
	static BlockState house(int x, int y, int z, Blueprint.Plot p, Palette pal) {
		int sx = p.sizeX(), sz = p.sizeZ();
		int h = wallHeight(p);
		if (y < 0) {
			return pal.foundation();
		}
		if (y < h) {
			boolean xEdge = x == 0 || x == sx - 1, zEdge = z == 0 || z == sz - 1;
			if (xEdge && zEdge) {
				return pal.corner();
			}
			if (y % 4 == 0 && y > 0) {
				if (h == 8 && y == 4 && x == sx - 2 && z == 1) {
					return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST);
				}
				return xEdge || zEdge ? pal.corner() : pal.floor();
			}
			if (xEdge || zEdge) {
				// La porte s'ouvre au niveau du plancher (posé en y = 0) : une marche pour entrer.
				if (z == 0 && x == sx / 2 && (y == 1 || y == 2)) {
					return door(pal.door(), y - 1, Direction.NORTH);
				}
				int along = zEdge ? x : z;
				boolean window = (y % 4 == 1 || y % 4 == 2) && along % 2 == 1 && !(z == 0 && Math.abs(x - sx / 2) <= 1);
				return window ? b(Blocks.GLASS_PANE) : pal.wall();
			}
			if (y == 0) {
				return pal.floor();
			}
			// Une échelle dans le coin, près de la porte, jusqu'à l'étage.
			if (h == 8 && x == sx - 2 && z == 1 && y <= 4) {
				return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST);
			}
			return AIR;
		}
		// Une cheminée sur la pente arrière, dans deux maisons sur trois ; un feu couve tout en haut.
		int ridge = h + (sz - 1) / 2;
		if (x == 1 && z == sz - 2 && y > h && p.noise(3, 3, 3, 3) != 0) {
			return y <= ridge ? b(Blocks.BRICKS) : y == ridge + 1 ? b(Blocks.CAMPFIRE) : null;
		}
		// Toit à deux pans : la pente monte de chaque côté vers le faîte, au milieu de z.
		int d = y - h;
		if (d > (sz - 1) / 2) {
			return null;
		}
		if (z == d && z == sz - 1 - d) {
			return pal.gable();
		}
		if (z == d) {
			return stairs(pal.roof(), Direction.SOUTH);
		}
		if (z == sz - 1 - d) {
			return stairs(pal.roof(), Direction.NORTH);
		}
		if (z > d && z < sz - 1 - d) {
			return x == 0 || x == sx - 1 ? pal.gable() : AIR;
		}
		return null;
	}

	// ---- Habitations meublées et habitants -------------------------------------------------------------

	/**
	 * Les meubles d'une maison, ou null. En bas : la table et sa chaise, le fourneau, le poste de
	 * travail au fond à droite ; le lit aussi dans une maison basse. À l'étage : deux lits, une
	 * bibliothèque, un tapis. Une lanterne par étage, pour qu'aucun monstre n'y naisse. L'axe de la
	 * porte et le pied de l'échelle restent libres.
	 *
	 * @param town meubles de ville (sinon de campagne) : bois, couleurs, métiers
	 */
	static BlockState furnish(int x, int y, int z, Blueprint.Plot p, boolean town) {
		int sx = p.sizeX(), sz = p.sizeZ();
		int back = sz - 2, right = sx - 2;
		boolean twoStoreys = wallHeight(p) == 8;
		if (x < 1 || x > right || z < 1 || z > back) {
			return null;
		}
		if (y >= 1 && y <= 3) {
			int dy = y - 1;
			if (dy == 0 && z == back) {
				if (x == right) {
					return workstation(p, town);
				}
				if (x == right - 1 && x != sx / 2) {
					return b(Blocks.FURNACE).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
				}
				if (!twoStoreys && x <= 2) {
					return bed(x == 1, p, town);
				}
				if (twoStoreys && x == 1) {
					return b(Blocks.BARREL);
				}
				if (twoStoreys && x == 2) {
					return b(Blocks.CRAFTING_TABLE);
				}
			}
			// La table, et sa lanterne dans une maison basse (l'étage la suspend au plafond).
			if (x == 1 && z == 2) {
				if (dy == 0) {
					return b(town ? Blocks.DARK_OAK_FENCE : Blocks.SPRUCE_FENCE);
				}
				if (dy == 1) {
					return twoStoreys ? b(town ? Blocks.DARK_OAK_PRESSURE_PLATE : Blocks.SPRUCE_PRESSURE_PLATE)
							: b(Blocks.LANTERN);
				}
			}
			if (x == 2 && z == 2 && dy == 0) {
				return stairs(town ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS, Direction.EAST);
			}
			if (twoStoreys && dy == 2 && x == sx / 2 && z == sz / 2) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			return null;
		}
		if (twoStoreys && y >= 5 && y <= 7) {
			int dy = y - 5;
			if (dy == 0 && x <= 2 && (z == back || z == back - 1)) {
				return bed(x == 1, p, town);
			}
			if (x == right && z == back) {
				return dy == 0 ? b(Blocks.BOOKSHELF) : dy == 1 ? b(Blocks.LANTERN) : null;
			}
			if (dy == 0 && (x == sx / 2 || x == sx / 2 - 1) && (z == sz / 2 || z == sz / 2 - 1)) {
				return b(Blocks.CARPET.pick(colour(p, town, 10)));
			}
		}
		return null;
	}

	/** Un lit couché le long de x, tête à l'ouest (contre le mur). */
	static BlockState bed(boolean head, Blueprint.Plot p, boolean town) {
		return b(Blocks.BED.pick(colour(p, town, 9))).setValue(BedBlock.FACING, Direction.WEST)
				.setValue(BedBlock.PART, head ? BedPart.HEAD : BedPart.FOOT);
	}

	static DyeColor colour(Blueprint.Plot p, boolean town, int salt) {
		DyeColor[] colours = town
				? new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.BLUE, DyeColor.RED}
				: new DyeColor[]{DyeColor.RED, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BROWN};
		return colours[p.noise(salt, 0, 0, colours.length)];
	}

	/**
	 * Le poste de travail qui donne son métier à l'habitant : bibliothécaire, cartographe (il vend
	 * les cartes des lieux du mod), forgerons... à la ville ; fermiers, bouchers, bergers à la
	 * campagne.
	 */
	static BlockState workstation(Blueprint.Plot p, boolean town) {
		Block[] jobs = town
				? new Block[]{Blocks.LECTERN, Blocks.CARTOGRAPHY_TABLE, Blocks.BREWING_STAND, Blocks.SMITHING_TABLE,
						Blocks.LOOM, Blocks.STONECUTTER, Blocks.FLETCHING_TABLE, Blocks.BLAST_FURNACE, Blocks.CARTOGRAPHY_TABLE}
				: new Block[]{Blocks.COMPOSTER, Blocks.COMPOSTER, Blocks.SMOKER, Blocks.LOOM, Blocks.FLETCHING_TABLE,
						Blocks.CARTOGRAPHY_TABLE, Blocks.BARREL};
		BlockState state = b(jobs[p.noise(8, 0, 0, jobs.length)]);
		return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				? state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH) : state;
	}

	static BlockState rockbell(int x, int y, int z, Blueprint.Plot p) {
		BlockState shell = house(x, y, z, p, RURAL_PALETTES[0]);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside || y != 1) {
			return shell;
		}
		// Établi le long du mur du fond : enclume, meule, table d'artisan, chaînes.
		if (z == sz - 2) {
			return switch (x % 4) {
				case 0 -> b(Blocks.ANVIL);
				case 1 -> b(Blocks.SMITHING_TABLE);
				case 2 -> b(Blocks.GRINDSTONE);
				default -> b(FmabBlocks.AUTOMAIL_BENCH);
			};
		}
		return shell;
	}

	static final Palette RUSH_VALLEY_PALETTE = new Palette(b(Blocks.SMOOTH_SANDSTONE), b(Blocks.CUT_SANDSTONE),
			b(Blocks.SPRUCE_PLANKS), b(Blocks.SANDSTONE), Blocks.SANDSTONE_STAIRS, b(Blocks.CUT_SANDSTONE),
			Blocks.SPRUCE_DOOR);

	static final Palette DEVILS_NEST_PALETTE = new Palette(b(Blocks.DARK_OAK_PLANKS), b(Blocks.DARK_OAK_LOG),
			b(Blocks.SPRUCE_PLANKS), b(Blocks.COBBLESTONE), Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_PLANKS),
			Blocks.DARK_OAK_DOOR);

	/**
	 * Le champ d'un massacre : une terre brûlée, des pans de murs écroulés, des os et du sang figé ; au
	 * centre, sur une estrade de pierre noire, le point de sang du cercle national.
	 */
	static BlockState bloodCrest(int x, int y, int z, Blueprint.Plot p) {
		int c = p.sizeX() / 2;
		double r = Math.hypot(x - c, z - c);
		if (r > c + 0.5) {
			return null;
		}
		if (y < 0) {
			return b(Blocks.COARSE_DIRT);
		}
		if (y == 0) {
			if (r <= 1.5) {
				return b(Blocks.POLISHED_BLACKSTONE);
			}
			int n = p.noise(x, 0, z, 9);
			return n == 0 ? b(FmabBlocks.CRYSTALLIZED_BLOOD) : n < 4 ? b(Blocks.GRAVEL) : b(Blocks.COARSE_DIRT);
		}
		if (y == 1 && x == c && z == c) {
			return b(FmabBlocks.BLOOD_CREST);
		}
		// Les murs écroulés, en arc de cercle, plus ou moins hauts.
		if (r > 5.5 && r < 7 && p.noise(x, 0, z, 3) != 0 && y <= p.noise(x, 1, z, 4)) {
			return p.noise(x, y, z, 3) == 0 ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.COBBLESTONE);
		}
		if (y == 1 && r > 2 && r < 5.5) {
			int n = p.noise(x, 2, z, 14);
			if (n == 0) {
				return b(Blocks.BONE_BLOCK);
			}
			if (n == 1) {
				return b(Blocks.SKELETON_SKULL);
			}
		}
		return AIR;
	}

	static final Palette[] DUBLITH_PALETTES = {
			new Palette(b(FmabBlocks.AMESTRIAN_BRICKS), b(Blocks.SPRUCE_LOG), b(Blocks.OAK_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), Blocks.OAK_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), b(Blocks.DARK_OAK_LOG), b(Blocks.SPRUCE_PLANKS),
					b(Blocks.COBBLESTONE), Blocks.BRICK_STAIRS, b(Blocks.BRICKS), Blocks.DARK_OAK_DOOR),
	};

	/** La place de Dublith : des pavés, un lampadaire aux quatre coins, un abreuvoir au centre. */
	static BlockState dublithSquare(int x, int y, int z, Blueprint.Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ(), cx = sx / 2, cz = sz / 2;
		boolean corner = (x == 1 || x == sx - 2) && (z == 1 || z == sz - 2);
		if (y < 0) {
			return b(Blocks.COBBLESTONE);
		}
		if (y == 0) {
			if (Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) {
				return x == cx && z == cz ? b(Blocks.WATER) : b(Blocks.STONE_BRICKS);
			}
			return p.noise(x, 0, z, 3) == 0 ? b(Blocks.ANDESITE) : b(Blocks.STONE_BRICKS);
		}
		if (corner) {
			return y == 1 ? b(Blocks.COBBLESTONE_WALL) : y == 2 ? b(Blocks.LANTERN) : AIR;
		}
		return AIR;
	}

	/** La place de Rush Valley : un dallage de grès, des lanternes aux coins, un puits au centre. */
	static BlockState square(int x, int y, int z, Blueprint.Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		int cx = sx / 2, cz = sz / 2;
		boolean well = Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1;
		boolean corner = (x == 1 || x == sx - 2) && (z == 1 || z == sz - 2);
		if (y < -1) {
			return b(Blocks.SANDSTONE);
		}
		if (y == -1) {
			if (well) {
				return x == cx && z == cz ? b(Blocks.WATER) : b(Blocks.CUT_SANDSTONE);
			}
			return (x + z) % 2 == 0 ? b(Blocks.SMOOTH_SANDSTONE) : b(Blocks.CUT_SANDSTONE);
		}
		if (well) {
			if (x == cx && z == cz) {
				return y == 0 ? AIR : y <= 2 ? AIR : null;
			}
			return y == 0 ? b(Blocks.SANDSTONE_WALL) : y <= 2 ? AIR : null;
		}
		if (corner) {
			return switch (y) {
				case 0, 1 -> b(Blocks.SANDSTONE_WALL);
				case 2 -> b(Blocks.LANTERN);
				default -> null;
			};
		}
		return y <= 2 ? AIR : null;
	}

	/** Champ de blé clos, avec un point d'eau au centre. */
	static BlockState field(int x, int y, int z, Blueprint.Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean border = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		boolean center = x == sx / 2 && z == sz / 2;
		if (y == -1) {
			if (border) {
				return b(Blocks.DIRT_PATH);
			}
			return center ? b(Blocks.WATER) : b(Blocks.FARMLAND).setValue(FarmlandBlock.MOISTURE, 7);
		}
		if (y == 0) {
			if (border) {
				// Gate du côté de la façade.
				if (z == 0 && x == sx / 2) {
					return AIR;
				}
				return fence(x, z, sx, sz);
			}
			return center ? AIR : b(Blocks.WHEAT).setValue(CropBlock.AGE, 3 + p.noise(x, 0, z, 5));
		}
		if (y < -1) {
			return b(Blocks.DIRT);
		}
		return y <= 2 ? AIR : null;
	}

	/** Clôture du bord d'un rectangle, reliée à ses voisines. */
	static BlockState fence(int x, int z, int sx, int sz) {
		boolean onX = x == 0 || x == sx - 1;
		boolean onZ = z == 0 || z == sz - 1;
		BlockState f = b(Blocks.OAK_FENCE);
		if (onX) {
			f = f.setValue(CrossCollisionBlock.NORTH, z > 0).setValue(CrossCollisionBlock.SOUTH, z < sz - 1);
		}
		if (onZ) {
			boolean gateLeft = z == 0 && x == sx / 2 - 1, gateRight = z == 0 && x == sx / 2 + 1;
			f = f.setValue(CrossCollisionBlock.WEST, x > 0 && !gateRight).setValue(CrossCollisionBlock.EAST,
					x < sx - 1 && !gateLeft);
		}
		return f;
	}

	static BlockState stairs(Block block, Direction facing) {
		return b(block).setValue(StairBlock.FACING, facing);
	}

	static BlockState door(Block block, int y, Direction facing) {
		return b(block).setValue(DoorBlock.FACING, facing)
				.setValue(DoorBlock.HALF, y == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
	}
}
