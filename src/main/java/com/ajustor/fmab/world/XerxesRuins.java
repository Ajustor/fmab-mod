package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Les ruines de Xerxès : un parvis de grès à demi enfoui, gravé du grand cercle qui a dévoré le
 * royaume en une nuit, et une forêt de colonnes brisées. Des fresques racontent la chute ;
 * Hohenheim, le seul survivant, y revient.
 */
final class XerxesRuins implements Blueprint {
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
		return List.of(Chest.loot(new BlockPos(c - 4, 1, c), "xerxes_murals"),
				Chest.loot(new BlockPos(c + 4, 1, c), "xerxes_sanctum"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(new Spawn(FmabEntities.HOHENHEIM, new BlockPos(c, 1, c + 2)));
	}
}
