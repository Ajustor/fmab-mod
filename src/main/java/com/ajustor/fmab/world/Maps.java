package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Optional;

/** Des cartes d'exploration vers les lieux du mod, comme celles des cartographes. */
public final class Maps {
	/**
	 * Rayon de recherche, en cases de la grille de placement, des lieux qui se répètent (les points de
	 * sang). Ceux-là sont nombreux : une recherche courte les trouve, une longue lirait des milliers
	 * de chunks sur le disque.
	 */
	static final int NEAR_RADIUS = 16;

	private Maps() {
	}

	/**
	 * Une carte vers le plus proche lieu de ce type, marqué d'une croix.
	 *
	 * @return la carte, ou une pile vide s'il n'y en a pas
	 */
	public static ItemStack toStructure(ServerLevel level, BlockPos from, TagKey<Structure> destination,
			Holder<MapDecorationType> decoration, Component name) {
		ItemStack map = toStructure(level, from, destination, decoration, (byte) 2);
		if (!map.isEmpty()) {
			map.set(DataComponents.ITEM_NAME, name);
		}
		return map;
	}

	static ItemStack toStructure(ServerLevel level, BlockPos from, TagKey<Structure> destination,
			Holder<MapDecorationType> decoration, byte zoom) {
		return find(level, from, destination).map(target -> {
			ItemStack map = MapItem.create(level, target.getX(), target.getZ(), zoom, true, true);
			MapItem.renderBiomePreviewMap(level, map);
			MapItemSavedData.addTargetDecoration(map, target, "+", decoration);
			return map;
		}).orElse(ItemStack.EMPTY);
	}

	/**
	 * Le plus proche lieu de ce type. Un lieu unique se trouve par sa place, connue d'avance (voir
	 * {@link UniqueSite#locate}) ; les autres se cherchent comme le jeu cherche ses structures, de près.
	 */
	public static Optional<BlockPos> find(ServerLevel level, BlockPos from, TagKey<Structure> destination) {
		BlockPos best = null;
		boolean repeated = false;
		for (Holder<Structure> structure : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(destination)
				.map(set -> set.stream().toList()).orElse(List.of())) {
			if (structure.value() instanceof UniqueSite.Sited sited && sited.unique()) {
				BlockPos site = UniqueSite.locate(level, structure).orElse(null);
				if (site != null && (best == null || site.distSqr(from) < best.distSqr(from))) {
					best = site;
				}
			} else {
				repeated = true;
			}
		}
		if (repeated) {
			BlockPos near = level.findNearestMapStructure(destination, from, NEAR_RADIUS, false);
			if (near != null && (best == null || near.distSqr(from) < best.distSqr(from))) {
				best = near;
			}
		}
		return Optional.ofNullable(best);
	}
}
