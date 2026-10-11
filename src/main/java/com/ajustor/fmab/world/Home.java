package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Une maison habitée : la coque de {@link #house}, ses meubles ({@link #furnish}) et ses
 * habitants, un villageois par étage. Le poste de travail du rez-de-chaussée leur donne un métier.
 */
final class Home implements Blueprint {
	private final Palette[] palettes;
	private final boolean town;

	Home(Palette[] palettes, boolean town) {
		this.palettes = palettes;
		this.town = town;
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, palettes[p.variant(palettes.length)]);
		if (shell != AIR) {
			return shell;
		}
		BlockState furniture = furnish(x, y, z, p, town);
		return furniture != null ? furniture : shell;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int cx = p.sizeX() / 2, cz = p.sizeZ() / 2;
		return wallHeight(p) == 8
				? List.of(new Spawn(EntityTypes.VILLAGER, new BlockPos(cx, 1, cz)),
						new Spawn(EntityTypes.VILLAGER, new BlockPos(cx, 1, cz - 1)))
				: List.of(new Spawn(EntityTypes.VILLAGER, new BlockPos(cx, 1, cz)));
	}
}
