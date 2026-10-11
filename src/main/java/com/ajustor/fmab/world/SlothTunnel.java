package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le grand tunnel que Sloth creuse sous Central, pour le cercle de transmutation national : un
 * boyau de pierre de cinq sur cinq, à trente blocs sous la ville, d'un rempart à l'autre. Un puits
 * à échelle y descend depuis l'avenue sud.
 */
final class SlothTunnel implements Blueprint {
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
		if (x == shaftX && z == SHAFT_Z && y > FLOOR) {
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
