package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le dispensaire de Marcoh : une maison de bois à l'écart, deux couchettes, un alambic, des
 * remèdes ; le docteur y soigne les gens du coin sous un faux nom.
 */
final class MarcohClinic implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, RURAL_PALETTES[0]);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside || y != 1) {
			return shell;
		}
		// Deux couchettes contre le mur du fond, un alambic et un tonneau de remèdes.
		if (z == sz - 2 && (x == 2 || x == 4)) {
			return b(Blocks.WOOL.pick(DyeColor.WHITE));
		}
		if (z == sz - 2 && x == sx - 2) {
			return b(Blocks.BREWING_STAND);
		}
		if (z == sz - 3 && x == sx - 2) {
			return b(Blocks.BARREL);
		}
		return shell;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(Chest.loot(new BlockPos(p.sizeX() - 2, 1, 2), "marcoh_clinic"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(FmabEntities.MARCOH, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2)));
	}
}
