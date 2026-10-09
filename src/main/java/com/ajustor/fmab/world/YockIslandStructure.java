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
		// En pleine mer : de l'eau tout autour, et la fondation descend jusqu'au fond le plus bas.
		for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
			int f = context.chunkGenerator().getFirstFreeHeight(cx + d[0] * SIZE / 2, cz + d[1] * SIZE / 2,
					Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			if (sea - f < 2) {
				return Optional.empty();
			}
			depth = Math.max(depth, sea - f);
		}
		if (depth < 3 || depth > MAX_DEPTH) {
			return Optional.empty();
		}
		BlockPos corner = new BlockPos(cx - SIZE / 2, sea, cz - SIZE / 2);
		long seed = context.random().nextLong();
		int ground = depth + 1;
		return Optional.of(new GenerationStub(new BlockPos(cx, sea, cz), builder -> builder.addPiece(
				new ProceduralPiece("yock_island", corner, SIZE, 10, SIZE, ground, Rotation.NONE, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.YOCK_ISLAND;
	}
}
