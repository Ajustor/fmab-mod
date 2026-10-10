package com.ajustor.fmab.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Un bourg posé sur le relief : une liste d'emplacements autour du centre, chacun bâti à la hauteur
 * du sol en son milieu. Une maison habillée ({@code *_dressed}) prend une marge d'un bloc tout
 * autour de l'emprise donnée, pour ses abords.
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

	static void build(StructurePiecesBuilder builder, Structure.GenerationContext context, BlockPos center,
			Spot[] layout) {
		for (Spot s : layout) {
			int m = s.kind().endsWith("_dressed") ? Dressing.MARGIN : 0;
			boolean turned = s.rotation() == Rotation.CLOCKWISE_90 || s.rotation() == Rotation.COUNTERCLOCKWISE_90;
			int x = center.getX() + s.dx() - m, z = center.getZ() + s.dz() - m;
			int sx = s.sx() + 2 * m, sz = s.sz() + 2 * m;
			int wx = turned ? sz : sx, wz = turned ? sx : sz;
			int y = height(context, x + wx / 2, z + wz / 2);
			int sizeY = s.floors() == 0 ? (s.kind().endsWith("_tree") ? 10 : 5) : Blueprints.houseHeight(s.floors(), s.sz());
			builder.addPiece(new ProceduralPiece(s.kind(), new BlockPos(x, y, z), sx, sizeY, sz, 3, s.rotation(),
					context.random().nextLong()));
		}
	}
}
