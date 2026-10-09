package com.ajustor.fmab.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Un lieu d'un seul tenant, posé en surface et centré sur son chunk : une seule pièce, dont le plan
 * et la taille viennent du JSON de la structure ({@code "piece"}, {@code "size"}…). Les ruines
 * d'Ishval et de Xerxès en sont.
 */
public class SingleSiteStructure extends Structure {
	public static final MapCodec<SingleSiteStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("piece").forGetter(s -> s.piece),
			Codec.INT.fieldOf("size").forGetter(s -> s.size),
			Codec.INT.fieldOf("height").forGetter(s -> s.height),
			Codec.INT.optionalFieldOf("ground", 3).forGetter(s -> s.ground)
	).apply(i, SingleSiteStructure::new));

	private final String piece;
	private final int size;
	private final int height;
	private final int ground;

	public SingleSiteStructure(StructureSettings settings, String piece, int size, int height, int ground) {
		super(settings);
		this.piece = piece;
		this.size = size;
		this.height = height;
		this.ground = ground;
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
		BlockPos corner = new BlockPos(cx - size / 2, y, cz - size / 2);
		Rotation rotation = Rotation.values()[context.random().nextInt(4)];
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(corner, builder -> builder.addPiece(
				new ProceduralPiece(piece, corner, size, height, size, ground, rotation, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.SINGLE_SITE;
	}
}
