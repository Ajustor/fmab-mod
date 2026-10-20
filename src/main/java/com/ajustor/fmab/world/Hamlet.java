package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Un bourg posé sur le relief : une liste d'emplacements autour du centre, chacun bâti à la hauteur
 * du sol en son milieu. Une maison habillée ({@code *_dressed}) prend une marge d'un bloc tout
 * autour de l'emprise donnée, pour ses abords. Un chemin relie chaque porte à la place (ou, sans
 * place, à la porte de la première maison). Le bourg s'étale d'un facteur donné autour de son
 * centre : la place et ses étals restent au milieu, maisons, champs et arbres s'en écartent.
 */
final class Hamlet {
	private Hamlet() {
	}

	/**
	 * Un emplacement : (dx, dz) le coin de la maison elle-même, sa sorte, sa largeur, sa profondeur,
	 * ses étages (0 pour une place, un champ, un arbre) et son orientation.
	 */
	record Spot(int dx, int dz, String kind, int sx, int sz, int floors, Rotation rotation) {
		static Spot tree(int dx, int dz, String kind) {
			return new Spot(dx, dz, kind, 5, 5, 0, Rotation.NONE);
		}
	}

	static int height(Structure.GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
				context.heightAccessor(), context.randomState());
	}

	/**
	 * L'emplacement écarté du centre : son milieu s'éloigne de {@code spread} fois sa distance, la place
	 * et les étals ne bougent pas.
	 */
	static Spot spread(Spot s, double spread) {
		if (s.kind().endsWith("_square") || s.kind().endsWith("_stall")) {
			return s;
		}
		boolean turned = s.rotation() == Rotation.CLOCKWISE_90 || s.rotation() == Rotation.COUNTERCLOCKWISE_90;
		int wx = turned ? s.sz() : s.sx(), wz = turned ? s.sx() : s.sz();
		int dx = (int) Math.round((s.dx() + wx / 2.0) * spread - wx / 2.0);
		int dz = (int) Math.round((s.dz() + wz / 2.0) * spread - wz / 2.0);
		return new Spot(dx, dz, s.kind(), s.sx(), s.sz(), s.floors(), s.rotation());
	}

	static void build(StructurePiecesBuilder builder, Structure.GenerationContext context, BlockPos center,
			Spot[] layout, PathPiece.Style paths, double spread) {
		List<ProceduralPiece> pieces = new ArrayList<>();
		List<BlockPos> doors = new ArrayList<>();
		for (Spot laid : layout) {
			Spot s = spread(laid, spread);
			int m = s.kind().endsWith("_dressed") ? Dressing.MARGIN : 0;
			boolean turned = s.rotation() == Rotation.CLOCKWISE_90 || s.rotation() == Rotation.COUNTERCLOCKWISE_90;
			int x = center.getX() + s.dx() - m, z = center.getZ() + s.dz() - m;
			int sx = s.sx() + 2 * m, sz = s.sz() + 2 * m;
			int wx = turned ? sz : sx, wz = turned ? sx : sz;
			int y = height(context, x + wx / 2, z + wz / 2);
			int sizeY = s.floors() == 0 ? (s.kind().endsWith("_tree") ? 10 : 5) : Blueprints.houseHeight(s.floors(), s.sz());
			ProceduralPiece piece = new ProceduralPiece(s.kind(), new BlockPos(x, y, z), sx, sizeY, sz, 3, s.rotation(),
					context.random().nextLong());
			pieces.add(piece);
			if (m > 0) {
				// Le chemin part un pas devant le seuil, au-delà de la marge où l'habillage pose le sien.
				BlockPos threshold = piece.worldAt(m + s.sx() / 2, 0);
				BlockPos inside = piece.worldAt(m + s.sx() / 2, 1);
				doors.add(threshold.offset(threshold.getX() - inside.getX(), 0, threshold.getZ() - inside.getZ()));
			}
		}
		// Les chemins d'abord : les maisons, les champs et les arbres les recouvrent là où ils passent.
		boolean square = Arrays.stream(layout).anyMatch(s -> s.kind().endsWith("_square"));
		BlockPos hub = square || doors.isEmpty() ? center : doors.getFirst();
		for (BlockPos door : doors) {
			builder.addPiece(new PathPiece(paths, door.getX(), door.getZ(), hub.getX(), hub.getZ(), center.getY()));
		}
		pieces.forEach(builder::addPiece);
	}
}
