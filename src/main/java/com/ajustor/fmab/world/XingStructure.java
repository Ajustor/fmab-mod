package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Un pavillon de Xing, au fond des jungles et des bosquets de cerisiers : une cour fermée de murs de
 * briques de boue, un pavillon aux piliers rouges et aux toits de tuiles en gradins. May Chang y
 * enseigne l'alkahestry.
 */
public class XingStructure extends Structure {
	public static final MapCodec<XingStructure> CODEC = simpleCodec(XingStructure::new);
	private static final int SIZE = 23;

	public XingStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int y = context.chunkGenerator().getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos corner = new BlockPos(cx - SIZE / 2, y, cz - SIZE / 2);
		Rotation rotation = Rotation.values()[context.random().nextInt(4)];
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(
				new ProceduralPiece("xing_pavilion", corner, SIZE, 12, SIZE, 0, rotation, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.XING;
	}
}
