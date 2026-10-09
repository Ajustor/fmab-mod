package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * L'île de Yock, au large de Dublith : un îlot boisé surgi de la mer, une cabane, un feu de camp. Il
 * se bâtit depuis le fond jusqu'à la surface ; Izumi y abandonne ses élèves.
 */
public class YockIslandStructure extends Structure {
	public static final MapCodec<YockIslandStructure> CODEC = simpleCodec(YockIslandStructure::new);
	private static final int SIZE = 25;
	private static final int MAX_DEPTH = 40;

	public YockIslandStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int sea = context.chunkGenerator().getSeaLevel();
		int floor = context.chunkGenerator().getFirstFreeHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG,
				context.heightAccessor(), context.randomState());
		int depth = sea - floor;
		if (depth < 3 || depth > MAX_DEPTH) {
			return Optional.empty();
		}
		BlockPos corner = new BlockPos(cx - SIZE / 2, sea, cz - SIZE / 2);
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(corner, builder -> builder.addPiece(
				new ProceduralPiece("yock_island", corner, SIZE, 10, SIZE, depth + 1, Rotation.NONE, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.YOCK_ISLAND;
	}
}
