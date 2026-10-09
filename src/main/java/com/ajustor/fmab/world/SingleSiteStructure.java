package com.ajustor.fmab.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.List;
import java.util.Optional;

/**
 * Un lieu d'un seul tenant, posé en surface et centré sur son chunk : une seule pièce, dont le plan
 * et la taille viennent du JSON de la structure ({@code "piece"}, {@code "size"}…). Les ruines
 * d'Ishval et de Xerxès en sont. Il ne se pose jamais dans Central, ni près des emplacements des
 * lieux listés dans {@code "avoid"}.
 */
public class SingleSiteStructure extends Structure {
	public static final MapCodec<SingleSiteStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("piece").forGetter(s -> s.piece),
			Codec.INT.fieldOf("size").forGetter(s -> s.size),
			Codec.INT.fieldOf("height").forGetter(s -> s.height),
			Codec.INT.optionalFieldOf("ground", 0).forGetter(s -> s.ground),
			ResourceKey.codec(Registries.STRUCTURE_SET).listOf().optionalFieldOf("avoid", List.of())
					.forGetter(s -> s.avoid)
	).apply(i, SingleSiteStructure::new));

	/** Distance minimale, en chunks, aux emplacements des lieux à éviter. */
	private static final int AVOID_CHUNKS = 6;

	private final String piece;
	private final int size;
	private final int height;
	private final int ground;
	private final List<ResourceKey<StructureSet>> avoid;

	public SingleSiteStructure(StructureSettings settings, String piece, int size, int height, int ground,
			List<ResourceKey<StructureSet>> avoid) {
		super(settings);
		this.piece = piece;
		this.size = size;
		this.height = height;
		this.ground = ground;
		this.avoid = List.copyOf(avoid);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (Clearance.nearCentral(context) || avoid.stream().anyMatch(s -> Clearance.near(context, s, AVOID_CHUNKS))) {
			return Optional.empty();
		}
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
		return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(
				new ProceduralPiece(piece, corner, size, height, size, ground, rotation, seed))));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.SINGLE_SITE;
	}
}
