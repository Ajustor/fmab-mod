package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.IzumiEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class FmabEntities {
	private static final ResourceKey<EntityType<?>> IZUMI_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("izumi"));

	public static final EntityType<IzumiEntity> IZUMI = Registry.register(BuiltInRegistries.ENTITY_TYPE, IZUMI_KEY,
			EntityType.Builder.<IzumiEntity>of(IzumiEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(IZUMI_KEY));

	private FmabEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(IZUMI, IzumiEntity.createAttributes());
	}
}
