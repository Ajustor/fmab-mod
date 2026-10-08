package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FmabBlockEntities {
	public static final BlockEntityType<TransmutationCircleBlockEntity> TRANSMUTATION_CIRCLE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Fmab.id("transmutation_circle"),
			FabricBlockEntityTypeBuilder.create(TransmutationCircleBlockEntity::new, FmabBlocks.TRANSMUTATION_CIRCLE)
					.build());

	private FmabBlockEntities() {
	}

	public static void register() {
	}
}
