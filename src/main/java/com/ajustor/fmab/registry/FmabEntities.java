package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.entity.StoneGolemEntity;
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

	private static final ResourceKey<EntityType<?>> EXAMINER_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("state_examiner"));
	private static final ResourceKey<EntityType<?>> GOLEM_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("stone_golem"));

	public static final EntityType<StateExaminerEntity> STATE_EXAMINER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, EXAMINER_KEY,
			EntityType.Builder.<StateExaminerEntity>of(StateExaminerEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(EXAMINER_KEY));

	public static final EntityType<StoneGolemEntity> STONE_GOLEM = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, GOLEM_KEY,
			EntityType.Builder.<StoneGolemEntity>of(StoneGolemEntity::new, MobCategory.MISC)
					.sized(1.4F, 2.7F)
					.clientTrackingRange(10)
					.build(GOLEM_KEY));

	private FmabEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(IZUMI, IzumiEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STATE_EXAMINER, StateExaminerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STONE_GOLEM, StoneGolemEntity.createAttributes());
	}
}
