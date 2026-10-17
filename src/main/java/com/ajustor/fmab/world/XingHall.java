package com.ajustor.fmab.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Les bâtiments de Xing autour du pavillon : un temple ouvert sur quatre piliers rouges sous deux toits
 * en gradins, ou une maison de briques de boue sous un toit de tuiles. La façade est du côté z = 0.
 */
final class XingHall implements Blueprint {
	private static final BlockState PILLAR = b(Blocks.STRIPPED_MANGROVE_LOG);
	private static final BlockState TILE = b(Blocks.DEEPSLATE_TILES);
	private static final int WALL = 4;

	private final boolean shrine;

	XingHall(boolean shrine) {
		this.shrine = shrine;
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		if (y < 0) {
			return b(Blocks.PACKED_MUD);
		}
		if (y == 0) {
			return shrine ? b(Blocks.POLISHED_GRANITE) : b(Blocks.PACKED_MUD);
		}
		boolean edgeX = x == 0 || x == sx - 1, edgeZ = z == 0 || z == sz - 1;
		if (y <= WALL) {
			if (shrine) {
				// Le temple : quatre piliers, un fond de planches claires, une lanterne au milieu.
				if (edgeX && edgeZ) {
					return PILLAR;
				}
				if (z == sz - 1) {
					return y == 3 ? AIR : b(Blocks.BIRCH_PLANKS);
				}
				if (y == WALL && x == sx / 2 && z == sz / 2) {
					return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
				}
				return AIR;
			}
			// La maison : des murs de briques de boue, une porte, des fenêtres de vitres claires.
			if (edgeX || edgeZ) {
				if (edgeX && edgeZ) {
					return PILLAR;
				}
				if (z == 0 && x == sx / 2 && y <= 2) {
					return AIR;
				}
				if (y == 2 && (x + z) % 3 == 0) {
					return b(Blocks.GLASS_PANE);
				}
				return b(Blocks.MUD_BRICKS);
			}
			return AIR;
		}
		// Les toits : un ou deux rangs de tuiles en pente vers l'extérieur.
		int tier = y - WALL - 1;
		int tiers = shrine ? 2 : 1;
		if (tier >= tiers) {
			return tier == tiers && x == sx / 2 && z == sz / 2 ? b(Blocks.GOLD_BLOCK) : null;
		}
		int inset = 2 * tier - 1;
		int x0 = inset, x1 = sx - 1 - inset, z0 = inset, z1 = sz - 1 - inset;
		if (x < x0 || x > x1 || z < z0 || z > z1) {
			return null;
		}
		if (z == z0) {
			return stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH);
		}
		if (z == z1) {
			return stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH);
		}
		if (x == x0) {
			return stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST);
		}
		if (x == x1) {
			return stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST);
		}
		return TILE;
	}
}
