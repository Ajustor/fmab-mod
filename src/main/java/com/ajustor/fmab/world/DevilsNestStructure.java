package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Le Devil's Nest : le bar où Greed tient sa cour, quelque part du côté de Dublith. */
public class DevilsNestStructure extends Structure {
	public static final MapCodec<DevilsNestStructure> CODEC = simpleCodec(DevilsNestStructure::new);
	private static final int SIZE_X = 13;
	private static final int SIZE_Z = 9;

	public DevilsNestStructure(StructureSettings settings) {
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
		return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ProceduralPiece("devils_nest",
				origin, SIZE_X, Blueprints.houseHeight(1, SIZE_Z), SIZE_Z, 3, rotation, context.random().nextLong()))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.DEVILS_NEST;
	}
}
