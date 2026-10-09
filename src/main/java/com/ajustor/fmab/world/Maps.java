package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Des cartes d'exploration vers les lieux du mod, comme celles des cartographes. */
public final class Maps {
	private static final int SEARCH_RADIUS = 100;

	private Maps() {
	}

	/**
	 * Une carte vers le plus proche lieu de ce type, marqué d'une croix.
	 *
	 * @return la carte, ou une pile vide s'il n'y en a pas à portée
	 */
	public static ItemStack toStructure(ServerLevel level, BlockPos from, TagKey<Structure> destination,
			Holder<MapDecorationType> decoration, Component name) {
		BlockPos target = level.findNearestMapStructure(destination, from, SEARCH_RADIUS, false);
		if (target == null) {
			return ItemStack.EMPTY;
		}
		ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, target, "+", decoration);
		map.set(DataComponents.ITEM_NAME, name);
		return map;
	}
}
