package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Le Laboratoire 5 : un poste de garde en ruine, et sous lui la salle où l'armée fabriquait des
 * Pierres philosophales. Lust et Gluttony le gardent.
 */
public class Laboratory5Structure extends Structure {
	public static final MapCodec<Laboratory5Structure> CODEC = simpleCodec(Laboratory5Structure::new);
	private static final int SIZE_X = 25;
	private static final int SIZE_Z = 21;
	/** Profondeur du laboratoire sous la surface, et hauteur du poste de garde au-dessus. */
	private static final int DEPTH = -Blueprints.Laboratory5.HALL_FLOOR;
	private static final int ABOVE = 6;

	public Laboratory5Structure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
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
