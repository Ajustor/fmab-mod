package com.ajustor.fmab.world;

import com.ajustor.fmab.world.Hamlet.Spot;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Resembool : un petit hameau de campagne. Quelques fermes, la maison des Rockbell et son atelier
 * d'automail, la maison des Elric et le bureau de Hohenheim, des champs de blé. Chaque bâtiment suit le relief à son emplacement.
 */
public class ResemboolStructure extends Structure {
	public static final MapCodec<ResemboolStructure> CODEC = simpleCodec(ResemboolStructure::new);

	private static final Spot[] LAYOUT = {
			new Spot(-4, -4, "rockbell_house_dressed", 11, 9, 2, Rotation.NONE),
			new Spot(-22, 4, "resembool_house_dressed", 9, 7, 1, Rotation.CLOCKWISE_90),
			new Spot(14, -14, "elric_house_dressed", 9, 7, 2, Rotation.CLOCKWISE_180),
			new Spot(18, 10, "resembool_house_dressed", 9, 7, 1, Rotation.COUNTERCLOCKWISE_90),
			new Spot(-8, -22, "resembool_house_dressed", 9, 7, 1, Rotation.CLOCKWISE_180),
			new Spot(-6, 14, "resembool_field", 11, 9, 0, Rotation.NONE),
			new Spot(-24, -18, "resembool_field", 9, 11, 0, Rotation.NONE),
			new Spot(4, 26, "resembool_field", 11, 9, 0, Rotation.NONE),
			new Spot(26, -4, "resembool_pasture", 11, 9, 0, Rotation.NONE),
			Spot.tree(-14, -8, "town_tree"),
			Spot.tree(6, -14, "birch_tree"),
			Spot.tree(-16, 18, "town_tree"),
			Spot.tree(24, 24, "town_tree"),
			Spot.tree(-30, -2, "birch_tree"),
			Spot.tree(30, 12, "town_tree"),
			Spot.tree(10, 12, "birch_tree"),
	};

	public ResemboolStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
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
		return FmabStructures.RESEMBOOL;
	}
}
