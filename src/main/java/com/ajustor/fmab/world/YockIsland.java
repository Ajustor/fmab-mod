package com.ajustor.fmab.world;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * L'île de Yock : un cône de roche et de sable surgi de la mer (y = 0 au niveau de l'eau), une
 * plage, de l'herbe, quelques arbres, la cabane des naufragés et un feu de camp.
 */
final class YockIsland implements Blueprint {
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
