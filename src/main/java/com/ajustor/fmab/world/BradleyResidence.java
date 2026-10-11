package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * La résidence Bradley, à Central : une demeure de briques à deux étages, éclairée de lanternes
 * (une pénombre où les ombres de Pride se plaisent). Le Généralissime et son fils y vivent.
 */
final class BradleyResidence implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, CITY_PALETTES[0]);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (inside && x % 5 == 2 && z % 4 == 2) {
			if (y == 3) {
				return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
			}
			if (y == 5) {
				return b(Blocks.LANTERN);
			}
		}
		if (inside && y == 1 && z == sz - 2 && x % 3 == 1) {
			return b(Blocks.BOOKSHELF);
		}
		return shell;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(
				new Spawn(FmabEntities.WRATH, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2)),
				new Spawn(FmabEntities.PRIDE, new BlockPos(3, 1, p.sizeZ() - 3)));
	}
}
