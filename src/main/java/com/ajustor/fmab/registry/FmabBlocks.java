package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class FmabBlocks {
	public static final Block TRANSMUTATION_CIRCLE = register("transmutation_circle", TransmutationCircleBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.SNOW)
					.noCollision()
					.instabreak()
					.sound(SoundType.SAND)
					.pushReaction(PushReaction.DESTROY)
					.replaceable());

	private FmabBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		return Blocks.register(ResourceKey.create(Registries.BLOCK, Fmab.id(name)), factory, properties);
	}

	public static void register() {
		// Le chargement de la classe suffit.
	}
}
