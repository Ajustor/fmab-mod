package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;
import static com.ajustor.fmab.world.CityPlans.*;

/** La muraille de Central, et un garde à chaque porte, côté ville. */
final class CityWalls implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		return cityWalls(x, y, z, p);
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int c = p.sizeX() / 2, in = WALL_INNER - 4;
		return List.of(new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(c + 2, 0, c - in)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(c - 2, 0, c + in)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(c + in, 0, c + 2)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(c - in, 0, c - 2)));
	}
}
