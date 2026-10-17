package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Un atelier d'automail de Rush Valley : grès, enclumes et établis contre le mur du fond, des
 * pièces dans le coffre. La boutique de Garfiel accueille Winry.
 */
final class Workshop implements Blueprint {
	private final boolean winry;

	Workshop(boolean winry) {
		this.winry = winry;
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, RUSH_VALLEY_PALETTE);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside || y != 1 || z != sz - 2 || x == 1) {
			return shell;
		}
		return switch ((x + p.variant(3)) % 4) {
			case 0 -> b(Blocks.ANVIL);
			case 1 -> b(FmabBlocks.AUTOMAIL_BENCH);
			case 2 -> b(Blocks.GRINDSTONE);
			default -> b(Blocks.SMITHING_TABLE);
		};
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(Chest.loot(new BlockPos(1, 1, p.sizeZ() - 2), "rush_valley_workshop"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		Spawn resident = new Spawn(winry ? FmabEntities.WINRY : EntityTypes.VILLAGER,
				new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2));
		return List.of(resident);
	}
}
