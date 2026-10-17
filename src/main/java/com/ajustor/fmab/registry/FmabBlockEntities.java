package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.block.TransmutationCircleBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.ajustor.fmab.platform.Platform;
import java.util.Set;

public final class FmabBlockEntities {
	public static final BlockEntityType<TransmutationCircleBlockEntity> TRANSMUTATION_CIRCLE = Platform.get().content().register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Fmab.id("transmutation_circle"),
			new BlockEntityType<>(TransmutationCircleBlockEntity::new, Set.of(FmabBlocks.TRANSMUTATION_CIRCLE)));

	private FmabBlockEntities() {
	}

	public static void register() {
	}
}
