package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Central City : une grande ville circulaire et fortifiée, posée sur un plateau aplani. Muraille à
 * quatre portes, avenues en croix, boulevard circulaire, place et fontaine ; au nord de la place le
 * quartier général et son arène, à l'est la bibliothèque, à l'ouest la résidence Bradley ; des
 * maisons partout ailleurs. Sous la ville, le tunnel de Sloth, et plus bas le repaire de Père.
 */
public class CentralStructure extends Structure {
	public static final MapCodec<CentralStructure> CODEC = simpleCodec(CentralStructure::new);

	private static final int R = Blueprints.CITY_RADIUS;
	/** Écart de hauteur toléré sur l'emprise : au-delà, le relief est trop accidenté pour une ville. */
	private static final int MAX_RELIEF = 16;
	private static final int LOT = 12;

	public CentralStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int y = height(context, cx, cz);
		int min = y, max = y;
		for (int k = 0; k < 8; k++) {
			double a = Math.PI / 4 * k;
			int h = height(context, cx + (int) (Math.cos(a) * (R - 10)), cz + (int) (Math.sin(a) * (R - 10)));
			min = Math.min(min, h);
			max = Math.max(max, h);
		}
		if (max - min > MAX_RELIEF || y <= context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos center = new BlockPos(cx, y, cz);
		return Optional.of(new GenerationStub(center, builder -> pieces(builder, center, context.random())));
	}

	private static int height(GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
	}

	private static void pieces(StructurePiecesBuilder builder, BlockPos center, WorldgenRandom random) {
		int cx = center.getX(), y = center.getY(), cz = center.getZ();
		BlockPos corner = new BlockPos(cx - R, y, cz - R);
		int size = 2 * R + 1;
		long seed = random.nextLong();
		builder.addPiece(new ProceduralPiece("central_ground", corner, size, 24, size, 6, Rotation.NONE, seed));
		builder.addPiece(new ProceduralPiece("central_roads", corner, size, 4, size, 1, Rotation.NONE, seed));
		builder.addPiece(new ProceduralPiece("central_walls", corner, size, 16, size, 0, Rotation.NONE, seed));
		builder.addPiece(new ProceduralPiece("central_fountain", new BlockPos(cx - 4, y, cz - 4), 9, 5, 9, 1,
				Rotation.NONE, seed));
		// Le quartier général ferme l'avenue nord ; sa façade regarde la place, au sud.
		builder.addPiece(new ProceduralPiece("central_hq", new BlockPos(cx - 15, y, cz - 44), 31, 14, 23, 1,
				Rotation.CLOCKWISE_180, seed));
		// La bibliothèque borde l'avenue est ; sa porte donne sur l'avenue, au nord.
		builder.addPiece(new ProceduralPiece("central_library", new BlockPos(cx + 18, y, cz + 6), 21, 11, 15, 1,
				Rotation.NONE, seed));
		// La résidence Bradley borde l'avenue ouest ; sa porte donne sur l'avenue, au nord.
		builder.addPiece(new ProceduralPiece("bradley_residence", new BlockPos(cx - 33, y, cz + 11), 15,
				Blueprints.houseHeight(2, 11), 11, 1, Rotation.NONE, seed));
		// Sous la ville, le tunnel de Sloth ; on y descend par un puits au milieu de l'avenue sud.
		int depth = -Blueprints.SlothTunnel.FLOOR;
		builder.addPiece(new ProceduralPiece("sloth_tunnel", new BlockPos(cx - R, y, cz + 30), size, depth + 1, 7, depth,
				Rotation.NONE, seed));
		// Plus bas encore, derrière une porte scellée de la paroi sud du tunnel : le repaire de Père.
		builder.addPiece(new ProceduralPiece("father_lair",
				new BlockPos(cx + Blueprints.SlothTunnel.FATHER_DOOR - 16, y, cz + 36), 33, 1, 41,
				-Blueprints.FatherLair.ROOM_FLOOR, Rotation.NONE, seed));
		for (int gx = -R + 6; gx < R - 6; gx += LOT) {
			for (int gz = -R + 6; gz < R - 6; gz += LOT) {
				if (!buildable(gx, gz)) {
					continue;
				}
				int sx = 7 + random.nextInt(3);
				int sz = 7 + random.nextInt(2);
				int floors = 1 + random.nextInt(2);
				Rotation rotation = Rotation.values()[random.nextInt(4)];
				builder.addPiece(new ProceduralPiece("central_house",
						new BlockPos(cx + gx - sx / 2, y, cz + gz - sz / 2), sx, Blueprints.houseHeight(floors, sz), sz,
						1, rotation, random.nextLong()));
			}
		}
	}

	/** Une parcelle de maison : ni sur la voirie, ni sur la place, ni contre la muraille, ni sur un monument. */
	static boolean buildable(int gx, int gz) {
		double r = Math.hypot(gx, gz);
		if (r > 58 || r < Blueprints.PLAZA + 8) {
			return false;
		}
		if (Math.abs(gx) <= 9 || Math.abs(gz) <= 9) {
			return false;
		}
		if (r > 40 && r < 57) {
			return false;
		}
		boolean headquarters = Math.abs(gx) <= 21 && gz >= -50 && gz <= -16;
		boolean library = gx >= 12 && gx <= 44 && gz >= 0 && gz <= 26;
		boolean residence = gx >= -39 && gx <= -13 && gz >= 5 && gz <= 27;
		return !headquarters && !library && !residence;
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.CENTRAL;
	}
}
