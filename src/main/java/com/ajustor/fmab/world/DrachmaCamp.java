package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Un camp de pillards à la solde de Drachma, dans les neiges : trois tentes de toile verte autour d'un
 * feu, la terre piétinée, un coffre de butin. Des pillards et un soldat de Drachma le gardent.
 */
final class DrachmaCamp implements Blueprint {
	/** Les tentes : le milieu de leur faîte, et leur sens (le long de x ou de z). */
	private static final int[][] TENTS = {{-4, -2, 0}, {4, -2, 0}, {0, 4, 1}};

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		int c = p.sizeX() / 2;
		double r = Math.hypot(x - c, z - c);
		if (y < 0 || r > c + 0.5) {
			return null;
		}
		if (y == 0) {
			// La terre piétinée, et de la neige tassée vers les bords.
			int n = p.noise(x, 0, z, 6);
			return r > c - 1.5 && n < 3 ? b(Blocks.SNOW_BLOCK) : n == 0 ? b(Blocks.GRAVEL) : b(Blocks.COARSE_DIRT);
		}
		if (y == 1 && x == c && z == c) {
			return b(Blocks.CAMPFIRE);
		}
		for (int[] t : TENTS) {
			int tx = c + t[0], tz = c + t[1];
			boolean alongZ = t[2] == 1;
			int across = alongZ ? x - tx : z - tz;
			int along = alongZ ? z - tz : x - tx;
			if (Math.abs(along) <= 2 && Math.abs(across) <= 1 && y <= 2) {
				// Une tente en A : les pans sur les côtés, le faîte au-dessus, ouverte aux deux bouts.
				boolean side = y == 1 && Math.abs(across) == 1;
				boolean ridge = y == 2 && across == 0;
				return side || ridge ? b(Blocks.WOOL.pick(DyeColor.GREEN)) : AIR;
			}
		}
		return y <= 3 ? AIR : null;
	}

	@Override
	public List<Chest> chests(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(Chest.loot(new BlockPos(c, 1, c + 4), "drachma_camp"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(new Spawn(EntityTypes.PILLAGER, new BlockPos(c - 2, 1, c + 1)),
				new Spawn(EntityTypes.PILLAGER, new BlockPos(c + 2, 1, c + 1)),
				new Spawn(EntityTypes.PILLAGER, new BlockPos(c, 1, c - 3)),
				new Spawn(FmabEntities.DRACHMA_SOLDIER, new BlockPos(c + 1, 1, c - 1)));
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
}
