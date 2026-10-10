package com.ajustor.fmab.world;

import com.ajustor.fmab.world.Hamlet.Spot;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Rush Valley : la ville des mécaniciens d'automail, en grès, autour de sa place et de son puits.
 * Des ateliers aux coffres pleins de pièces, et la boutique de Garfiel où travaille Winry. Chaque
 * bâtiment suit le relief à son emplacement.
 */
public class RushValleyStructure extends Structure {
	public static final MapCodec<RushValleyStructure> CODEC = simpleCodec(RushValleyStructure::new);

	private static final Spot[] LAYOUT = {
			new Spot(-6, -6, "rush_valley_square", 13, 13, 0, Rotation.NONE),
			new Spot(-4, -4, "desert_stall", 3, 3, 0, Rotation.NONE),
			new Spot(2, 2, "desert_stall", 3, 3, 0, Rotation.CLOCKWISE_180),
			new Spot(-6, -18, "rush_valley_shop_dressed", 11, 9, 2, Rotation.CLOCKWISE_180),
			new Spot(10, -8, "rush_valley_workshop_dressed", 9, 7, 1, Rotation.COUNTERCLOCKWISE_90),
			new Spot(-18, -6, "rush_valley_workshop_dressed", 9, 7, 2, Rotation.CLOCKWISE_90),
			new Spot(-4, 10, "rush_valley_workshop_dressed", 9, 7, 1, Rotation.NONE),
			new Spot(10, 8, "rush_valley_workshop_dressed", 9, 7, 2, Rotation.NONE),
			new Spot(-20, 10, "rush_valley_workshop_dressed", 9, 7, 1, Rotation.NONE),
			new Spot(20, -6, "rush_valley_workshop_dressed", 9, 7, 2, Rotation.COUNTERCLOCKWISE_90),
			Spot.tree(-14, -16, "acacia_tree"),
			Spot.tree(14, -18, "acacia_tree"),
			Spot.tree(6, 20, "palm_tree"),
			Spot.tree(22, 8, "palm_tree"),
	};

	public RushValleyStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (Clearance.nearCentral(context)) {
			return Optional.empty();
		}
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int y = Hamlet.height(context, cx, cz);
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos center = new BlockPos(cx, y, cz);
		return Optional.of(new GenerationStub(center, builder -> Hamlet.build(builder, context, center, LAYOUT)));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.RUSH_VALLEY;
	}
}
