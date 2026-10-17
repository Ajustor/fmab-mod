package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le fort de Drachma, de l'autre côté des montagnes : une enceinte de pierre aux tours de rondins,
 * une cour enneigée, et au centre le donjon où le général de Drachma attend, sa garde dans la cour.
 */
final class DrachmaFort implements Blueprint {
	private static final int WALL = 6;
	private static final int TOWER = 9;
	private static final int KEEP_HALF = 4;
	private static final int KEEP = 8;

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		int s = p.sizeX(), c = s / 2;
		if (y < 0) {
			return null;
		}
		if (y == 0) {
			// La cour, enneigée ; le sol du donjon, dallé.
			boolean keep = Math.abs(x - c) <= KEEP_HALF && Math.abs(z - c) <= KEEP_HALF;
			return keep ? b(Blocks.POLISHED_ANDESITE) : p.noise(x, 0, z, 4) == 0 ? b(Blocks.GRAVEL) : b(Blocks.SNOW_BLOCK);
		}
		boolean corner = (x < 3 || x > s - 4) && (z < 3 || z > s - 4);
		if (corner) {
			// Les tours d'angle, en rondins, plus hautes que les murs.
			return y <= TOWER ? b(Blocks.SPRUCE_LOG) : y == TOWER + 1 && (x + z) % 2 == 0 ? b(Blocks.SPRUCE_FENCE) : null;
		}
		boolean wall = x == 0 || x == s - 1 || z == 0 || z == s - 1;
		if (wall) {
			// La porte, au milieu du mur de devant.
			if (z == 0 && Math.abs(x - c) <= 1 && y <= 4) {
				return AIR;
			}
			if (y <= WALL) {
				return p.noise(x, y, z, 5) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			}
			// Les créneaux, et les couleurs de Drachma.
			if (y == WALL + 1) {
				return (x + z) % 2 == 0 ? b(Blocks.STONE_BRICKS) : (x + z) % 6 == 1 ? b(Blocks.WOOL.pick(DyeColor.RED)) : null;
			}
			return null;
		}
		int dx = Math.abs(x - c), dz = Math.abs(z - c);
		if (dx <= KEEP_HALF && dz <= KEEP_HALF) {
			boolean keepWall = dx == KEEP_HALF || dz == KEEP_HALF;
			if (keepWall) {
				// La porte du donjon, face à celle du fort.
				if (z == c - KEEP_HALF && x == c && y <= 2) {
					return AIR;
				}
				return y <= KEEP ? b(Blocks.STONE_BRICKS) : y == KEEP + 1 && (x + z) % 2 == 0 ? b(Blocks.STONE_BRICKS) : null;
			}
			if (y == KEEP) {
				return b(Blocks.SPRUCE_PLANKS);
			}
			return y < KEEP ? AIR : null;
		}
		return y <= TOWER + 1 ? AIR : null;
	}

	@Override
	public List<Chest> chests(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(Chest.loot(new BlockPos(c, 1, c + KEEP_HALF - 1), "drachma_fort"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(new Spawn(FmabEntities.DRACHMA_GENERAL, new BlockPos(c, 1, c)),
				new Spawn(FmabEntities.DRACHMA_SOLDIER, new BlockPos(c - 7, 1, c - 7)),
				new Spawn(FmabEntities.DRACHMA_SOLDIER, new BlockPos(c + 7, 1, c - 7)),
				new Spawn(FmabEntities.DRACHMA_SOLDIER, new BlockPos(c - 7, 1, c + 7)),
				new Spawn(FmabEntities.DRACHMA_SOLDIER, new BlockPos(c + 7, 1, c + 7)));
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
}
