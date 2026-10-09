package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Fort Briggs, dans les montagnes enneigées du Nord : une muraille colossale qui barre la passe face
 * à Drachma, deux tours, et derrière, la caserne où la générale Armstrong tient garnison. Les
 * soldats de Drachma rôdent la nuit au pied du mur.
 */
public class FortBriggsStructure extends Structure {
	public static final MapCodec<FortBriggsStructure> CODEC = simpleCodec(FortBriggsStructure::new);
	/** Écart de hauteur toléré entre les coins de l'emprise. */
	private static final int MAX_RELIEF = 14;

	public FortBriggsStructure(StructureSettings settings) {
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
		int sx = Blueprints.FortBriggs.SIZE_X, sz = Blueprints.FortBriggs.SIZE_Z;
		// Un col assez régulier : sur une arête trop accidentée, la muraille flotterait ou s'enterrerait.
		int min = y, max = y;
		for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
			int h = context.chunkGenerator().getFirstFreeHeight(cx + d[0] * sx / 2, cz + d[1] * sz / 2,
					Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			min = Math.min(min, h);
			max = Math.max(max, h);
		}
		if (max - min > MAX_RELIEF) {
			return Optional.empty();
		}
		BlockPos corner = new BlockPos(cx - sx / 2, y, cz - sz / 2);
		Rotation rotation = Rotation.values()[context.random().nextInt(4)];
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(
				new ProceduralPiece("fort_briggs", corner, sx, Blueprints.FortBriggs.SIZE_Y, sz, 1, rotation, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.FORT_BRIGGS;
	}
}
