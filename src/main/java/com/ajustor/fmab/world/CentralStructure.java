package com.ajustor.fmab.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Central City : une grande ville circulaire et fortifiée, posée sur un plateau aplani. Muraille à
 * quatre portes, avenues en croix, boulevard circulaire, place et fontaine ; au nord de la place le
 * quartier général et son arène, à l'est la bibliothèque, à l'ouest la résidence Bradley ; des
 * maisons en couronnes, des arbres le long des avenues, un marché autour de la fontaine. Sous la ville, le tunnel de Sloth, et plus bas le repaire de Père.
 */
public class CentralStructure extends Structure {
	public static final MapCodec<CentralStructure> CODEC = simpleCodec(CentralStructure::new);

	private static final int R = Blueprints.CITY_RADIUS;
	/** Écart de hauteur toléré sur l'emprise : au-delà, le relief est trop accidenté pour une ville. */
	private static final int MAX_RELIEF = 16;

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
				new BlockPos(cx + Blueprints.SlothTunnel.FATHER_DOOR - Blueprints.FatherLair.DOOR_X, y, cz + 36), 33, 1, 41,
				-Blueprints.FatherLair.ROOM_FLOOR, Rotation.NONE, seed));
		List<Rect> taken = new ArrayList<>(MONUMENTS);
		// Le marché : quatre étals autour de la fontaine, tournés vers elle.
		for (int[] at : new int[][]{{6, 6}, {-9, 6}, {6, -9}, {-9, -9}}) {
			taken.add(new Rect(at[0], at[1], at[0] + 2, at[1] + 2));
			builder.addPiece(new ProceduralPiece("market_stall", new BlockPos(cx + at[0], y, cz + at[1]), 3, 5, 3, 0,
					at[1] > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180, random.nextLong()));
		}
		// Des arbres en rangées le long des avenues, de part et d'autre.
		for (int t = Blueprints.PLAZA + 6; t < Blueprints.WALL_INNER - 6; t += 12) {
			for (int side : new int[]{-1, 1}) {
				for (int sign : new int[]{-1, 1}) {
					int across = side * (Blueprints.AVENUE + 4), along = sign * t;
					tree(builder, taken, cx, y, cz, across, along, random);
					tree(builder, taken, cx, y, cz, along, across, random);
				}
			}
		}
		// Les maisons, en couronnes : autour de la place, tournées vers elle ; de part et d'autre du
		// boulevard, tournées vers lui.
		for (int[] band : new int[][]{{24, -1}, {39, 1}, {59, -1}}) {
			int r = band[0];
			double step = 12.0 / r;
			double start = random.nextDouble() * step;
			for (double a = start; a < Math.PI * 2 + start - step / 2; a += step) {
				house(builder, taken, cx, y, cz, r, a, band[1], random);
			}
		}
		// Des arbres encore, là où il reste de la place.
		for (int k = 0; k < 160; k++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = Blueprints.PLAZA + 4 + random.nextDouble() * (Blueprints.WALL_INNER - Blueprints.PLAZA - 8);
			tree(builder, taken, cx, y, cz, (int) (Math.cos(a) * r), (int) (Math.sin(a) * r), random);
		}
	}

	/** Un rectangle d'emprise, relatif au centre de la ville, bornes comprises. */
	private record Rect(int minX, int minZ, int maxX, int maxZ) {
		boolean overlaps(Rect o, int gap) {
			return minX - gap <= o.maxX && maxX + gap >= o.minX && minZ - gap <= o.maxZ && maxZ + gap >= o.minZ;
		}

		/** Le point le plus proche du centre. */
		double near() {
			int nx = minX > 0 ? minX : maxX < 0 ? -maxX : 0;
			int nz = minZ > 0 ? minZ : maxZ < 0 ? -maxZ : 0;
			return Math.hypot(nx, nz);
		}

		/** Le coin le plus loin du centre. */
		double far() {
			return Math.hypot(Math.max(Math.abs(minX), Math.abs(maxX)), Math.max(Math.abs(minZ), Math.abs(maxZ)));
		}
	}

	/** Le quartier général, la bibliothèque, la résidence Bradley et la fontaine. */
	private static final List<Rect> MONUMENTS = List.of(new Rect(-15, -44, 15, -22), new Rect(18, 6, 38, 20),
			new Rect(-33, 11, -19, 21), new Rect(-4, -4, 4, 4));

	/** Une parcelle libre : ni sur la voirie, ni sur la place, ni contre la muraille, ni sur un voisin. */
	private static boolean free(Rect rect, List<Rect> taken) {
		if (rect.far() > Blueprints.WALL_INNER - 2 || rect.near() < Blueprints.PLAZA + 2) {
			return false;
		}
		int lane = Blueprints.AVENUE + 1;
		if (rect.minX() <= lane && rect.maxX() >= -lane || rect.minZ() <= lane && rect.maxZ() >= -lane) {
			return false;
		}
		if (rect.far() >= Blueprints.RING_INNER - 1 && rect.near() <= Blueprints.RING_OUTER + 1) {
			return false;
		}
		return taken.stream().noneMatch(o -> o.overlaps(rect, 1));
	}

	/** Un arbre de 5 sur 5 dont le tronc est en (dx, dz), s'il y a la place. */
	private static void tree(StructurePiecesBuilder builder, List<Rect> taken, int cx, int y, int cz, int dx, int dz,
			WorldgenRandom random) {
		Rect rect = new Rect(dx - 2, dz - 2, dx + 2, dz + 2);
		if (!free(rect, taken)) {
			return;
		}
		taken.add(rect);
		builder.addPiece(new ProceduralPiece(random.nextInt(3) == 0 ? "birch_tree" : "town_tree",
				new BlockPos(cx + rect.minX(), y, cz + rect.minZ()), 5, 10, 5, 1, Rotation.NONE, random.nextLong()));
	}

	/**
	 * Une maison sur la couronne de rayon r, à l'angle a, sa façade tournée vers le centre
	 * ({@code facing} −1) ou vers l'extérieur (+1), au point cardinal le plus proche.
	 */
	private static void house(StructurePiecesBuilder builder, List<Rect> taken, int cx, int y, int cz, int r, double a,
			int facing, WorldgenRandom random) {
		int sx = 7 + random.nextInt(3), sz = 7 + random.nextInt(2), floors = 1 + random.nextInt(2);
		double fx = Math.cos(a) * facing, fz = Math.sin(a) * facing;
		Rotation rotation = Math.abs(fx) > Math.abs(fz)
				? (fx > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90)
				: (fz > 0 ? Rotation.CLOCKWISE_180 : Rotation.NONE);
		boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
		int px = sx + 2 * Dressing.MARGIN, pz = sz + 2 * Dressing.MARGIN;
		int wx = turned ? pz : px, wz = turned ? px : pz;
		int x0 = (int) Math.round(Math.cos(a) * r) - wx / 2, z0 = (int) Math.round(Math.sin(a) * r) - wz / 2;
		Rect rect = new Rect(x0, z0, x0 + wx - 1, z0 + wz - 1);
		if (!free(rect, taken)) {
			return;
		}
		taken.add(rect);
		builder.addPiece(new ProceduralPiece("central_house_dressed", new BlockPos(cx + x0, y, cz + z0), px,
				Blueprints.houseHeight(floors, sz), pz, 1, rotation, random.nextLong()));
	}

	@Override
	public StructureType<?> type() {
		return FmabStructures.CENTRAL;
	}
}
