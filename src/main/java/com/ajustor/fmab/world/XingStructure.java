package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
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
public class XingStructure extends Structure implements UniqueSite.Sited {
	public static final MapCodec<XingStructure> CODEC = simpleCodec(XingStructure::new);
	static final int SIZE = 23;
	/** Le domaine dont le biome devient celui de Xing. */
	static final int GROUNDS = 71;
	/** Le temple et les maisons. */
	static final int HALL = 9;

	public XingStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return UniqueSite.only(this, context, this::siteAt);
	}

	/** Le lieu sur ce chunk-là, si le terrain s'y prête (voir {@link UniqueSite}). */
	@Override
	public Optional<GenerationStub> siteAt(GenerationContext context, ChunkPos chunk) {
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		int y = context.chunkGenerator().getFirstFreeHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos corner = new BlockPos(cx - SIZE / 2, y, cz - SIZE / 2);
		Rotation rotation = Rotation.values()[context.random().nextInt(4)];
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> {
			builder.addPiece(new ProceduralPiece("xing_grounds", new BlockPos(cx - GROUNDS / 2, y, cz - GROUNDS / 2),
					GROUNDS, 1, GROUNDS, 0, Rotation.NONE, seed));
			builder.addPiece(new ProceduralPiece("xing_pavilion", corner, SIZE, 12, SIZE, 0, rotation, seed));
			// Autour du pavillon : le temple d'un côté, deux maisons des autres.
			int[][] halls = {{SIZE / 2 + 5, -HALL / 2}, {-SIZE / 2 - 5 - HALL, -HALL / 2}, {-HALL / 2, SIZE / 2 + 5}};
			for (int i = 0; i < halls.length; i++) {
				int hx = cx + halls[i][0], hz = cz + halls[i][1];
				int hy = context.chunkGenerator().getFirstFreeHeight(hx + HALL / 2, hz + HALL / 2,
						Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				builder.addPiece(new ProceduralPiece(i == 0 ? "xing_shrine" : "xing_house", new BlockPos(hx, hy - 1, hz),
						HALL, 8, HALL, 1, Rotation.values()[(i + 1) % 4], seed ^ i));
			}
		}));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.XING;
	}
}
