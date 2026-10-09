package com.ajustor.fmab.registry;

import com.ajustor.fmab.Fmab;
import com.ajustor.fmab.block.AlchemistTableBlock;
import com.ajustor.fmab.block.AutomailBenchBlock;
import com.ajustor.fmab.block.TransmutationCircleBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class FmabBlocks {
	public static final Block TRANSMUTATION_CIRCLE = register("transmutation_circle", TransmutationCircleBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.SNOW)
					.noCollision()
					.instabreak()
					.sound(SoundType.SAND)
					.pushReaction(PushReaction.DESTROY)
					.replaceable());

	public static final Block ALCHEMIST_TABLE = register("alchemist_table", AlchemistTableBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD)
					.strength(2.5f)
					.sound(SoundType.WOOD));

	/** Le sol de l'Espace blanc : blanc à perte de vue, incassable. */
	public static final Block WHITE_FLOOR = register("white_floor", Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.SNOW)
					.strength(-1.0f, 3600000.0f)
					.noLootTable()
					.sound(SoundType.STONE));

	/** La pierre sculptée de la Porte de la Vérité. */
	public static final Block GATE_STONE = register("gate_stone", Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.strength(-1.0f, 3600000.0f)
					.noLootTable()
					.sound(SoundType.STONE));

	/** Ce qu'il y a derrière la Porte : du noir, et des yeux. */
	public static final Block GATE_DARKNESS = register("gate_darkness", Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(-1.0f, 3600000.0f)
					.noLootTable()
					.sound(SoundType.SCULK));

	/** Établi d'automail : on y pose un bras ou une jambe sur un membre perdu. */
	public static final Block AUTOMAIL_BENCH = register("automail_bench", AutomailBenchBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5f)
					.requiresCorrectToolForDrops()
					.sound(SoundType.ANVIL));

	private FmabBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		return Blocks.register(ResourceKey.create(Registries.BLOCK, Fmab.id(name)), factory, properties);
	}

	public static void register() {
		// Le chargement de la classe suffit.
	}
}
