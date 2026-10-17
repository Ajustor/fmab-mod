package com.ajustor.fmab.world;

import com.ajustor.fmab.Fmab;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Le domaine de Xing : il ne pose rien, il change le pays autour du pavillon en biome de Xing (bambous,
 * herbe vive, eau turquoise). La végétation de ce biome pousse ensuite d'elle-même.
 */
final class XingGrounds implements Blueprint {
	static final ResourceKey<Biome> XING = ResourceKey.create(Registries.BIOME, Fmab.id("xing"));

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		return null;
	}

	@Override
	public Optional<ResourceKey<Biome>> biome() {
		return Optional.of(XING);
	}
}
