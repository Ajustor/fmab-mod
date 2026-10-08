package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Ce que chaque glyphe d'élément vise. Les blocs et items concernés sont listés dans des tags, donc
 * modifiables par data pack : {@code data/fmab/tags/block/element/earth.json}, etc.
 */
public final class FmabTags {
	private FmabTags() {
	}

	/** Ce que le burin peut graver : la pierre et le métal. */
	public static final TagKey<Block> ENGRAVABLE = TagKey.create(Registries.BLOCK, Fmab.id("engravable"));

	public static TagKey<Block> elementBlocks(String element) {
		return TagKey.create(Registries.BLOCK, Fmab.id("element/" + element));
	}

	public static TagKey<Item> elementItems(String element) {
		return TagKey.create(Registries.ITEM, Fmab.id("element/" + element));
	}
}
