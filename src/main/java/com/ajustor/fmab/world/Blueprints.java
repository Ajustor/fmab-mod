package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les plans des structures du mod, par sorte. Central : terrassement, muraille, voirie, fontaine,
 * quartier général (avec l'arène de l'examen), bibliothèque, maisons de ville. Resembool : fermes,
 * maison des Rockbell, champs.
 */
public final class Blueprints {
	/** Rayon de Central, du centre à l'extérieur de la muraille. */
	public static final int CITY_RADIUS = 74;
	private static final int WALL_INNER = 68;
	private static final int WALL_OUTER = 71;
	private static final int WALL_HEIGHT = 10;
	private static final int TOWER_HEIGHT = 14;
	private static final int AVENUE = 3;
	private static final int RING_INNER = 46;
	private static final int RING_OUTER = 51;
	public static final int PLAZA = 14;

	/**
	 * Construit au premier usage : les plans s'appuient sur les palettes, déclarées plus bas. La
	 * génération du monde est parallèle, d'où la publication par une variable volatile.
	 */
	private static volatile Map<String, Blueprint> plans;

	private static Map<String, Blueprint> plans() {
		if (plans != null) {
			return plans;
		}
		Map<String, Blueprint> out = new HashMap<>();
		out.put("central_ground", Blueprints::cityGround);
		out.put("central_walls", Blueprints::cityWalls);
		out.put("central_roads", Blueprints::cityRoads);
		out.put("central_fountain", Blueprints::fountain);
		out.put("central_house", (x, y, z, p) -> house(x, y, z, p, CITY_PALETTES[p.variant(CITY_PALETTES.length)]));
		out.put("central_library", Blueprints::library);
		out.put("central_hq", new Headquarters());
		out.put("resembool_house", (x, y, z, p) -> house(x, y, z, p, RURAL_PALETTES[p.variant(RURAL_PALETTES.length)]));
		out.put("rockbell_house", Blueprints::rockbell);
		out.put("resembool_field", Blueprints::field);
		plans = out;
		return plans;
	}

	private Blueprints() {
	}

	public static Blueprint get(String kind) {
		return plans().get(kind);
	}

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private static final BlockState AIR = b(Blocks.AIR);

	// ---- Central : la ville circulaire ---------------------------------------------------------------

	private static double radius(int x, int z, Blueprint.Plot p) {
		return Math.hypot(x - p.sizeX() / 2, z - p.sizeZ() / 2);
	}

	/** Le plateau de la ville : on dégage au-dessus du sol, on comble dessous. */
	private static BlockState cityGround(int x, int y, int z, Blueprint.Plot p) {
		double r = radius(x, z, p);
		if (r > CITY_RADIUS) {
			return null;
		}
		if (y >= 0) {
			return AIR;
		}
		if (y == -1) {
			return b(Blocks.GRASS_BLOCK);
		}
		return y >= -3 ? b(Blocks.DIRT) : b(Blocks.STONE);
	}

	/** Muraille circulaire à créneaux, quatre portes sur les avenues, huit tours. */
	private static BlockState cityWalls(int x, int y, int z, Blueprint.Plot p) {
		int dx = x - p.sizeX() / 2, dz = z - p.sizeZ() / 2;
		double r = Math.hypot(dx, dz);
		if (y < 0 || r > CITY_RADIUS) {
			return null;
		}
		boolean gate = Math.abs(dx) <= AVENUE || Math.abs(dz) <= AVENUE;
		// Tours entre les portes.
		for (int k = 0; k < 8; k++) {
			double a = Math.toRadians(22.5 + 45 * k);
			double tx = Math.cos(a) * 69.5, tz = Math.sin(a) * 69.5;
			double d = Math.hypot(dx - tx, dz - tz);
			if (d <= 4) {
				if (y < TOWER_HEIGHT) {
					return d >= 3 ? stone(x, y, z, p) : (y == 0 || y == TOWER_HEIGHT - 4 ? b(Blocks.STONE_BRICKS) : AIR);
				}
				if (y == TOWER_HEIGHT && d >= 3 && (x + z) % 2 == 0) {
					return b(Blocks.STONE_BRICKS);
				}
				return null;
			}
		}
		if (r < WALL_INNER || r >= WALL_OUTER) {
			return null;
		}
		if (gate && y < 6) {
			return AIR;
		}
		if (y < WALL_HEIGHT) {
			return stone(x, y, z, p);
		}
		if (y == WALL_HEIGHT && r >= WALL_OUTER - 1 && Math.floorMod(Math.round(Math.atan2(dz, dx) * 40), 2) == 0) {
			return b(Blocks.STONE_BRICKS);
		}
		return null;
	}

	/** Pierre de taille, un peu moussue et fissurée par endroits. */
	private static BlockState stone(int x, int y, int z, Blueprint.Plot p) {
		return switch (p.noise(x, y, z, 12)) {
			case 0 -> b(Blocks.MOSSY_STONE_BRICKS);
			case 1 -> b(Blocks.CRACKED_STONE_BRICKS);
			default -> b(Blocks.STONE_BRICKS);
		};
	}

	/** Avenues en croix, boulevard circulaire, place centrale et réverbères. */
	private static BlockState cityRoads(int x, int y, int z, Blueprint.Plot p) {
		int dx = x - p.sizeX() / 2, dz = z - p.sizeZ() / 2;
		double r = Math.hypot(dx, dz);
		if (r >= WALL_INNER) {
			return null;
		}
		int adx = Math.abs(dx), adz = Math.abs(dz);
		boolean avenue = adx <= AVENUE || adz <= AVENUE;
		boolean ring = r >= RING_INNER && r <= RING_OUTER;
		boolean plaza = r <= PLAZA;
		if (y == -1) {
			if (plaza) {
				return (dx + dz) % 4 == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			}
			if (avenue || ring) {
				boolean edge = adx == AVENUE || adz == AVENUE;
				return edge && !ring ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS);
			}
			return null;
		}
		// Réverbères le long des avenues, tous les dix blocs.
		boolean lampX = adx == AVENUE + 1 && adz > PLAZA + 2 && adz % 10 == 0;
		boolean lampZ = adz == AVENUE + 1 && adx > PLAZA + 2 && adx % 10 == 0;
		if ((lampX || lampZ) && !ring && r < RING_INNER - 1 || (lampX || lampZ) && r > RING_OUTER + 1 && r < WALL_INNER - 2) {
			if (y <= 1) {
				return b(Blocks.STONE_BRICK_WALL);
			}
			if (y == 2) {
				return b(Blocks.LANTERN);
			}
		}
		return null;
	}

	/** Fontaine de la place centrale. */
	private static BlockState fountain(int x, int y, int z, Blueprint.Plot p) {
		double r = Math.hypot(x - 4, z - 4);
		if (r <= 0.5) {
			return y <= 2 ? b(Blocks.CHISELED_STONE_BRICKS) : y == 3 ? b(Blocks.LANTERN) : null;
		}
		if (y == 0 && r >= 3 && r <= 4.3) {
			return b(Blocks.STONE_BRICKS);
		}
		if (y == 0 && r < 3) {
			return b(Blocks.WATER);
		}
		if (y == -1 && r <= 4.3) {
			return b(Blocks.STONE_BRICKS);
		}
		return null;
	}

	/** Bibliothèque : murs de brique, hautes fenêtres, rayonnages en rangées. */
	private static BlockState library(int x, int y, int z, Blueprint.Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		int h = 9;
		if (y == -1) {
			return b(Blocks.POLISHED_ANDESITE);
		}
		boolean wall = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		boolean corner = (x == 0 || x == sx - 1) && (z == 0 || z == sz - 1);
		if (y >= h) {
			return y == h && wall ? b(Blocks.STONE_BRICK_SLAB) : null;
		}
		if (y == h - 1) {
			return b(Blocks.STONE_BRICKS);
		}
		if (wall) {
			if (corner) {
				return b(Blocks.STONE_BRICKS);
			}
			if (z == 0 && Math.abs(x - sx / 2) <= 1 && y <= 2) {
				return x == sx / 2 && y <= 1 ? door(Blocks.DARK_OAK_DOOR, y, Direction.NORTH) : AIR;
			}
			boolean window = y >= 2 && y <= 5 && (x + z) % 3 == 0;
			return window ? b(Blocks.GLASS_PANE) : b(Blocks.BRICKS);
		}
		// Intérieur : rayonnages sur deux hauteurs, allée centrale.
		if (y <= 2 && z >= 3 && z <= sz - 3 && x % 3 == 1 && Math.abs(x - sx / 2) > 1) {
			return b(Blocks.BOOKSHELF);
		}
		if (y == 0 && z == sz - 3 && x == sx / 2) {
			return b(Blocks.LECTERN);
		}
		if (y == h - 2 && x % 5 == 2 && z % 5 == 2) {
			return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
		}
		return AIR;
	}

	/**
	 * Quartier général de l'armée : un bâtiment carré autour d'une cour à ciel ouvert, l'arène de
	 * l'examen d'État. L'examinateur s'y tient.
	 */
	private static final class Headquarters implements Blueprint {
		private static final int H = 12;

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean courtyard = x >= 6 && x <= sx - 7 && z >= 7 && z <= sz - 7;
			if (y == -1) {
				return courtyard ? b(Blocks.SAND) : b(Blocks.POLISHED_ANDESITE);
			}
			if (y > H) {
				return null;
			}
			boolean outer = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
			boolean inner = (x == 5 || x == sx - 6) && z >= 6 && z <= sz - 6 || (z == 6 || z == sz - 6) && x >= 5 && x <= sx - 6;
			if (courtyard) {
				return y == 0 && (x == 6 || x == sx - 7 || z == 7 || z == sz - 7) ? b(Blocks.STONE_BRICK_SLAB) : AIR;
			}
			if (y == H) {
				return outer ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.STONE_BRICK_SLAB);
			}
			if (y == H - 1 || y == 5) {
				return outer || inner || y == H - 1 ? b(Blocks.STONE_BRICKS) : b(Blocks.SPRUCE_PLANKS);
			}
			// Portail : au milieu de la façade et vers la cour.
			if ((z == 0 || z == 6) && Math.abs(x - sx / 2) <= 1 && y <= 3) {
				return AIR;
			}
			if (outer) {
				// Étendards bleus de l'armée de part et d'autre du portail.
				if (z == 0 && Math.abs(x - sx / 2) == 3 && y >= 6 && y <= 9) {
					return b(Blocks.WOOL.pick(DyeColor.BLUE));
				}
				boolean window = (y == 2 || y == 3 || y == 7 || y == 8) && x % 3 == 1 && z == 0
						|| (y == 2 || y == 3 || y == 7 || y == 8) && z % 3 == 1 && (x == 0 || x == sx - 1);
				return window ? b(Blocks.GLASS_PANE) : stone(x, y, z, p);
			}
			if (inner) {
				return (y <= 3 && (x + z) % 4 == 0) ? AIR : b(Blocks.STONE_BRICKS);
			}
			if (y == 4 && x % 6 == 3 && z % 6 == 3) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			return AIR;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			// L'examinateur attend dans la cour, face au portail.
			return List.of(new Spawn(FmabEntities.STATE_EXAMINER, new BlockPos(p.sizeX() / 2, 0, 9)));
		}
	}

	// ---- Maisons ---------------------------------------------------------------------------------------

	/** Matériaux d'une maison. */
	private record Palette(BlockState wall, BlockState corner, BlockState floor, BlockState foundation,
			Block roof, BlockState gable, Block door) {
	}

	private static final Palette[] CITY_PALETTES = {
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

	private static final Palette[] RURAL_PALETTES = {
			new Palette(b(Blocks.BIRCH_PLANKS), b(Blocks.OAK_LOG), b(Blocks.OAK_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.SPRUCE_STAIRS, b(Blocks.BIRCH_PLANKS), Blocks.OAK_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), b(Blocks.SPRUCE_LOG), b(Blocks.SPRUCE_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.DARK_OAK_STAIRS, b(Blocks.SPRUCE_PLANKS), Blocks.SPRUCE_DOOR),
	};

	/** Hauteur d'une pièce de maison : murs d'un ou deux étages, puis le toit. */
	public static int houseHeight(int floors, int depth) {
		return 4 * floors + (depth + 1) / 2 + 1;
	}

	/**
	 * Une maison : fondations, murs à pans, fenêtres, porte au milieu de la façade (z = 0), toit à
	 * deux pans le long de x.
	 */
	private static BlockState house(int x, int y, int z, Blueprint.Plot p, Palette pal) {
		int sx = p.sizeX(), sz = p.sizeZ();
		// La pièce mesure les murs, plus le toit ((sz + 1) / 2 rangées) et une marge.
		int h = p.sizeY() - (sz + 1) / 2 - 1 <= 4 ? 4 : 8;
		if (y < 0) {
			return pal.foundation();
		}
		if (y < h) {
			boolean xEdge = x == 0 || x == sx - 1, zEdge = z == 0 || z == sz - 1;
			if (xEdge && zEdge) {
				return pal.corner();
			}
			if (y % 4 == 0 && y > 0) {
				return xEdge || zEdge ? pal.corner() : pal.floor();
			}
			if (xEdge || zEdge) {
				if (z == 0 && x == sx / 2 && y <= 1) {
					return door(pal.door(), y, Direction.NORTH);
				}
				int along = zEdge ? x : z;
				boolean window = (y % 4 == 1 || y % 4 == 2) && along % 2 == 1 && !(z == 0 && Math.abs(x - sx / 2) <= 1);
				return window ? b(Blocks.GLASS_PANE) : pal.wall();
			}
			if (y == 0) {
				return pal.floor();
			}
			return AIR;
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

	/** Maison des Rockbell : une ferme, avec l'atelier d'automail au rez-de-chaussée. */
	private static BlockState rockbell(int x, int y, int z, Blueprint.Plot p) {
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
				default -> b(Blocks.CRAFTING_TABLE);
			};
		}
		return shell;
	}

	/** Champ de blé clos, avec un point d'eau au centre. */
	private static BlockState field(int x, int y, int z, Blueprint.Plot p) {
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
		return y <= 2 ? AIR : null;
	}

	/** Clôture du bord d'un rectangle, reliée à ses voisines. */
	private static BlockState fence(int x, int z, int sx, int sz) {
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

	private static BlockState stairs(Block block, Direction facing) {
		return b(block).setValue(StairBlock.FACING, facing);
	}

	private static BlockState door(Block block, int y, Direction facing) {
		return b(block).setValue(DoorBlock.FACING, facing)
				.setValue(DoorBlock.HALF, y == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
	}
}
