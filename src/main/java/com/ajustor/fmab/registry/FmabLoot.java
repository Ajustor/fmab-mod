package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.platform.Platform;
import com.ajustor.fmab.world.SiteMapFunction;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.Map;
import com.ajustor.fmab.platform.event.ContentEvents;

/**
 * Ce que le mod glisse dans les coffres du jeu de base. Les Notes sur la transmutation humaine
 * dorment, très rares, dans les bibliothèques des forts et les cités antiques (les lieux du mod en
 * gardent aussi : Laboratoire 5, ruines de Xerxès).
 */
public final class FmabLoot {
	/** Chance qu'un coffre en contienne un exemplaire. */
	private static final Map<ResourceKey<LootTable>, Float> HUMAN_TRANSMUTATION_NOTES = Map.of(
			BuiltInLootTables.STRONGHOLD_LIBRARY, 0.02f,
			BuiltInLootTables.ANCIENT_CITY, 0.01f);

	/** Les cartes des cartographes vers les lieux du mod (voir {@link SiteMapFunction}). */
	public static final MapCodec<SiteMapFunction> SITE_MAP = Platform.get().content().register(
			BuiltInRegistries.LOOT_FUNCTION_TYPE, Fmab.id("site_map"), SiteMapFunction.MAP_CODEC);

	private FmabLoot() {
	}

	public static void register() {
		ContentEvents.MODIFY_LOOT.register((key, table, builtin, registries) -> {
			Float chance = HUMAN_TRANSMUTATION_NOTES.get(key);
			if (chance == null || !builtin) {
				return;
			}
			table.withPool(LootPool.lootPool()
					.when(LootItemRandomChanceCondition.randomChance(chance))
					.add(LootItem.lootTableItem(FmabItems.HUMAN_TRANSMUTATION_NOTES)));
		});
	}
}
