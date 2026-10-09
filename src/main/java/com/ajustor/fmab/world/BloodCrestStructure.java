package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Un point de sang du cercle national : le champ d'un ancien massacre, des murs écroulés, des os, du
 * sang figé, et au centre le sceau sur son estrade. On en trouve un peu partout en Amestris.
 */
public class BloodCrestStructure extends Structure {
	public static final MapCodec<BloodCrestStructure> CODEC = simpleCodec(BloodCrestStructure::new);
	private static final int SIZE = 15;

	public BloodCrestStructure(StructureSettings settings) {
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
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(
				new ProceduralPiece("blood_crest", corner, SIZE, 6, SIZE, 1, Rotation.NONE, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.BLOOD_CREST;
	}
}
