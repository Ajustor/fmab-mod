package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Maison des Rockbell : une ferme, avec l'atelier d'automail au rez-de-chaussée et les chambres à
 * l'étage. Un habitant de Resembool y vit.
 */
final class RockbellHouse implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = rockbell(x, y, z, p);
		if (shell == AIR && y > 4) {
			BlockState furniture = furnish(x, y, z, p, false);
			return furniture != null ? furniture : shell;
		}
		return shell;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(EntityTypes.VILLAGER, new BlockPos(p.sizeX() / 2, 1, 2)));
	}
}
