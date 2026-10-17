package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * La maison des Elric : une ferme ordinaire, mais le bureau de Hohenheim est resté tel qu'il
 * l'a laissé. Ses notes de recherche dorment dans un coffre, sous les étagères.
 */
final class ElricHouse implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, RURAL_PALETTES[1]);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside) {
			return shell;
		}
		// Le bureau : des étagères le long du mur du fond, un pupitre.
		if (z == sz - 2 && (y == 1 || y == 2) && x > 1) {
			return b(Blocks.BOOKSHELF);
		}
		if (z == sz - 3 && y == 1 && x == sx / 2) {
			return b(Blocks.LECTERN);
		}
		return shell;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(Chest.loot(new BlockPos(1, 1, p.sizeZ() - 2), "elric_house"));
	}
}
