package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.EnvyEntity;
import com.ajustor.fmab.entity.FatherEntity;
import com.ajustor.fmab.entity.GluttonyEntity;
import com.ajustor.fmab.entity.GreedEntity;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.LustEntity;
import com.ajustor.fmab.entity.PrideEntity;
import com.ajustor.fmab.entity.SlothEntity;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.entity.StoneGolemEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.entity.WinryEntity;
import com.ajustor.fmab.entity.WrathEntity;
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

	private static final ResourceKey<EntityType<?>> TRUTH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("truth"));

	/** La Vérité, devant la Porte. */
	public static final EntityType<TruthEntity> TRUTH = Registry.register(BuiltInRegistries.ENTITY_TYPE, TRUTH_KEY,
			EntityType.Builder.<TruthEntity>of(TruthEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(TRUTH_KEY));

	private static final ResourceKey<EntityType<?>> WINRY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("winry"));

	/** Winry Rockbell, mécanicienne d'automail à Rush Valley. */
	public static final EntityType<WinryEntity> WINRY = Registry.register(BuiltInRegistries.ENTITY_TYPE, WINRY_KEY,
			EntityType.Builder.<WinryEntity>of(WinryEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(WINRY_KEY));

	private static final ResourceKey<EntityType<?>> LUST_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("lust"));
	private static final ResourceKey<EntityType<?>> GLUTTONY_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("gluttony"));

	/** Lust (Luxure), gardienne du Laboratoire 5. */
	public static final EntityType<LustEntity> LUST = Registry.register(BuiltInRegistries.ENTITY_TYPE, LUST_KEY,
			EntityType.Builder.<LustEntity>of(LustEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(LUST_KEY));

	/** Gluttony (Gourmandise), gardien du Laboratoire 5 ; sa taille vient de son attribut d'échelle. */
	public static final EntityType<GluttonyEntity> GLUTTONY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			GLUTTONY_KEY,
			EntityType.Builder.<GluttonyEntity>of(GluttonyEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(GLUTTONY_KEY));

	private static final ResourceKey<EntityType<?>> ENVY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("envy"));

	/** Envy (Envie) : erre déguisé ; sa taille change avec sa forme (attribut d'échelle). */
	public static final EntityType<EnvyEntity> ENVY = Registry.register(BuiltInRegistries.ENTITY_TYPE, ENVY_KEY,
			EntityType.Builder.<EnvyEntity>of(EnvyEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(ENVY_KEY));

	private static final ResourceKey<EntityType<?>> GREED_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("greed"));

	/** Greed (Avarice), dans son bar du Devil's Nest. */
	public static final EntityType<GreedEntity> GREED = Registry.register(BuiltInRegistries.ENTITY_TYPE, GREED_KEY,
			EntityType.Builder.<GreedEntity>of(GreedEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(GREED_KEY));

	private static final ResourceKey<EntityType<?>> SLOTH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("sloth"));

	/** Sloth (Paresse), dans le grand tunnel sous Central ; sa taille vient de son échelle. */
	public static final EntityType<SlothEntity> SLOTH = Registry.register(BuiltInRegistries.ENTITY_TYPE, SLOTH_KEY,
			EntityType.Builder.<SlothEntity>of(SlothEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(SLOTH_KEY));

	private static final ResourceKey<EntityType<?>> WRATH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("wrath"));

	/** Wrath (Colère), King Bradley, dans sa résidence de Central. */
	public static final EntityType<WrathEntity> WRATH = Registry.register(BuiltInRegistries.ENTITY_TYPE, WRATH_KEY,
			EntityType.Builder.<WrathEntity>of(WrathEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(WRATH_KEY));

	private static final ResourceKey<EntityType<?>> PRIDE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("pride"));

	/** Pride (Orgueil), Selim Bradley : un enfant, à son échelle. */
	public static final EntityType<PrideEntity> PRIDE = Registry.register(BuiltInRegistries.ENTITY_TYPE, PRIDE_KEY,
			EntityType.Builder.<PrideEntity>of(PrideEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(PRIDE_KEY));

	private static final ResourceKey<EntityType<?>> FATHER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("father"));

	/** Père, l'Homonculus originel, sur son trône sous Central ; sa forme divine grandit (échelle). */
	public static final EntityType<FatherEntity> FATHER = Registry.register(BuiltInRegistries.ENTITY_TYPE, FATHER_KEY,
			EntityType.Builder.<FatherEntity>of(FatherEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(FATHER_KEY));

	private FmabEntities() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(IZUMI, IzumiEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STATE_EXAMINER, StateExaminerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(STONE_GOLEM, StoneGolemEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TRUTH, TruthEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(WINRY, WinryEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LUST, LustEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GLUTTONY, GluttonyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ENVY, EnvyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GREED, GreedEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SLOTH, SlothEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(WRATH, WrathEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(PRIDE, PrideEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FATHER, FatherEntity.createAttributes());
	}
}
