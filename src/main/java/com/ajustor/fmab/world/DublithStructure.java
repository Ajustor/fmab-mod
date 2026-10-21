package com.ajustor.fmab.world;

import com.ajustor.fmab.world.Hamlet.Spot;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Dublith, petite ville du Sud : une place pavée, quelques maisons de brique et de bois, et la
 * boucherie des Curtis, où Izumi accueille (ou chasse) les apprentis alchimistes. L'île de Yock, son
 * terrain d'épreuve, est quelque part en mer.
 */
public class DublithStructure extends Structure implements UniqueSite.Sited {
	public static final MapCodec<DublithStructure> CODEC = simpleCodec(DublithStructure::new);

	/** Une petite ville, mais aérée : de la place entre les maisons. */
	private static final double SPREAD = 1.45;

	private static final Spot[] LAYOUT = {
			new Spot(-6, -6, "dublith_square", 13, 13, 0, Rotation.NONE),
			new Spot(-4, -4, "market_stall", 3, 3, 0, Rotation.NONE),
			new Spot(2, 2, "market_stall", 3, 3, 0, Rotation.CLOCKWISE_180),
			new Spot(-6, -17, "curtis_butcher_dressed", 11, 9, 2, Rotation.CLOCKWISE_180),
			new Spot(10, -7, "dublith_house_dressed", 9, 7, 2, Rotation.COUNTERCLOCKWISE_90),
			new Spot(-18, -5, "dublith_house_dressed", 9, 7, 1, Rotation.CLOCKWISE_90),
			new Spot(-4, 10, "dublith_house_dressed", 9, 7, 2, Rotation.NONE),
			new Spot(10, 9, "dublith_house_dressed", 8, 7, 1, Rotation.NONE),
			new Spot(-20, 10, "dublith_house_dressed", 9, 7, 1, Rotation.NONE),
			new Spot(21, -4, "dublith_house_dressed", 9, 7, 2, Rotation.COUNTERCLOCKWISE_90),
			new Spot(-6, 22, "dublith_house_dressed", 9, 7, 1, Rotation.CLOCKWISE_180),
			Spot.tree(-15, -15, "town_tree"),
			Spot.tree(14, -16, "town_tree"),
			Spot.tree(6, 20, "birch_tree"),
	};

	public DublithStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return UniqueSite.only(this, context, this::siteAt);
	}

	/** Le lieu sur ce chunk-là, si le terrain s'y prête (voir {@link UniqueSite}). */
	@Override
	public Optional<GenerationStub> siteAt(GenerationContext context, ChunkPos chunk) {
		if (Clearance.nearCentral(context, chunk)) {
			return Optional.empty();
		}
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		int y = Hamlet.height(context, cx, cz);
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos center = new BlockPos(cx, y, cz);
		return Optional.of(new GenerationStub(center, builder -> Hamlet.build(builder, context, center, LAYOUT,
				PathPiece.Style.DUBLITH, SPREAD)));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.DUBLITH;
	}
}
