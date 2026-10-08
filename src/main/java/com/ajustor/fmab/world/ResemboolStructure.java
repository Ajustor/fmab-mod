package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Resembool : un petit hameau de campagne. Quelques fermes, la maison des Rockbell et son atelier
 * d'automail, des champs de blé. Chaque bâtiment suit le relief à son emplacement.
 */
public class ResemboolStructure extends Structure {
	public static final MapCodec<ResemboolStructure> CODEC = simpleCodec(ResemboolStructure::new);

	/** Emplacements autour du centre du hameau : (dx, dz, sorte, largeur, profondeur, étages, orientation). */
	private record Spot(int dx, int dz, String kind, int sx, int sz, int floors, Rotation rotation) {
	}

	private static final Spot[] LAYOUT = {
			new Spot(-4, -4, "rockbell_house", 11, 9, 2, Rotation.NONE),
			new Spot(-22, 4, "resembool_house", 9, 7, 1, Rotation.CLOCKWISE_90),
			new Spot(14, -14, "resembool_house", 9, 7, 2, Rotation.CLOCKWISE_180),
			new Spot(18, 10, "resembool_house", 9, 7, 1, Rotation.COUNTERCLOCKWISE_90),
			new Spot(-6, 14, "resembool_field", 11, 9, 0, Rotation.NONE),
			new Spot(-24, -18, "resembool_field", 9, 11, 0, Rotation.NONE),
			new Spot(4, 26, "resembool_field", 11, 9, 0, Rotation.NONE),
	};

	public ResemboolStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int y = height(context, cx, cz);
		if (y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos center = new BlockPos(cx, y, cz);
		return Optional.of(new GenerationStub(center, builder -> pieces(builder, context, center)));
	}

	private static int height(GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
	}

	private static void pieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos center) {
		for (Spot s : LAYOUT) {
			int x = center.getX() + s.dx(), z = center.getZ() + s.dz();
			int y = height(context, x + s.sx() / 2, z + s.sz() / 2);
			int sizeY = s.floors() == 0 ? 3 : Blueprints.houseHeight(s.floors(), s.sz());
			builder.addPiece(new ProceduralPiece(s.kind(), new BlockPos(x, y, z), s.sx(), sizeY, s.sz(), 3,
					s.rotation(), context.random().nextLong()));
		}
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.RESEMBOOL;
	}
}
