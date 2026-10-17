package com.ajustor.fmab.world;

import com.ajustor.fmab.entity.character.StoryCharacters;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le pavillon de Xing : une cour de 23 sur 23 fermée de murs de briques de boue, une porte entre
 * deux piliers rouges, des lanternes de pierre aux coins, des massifs d'azalées ; au fond, le
 * pavillon surélevé, piliers rouges, cloisons claires et trois rangs de toits de tuiles.
 */
final class XingPavilion implements Blueprint {
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
		return y == 4 ? b(Blocks.DEEPSLATE_TILE_SLAB) : b(FmabBlocks.XING_BRICKS);
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
		return List.of(Chest.loot(new BlockPos(P1 - 2, 2, Z1 - 2), "xing_pavilion"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(FmabEntities.MAY_CHANG, new BlockPos((P0 + P1) / 2, 2, Z0 + 3)),
				new Spawn(FmabEntities.character(StoryCharacters.LING), new BlockPos((P0 + P1) / 2 - 2, 2, Z0 + 3)),
				new Spawn(FmabEntities.character(StoryCharacters.LAN_FAN), new BlockPos((P0 + P1) / 2 + 2, 2, Z0 + 3)));
	}
}
