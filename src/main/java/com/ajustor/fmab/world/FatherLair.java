package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le repaire de Père, sous le tunnel de Sloth. Une porte scellée par l'Ouroboros perce la paroi
 * sud du tunnel ; derrière, un boyau, puis un puits qui plonge trente blocs plus bas dans la salle
 * du trône : un sol gravé du cercle national, des tuyaux partout, et le trône sur son estrade.
 *
 * <p>Coordonnées : la porte en (16, z 0), dans la paroi du tunnel ; le boyau en z 1..2 ; le puits
 * en (16, 4) ; la salle couvre x 0..32, z 2..40, du sol en y = −58 au plafond en y = −44.
 */
final class FatherLair implements Blueprint {
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
