package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/**
 * Liore, ville du désert, dans son enceinte de grès percée de trois portes. Au nord, le temple de
 * Leto, colonnes et dôme d'or, où le père Cornello fait ses « miracles » ; l'allée bordée de
 * lanternes et de palmiers qui y mène ; au centre, la place, sa fontaine et ses étals ; à l'est, le
 * marché couvert de toile. Tout autour, une vingtaine de maisons à toit plat, d'un à trois étages,
 * chacune avec son échelle jusqu'à la terrasse. Sous l'autel, la pierre rouge ; sur l'allée, le
 * point de sang de l'émeute.
 *
 * <p>Le plan est dessiné pour une parcelle de 49 sur 49 (voir {@code worldgen/structure/liore.json}) :
 * le temple au nord (z 1 à 11), la rue principale d'ouest en est (z 27 à 29), la rue du sud (x 23 à
 * 25). Les maisons portent leurs abords ({@link Dressing#DESERT}) ; palmiers et étals viennent de
 * {@link Townscape}.
 */
final class Liore implements Blueprint {
	/** Le milieu de la parcelle. */
	private static final int C = 24;
	/** Le bord de la parcelle, où court l'enceinte. */
	private static final int EDGE = 48;

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private static final BlockState AIR = b(Blocks.AIR);

	/**
	 * Un plan posé dans un rectangle de la ville, sa façade (le z = 0 du plan) tournée vers
	 * {@code front}. Les plans posés se tiennent un bloc plus haut que la ville : leur sol (y = −1)
	 * est le pavé de la ville (y = 0).
	 *
	 * @param x0 coin nord-ouest du rectangle, dans la ville
	 * @param sx largeur du plan, le long de sa façade
	 * @param sz profondeur du plan
	 */
	private record Lot(int x0, int z0, int sx, int sz, int sizeY, Direction front, Blueprint plan, int salt) {
		private boolean across() {
			return front == Direction.WEST || front == Direction.EAST;
		}

		/** L'étendue du rectangle dans la ville, d'ouest en est. */
		int ex() {
			return across() ? sz : sx;
		}

		/** L'étendue du rectangle dans la ville, du nord au sud. */
		int ez() {
			return across() ? sx : sz;
		}

		boolean contains(int x, int z) {
			return x >= x0 && x < x0 + ex() && z >= z0 && z < z0 + ez();
		}

		Plot plot(Plot city) {
			return new Plot(sx, sizeY, sz, 0, city.seed() ^ salt * 0x9E3779B97F4A7C15L);
		}

		BlockState at(int x, int y, int z, Plot city) {
			int dx = x - x0, dz = z - z0;
			int cx, cz;
			Rotation turn;
			switch (front) {
				case SOUTH -> {
					cx = ex() - 1 - dx;
					cz = ez() - 1 - dz;
					turn = Rotation.CLOCKWISE_180;
				}
				case WEST -> {
					cx = ez() - 1 - dz;
					cz = dx;
					turn = Rotation.COUNTERCLOCKWISE_90;
				}
				case EAST -> {
					cx = dz;
					cz = ex() - 1 - dx;
					turn = Rotation.CLOCKWISE_90;
				}
				default -> {
					cx = dx;
					cz = dz;
					turn = Rotation.NONE;
				}
			}
			BlockState state = plan.at(cx, y - 1, cz, plot(city));
			return state == null ? null : state.rotate(turn);
		}
	}

	private static final Blueprint HOUSE = new Dressing(new Dwelling(), Dressing.DESERT);
	private static final Blueprint PALM = Townscape.palm(Blocks.SAND);
	private static final Blueprint STALL = Townscape.stall(Blocks.JUNGLE_FENCE, Blocks.SMOOTH_SANDSTONE_SLAB);

	/**
	 * Les maisons, par quartier : coin nord-ouest, largeur et profondeur des murs dans la ville,
	 * étages, et le côté de la porte.
	 */
	private static final Object[][] HOUSES = {
			// Nord-ouest, entre l'enceinte et le temple.
			{2, 2, 7, 6, 2, Direction.SOUTH}, {10, 2, 6, 7, 1, Direction.SOUTH}, {2, 10, 6, 7, 3, Direction.EAST},
			{10, 11, 6, 6, 2, Direction.EAST}, {2, 19, 7, 6, 1, Direction.SOUTH}, {10, 19, 6, 6, 2, Direction.SOUTH},
			// Nord-est, au-dessus du marché.
			{33, 2, 6, 7, 1, Direction.SOUTH}, {40, 2, 7, 6, 2, Direction.SOUTH}, {33, 11, 6, 6, 2, Direction.WEST},
			{41, 10, 6, 7, 3, Direction.WEST},
			// Sud-ouest.
			{2, 31, 7, 6, 2, Direction.NORTH}, {10, 31, 5, 6, 1, Direction.NORTH}, {2, 39, 6, 7, 1, Direction.NORTH},
			{9, 39, 6, 7, 3, Direction.NORTH}, {16, 38, 6, 6, 2, Direction.EAST},
			// Sud-est.
			{34, 31, 6, 6, 1, Direction.NORTH}, {41, 31, 6, 7, 2, Direction.NORTH}, {34, 39, 7, 7, 2, Direction.NORTH},
			{42, 40, 5, 6, 1, Direction.NORTH}, {27, 38, 6, 6, 3, Direction.WEST},
	};

	/** Tout ce qui se pose dans la ville : maisons, étals de la place et du marché, palmiers. */
	private static final List<Lot> LOTS = lots();

	private static List<Lot> lots() {
		List<Lot> out = new ArrayList<>();
		int salt = 1;
		for (Object[] h : HOUSES) {
			int x0 = (int) h[0], z0 = (int) h[1], ex = (int) h[2], ez = (int) h[3], floors = (int) h[4];
			Direction front = (Direction) h[5];
			boolean across = front == Direction.WEST || front == Direction.EAST;
			int m = Dressing.MARGIN;
			out.add(new Lot(x0 - m, z0 - m, (across ? ez : ex) + 2 * m, (across ? ex : ez) + 2 * m,
					4 * floors + 4, front, HOUSE, salt++));
		}
		// Les étals de la place, comptoir tourné vers la fontaine, et ceux du marché, vers la rue.
		for (int z : new int[]{22, 32}) {
			out.add(new Lot(16, z, 3, 3, 5, Direction.EAST, STALL, salt++));
			out.add(new Lot(30, z, 3, 3, 5, Direction.WEST, STALL, salt++));
		}
		for (int x = 35; x <= 43; x += 4) {
			out.add(new Lot(x, 20, 3, 3, 5, Direction.SOUTH, STALL, salt++));
		}
		// Les palmiers : deux de part et d'autre de l'allée, quatre autour de la fontaine.
		for (int[] at : new int[][]{{19, 17}, {29, 17}, {21, 23}, {27, 23}, {21, 33}, {27, 33}}) {
			out.add(new Lot(at[0] - 2, at[1] - 2, 5, 5, 10, Direction.NORTH, PALM, salt++));
		}
		return List.copyOf(out);
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		if (y < 0) {
			return b(Blocks.SANDSTONE);
		}
		for (BlockState fixed : new BlockState[]{wall(x, y, z), temple(x, y, z), fountain(x, y, z), market(x, y, z),
				avenue(x, y, z)}) {
			if (fixed != null) {
				return fixed;
			}
		}
		for (Lot lot : LOTS) {
			if (lot.contains(x, z)) {
				// Un palmier ne garde de son carré de sable que le pied du tronc : la place reste pavée.
				if (lot.plan() == PALM && y == 0) {
					return x == lot.x0() + 2 && z == lot.z0() + 2 ? b(Blocks.SAND) : ground(x, z);
				}
				BlockState state = lot.at(x, y, z, p);
				if (state != null) {
					return state;
				}
				break;
			}
		}
		if (y == 0) {
			return ground(x, z);
		}
		return y <= 12 ? AIR : null;
	}

	/** Le pavé : grès lisse dans les rues, filets de terre cuite sur la place et l'allée, sable entre les maisons. */
	private static BlockState ground(int x, int z) {
		boolean plaza = x >= 16 && x <= 32 && z >= 21 && z <= 35;
		if (plaza) {
			boolean border = x == 16 || x == 32 || z == 21 || z == 35;
			boolean ring = Math.max(Math.abs(x - C), Math.abs(z - 28)) == 3;
			return border || ring ? b(Blocks.TERRACOTTA) : b(Blocks.SMOOTH_SANDSTONE);
		}
		if (x >= 22 && x <= 26 && z >= 12 && z <= 20) {
			return x == C ? b(Blocks.TERRACOTTA) : b(Blocks.SMOOTH_SANDSTONE);
		}
		boolean street = z >= 27 && z <= 29 || x >= 23 && x <= 25 && z > 29;
		return street ? b(Blocks.SMOOTH_SANDSTONE) : b(Blocks.SAND);
	}

	/** L'enceinte : un mur crénelé, trois portes à linteau flanquées de piliers à lanterne. */
	private static BlockState wall(int x, int y, int z) {
		boolean onX = x == 0 || x == EDGE, onZ = z == 0 || z == EDGE;
		if (!onX && !onZ || y > 5) {
			return null;
		}
		// Les portes : à l'ouest et à l'est sur la rue principale, au sud sur la rue du sud.
		int along = onX ? z : x;
		boolean gate = onX ? Math.abs(along - 28) <= 1 : z == EDGE && Math.abs(along - C) <= 1;
		boolean pillar = onX ? Math.abs(along - 28) == 2 : z == EDGE && Math.abs(along - C) == 2;
		if (pillar) {
			return y <= 4 ? b(Blocks.CHISELED_SANDSTONE) : b(Blocks.LANTERN);
		}
		if (gate) {
			return y == 0 ? b(Blocks.SMOOTH_SANDSTONE) : y <= 3 ? AIR : y == 4 ? b(Blocks.CUT_SANDSTONE) : null;
		}
		if (y == 5) {
			return null;
		}
		if (y == 4) {
			return (x + z) % 2 == 0 ? b(Blocks.SANDSTONE) : AIR;
		}
		return y == 3 ? b(Blocks.CUT_SANDSTONE) : b(Blocks.SANDSTONE);
	}

	/** L'allée de Leto : des piliers à lanterne de part et d'autre, et le point de sang de l'émeute. */
	private static BlockState avenue(int x, int y, int z) {
		if ((x == 21 || x == 27) && (z == 13 || z == 16 || z == 19) && y >= 1 && y <= 3) {
			return y == 3 ? b(Blocks.LANTERN) : b(Blocks.CHISELED_SANDSTONE);
		}
		if (y == 1 && x == C && z == 15) {
			return b(FmabBlocks.BLOOD_CREST);
		}
		return null;
	}

	/** La fontaine de la place : un bassin d'eau au ras du pavé, sa margelle et sa colonne. */
	private static BlockState fountain(int x, int y, int z) {
		int dx = x - C, dz = z - 28, d = Math.max(Math.abs(dx), Math.abs(dz));
		if (d > 2 || y > 3) {
			return null;
		}
		if (d == 0) {
			return y == 3 ? b(Blocks.LANTERN) : b(Blocks.CHISELED_SANDSTONE);
		}
		if (y == 0) {
			return d == 1 ? b(Blocks.WATER) : b(Blocks.CUT_SANDSTONE);
		}
		return y == 1 && d == 2 ? b(Blocks.SMOOTH_SANDSTONE_SLAB) : AIR;
	}

	/** Le marché couvert : des piliers, une toile rayée tendue par-dessus, des lanternes pendues. */
	private static BlockState market(int x, int y, int z) {
		int x0 = 34, x1 = 46, z0 = 19, z1 = 25;
		if (x < x0 || x > x1 || z < z0 || z > z1 || y > 6) {
			return null;
		}
		boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
		if (y == 0) {
			return edge ? b(Blocks.CUT_SANDSTONE) : b(Blocks.SMOOTH_SANDSTONE);
		}
		if (y == 6) {
			return edge ? b(Blocks.SMOOTH_SANDSTONE_SLAB) : null;
		}
		if (y == 5) {
			return edge ? b(Blocks.SMOOTH_SANDSTONE) : b(Blocks.WOOL.pick(x % 2 == 0 ? DyeColor.ORANGE : DyeColor.WHITE));
		}
		boolean pillar = (x - x0) % 4 == 0 && (z == z0 || z == z1 || z == 22 && (x == x0 || x == x1));
		if (pillar) {
			return y == 4 ? b(Blocks.CHISELED_SANDSTONE) : b(Blocks.CUT_SANDSTONE);
		}
		if (y == 4 && z == 23 && (x - x0) % 4 == 2) {
			return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
		}
		return null;
	}

	/** Le temple de Leto : 13 de large, 11 de profond, colonnes, autel, dôme d'or. */
	private static BlockState temple(int x, int y, int z) {
		int x0 = C - 6, x1 = C + 6, z0 = 1, z1 = 11;
		if (x < x0 || x > x1 || z < z0 || z > z1 || y > 13) {
			return null;
		}
		if (y == 0) {
			return b(Blocks.SMOOTH_SANDSTONE);
		}
		// Le plan est dessiné façade au nord ; on le retourne pour qu'elle regarde la place.
		z = z0 + z1 - z;
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
			if (z == z1 - 2 && Math.abs(x - C) <= 1) {
				if (y == 1) {
					return b(Blocks.CHISELED_SANDSTONE);
				}
				if ((y == 2 || y == 3) && x == C) {
					return b(Blocks.GOLD_BLOCK);
				}
			}
			if (y == 1 && Math.abs(x - C) <= 1 && z == z1 - 1) {
				return b(FmabBlocks.RED_STONE_ORE);
			}
			return AIR;
		}
		// Le dôme d'or.
		double d = Math.hypot(x - C, (z - (z0 + z1) / 2.0) * 1.2);
		double r = 6 - (y - 8) * 1.2;
		return r > 0 && d <= r && d > r - 1.2 ? b(Blocks.GOLD_BLOCK) : AIR;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(Chest.loot(new BlockPos(C + 4, 1, 3), "liore"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		List<Spawn> out = new ArrayList<>();
		out.add(new Spawn(FmabEntities.CORNELLO, new BlockPos(C, 1, 5)));
		// Un habitant dans une maison sur deux (le plancher est un bloc au-dessus du pavé), deux marchands.
		for (int i = 0; i < HOUSES.length; i += 2) {
			Object[] h = HOUSES[i];
			out.add(new Spawn(EntityTypes.VILLAGER, new BlockPos((int) h[0] + (int) h[2] / 2, 2, (int) h[1] + (int) h[3] / 2)));
		}
		out.add(new Spawn(EntityTypes.VILLAGER, new BlockPos(37, 1, 24)));
		out.add(new Spawn(EntityTypes.VILLAGER, new BlockPos(43, 1, 24)));
		return out;
	}

	/**
	 * Une maison de Liore : des murs de grès ou de terre crue, un bandeau à chaque étage, un toit plat
	 * cerclé d'un parapet. Une échelle près de la porte monte d'étage en étage jusqu'à la terrasse,
	 * où l'on trouve un auvent, des jarres ou une citerne. Ses étages se lisent sur la hauteur de la
	 * parcelle : quatre blocs chacun, plus quatre pour la terrasse.
	 */
	static final class Dwelling implements Blueprint {
		/** Les murs, le bandeau des étages et le parapet ; le grès plus souvent que la terre crue. */
		private record Palette(Block wall, Block band, Block slab) {
		}

		private static final Palette[] PALETTES = {
				new Palette(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE_SLAB),
				new Palette(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE_SLAB),
				new Palette(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE_SLAB),
				new Palette(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MUD_BRICK_SLAB),
				new Palette(Blocks.SMOOTH_SANDSTONE, Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE), Blocks.SMOOTH_SANDSTONE_SLAB),
		};

		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			int h = p.sizeY() - 4;
			Palette pal = PALETTES[p.variant(PALETTES.length)];
			boolean xEdge = x == 0 || x == sx - 1, zEdge = z == 0 || z == sz - 1, edge = xEdge || zEdge;
			boolean ladder = x == sx - 2 && z == 1;
			if (y < 0) {
				return b(pal.wall());
			}
			if (y > h + 3) {
				return null;
			}
			if (y > h) {
				return terrace(x, y - h, z, sx, sz, edge, pal, p);
			}
			if (ladder && y > 0) {
				return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST);
			}
			if (y == 0 || y == h) {
				return edge ? b(y == h ? pal.band() : pal.wall()) : b(Blocks.SMOOTH_SANDSTONE);
			}
			if (xEdge && zEdge || edge && y % 4 == 0) {
				return b(pal.band());
			}
			if (edge) {
				if (z == 0 && x == sx / 2 && y <= 2) {
					return b(Blocks.JUNGLE_DOOR).setValue(DoorBlock.FACING, Direction.NORTH)
							.setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
				}
				int along = zEdge ? x : z;
				// Pas de fenêtre derrière l'échelle, ni de part et d'autre de la porte.
				boolean window = (y % 4 == 1 || y % 4 == 2) && along % 2 == 1 && !(x == sx - 1 && z == 1)
						&& !(z == 0 && y < 4 && Math.abs(x - sx / 2) <= 1);
				return window ? b(Blocks.GLASS_PANE) : b(pal.wall());
			}
			if (y % 4 == 0) {
				return b(Blocks.SMOOTH_SANDSTONE);
			}
			return y % 4 == 1 ? furniture(x, y, z, sx, sz, p) : y % 4 == 2 && x == 1 && z == 1 ? b(Blocks.LANTERN) : AIR;
		}

		/** Les meubles au ras du plancher : un tonneau près de la porte, un lit au fond, un poste de travail en bas. */
		private static BlockState furniture(int x, int y, int z, int sx, int sz, Plot p) {
			if (x == 1 && z == 1) {
				return b(Blocks.BARREL);
			}
			if (z == sz - 2 && x <= 2) {
				return b(Blocks.BED.pick(y < 4 ? DyeColor.ORANGE : DyeColor.YELLOW)).setValue(BedBlock.FACING, Direction.WEST)
						.setValue(BedBlock.PART, x == 1 ? BedPart.HEAD : BedPart.FOOT);
			}
			if (z == sz - 2 && x == sx - 2) {
				if (y >= 4) {
					return b(Blocks.DECORATED_POT);
				}
				Block[] jobs = {Blocks.COMPOSTER, Blocks.CARTOGRAPHY_TABLE, Blocks.SMOKER, Blocks.LOOM,
						Blocks.BREWING_STAND, Blocks.FLETCHING_TABLE};
				BlockState job = b(jobs[p.noise(1, 0, 1, jobs.length)]);
				return job.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
						? job.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH) : job;
			}
			return AIR;
		}

		/** Au-dessus du toit ({@code t} de 1 à 3) : le parapet, et de quoi vivre sur la terrasse. */
		private static BlockState terrace(int x, int t, int z, int sx, int sz, boolean edge, Palette pal, Plot p) {
			if (edge) {
				return t == 1 ? b(pal.slab()).setValue(SlabBlock.TYPE, SlabType.BOTTOM) : AIR;
			}
			boolean back = z == sz - 2, post = back && (x == 1 || x == sx - 2);
			switch (p.noise(7, 7, 7, 3)) {
				case 0 -> {
					// Un auvent de toile sur deux poteaux, une table dessous.
					if (post && t <= 2) {
						return b(Blocks.JUNGLE_FENCE);
					}
					if (t == 3 && z >= sz - 3) {
						return b(Blocks.WOOL.pick(x % 2 == 0 ? DyeColor.RED : DyeColor.WHITE));
					}
					if (t == 1 && back && x == 2) {
						return b(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP);
					}
				}
				case 1 -> {
					// Des jarres et un cactus en pot.
					if (t == 1 && post) {
						return x == 1 ? b(Blocks.DECORATED_POT) : b(Blocks.POTTED_CACTUS);
					}
				}
				default -> {
					// La citerne d'eau de pluie, et une réserve.
					if (t == 1 && post) {
						return x == 1 ? b(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3)
								: b(Blocks.BARREL);
					}
				}
			}
			return AIR;
		}
	}
}
