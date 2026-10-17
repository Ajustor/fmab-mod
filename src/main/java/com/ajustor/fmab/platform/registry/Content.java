package com.ajustor.fmab.platform.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

/**
 * Le port des enregistrements : tout ce que chaque plateforme enregistre à sa façon. Le mod décrit
 * son contenu, la plateforme l'inscrit au bon moment.
 */
public interface Content {
	/** Inscrit une valeur dans un registre du jeu (objets, blocs, entités, sons…) et la rend. */
	<T, V extends T> V register(Registry<T> registry, ResourceKey<T> key, V value);

	default <T, V extends T> V register(Registry<T> registry, Identifier id, V value) {
		return register(registry, ResourceKey.create(registry.key(), id), value);
	}

	/**
	 * Un registre de données lu dans les packs ({@code data/<ns>/<dossier>/…}).
	 *
	 * @param synced envoyé aux clients à la connexion
	 */
	<T> void dataRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, boolean synced);

	/** Les attributs (vie, vitesse…) des créatures d'un type. */
	<T extends LivingEntity> void attributes(EntityType<T> type, AttributeSupplier.Builder attributes);

	/** Un onglet du mode créatif, à remplir avec son constructeur. */
	CreativeModeTab.Builder creativeTab();

	/** Fait apparaître une créature dans les biomes d'un tag, sauf ceux exclus. */
	void addSpawn(TagKey<Biome> biomes, Set<ResourceKey<Biome>> except, MobCategory category, EntityType<?> type,
			int weight, int minGroup, int maxGroup);
}
