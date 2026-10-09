package com.ajustor.fmab.world;

import com.ajustor.fmab.item.Tomes;
import com.ajustor.fmab.item.XerxesMuralItem;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
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
		out.put("elric_house", new ElricHouse());
		out.put("rush_valley_workshop", new Workshop(false));
		out.put("rush_valley_shop", new Workshop(true));
		out.put("rush_valley_square", Blueprints::square);
		out.put("laboratory_5", new Laboratory5());
		out.put("devils_nest", new DevilsNest());
		out.put("bradley_residence", new BradleyResidence());
		out.put("sloth_tunnel", new SlothTunnel());
		out.put("father_lair", new FatherLair());
		out.put("xing_pavilion", new XingPavilion());
		out.put("fort_briggs", new FortBriggs());
		out.put("blood_crest", Blueprints::bloodCrest);
		out.put("liore", new Liore());
		out.put("dublith_square", Blueprints::dublithSquare);
		out.put("dublith_house", (x, y, z, p) -> house(x, y, z, p, DUBLITH_PALETTES[p.variant(DUBLITH_PALETTES.length)]));
		out.put("curtis_butcher", new CurtisButcher());
		out.put("ishval_ruins", new IshvalRuins());
		out.put("xerxes_ruins", new XerxesRuins());
		out.put("yock_island", new YockIsland());
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
			// L'examinateur attend dans la cour, face au portail ; la garde se tient au portail et dans la cour.
			int sx = p.sizeX(), sz = p.sizeZ();
			return List.of(new Spawn(FmabEntities.STATE_EXAMINER, new BlockPos(sx / 2, 0, 9)),
					new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx / 2 - 2, 0, 2)),
					new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx / 2 + 2, 0, 2)),
					new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(8, 0, sz - 9)),
					new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx - 9, 0, sz - 9)));
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
				default -> b(FmabBlocks.AUTOMAIL_BENCH);
			};
		}
		return shell;
	}

	/**
	 * La maison des Elric : une ferme ordinaire, mais le bureau de Hohenheim est resté tel qu'il
	 * l'a laissé. Ses notes de recherche dorment dans un coffre, sous les étagères.
	 */
	private static final class ElricHouse implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			BlockState shell = house(x, y, z, p, RURAL_PALETTES[1]);
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
			if (!inside) {
				return shell;
			}
			// Le bureau : des étagères le long du mur du fond, un pupitre.
			if (z == sz - 2 && (y == 1 || y == 2) && x > 1) {
				return b(Blocks.BOOKSHELF);
			}
			if (z == sz - 3 && y == 1 && x == sx / 2) {
				return b(Blocks.LECTERN);
			}
			return shell;
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), plot -> List.of(
					Tomes.stack(Tomes.HOHENHEIM),
					Tomes.stack(Tomes.RUDIMENTS),
					new ItemStack(Items.PAPER, 3 + plot.noise(1, 1, 1, 5)),
					new ItemStack(Items.BONE_MEAL, 2 + plot.noise(2, 1, 1, 4)))));
		}
	}

	private static final Palette RUSH_VALLEY_PALETTE = new Palette(b(Blocks.SMOOTH_SANDSTONE), b(Blocks.CUT_SANDSTONE),
			b(Blocks.SPRUCE_PLANKS), b(Blocks.SANDSTONE), Blocks.SANDSTONE_STAIRS, b(Blocks.CUT_SANDSTONE),
			Blocks.SPRUCE_DOOR);

	/**
	 * Un atelier d'automail de Rush Valley : grès, enclumes et établis contre le mur du fond, des
	 * pièces dans le coffre. La boutique de Garfiel accueille Winry.
	 */
	private static final class Workshop implements Blueprint {
		private final boolean winry;

		Workshop(boolean winry) {
			this.winry = winry;
		}

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			BlockState shell = house(x, y, z, p, RUSH_VALLEY_PALETTE);
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
			if (!inside || y != 1 || z != sz - 2 || x == 1) {
				return shell;
			}
			return switch ((x + p.variant(3)) % 4) {
				case 0 -> b(Blocks.ANVIL);
				case 1 -> b(FmabBlocks.AUTOMAIL_BENCH);
				case 2 -> b(Blocks.GRINDSTONE);
				default -> b(Blocks.SMITHING_TABLE);
			};
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), Workshop::stock));
		}

		/** Des pièces de rechange, du fer, et parfois un automail complet. */
		private static List<ItemStack> stock(Plot p) {
			Item[] parts = {FmabItems.RUSH_VALLEY_AUTOMAIL_ARM, FmabItems.RUSH_VALLEY_AUTOMAIL_LEG,
					FmabItems.IRON_AUTOMAIL_ARM, FmabItems.IRON_AUTOMAIL_LEG};
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.IRON_INGOT, 2 + p.noise(3, 0, 0, 6)));
			out.add(new ItemStack(Items.REDSTONE, 1 + p.noise(4, 0, 0, 4)));
			if (p.noise(5, 0, 0, 3) == 0) {
				out.add(new ItemStack(parts[p.noise(6, 0, 0, parts.length)]));
			}
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return winry ? List.of(new Spawn(FmabEntities.WINRY, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2)))
					: List.of();
		}
	}

	/**
	 * Le Laboratoire 5 : en surface, un poste de garde en ruine ; un puits à échelle descend dans une
	 * grande salle souterraine. Des cellules le long du mur du fond, un cercle tracé au sang au milieu
	 * du sol, des coffres de recherche, et ses gardiens : Lust et Gluttony.
	 *
	 * <p>Coordonnées : le poste occupe x 8..16, z 0..8 en surface (y ≥ 0) ; le puits descend en
	 * (12, z 4) ; la salle couvre toute la parcelle, sol en y = −18, plafond en y = −12.
	 */
	static final class Laboratory5 implements Blueprint {
		static final int HALL_FLOOR = -18;
		static final int HALL_CEILING = -12;
		private static final int SHAFT_X = 12;
		private static final int SHAFT_Z = 4;

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			if (y >= HALL_FLOOR && y <= HALL_CEILING) {
				return hall(x, y, z, p);
			}
			if (y > HALL_CEILING && y < 0) {
				return shaft(x, y, z);
			}
			return guardhouse(x, y, z, p);
		}

		private BlockState hall(int x, int y, int z, Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean wall = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
			if (y == HALL_FLOOR) {
				double d = Math.hypot(x - sx / 2.0 + 0.5, z - 9);
				if (Math.abs(d - 5) < 0.6 || (d < 5 && (x == sx / 2 || z == 9))) {
					return b(Blocks.CONCRETE.pick(DyeColor.RED));
				}
				return (x + z) % 2 == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS);
			}
			if (y == HALL_CEILING) {
				return x == SHAFT_X && z == SHAFT_Z ? ladder() : b(Blocks.STONE_BRICKS);
			}
			if (wall) {
				return p.noise(x, y, z, 7) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			}
			// Le puits continue jusqu'au sol, son échelle contre un pilier.
			if (x == SHAFT_X && z == SHAFT_Z) {
				return ladder();
			}
			if (x == SHAFT_X && z == SHAFT_Z + 1) {
				return b(Blocks.STONE_BRICKS);
			}
			// Les cellules : des barreaux, des murs tous les cinq blocs.
			if (z == sz - 5) {
				return x % 5 == 0 ? b(Blocks.STONE_BRICKS) : y < HALL_CEILING - 1 ? ironBars(x, sx) : b(Blocks.STONE_BRICKS);
			}
			if (z > sz - 5 && x % 5 == 0) {
				return b(Blocks.STONE_BRICKS);
			}
			// Lanternes d'âmes aux coins, chaînes qui pendent du plafond.
			if (y == HALL_FLOOR + 1 && (x == 1 || x == sx - 2) && (z == 1 || z == sz - 6)) {
				return b(Blocks.SOUL_LANTERN);
			}
			if (y == HALL_CEILING - 1 && x % 6 == 3 && z % 6 == 3) {
				return b(Blocks.IRON_CHAIN);
			}
			return AIR;
		}

		private static BlockState ironBars(int x, int sx) {
			return b(Blocks.IRON_BARS).setValue(CrossCollisionBlock.WEST, x % 5 != 1).setValue(CrossCollisionBlock.EAST,
					x % 5 != 4);
		}

		private BlockState shaft(int x, int y, int z) {
			if (Math.abs(x - SHAFT_X) > 1 || Math.abs(z - SHAFT_Z) > 1) {
				return null;
			}
			if (x == SHAFT_X && z == SHAFT_Z) {
				return ladder();
			}
			return b(Blocks.STONE_BRICKS);
		}

		private static BlockState ladder() {
			// Tournée vers le nord : elle s'appuie sur le bloc au sud.
			return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
		}

		/** Le poste de garde : quatre murs à moitié écroulés, un toit percé, la trappe du puits. */
		private BlockState guardhouse(int x, int y, int z, Plot p) {
			if (x < 8 || x > 16 || z > 8) {
				return null;
			}
			boolean edge = x == 8 || x == 16 || z == 0 || z == 8;
			if (y == -1) {
				return x == SHAFT_X && z == SHAFT_Z ? ladder() : b(Blocks.STONE_BRICKS);
			}
			if (x == SHAFT_X && z == SHAFT_Z + 1 && y <= 1) {
				return b(Blocks.STONE_BRICKS);
			}
			if (x == SHAFT_X && z == SHAFT_Z && y == 0) {
				return ladder();
			}
			if (y <= 3) {
				if (!edge) {
					return AIR;
				}
				if (z == 0 && x == 12 && y <= 1) {
					return AIR;
				}
				// La ruine : plus on monte, plus il manque de pierres.
				if (p.noise(x, y, z, 6) < y - 1) {
					return AIR;
				}
				return p.noise(x, y, z, 3) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.MOSSY_STONE_BRICKS);
			}
			if (y == 4) {
				return p.noise(x, y, z, 3) == 0 ? b(Blocks.STONE_BRICK_SLAB) : AIR;
			}
			return null;
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(
					new Chest(new BlockPos(2, HALL_FLOOR + 1, 2), Laboratory5::research),
					new Chest(new BlockPos(p.sizeX() - 3, HALL_FLOOR + 1, 2), Laboratory5::research));
		}

		/** Les restes des recherches de l'armée sur la Pierre. */
		private static List<ItemStack> research(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.PAPER, 2 + p.noise(7, 0, 0, 6)));
			out.add(new ItemStack(Items.REDSTONE, 3 + p.noise(8, 0, 0, 8)));
			out.add(new ItemStack(Items.GOLD_INGOT, 1 + p.noise(9, 0, 0, 3)));
			out.add(new ItemStack(FmabItems.ALCHEMICAL_INK, 1 + p.noise(10, 0, 0, 2)));
			if (p.noise(11, 0, 0, 2) == 0) {
				out.add(Tomes.stack(Tomes.FORMS));
			}
			// Kimblee y a fait ses recherches avant sa prison.
			if (p.noise(12, 0, 0, 4) == 0) {
				out.add(new ItemStack(FmabItems.CRIMSON_SEALS));
			}
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			return List.of(
					// Barry le Boucher garde le poste en ruine, au-dessus du puits.
					new Spawn(FmabEntities.BARRY, new BlockPos(10, 0, 6)),
					new Spawn(FmabEntities.LUST, new BlockPos(sx / 2 + 3, HALL_FLOOR + 1, 12)),
					new Spawn(FmabEntities.GLUTTONY, new BlockPos(sx / 2 - 4, HALL_FLOOR + 1, 10)),
					new Spawn(FmabEntities.HAUNTED_ARMOR, new BlockPos(4, HALL_FLOOR + 1, 6)),
					new Spawn(FmabEntities.HAUNTED_ARMOR, new BlockPos(sx - 5, HALL_FLOOR + 1, 6)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(sx / 2, HALL_FLOOR + 1, 7)),
					// Dans les cellules, les essais ratés de l'armée.
					new Spawn(FmabEntities.CHIMERA_CRAWLER, new BlockPos(2, HALL_FLOOR + 1, sz - 3)),
					// Au milieu d'une cellule : la bête est large.
					new Spawn(FmabEntities.CHIMERA_BEAST, new BlockPos(12, HALL_FLOOR + 1, sz - 3)),
					new Spawn(FmabEntities.CHIMERA_CRAWLER, new BlockPos(sx - 3, HALL_FLOOR + 1, sz - 3)));
		}
	}

	private static final Palette DEVILS_NEST_PALETTE = new Palette(b(Blocks.DARK_OAK_PLANKS), b(Blocks.DARK_OAK_LOG),
			b(Blocks.SPRUCE_PLANKS), b(Blocks.COBBLESTONE), Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_PLANKS),
			Blocks.DARK_OAK_DOOR);

	/**
	 * Le Devil's Nest, le bar de Greed à Dublith : un comptoir le long du mur du fond, des tonneaux,
	 * des tables, une lanterne au plafond, et la réserve d'or du patron.
	 */
	static final class DevilsNest implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			BlockState shell = house(x, y, z, p, DEVILS_NEST_PALETTE);
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
			if (!inside) {
				return shell;
			}
			if (y == 1) {
				// Le comptoir : des tonneaux contre le mur, le zinc devant.
				if (z == sz - 2) {
					return x % 3 == 0 ? b(Blocks.BREWING_STAND) : b(Blocks.BARREL);
				}
				if (z == sz - 3 && x > 1 && x < sx - 2) {
					return b(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
				}
				// Les tables : un piquet et un plateau.
				if (z == 2 && x % 4 == 2) {
					return b(Blocks.DARK_OAK_FENCE);
				}
			}
			if (y == 2 && z == 2 && x % 4 == 2) {
				return b(Blocks.DARK_OAK_PRESSURE_PLATE);
			}
			if (y == 3 && x == sx / 2 && z == sz / 2) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			return shell;
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), plot -> List.of(
					new ItemStack(Items.GOLD_INGOT, 6 + plot.noise(1, 0, 0, 10)),
					new ItemStack(Items.EMERALD, 2 + plot.noise(2, 0, 0, 6)),
					new ItemStack(Items.GLASS_BOTTLE, 3 + plot.noise(3, 0, 0, 5)))));
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(new Spawn(FmabEntities.GREED, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() - 4)));
		}
	}

	/**
	 * La résidence Bradley, à Central : une demeure de briques à deux étages, éclairée de lanternes
	 * (une pénombre où les ombres de Pride se plaisent). Le Généralissime et son fils y vivent.
	 */
	static final class BradleyResidence implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			BlockState shell = house(x, y, z, p, CITY_PALETTES[0]);
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
			if (inside && (y == 3 || y == 7) && x % 5 == 2 && z % 4 == 2) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			if (inside && y == 1 && z == sz - 2 && x % 3 == 1) {
				return b(Blocks.BOOKSHELF);
			}
			return shell;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(
					new Spawn(FmabEntities.WRATH, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2)),
					new Spawn(FmabEntities.PRIDE, new BlockPos(3, 1, p.sizeZ() - 3)));
		}
	}

	/**
	 * Le grand tunnel que Sloth creuse sous Central, pour le cercle de transmutation national : un
	 * boyau de pierre de cinq sur cinq, à trente blocs sous la ville, d'un rempart à l'autre. Un puits
	 * à échelle y descend depuis l'avenue sud.
	 */
	static final class SlothTunnel implements Blueprint {
		static final int FLOOR = -28;
		static final int CEILING = -22;
		private static final int SHAFT_Z = 3;
		/** La porte de Père, dans la paroi sud, à quelques pas du puits : laissée au plan de Père. */
		static final int FATHER_DOOR = 6;

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int sx = p.sizeX();
			int shaftX = sx / 2;
			if (y > CEILING) {
				// Le puits, de l'avenue jusqu'au plafond du tunnel.
				if (y >= 0 || Math.abs(x - shaftX) > 1 || Math.abs(z - SHAFT_Z) > 1) {
					return null;
				}
				if (x == shaftX && z == SHAFT_Z) {
					return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
				}
				return b(Blocks.STONE_BRICKS);
			}
			if (y < FLOOR || x < 10 || x > sx - 11) {
				return null;
			}
			if (x == shaftX + FATHER_DOOR && z == p.sizeZ() - 1 && (y == FLOOR + 1 || y == FLOOR + 2)) {
				return null;
			}
			boolean end = x == 10 || x == sx - 11;
			boolean shell = y == FLOOR || y == CEILING || z == 0 || z == p.sizeZ() - 1 || end;
			if (x == shaftX && z == SHAFT_Z && y == CEILING) {
				return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
			}
			if (x == shaftX && z == SHAFT_Z) {
				return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
			}
			if (x == shaftX && z == SHAFT_Z + 1) {
				return b(Blocks.STONE_BRICKS);
			}
			if (shell) {
				return p.noise(x, y, z, 5) == 0 ? b(Blocks.COBBLED_DEEPSLATE) : b(Blocks.DEEPSLATE_BRICKS);
			}
			if (y == FLOOR + 1 && z == 1 && x % 12 == 0) {
				return b(Blocks.SOUL_LANTERN);
			}
			return AIR;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int mid = p.sizeX() / 2;
			return List.of(new Spawn(FmabEntities.SLOTH, new BlockPos(mid + 30, FLOOR + 1, 3)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(mid - 20, FLOOR + 1, 3)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(mid + 12, FLOOR + 1, 3)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(mid + 50, FLOOR + 1, 3)));
		}
	}

	/**
	 * Le repaire de Père, sous le tunnel de Sloth. Une porte scellée par l'Ouroboros perce la paroi
	 * sud du tunnel ; derrière, un boyau, puis un puits qui plonge trente blocs plus bas dans la salle
	 * du trône : un sol gravé du cercle national, des tuyaux partout, et le trône sur son estrade.
	 *
	 * <p>Coordonnées : la porte en (16, z 0), dans la paroi du tunnel ; le boyau en z 1..2 ; le puits
	 * en (16, 4) ; la salle couvre x 0..32, z 2..40, du sol en y = −58 au plafond en y = −44.
	 */
	static final class FatherLair implements Blueprint {
		static final int ROOM_FLOOR = -58;
		static final int ROOM_CEILING = -44;
		static final int DOOR_X = 16;
		private static final int SHAFT_Z = 4;
		private static final int ROOM_Z = 2;
		private static final int CIRCLE_Z = 22;
		private static final int THRONE_Z = 37;

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int tunnel = SlothTunnel.FLOOR;
			if (y >= tunnel) {
				return corridor(x, y, z, tunnel);
			}
			if (y > ROOM_CEILING) {
				return shaft(x, z);
			}
			if (y >= ROOM_FLOOR) {
				return room(x, y, z, p);
			}
			return null;
		}

		/** La porte scellée, puis le boyau jusqu'au puits. */
		private static BlockState corridor(int x, int y, int z, int floor) {
			if (z == 0) {
				return x == DOOR_X && (y == floor + 1 || y == floor + 2) ? b(FmabBlocks.FATHER_SEAL) : null;
			}
			if (Math.abs(x - DOOR_X) > 1 || z > SHAFT_Z + 1 || y > floor + 3) {
				return null;
			}
			if (x == DOOR_X && z == SHAFT_Z && y < floor + 3) {
				return ladder();
			}
			boolean open = x == DOOR_X && y > floor && y < floor + 3 && z < SHAFT_Z + 1;
			return open ? AIR : b(Blocks.DEEPSLATE_TILES);
		}

		private static BlockState shaft(int x, int z) {
			if (Math.abs(x - DOOR_X) > 1 || Math.abs(z - SHAFT_Z) > 1) {
				return null;
			}
			return x == DOOR_X && z == SHAFT_Z ? ladder() : b(Blocks.DEEPSLATE_TILES);
		}

		private static BlockState room(int x, int y, int z, Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			if (z < ROOM_Z) {
				return null;
			}
			boolean wall = x == 0 || x == sx - 1 || z == ROOM_Z || z == sz - 1;
			if (y == ROOM_FLOOR) {
				return floor(x, z);
			}
			if (y == ROOM_CEILING) {
				return x == DOOR_X && z == SHAFT_Z ? ladder() : b(Blocks.DEEPSLATE_TILES);
			}
			if (wall) {
				return p.noise(x, y, z, 6) == 0 ? b(Blocks.CRACKED_DEEPSLATE_BRICKS) : b(Blocks.DEEPSLATE_BRICKS);
			}
			// L'échelle du puits descend jusqu'au sol, contre un pilier de tuyaux.
			if (x == DOOR_X && z == SHAFT_Z) {
				return ladder();
			}
			if (x == DOOR_X && z == SHAFT_Z + 1) {
				return pipe(Direction.Axis.Y);
			}
			// Les tuyaux : verticaux le long des murs, horizontaux sous le plafond vers le trône.
			if ((x == 1 || x == sx - 2) && z % 3 == 0 || z == sz - 2 && x % 3 == 0) {
				return pipe(Direction.Axis.Y);
			}
			if (y == ROOM_CEILING - 1 && (x == DOOR_X - 4 || x == DOOR_X + 4)) {
				return pipe(Direction.Axis.Z);
			}
			if (y == ROOM_CEILING - 1 && z == THRONE_Z && x > 1 && x < sx - 2) {
				return pipe(Direction.Axis.X);
			}
			BlockState throne = throne(x, y, z);
			if (throne != null) {
				return throne;
			}
			if (y == ROOM_FLOOR + 1 && (x == 3 || x == sx - 4) && z % 8 == 6) {
				return b(Blocks.SOUL_LANTERN);
			}
			return AIR;
		}

		/** Le sol : le cercle de transmutation national, gravé au rouge dans l'ardoise. */
		private static BlockState floor(int x, int z) {
			double d = Math.hypot(x - DOOR_X, z - CIRCLE_Z);
			double a = Math.atan2(z - CIRCLE_Z, x - DOOR_X);
			boolean ring = Math.abs(d - 13) < 0.6 || Math.abs(d - 8) < 0.5;
			// Un heptagone entre les deux anneaux : sept pointes, comme le pays.
			boolean spoke = d > 8 && d < 13 && Math.abs(Math.sin(3.5 * a)) < 0.08;
			if (ring || spoke || d < 1.5) {
				return b(Blocks.CONCRETE.pick(DyeColor.RED));
			}
			return (x + z) % 2 == 0 ? b(Blocks.POLISHED_DEEPSLATE) : b(Blocks.DEEPSLATE_TILES);
		}

		/** L'estrade (trois marches) et le trône, dossier de tuyaux. */
		private static BlockState throne(int x, int y, int z) {
			if (Math.abs(x - DOOR_X) > 5 || z < THRONE_Z - 4) {
				return null;
			}
			int step = Math.min(3, z - (THRONE_Z - 5));
			int h = y - ROOM_FLOOR;
			if (h <= step) {
				return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
			}
			if (x == DOOR_X && z == THRONE_Z && h == 4) {
				return stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH);
			}
			if (x == DOOR_X && z == THRONE_Z + 1 && h >= 4 && h <= 8) {
				return pipe(Direction.Axis.Y);
			}
			if (Math.abs(x - DOOR_X) == 1 && z == THRONE_Z && h == 4) {
				return b(Blocks.POLISHED_BLACKSTONE_WALL);
			}
			return null;
		}

		private static BlockState pipe(Direction.Axis axis) {
			return b(FmabBlocks.FATHER_PIPE).setValue(RotatedPillarBlock.AXIS, axis);
		}

		private static BlockState ladder() {
			return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(new Spawn(FmabEntities.FATHER, new BlockPos(DOOR_X, ROOM_FLOOR + 4, THRONE_Z - 1)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(6, ROOM_FLOOR + 1, 12)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(26, ROOM_FLOOR + 1, 12)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(6, ROOM_FLOOR + 1, 30)),
					new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(26, ROOM_FLOOR + 1, 30)));
		}
	}

	/**
	 * Le pavillon de Xing : une cour de 23 sur 23 fermée de murs de briques de boue, une porte entre
	 * deux piliers rouges, des lanternes de pierre aux coins, des massifs d'azalées ; au fond, le
	 * pavillon surélevé, piliers rouges, cloisons claires et trois rangs de toits de tuiles.
	 */
	static final class XingPavilion implements Blueprint {
		private static final int P0 = 6;
		private static final int P1 = 16;
		private static final int Z0 = 8;
		private static final int Z1 = 18;
		private static final BlockState PILLAR = b(Blocks.STRIPPED_MANGROVE_LOG);
		private static final BlockState TILE = b(Blocks.DEEPSLATE_TILES);

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int s = p.sizeX();
			if (y < 0) {
				return b(Blocks.PACKED_MUD);
			}
			if (y == 0) {
				boolean path = Math.abs(x - s / 2) <= 1 && z < Z0;
				return path ? b(Blocks.MUD_BRICKS) : p.noise(x, y, z, 4) == 0 ? b(Blocks.GRAVEL) : b(Blocks.PACKED_MUD);
			}
			BlockState wall = wall(x, y, z, s);
			if (wall != null) {
				return wall;
			}
			BlockState pavilion = pavilion(x, y, z);
			if (pavilion != null) {
				return pavilion;
			}
			BlockState roof = roof(x, y, z);
			if (roof != null) {
				return roof;
			}
			boolean corner = (x == 3 || x == s - 4) && (z == 3 || z == s - 4);
			if (corner && y == 1) {
				return b(Blocks.STONE_BRICK_WALL);
			}
			if (corner && y == 2) {
				return b(Blocks.LANTERN);
			}
			if (y == 1 && (x == 2 || x == s - 3) && z > 5 && z < s - 5 && z % 3 == 0) {
				return b(Blocks.FLOWERING_AZALEA_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
			}
			return AIR;
		}

		/** Le mur d'enceinte, et la porte entre deux piliers rouges sous un petit toit. */
		private static BlockState wall(int x, int y, int z, int s) {
			boolean edge = x == 0 || x == s - 1 || z == 0 || z == s - 1;
			int mid = s / 2;
			if (z == 0 && (x == mid - 2 || x == mid + 2) && y <= 5) {
				return PILLAR;
			}
			if (z == 0 && Math.abs(x - mid) <= 3 && y == 6) {
				return b(Blocks.DEEPSLATE_TILE_SLAB);
			}
			if (z == 0 && Math.abs(x - mid) <= 2 && y == 5) {
				return TILE;
			}
			if (!edge || y > 4) {
				return null;
			}
			if (z == 0 && Math.abs(x - mid) <= 1) {
				return AIR;
			}
			return y == 4 ? b(Blocks.DEEPSLATE_TILE_SLAB) : b(Blocks.MUD_BRICKS);
		}

		/** Le pavillon : l'estrade, les piliers, les cloisons, et ce qu'on trouve dedans. */
		private static BlockState pavilion(int x, int y, int z) {
			if (x < P0 || x > P1 || z < Z0 || z > Z1 || y > 5) {
				return null;
			}
			if (y == 1) {
				return b(Blocks.POLISHED_GRANITE);
			}
			boolean pillar = (x == P0 || x == P1 || x == (P0 + P1) / 2) && (z == Z0 || z == Z1 || z == (Z0 + Z1) / 2)
					&& !(x == (P0 + P1) / 2 && z == (Z0 + Z1) / 2);
			if (pillar) {
				return PILLAR;
			}
			boolean side = x == P0 || x == P1 || z == Z1;
			if (side) {
				// Des cloisons claires, une bande ouverte à hauteur des yeux.
				return y == 3 ? AIR : b(Blocks.BIRCH_PLANKS);
			}
			if (y == 5 && x == (P0 + P1) / 2 && z == (Z0 + Z1) / 2) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			return AIR;
		}

		/** Trois rangs de toits en gradins, les tuiles en pente vers l'extérieur. */
		private static BlockState roof(int x, int y, int z) {
			int tier = y - 6;
			if (tier < 0 || tier > 3) {
				return null;
			}
			int cx = (P0 + P1) / 2, cz = (Z0 + Z1) / 2;
			if (tier == 3) {
				return x == cx && z == cz ? b(Blocks.GOLD_BLOCK) : null;
			}
			int half = 6 - 2 * tier;
			int dx = x - cx, dz = z - cz;
			if (Math.abs(dx) > half || Math.abs(dz) > half) {
				return null;
			}
			if (Math.abs(dz) == half) {
				return stairs(Blocks.DEEPSLATE_TILE_STAIRS, dz < 0 ? Direction.SOUTH : Direction.NORTH);
			}
			if (Math.abs(dx) == half) {
				return stairs(Blocks.DEEPSLATE_TILE_STAIRS, dx < 0 ? Direction.EAST : Direction.WEST);
			}
			return TILE;
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(new Chest(new BlockPos(P1 - 2, 2, Z1 - 2), XingPavilion::stock));
		}

		private static List<ItemStack> stock(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(FmabItems.KUNAI, 3 + p.noise(1, 0, 0, 5)));
			out.add(new ItemStack(Items.EMERALD, 2 + p.noise(2, 0, 0, 4)));
			out.add(new ItemStack(Items.BAMBOO, 4 + p.noise(3, 0, 0, 8)));
			out.add(new ItemStack(Items.PAPER, 2 + p.noise(4, 0, 0, 4)));
			if (p.noise(5, 0, 0, 2) == 0) {
				out.add(new ItemStack(FmabItems.XING_SWORD));
			}
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(new Spawn(FmabEntities.MAY_CHANG, new BlockPos((P0 + P1) / 2, 2, Z0 + 3)));
		}
	}

	/**
	 * Fort Briggs. Au nord (z 0..13), le glacis : on n'y touche pas, c'est là que rôde Drachma. Puis la
	 * muraille, épaisse de cinq blocs et haute de vingt, crénelée, deux tours pleines à ses bouts. Dos
	 * au mur, la caserne de trois niveaux (une échelle monte jusqu'au chemin de ronde), et derrière,
	 * la cour d'arrivée, ouverte vers Amestris.
	 */
	static final class FortBriggs implements Blueprint {
		static final int SIZE_X = 41;
		static final int SIZE_Y = 27;
		static final int SIZE_Z = 37;
		private static final int WALL_Z0 = 14;
		private static final int WALL_Z1 = 18;
		private static final int WALL_TOP = 20;
		private static final int KEEP_Z1 = 30;
		private static final int KEEP_TOP = 18;
		/** Hors des tours (x ≤ 4), contre la muraille. */
		private static final int LADDER_X = 5;
		private static final int MID = SIZE_X / 2;

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			if (z < WALL_Z0 - 1) {
				return null;
			}
			boolean tower = (x <= 4 || x >= SIZE_X - 5) && z >= WALL_Z0 - 1 && z <= WALL_Z1 + 1;
			if (tower) {
				return tower(x, y, z);
			}
			if (z < WALL_Z0) {
				return null;
			}
			if (y < 0) {
				return b(Blocks.STONE_BRICKS);
			}
			if (z <= WALL_Z1) {
				return wall(x, y, z, p);
			}
			if (z <= KEEP_Z1) {
				return keep(x, y, z, p);
			}
			return yard(x, y, z);
		}

		private static BlockState tower(int x, int y, int z) {
			if (y < 0 || y <= 24) {
				return b(Blocks.STONE_BRICKS);
			}
			boolean rim = x == 0 || x == 4 || x == SIZE_X - 5 || x == SIZE_X - 1 || z == WALL_Z0 - 1 || z == WALL_Z1 + 1;
			return y == 25 && rim && (x + z) % 2 == 0 ? b(Blocks.STONE_BRICK_WALL) : AIR;
		}

		private static BlockState wall(int x, int y, int z, Plot p) {
			if (y <= WALL_TOP) {
				if (y == WALL_TOP) {
					return b(Blocks.SMOOTH_STONE);
				}
				return p.noise(x, y, z, 9) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			}
			if (y == WALL_TOP + 1 && z == WALL_Z0 && x % 2 == 0) {
				return b(Blocks.STONE_BRICK_WALL);
			}
			if (y == WALL_TOP + 1 && z == WALL_Z0 + 2 && x % 8 == 4) {
				// Les canons de Briggs, pointés vers le nord.
				return b(Blocks.BLACKSTONE);
			}
			if (y == WALL_TOP + 1 && z == WALL_Z0 + 1 && x % 8 == 4) {
				return b(Blocks.POLISHED_BLACKSTONE_WALL);
			}
			if (y == WALL_TOP + 2 && z == WALL_Z0 + 2 && x % 8 == 4) {
				return b(Blocks.LANTERN);
			}
			return AIR;
		}

		private static BlockState keep(int x, int y, int z, Plot p) {
			if (x <= 1 || x >= SIZE_X - 2) {
				return y <= KEEP_TOP ? b(Blocks.STONE_BRICKS) : AIR;
			}
			if (y > KEEP_TOP) {
				// L'échelle continue jusqu'au chemin de ronde, contre la muraille.
				return x == LADDER_X && z == WALL_Z1 + 1 && y <= WALL_TOP ? ladder() : AIR;
			}
			if (x == LADDER_X && z == WALL_Z1 + 1 && y > 0) {
				return ladder();
			}
			boolean shell = x == 2 || x == SIZE_X - 3 || z == KEEP_Z1;
			if (y == 0) {
				return b(Blocks.POLISHED_ANDESITE);
			}
			if (y == KEEP_TOP) {
				return b(Blocks.SMOOTH_STONE);
			}
			if (shell) {
				if (z == KEEP_Z1 && Math.abs(x - MID) <= 1 && y <= 3) {
					return AIR;
				}
				if (z == KEEP_Z1 && y % 6 >= 2 && y % 6 <= 3 && x % 4 == 0) {
					return b(Blocks.GLASS);
				}
				return p.noise(x, y, z, 11) == 0 ? b(Blocks.MOSSY_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			}
			if (y == 6 || y == 12) {
				return b(Blocks.SPRUCE_PLANKS);
			}
			if ((y == 1 || y == 7 || y == 13) && z == KEEP_Z1 - 1 && x % 6 == 3) {
				return b(Blocks.LANTERN);
			}
			if (y == 1 && z == WALL_Z1 + 4 && x == MID) {
				return b(Blocks.CAMPFIRE);
			}
			return AIR;
		}

		private static BlockState yard(int x, int y, int z) {
			if (y == 0) {
				return b(Blocks.STONE_BRICKS);
			}
			if ((x == 0 || x == SIZE_X - 1) && y <= 2) {
				return b(Blocks.COBBLESTONE_WALL);
			}
			return y <= 6 ? AIR : null;
		}

		private static BlockState ladder() {
			// Tournée vers le sud : elle s'appuie sur la muraille, au nord.
			return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(
					new Chest(new BlockPos(6, 1, KEEP_Z1 - 2), FortBriggs::armory),
					new Chest(new BlockPos(SIZE_X - 7, 1, KEEP_Z1 - 2), FortBriggs::armory),
					new Chest(new BlockPos(MID, 7, KEEP_Z1 - 2), FortBriggs::stores));
		}

		private static List<ItemStack> armory(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.IRON_INGOT, 3 + p.noise(1, 0, 0, 6)));
			out.add(new ItemStack(Items.ARROW, 6 + p.noise(2, 0, 0, 10)));
			if (p.noise(3, 0, 0, 3) == 0) {
				out.add(new ItemStack(FmabItems.BRIGGS_SABRE));
			}
			if (p.noise(4, 0, 0, 4) == 0) {
				out.add(new ItemStack(p.noise(5, 0, 0, 2) == 0 ? FmabItems.BRIGGS_AUTOMAIL_ARM : FmabItems.BRIGGS_AUTOMAIL_LEG));
			}
			return out;
		}

		private static List<ItemStack> stores(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.BREAD, 4 + p.noise(6, 0, 0, 6)));
			out.add(new ItemStack(Items.COAL, 6 + p.noise(7, 0, 0, 10)));
			out.add(new ItemStack(Items.LEATHER, 2 + p.noise(8, 0, 0, 4)));
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(new Spawn(FmabEntities.OLIVIER, new BlockPos(MID, 1, WALL_Z1 + 6)),
					new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(8, 1, WALL_Z1 + 4)),
					new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(SIZE_X - 9, 1, WALL_Z1 + 4)),
					new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(MID, 7, WALL_Z1 + 5)),
					new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(MID - 6, 1, KEEP_Z1 + 3)));
		}
	}

	/**
	 * Le champ d'un massacre : une terre brûlée, des pans de murs écroulés, des os et du sang figé ; au
	 * centre, sur une estrade de pierre noire, le point de sang du cercle national.
	 */
	private static BlockState bloodCrest(int x, int y, int z, Blueprint.Plot p) {
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

	private static final Palette[] DUBLITH_PALETTES = {
			new Palette(b(Blocks.BRICKS), b(Blocks.SPRUCE_LOG), b(Blocks.OAK_PLANKS), b(Blocks.COBBLESTONE),
					Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), Blocks.OAK_DOOR),
			new Palette(b(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)), b(Blocks.DARK_OAK_LOG), b(Blocks.SPRUCE_PLANKS),
					b(Blocks.COBBLESTONE), Blocks.BRICK_STAIRS, b(Blocks.BRICKS), Blocks.DARK_OAK_DOOR),
	};

	/** La place de Dublith : des pavés, un lampadaire aux quatre coins, un abreuvoir au centre. */
	private static BlockState dublithSquare(int x, int y, int z, Blueprint.Plot p) {
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

	/**
	 * La boucherie des Curtis : une maison de brique à deux étages ; au rez-de-chaussée, l'étal (des
	 * tonneaux, un fumoir), des chaînes et leurs crochets au plafond ; Izumi derrière son comptoir.
	 */
	static final class CurtisButcher implements Blueprint {
		private static final Palette PALETTE = new Palette(b(Blocks.BRICKS), b(Blocks.DARK_OAK_LOG),
				b(Blocks.SPRUCE_PLANKS), b(Blocks.STONE_BRICKS), Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.BRICKS),
				Blocks.DARK_OAK_DOOR);

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			BlockState shell = house(x, y, z, p, PALETTE);
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
			if (!inside) {
				return shell;
			}
			// L'étal, en travers de la boutique, une ouverture au milieu.
			if (y == 1 && z == 3 && Math.abs(x - sx / 2) > 1) {
				return x % 2 == 0 ? b(Blocks.BARREL) : b(Blocks.SMOKER);
			}
			// Les crochets à viande : des chaînes qui pendent du plafond, au fond.
			if ((y == 2 || y == 3) && z >= 5 && z <= sz - 2 && x % 2 == 1) {
				return b(Blocks.IRON_CHAIN);
			}
			if (y == 1 && z == sz - 2 && x == sx - 2) {
				return b(Blocks.CAULDRON);
			}
			return shell;
		}

		@Override
		public List<Chest> chests(Plot p) {
			return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), CurtisButcher::stock));
		}

		private static List<ItemStack> stock(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.BEEF, 3 + p.noise(1, 0, 0, 5)));
			out.add(new ItemStack(Items.PORKCHOP, 2 + p.noise(2, 0, 0, 5)));
			out.add(new ItemStack(Items.MUTTON, 2 + p.noise(3, 0, 0, 4)));
			out.add(new ItemStack(Items.BONE, 2 + p.noise(4, 0, 0, 4)));
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			return List.of(new Spawn(FmabEntities.IZUMI, new BlockPos(p.sizeX() / 2, 1, 4)));
		}
	}

	/**
	 * Liore, ville du désert : une place de grès, des maisons basses autour, et au nord le temple de
	 * Leto, colonnes et dôme d'or, où le père Cornello fait ses « miracles ». Sous l'autel, la pierre
	 * rouge ; sur le parvis, le point de sang de l'émeute.
	 */
	static final class Liore implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int s = p.sizeX(), c = s / 2;
			if (y < 0) {
				return b(Blocks.SANDSTONE);
			}
			if (y == 0) {
				return Math.abs(x - c) <= 2 || Math.abs(z - c) <= 2 ? b(Blocks.SMOOTH_SANDSTONE) : b(Blocks.SAND);
			}
			BlockState temple = temple(x, y, z, c);
			if (temple != null) {
				return temple;
			}
			// Des maisons basses de part et d'autre du temple, et aux coins sud.
			for (int[] h : new int[][]{{2, 4}, {s - 9, 4}, {2, s - 9}, {s - 9, s - 9}}) {
				if (x >= h[0] && x < h[0] + 7 && z >= h[1] && z < h[1] + 7 && y <= 5) {
					boolean edge = x == h[0] || x == h[0] + 6 || z == h[1] || z == h[1] + 6;
					if (y == 5) {
						return b(Blocks.SMOOTH_SANDSTONE_SLAB);
					}
					if (!edge) {
						return y == 1 && x == h[0] + 1 && z == h[1] + 1 ? b(Blocks.BARREL) : AIR;
					}
					boolean door = z == h[1] && x == h[0] + 3 && y <= 2;
					boolean window = y == 3 && (x == h[0] + 3 || z == h[1] + 3);
					return door || window ? AIR : b(Blocks.SANDSTONE);
				}
			}
			if (y == 1 && x == c && z == c + 4) {
				return b(FmabBlocks.BLOOD_CREST);
			}
			return y <= 8 ? AIR : null;
		}

		/** Le temple de Leto : 13 de large, 11 de profond, colonnes, autel, dôme d'or. */
		private static BlockState temple(int x, int y, int z, int c) {
			int x0 = c - 6, x1 = c + 6, z0 = 1, z1 = 11;
			if (x < x0 || x > x1 || z < z0 || z > z1 || y > 13) {
				return null;
			}
			boolean wall = x == x0 || x == x1 || z == z1;
			if (y <= 7) {
				if (z == z0) {
					// La façade : des colonnes, la porte au milieu.
					return (x - x0) % 3 == 0 ? b(Blocks.CUT_SANDSTONE) : AIR;
				}
				if (y == 7) {
					return b(Blocks.SMOOTH_SANDSTONE);
				}
				if (wall) {
					return y == 4 && (x - x0) % 3 == 1 ? b(Blocks.STAINED_GLASS.pick(DyeColor.RED)) : b(Blocks.SMOOTH_SANDSTONE);
				}
				// L'autel, la statue de Leto, la pierre rouge cachée dessous.
				if (z == z1 - 2 && Math.abs(x - c) <= 1) {
					if (y == 1) {
						return b(Blocks.CHISELED_SANDSTONE);
					}
					if (y == 2 && x == c) {
						return b(Blocks.GOLD_BLOCK);
					}
					if (y == 3 && x == c) {
						return b(Blocks.GOLD_BLOCK);
					}
				}
				if (y == 1 && Math.abs(x - c) <= 1 && z == z1 - 1) {
					return b(FmabBlocks.RED_STONE_ORE);
				}
				return AIR;
			}
			// Le dôme d'or.
			double d = Math.hypot(x - c, (z - (z0 + z1) / 2.0) * 1.2);
			double r = 6 - (y - 8) * 1.2;
			return r > 0 && d <= r && d > r - 1.2 ? b(Blocks.GOLD_BLOCK) : null;
		}

		@Override
		public List<Chest> chests(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Chest(new BlockPos(c + 4, 1, 9), Liore::offerings));
		}

		private static List<ItemStack> offerings(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.GOLD_INGOT, 2 + p.noise(1, 0, 0, 5)));
			out.add(new ItemStack(Items.CANDLE, 3 + p.noise(2, 0, 0, 4)));
			if (p.noise(3, 0, 0, 2) == 0) {
				out.add(new ItemStack(FmabItems.RED_STONE_SHARD));
			}
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Spawn(FmabEntities.CORNELLO, new BlockPos(c, 1, 7)));
		}
	}

	/**
	 * Les ruines d'Ishval : des maisons de terre écroulées, un temple au toit effondré, et au centre le
	 * point de sang de la guerre d'extermination. Scar y erre.
	 */
	static final class IshvalRuins implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int s = p.sizeX(), c = s / 2;
			if (y < 0) {
				return b(Blocks.SANDSTONE);
			}
			if (y == 0) {
				int n = p.noise(x, 0, z, 6);
				return n == 0 ? b(Blocks.COARSE_DIRT) : n == 1 ? b(Blocks.RED_SAND) : b(Blocks.SAND);
			}
			if (y == 1 && x == c && z == c) {
				return b(FmabBlocks.BLOOD_CREST);
			}
			// Le temple au centre : des murs à hauteur variable, sans toit.
			int dx = Math.abs(x - c), dz = Math.abs(z - c);
			if (Math.max(dx, dz) == 5 && y <= 2 + p.noise(x, 7, z, 5) && !(dz == 5 && dx <= 1 && z > c)) {
				return p.noise(x, y, z, 3) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.CUT_RED_SANDSTONE);
			}
			// Les maisons de terre, en ruine, en cercle autour.
			for (int k = 0; k < 6; k++) {
				double a = Math.PI / 3 * k + 0.3;
				int hx = c + (int) Math.round(Math.cos(a) * 11), hz = c + (int) Math.round(Math.sin(a) * 11);
				int ex = Math.abs(x - hx), ez = Math.abs(z - hz);
				if (Math.max(ex, ez) == 2 && y <= 1 + p.noise(x, k, z, 4)) {
					return p.noise(x, y, z, 4) == 0 ? b(Blocks.PACKED_MUD) : b(Blocks.TERRACOTTA);
				}
			}
			if (y == 1 && p.noise(x, 3, z, 40) == 0) {
				return b(Blocks.DEAD_BUSH);
			}
			return y <= 6 ? AIR : null;
		}

		@Override
		public List<Chest> chests(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Chest(new BlockPos(c + 3, 1, c - 3), IshvalRuins::remains));
		}

		private static List<ItemStack> remains(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			out.add(new ItemStack(Items.BONE, 3 + p.noise(1, 0, 0, 5)));
			out.add(new ItemStack(Items.CLAY_BALL, 2 + p.noise(2, 0, 0, 4)));
			out.add(new ItemStack(Items.PAPER, 1 + p.noise(3, 0, 0, 3)));
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Spawn(FmabEntities.SCAR, new BlockPos(c - 2, 1, c + 2)));
		}
	}

	/**
	 * Les ruines de Xerxès : un parvis de grès à demi enfoui, gravé du grand cercle qui a dévoré le
	 * royaume en une nuit, et une forêt de colonnes brisées. Des fresques racontent la chute ;
	 * Hohenheim, le seul survivant, y revient.
	 */
	static final class XerxesRuins implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int s = p.sizeX(), c = s / 2;
			double d = Math.hypot(x - c, z - c);
			if (y < 0) {
				return b(Blocks.SANDSTONE);
			}
			if (y == 0) {
				if (d > c) {
					return null;
				}
				double a = Math.atan2(z - c, x - c);
				boolean ring = Math.abs(d - 14) < 0.6 || Math.abs(d - 9) < 0.5;
				boolean star = d < 14 && d > 3 && Math.abs(Math.sin(2.5 * a)) < 0.06;
				if (ring || star) {
					return b(Blocks.CUT_RED_SANDSTONE);
				}
				return p.noise(x, 0, z, 5) == 0 ? b(Blocks.SAND) : b(Blocks.SMOOTH_SANDSTONE);
			}
			// Les colonnes brisées, sur deux anneaux.
			boolean column = (Math.abs(d - 16) < 0.8 || Math.abs(d - 6) < 0.6) && p.noise(x, 0, z, 3) == 0;
			if (column && y <= 2 + p.noise(x, 1, z, 9)) {
				return y % 4 == 0 ? b(Blocks.CHISELED_SANDSTONE) : b(Blocks.CUT_SANDSTONE);
			}
			return d <= c && y <= 12 ? AIR : null;
		}

		@Override
		public List<Chest> chests(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Chest(new BlockPos(c - 4, 1, c), XerxesRuins::murals),
					new Chest(new BlockPos(c + 4, 1, c), XerxesRuins::murals));
		}

		/** Deux fragments de fresque, et l'or d'un royaume disparu. */
		private static List<ItemStack> murals(Plot p) {
			List<ItemStack> out = new ArrayList<>();
			int first = 1 + p.noise(4, 0, 0, XerxesMuralItem.COUNT);
			out.add(XerxesMuralItem.of(FmabItems.XERXES_MURAL, first));
			out.add(XerxesMuralItem.of(FmabItems.XERXES_MURAL, 1 + first % XerxesMuralItem.COUNT));
			out.add(new ItemStack(Items.GOLD_NUGGET, 4 + p.noise(5, 0, 0, 10)));
			return out;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int c = p.sizeX() / 2;
			return List.of(new Spawn(FmabEntities.HOHENHEIM, new BlockPos(c, 1, c + 2)));
		}
	}

	/**
	 * L'île de Yock : un cône de roche et de sable surgi de la mer (y = 0 au niveau de l'eau), une
	 * plage, de l'herbe, quelques arbres, la cabane des naufragés et un feu de camp.
	 */
	static final class YockIsland implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int c = p.sizeX() / 2;
			double d = Math.hypot(x - c, z - c);
			if (y < 0) {
				// Sous l'eau, le cône s'élargit vers le fond.
				double r = 10 + Math.min(2, -y * 0.25);
				return d <= r ? y > -3 ? b(Blocks.SAND) : b(Blocks.STONE) : null;
			}
			if (y == 0) {
				return d <= 10 ? d <= 7 ? b(Blocks.DIRT) : b(Blocks.SAND) : null;
			}
			if (y == 1) {
				if (d <= 7) {
					return b(Blocks.GRASS_BLOCK);
				}
				return d <= 9 ? b(Blocks.SAND) : null;
			}
			// La cabane : 5 sur 5, planches et toit de chaume (foin).
			int hx = c - 5, hz = c - 2;
			if (x >= hx && x < hx + 5 && z >= hz && z < hz + 5 && y >= 2 && y <= 5) {
				boolean edge = x == hx || x == hx + 4 || z == hz || z == hz + 4;
				if (y == 5) {
					return b(Blocks.HAY_BLOCK);
				}
				if (!edge) {
					return AIR;
				}
				return z == hz + 4 && x == hx + 2 && y <= 3 ? AIR : b(Blocks.OAK_PLANKS);
			}
			if (y == 2 && x == c + 2 && z == c + 2) {
				return b(Blocks.CAMPFIRE);
			}
			// Trois arbres.
			for (int[] t : new int[][]{{c + 4, c - 3}, {c - 1, c + 5}, {c + 5, c + 3}}) {
				double td = Math.hypot(x - t[0], z - t[1]);
				if (x == t[0] && z == t[1] && y >= 2 && y <= 5) {
					return b(Blocks.OAK_LOG);
				}
				if (y >= 5 && y <= 7 && td <= 2.5 - (y - 5) * 0.6 && !(x == t[0] && z == t[1] && y == 5)) {
					return b(Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
				}
			}
			return d <= 9 && y <= 8 ? AIR : null;
		}
	}

	/** La place de Rush Valley : un dallage de grès, des lanternes aux coins, un puits au centre. */
	private static BlockState square(int x, int y, int z, Blueprint.Plot p) {
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
