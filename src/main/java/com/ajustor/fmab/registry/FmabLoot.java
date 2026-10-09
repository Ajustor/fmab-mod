package com.ajustor.fmab.registry;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Map;

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

	private FmabLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			Float chance = HUMAN_TRANSMUTATION_NOTES.get(key);
			if (chance == null || !source.isBuiltin()) {
				return;
			}
			table.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1))
					.when(LootItemRandomChanceCondition.randomChance(chance))
					.add(LootItem.lootTableItem(FmabItems.HUMAN_TRANSMUTATION_NOTES)));
		});
	}
}
