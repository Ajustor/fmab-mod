package com.ajustor.fmab.fabric;

import com.ajustor.fmab.platform.registry.Content;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

/** Les enregistrements du mod sur Fabric : registres du jeu tout de suite, le reste par Fabric API. */
final class FabricContent implements Content {
	@Override
	public <T, V extends T> V register(Registry<T> registry, ResourceKey<T> key, V value) {
		return Registry.register(registry, key, value);
	}

	@Override
	public <T> void dataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced) {
		if (synced) {
			DynamicRegistries.registerSynced(key, codec);
		} else {
			DynamicRegistries.register(key, codec);
		}
	}

	@Override
	public <T extends LivingEntity> void attributes(EntityType<T> type, AttributeSupplier.Builder attributes) {
		FabricDefaultAttributeRegistry.register(type, attributes);
	}

	@Override
	public CreativeModeTab.Builder creativeTab() {
		return FabricCreativeModeTab.builder();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addSpawn(TagKey<Biome> biomes, Set<ResourceKey<Biome>> except, MobCategory category, EntityType<?> type,
			int weight, int minGroup, int maxGroup) {
		BiomeModifications.addSpawn(BiomeSelectors.tag(biomes).and(BiomeSelectors.excludeByKey(except)), category,
				(EntityType<? extends net.minecraft.world.entity.Mob>) type, weight, minGroup, maxGroup);
	}
}
