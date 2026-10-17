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
 * Le Laboratoire 5 : un complexe de l'armée à l'abandon, et sous lui la salle où l'armée fabriquait
 * des Pierres philosophales. Lust et Gluttony le gardent.
 */
public class Laboratory5Structure extends Structure {
	public static final MapCodec<Laboratory5Structure> CODEC = simpleCodec(Laboratory5Structure::new);
	static final int SIZE_X = 25;
	static final int SIZE_Z = 21;
	/** Profondeur du laboratoire sous la surface, et hauteur du bâtiment au-dessus. */
	static final int DEPTH = -Laboratory5.HALL_FLOOR;
	static final int ABOVE = Laboratory5.SURFACE_TOP + 1;

	public Laboratory5Structure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return UniqueSite.only(this, context, this::at);
	}

	/** Le lieu sur ce chunk-là, si le terrain s'y prête (voir {@link UniqueSite}). */
	private Optional<GenerationStub> at(GenerationContext context, ChunkPos chunk) {
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		int y = context.chunkGenerator().getFirstFreeHeight(cx + SIZE_X / 2, cz + SIZE_Z / 2,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos origin = new BlockPos(cx, y, cz);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ProceduralPiece("laboratory_5",
				origin, SIZE_X, DEPTH + ABOVE, SIZE_Z, DEPTH, rotation, context.random().nextLong()))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.LABORATORY_5;
	}
}
