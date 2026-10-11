package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

/**
 * Ce qui fait vivre les rues entre les bâtiments : des arbres, des étals de marché, un pré clos.
 */
final class Townscape {
	private Townscape() {
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	/** Un arbre de 5 sur 5 : un tronc, une couronne arrondie et touffue, un peu de terre au pied. */
	static Blueprint tree(Block log, Block leaves, Block soil) {
		BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		return (x, y, z, p) -> {
			int trunk = 3 + p.variant(2);
			int dx = x - 2, dz = z - 2;
			if (y < 0) {
				return y == -1 ? soil.defaultBlockState() : Blocks.DIRT.defaultBlockState();
			}
			if (dx == 0 && dz == 0 && y <= trunk) {
				return log.defaultBlockState();
			}
			// La couronne : une boule aplatie centrée au-dessus du tronc, échancrée au hasard sur le bord.
			double r = Math.hypot(dx, dz), dy = y - trunk - 0.5;
			double edge = Math.sqrt(dx * dx + dz * dz + dy * dy * 2.2);
			if (y >= trunk - 1 && edge <= 2.6 && !(edge > 2.1 && p.noise(x, y, z, 3) == 0)) {
				return leaf;
			}
			return y <= 2 && r > 0 ? AIR : null;
		};
	}

	/** Un palmier de désert : un tronc qui monte, et ses palmes en croix qui retombent. */
	static Blueprint palm(Block soil) {
		BlockState leaf = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		return (x, y, z, p) -> {
			int top = 5 + p.variant(2);
			int dx = x - 2, dz = z - 2;
			if (y < 0) {
				return y == -1 ? soil.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
			}
			if (dx == 0 && dz == 0) {
				return y <= top ? Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState() : y == top + 1 ? leaf : null;
			}
			boolean cross = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
			int reach = Math.max(Math.abs(dx), Math.abs(dz));
			if (cross && (y == top + 1 && reach == 1 || y == top && reach == 2)) {
				return leaf;
			}
			return y <= 2 ? AIR : null;
		};
	}

	/**
	 * Un étal de marché, 3 sur 3 : quatre poteaux, une toile rayée, un comptoir en façade (z = 0) et
	 * des marchandises derrière. La couleur de la toile change d'un étal à l'autre.
	 */
	static Blueprint stall(Block post, Block counter) {
		DyeColor[] colours = {DyeColor.RED, DyeColor.BLUE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.ORANGE};
		return (x, y, z, p) -> {
			DyeColor colour = colours[p.variant(colours.length)];
			boolean corner = (x == 0 || x == 2) && (z == 0 || z == 2);
			if (y < 0) {
				return null;
			}
			if (y == 3) {
				return Blocks.WOOL.pick(x % 2 == 0 ? colour : DyeColor.WHITE).defaultBlockState();
			}
			if (y == 4 && x == 1) {
				return Blocks.CARPET.pick(colour).defaultBlockState();
			}
			if (corner && y <= 2) {
				return post.defaultBlockState();
			}
			if (y == 0 && z == 0) {
				return counter.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
			}
			if (y == 1 && z == 0 && x == 1) {
				return wares(p);
			}
			if (y == 0 && z == 2) {
				return x == 1 ? Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP)
						: Blocks.HAY_BLOCK.defaultBlockState();
			}
			return y <= 2 ? AIR : null;
		};
	}

	private static BlockState wares(Blueprint.Plot p) {
		Block[] wares = {Blocks.MELON, Blocks.PUMPKIN, Blocks.POTTED_RED_TULIP, Blocks.DECORATED_POT, Blocks.CAKE,
				Blocks.FLOWER_POT, Blocks.BROWN_MUSHROOM_BLOCK};
		return wares[p.noise(1, 2, 3, wares.length)].defaultBlockState();
	}

	/** Un pré clos d'une clôture : de l'herbe, une meule de foin, un abreuvoir, et ses moutons. */
	static final class Pasture implements Blueprint {
		@Override
		public BlockState at(int x, int y, int z, Plot p) {
			int sx = p.sizeX(), sz = p.sizeZ();
			boolean border = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
			if (y < -1) {
				return Blocks.DIRT.defaultBlockState();
			}
			if (y == -1) {
				return Blocks.GRASS_BLOCK.defaultBlockState();
			}
			if (y == 0 && border) {
				return fence(x, z, sx, sz);
			}
			if (y == 0 && x == 1 && z == sz - 2) {
				return Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3);
			}
			if (x == sx - 2 && z == sz - 2 && y <= 1 || x == sx - 3 && z == sz - 2 && y == 0) {
				return Blocks.HAY_BLOCK.defaultBlockState();
			}
			if (y == 0 && p.noise(x, 0, z, 4) == 0) {
				return Blocks.SHORT_GRASS.defaultBlockState();
			}
			return y <= 2 ? AIR : null;
		}

		@Override
		public List<Spawn> spawns(Plot p) {
			int cx = p.sizeX() / 2, cz = p.sizeZ() / 2;
			return List.of(new Spawn(EntityTypes.SHEEP, new BlockPos(cx - 1, 0, cz)),
					new Spawn(EntityTypes.SHEEP, new BlockPos(cx + 1, 0, cz - 1)),
					new Spawn(EntityTypes.SHEEP, new BlockPos(cx, 0, cz + 1)),
					new Spawn(EntityTypes.COW, new BlockPos(cx + 2, 0, cz + 1)));
		}
	}

	/** Clôture du bord d'un rectangle, reliée à ses voisines. */
	private static BlockState fence(int x, int z, int sx, int sz) {
		boolean onX = x == 0 || x == sx - 1;
		boolean onZ = z == 0 || z == sz - 1;
		BlockState f = Blocks.OAK_FENCE.defaultBlockState();
		if (onX) {
			f = f.setValue(CrossCollisionBlock.NORTH, z > 0).setValue(CrossCollisionBlock.SOUTH, z < sz - 1);
		}
		if (onZ) {
			f = f.setValue(CrossCollisionBlock.WEST, x > 0).setValue(CrossCollisionBlock.EAST, x < sx - 1);
		}
		return f;
	}
}
