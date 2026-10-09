package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.entity.AlchemicalMineEntity;
import com.ajustor.fmab.entity.AmestrianSoldierEntity;
import com.ajustor.fmab.entity.BarryEntity;
import com.ajustor.fmab.entity.ChimeraBeastEntity;
import com.ajustor.fmab.entity.ChimeraCrawlerEntity;
import com.ajustor.fmab.entity.CornelloEntity;
import com.ajustor.fmab.entity.DrachmaSoldierEntity;
import com.ajustor.fmab.entity.EnvyEntity;
import com.ajustor.fmab.entity.FatherEntity;
import com.ajustor.fmab.entity.GateHandEntity;
import com.ajustor.fmab.entity.GluttonyEntity;
import com.ajustor.fmab.entity.GreedEntity;
import com.ajustor.fmab.entity.HauntedArmorEntity;
import com.ajustor.fmab.entity.HohenheimEntity;
import com.ajustor.fmab.entity.ImmortalSoldierEntity;
import com.ajustor.fmab.entity.IzumiEntity;
import com.ajustor.fmab.entity.KunaiEntity;
import com.ajustor.fmab.entity.LustEntity;
import com.ajustor.fmab.entity.MayChangEntity;
import com.ajustor.fmab.entity.OlivierEntity;
import com.ajustor.fmab.entity.PrideEntity;
import com.ajustor.fmab.entity.ScarEntity;
import com.ajustor.fmab.entity.SlothEntity;
import com.ajustor.fmab.entity.StateExaminerEntity;
import com.ajustor.fmab.entity.StoneGolemEntity;
import com.ajustor.fmab.entity.ThrowingKnifeEntity;
import com.ajustor.fmab.entity.TruthEntity;
import com.ajustor.fmab.entity.WinryEntity;
import com.ajustor.fmab.entity.WrathEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

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

	private static final ResourceKey<EntityType<?>> KUNAI_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("kunai"));

	/** Un kunaï d'alkahestry lancé, qui se plante comme une flèche. */
	public static final EntityType<KunaiEntity> KUNAI = Registry.register(BuiltInRegistries.ENTITY_TYPE, KUNAI_KEY,
			EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
					.sized(0.4F, 0.4F)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(KUNAI_KEY));

	private static final ResourceKey<EntityType<?>> MAY_CHANG_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("may_chang"));

	/** May Chang, alkahestriste de Xing, dans son pavillon ; petite (attribut d'échelle). */
	public static final EntityType<MayChangEntity> MAY_CHANG = Registry.register(BuiltInRegistries.ENTITY_TYPE, MAY_CHANG_KEY,
			EntityType.Builder.<MayChangEntity>of(MayChangEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.62F)
					.clientTrackingRange(10)
					.build(MAY_CHANG_KEY));

	private static final ResourceKey<EntityType<?>> DRACHMA_SOLDIER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("drachma_soldier"));

	/** Un soldat de Drachma, la nuit au pied de Fort Briggs. */
	public static final EntityType<DrachmaSoldierEntity> DRACHMA_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, DRACHMA_SOLDIER_KEY,
			EntityType.Builder.<DrachmaSoldierEntity>of(DrachmaSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(DRACHMA_SOLDIER_KEY));

	private static final ResourceKey<EntityType<?>> OLIVIER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("olivier"));

	/** La générale Olivier Mira Armstrong, qui tient Fort Briggs. */
	public static final EntityType<OlivierEntity> OLIVIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, OLIVIER_KEY,
			EntityType.Builder.<OlivierEntity>of(OlivierEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(OLIVIER_KEY));

	private static final ResourceKey<EntityType<?>> AMESTRIAN_SOLDIER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("amestrian_soldier"));

	/** Un soldat d'Amestris, en uniforme bleu. */
	public static final EntityType<AmestrianSoldierEntity> AMESTRIAN_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, AMESTRIAN_SOLDIER_KEY,
			EntityType.Builder.<AmestrianSoldierEntity>of(AmestrianSoldierEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(AMESTRIAN_SOLDIER_KEY));

	private static final ResourceKey<EntityType<?>> IMMORTAL_SOLDIER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("immortal_soldier"));

	/** Un soldat immortel, sous Central et au Laboratoire 5. */
	public static final EntityType<ImmortalSoldierEntity> IMMORTAL_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE, IMMORTAL_SOLDIER_KEY,
			EntityType.Builder.<ImmortalSoldierEntity>of(ImmortalSoldierEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(IMMORTAL_SOLDIER_KEY));

	private static final ResourceKey<EntityType<?>> HAUNTED_ARMOR_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("haunted_armor"));

	/** Une armure habitée, liée par un sceau de sang. */
	public static final EntityType<HauntedArmorEntity> HAUNTED_ARMOR = Registry.register(BuiltInRegistries.ENTITY_TYPE, HAUNTED_ARMOR_KEY,
			EntityType.Builder.<HauntedArmorEntity>of(HauntedArmorEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 2.0F)
					.eyeHeight(1.8F)
					.clientTrackingRange(10)
					.build(HAUNTED_ARMOR_KEY));

	private static final ResourceKey<EntityType<?>> BARRY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("barry"));

	/** Barry le Boucher, armure habitée au-dessus du Laboratoire 5. */
	public static final EntityType<BarryEntity> BARRY = Registry.register(BuiltInRegistries.ENTITY_TYPE, BARRY_KEY,
			EntityType.Builder.<BarryEntity>of(BarryEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 2.0F)
					.eyeHeight(1.8F)
					.clientTrackingRange(10)
					.build(BARRY_KEY));

	private static final ResourceKey<EntityType<?>> CHIMERA_BEAST_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("chimera_beast"));

	/** Une chimère massive, lion, serpent et bouc. */
	public static final EntityType<ChimeraBeastEntity> CHIMERA_BEAST = Registry.register(BuiltInRegistries.ENTITY_TYPE, CHIMERA_BEAST_KEY,
			EntityType.Builder.<ChimeraBeastEntity>of(ChimeraBeastEntity::new, MobCategory.MONSTER)
					.sized(1.4F, 1.4F)
					.eyeHeight(1.2F)
					.clientTrackingRange(10)
					.build(CHIMERA_BEAST_KEY));

	private static final ResourceKey<EntityType<?>> CHIMERA_CRAWLER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("chimera_crawler"));

	/** Une chimère rampante, qui grimpe aux murs. */
	public static final EntityType<ChimeraCrawlerEntity> CHIMERA_CRAWLER = Registry.register(BuiltInRegistries.ENTITY_TYPE, CHIMERA_CRAWLER_KEY,
			EntityType.Builder.<ChimeraCrawlerEntity>of(ChimeraCrawlerEntity::new, MobCategory.MONSTER)
					.sized(1.2F, 0.9F)
					.eyeHeight(0.65F)
					.clientTrackingRange(10)
					.build(CHIMERA_CRAWLER_KEY));

	private static final ResourceKey<EntityType<?>> BRIGGS_SOLDIER_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("briggs_soldier"));

	/** Un soldat de Briggs, en manteau d'hiver : la même classe que le soldat d'Amestris. */
	public static final EntityType<AmestrianSoldierEntity> BRIGGS_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			BRIGGS_SOLDIER_KEY,
			EntityType.Builder.<AmestrianSoldierEntity>of(AmestrianSoldierEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(BRIGGS_SOLDIER_KEY));

	private static final ResourceKey<EntityType<?>> ALCHEMICAL_MINE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("alchemical_mine"));

	/** Une mine alchimique de Kimblee, posée sur un bloc. */
	public static final EntityType<AlchemicalMineEntity> ALCHEMICAL_MINE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			ALCHEMICAL_MINE_KEY,
			EntityType.Builder.<AlchemicalMineEntity>of(AlchemicalMineEntity::new, MobCategory.MISC)
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.build(ALCHEMICAL_MINE_KEY));

	private static final ResourceKey<EntityType<?>> THROWING_KNIFE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("throwing_knife"));

	/** Un couteau de lancer de Hughes. */
	public static final EntityType<ThrowingKnifeEntity> THROWING_KNIFE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			THROWING_KNIFE_KEY,
			EntityType.Builder.<ThrowingKnifeEntity>of(ThrowingKnifeEntity::new, MobCategory.MISC)
					.sized(0.4F, 0.4F)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(THROWING_KNIFE_KEY));

	private static final ResourceKey<EntityType<?>> CORNELLO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("cornello"));

	/** Le père Cornello, faux prophète de Liore. */
	public static final EntityType<CornelloEntity> CORNELLO = Registry.register(BuiltInRegistries.ENTITY_TYPE, CORNELLO_KEY,
			EntityType.Builder.<CornelloEntity>of(CornelloEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(CORNELLO_KEY));

	private static final ResourceKey<EntityType<?>> SCAR_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("scar"));

	/** Scar, l'Ishvalien au bras tatoué, dans les ruines d'Ishval. */
	public static final EntityType<ScarEntity> SCAR = Registry.register(BuiltInRegistries.ENTITY_TYPE, SCAR_KEY,
			EntityType.Builder.<ScarEntity>of(ScarEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(SCAR_KEY));

	private static final ResourceKey<EntityType<?>> HOHENHEIM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("hohenheim"));

	/** Van Hohenheim, dans les ruines de Xerxès. */
	public static final EntityType<HohenheimEntity> HOHENHEIM = Registry.register(BuiltInRegistries.ENTITY_TYPE, HOHENHEIM_KEY,
			EntityType.Builder.<HohenheimEntity>of(HohenheimEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.9F)
					.eyeHeight(1.7F)
					.clientTrackingRange(10)
					.build(HOHENHEIM_KEY));

	private static final ResourceKey<EntityType<?>> GATE_HAND_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Fmab.id("gate_hand"));

	/** Un bras noir de la Porte, le temps d'une cinématique. */
	public static final EntityType<GateHandEntity> GATE_HAND = Registry.register(BuiltInRegistries.ENTITY_TYPE, GATE_HAND_KEY,
			EntityType.Builder.<GateHandEntity>of(GateHandEntity::new, MobCategory.MISC)
					.noSave()
					.sized(0.4F, 0.4F)
					.clientTrackingRange(10)
					.updateInterval(2)
					.build(GATE_HAND_KEY));

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
		FabricDefaultAttributeRegistry.register(MAY_CHANG, MayChangEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DRACHMA_SOLDIER, DrachmaSoldierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(OLIVIER, OlivierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(AMESTRIAN_SOLDIER, AmestrianSoldierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(IMMORTAL_SOLDIER, ImmortalSoldierEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HAUNTED_ARMOR, HauntedArmorEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BARRY, BarryEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CHIMERA_BEAST, ChimeraBeastEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CHIMERA_CRAWLER, ChimeraCrawlerEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CORNELLO, CornelloEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SCAR, ScarEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HOHENHEIM, HohenheimEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BRIGGS_SOLDIER, AmestrianSoldierEntity.createAttributes());
		// Les chimères rôdent la nuit, comme tout monstre ; on en croise un peu partout.
		SpawnPlacements.register(CHIMERA_BEAST, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(CHIMERA_CRAWLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(IMMORTAL_SOLDIER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		// Partout où rôdent les monstres : pas dans les îles champignons ni au fond des grottes profondes.
		var chimeraLands = BiomeSelectors.tag(BiomeTags.IS_OVERWORLD)
				.and(BiomeSelectors.excludeByKey(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_DARK));
		BiomeModifications.addSpawn(chimeraLands, MobCategory.MONSTER, CHIMERA_BEAST, 6, 1, 1);
		BiomeModifications.addSpawn(chimeraLands, MobCategory.MONSTER, CHIMERA_CRAWLER, 10, 1, 2);
		// Les soldats de Drachma n'apparaissent que dans le noir, comme tout monstre.
		// Ils rôdent au pied du mur, pas dans le fort : jamais sur un sol bâti.
		SpawnPlacements.register(DRACHMA_SOLDIER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
						&& !level.getBlockState(pos.below()).is(BlockTags.STONE_BRICKS)
						&& !level.getBlockState(pos.below()).is(BlockTags.PLANKS)
						&& !level.getBlockState(pos.below()).is(Blocks.POLISHED_ANDESITE)
						&& !level.getBlockState(pos.below()).is(Blocks.SMOOTH_STONE));
	}
}
