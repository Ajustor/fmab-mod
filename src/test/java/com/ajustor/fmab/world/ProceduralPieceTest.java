package com.ajustor.fmab.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les rotations des pièces procédurales : une erreur d'indice casserait toutes les structures
 * tournées. Plans volontairement non carrés, pour que largeur et profondeur ne se confondent pas.
 */
class ProceduralPieceTest {
	private static final int[][] SIZES = {{5, 3}, {3, 5}, {7, 7}, {1, 4}};

	@Test
	void localThenOffsetComesBackToTheSamePlace() {
		for (Rotation rotation : Rotation.values()) {
			for (int[] size : SIZES) {
				int sx = size[0], sz = size[1];
				for (int lx = 0; lx < sx; lx++) {
					for (int lz = 0; lz < sz; lz++) {
						int[] d = ProceduralPiece.offset(rotation, sx, sz, lx, lz);
						assertArrayEquals(new int[]{lx, lz}, ProceduralPiece.local(rotation, sx, sz, d[0], d[1]),
								rotation + " " + sx + "×" + sz + " en (" + lx + ", " + lz + ")");
					}
				}
			}
		}
	}

	@Test
	void everyLocalPositionLandsInsideTheTurnedBox() {
		for (Rotation rotation : Rotation.values()) {
			boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
			for (int[] size : SIZES) {
				int sx = size[0], sz = size[1];
				int bx = turned ? sz : sx, bz = turned ? sx : sz;
				boolean[][] seen = new boolean[bx][bz];
				for (int lx = 0; lx < sx; lx++) {
					for (int lz = 0; lz < sz; lz++) {
						int[] d = ProceduralPiece.offset(rotation, sx, sz, lx, lz);
						assertTrue(d[0] >= 0 && d[0] < bx && d[1] >= 0 && d[1] < bz,
								rotation + " " + sx + "×" + sz + " : (" + d[0] + ", " + d[1] + ") hors de la boîte");
						seen[d[0]][d[1]] = true;
					}
				}
				// Et chaque case de la boîte vient d'une seule case du plan : rien ne se chevauche.
				for (boolean[] column : seen) {
					for (boolean cell : column) {
						assertTrue(cell, rotation + " " + sx + "×" + sz + " : une case de la boîte reste vide");
					}
				}
			}
		}
	}

	/** La façade d'un plan (z = 0) regarde là où la rotation tourne le nord : portes et escaliers suivent. */
	@Test
	void theFrontFacesWhereTheBlockStatesTurn() {
		int sx = 7, sz = 5;
		for (Rotation rotation : Rotation.values()) {
			int[] front = ProceduralPiece.offset(rotation, sx, sz, sx / 2, 0);
			int[] back = ProceduralPiece.offset(rotation, sx, sz, sx / 2, sz - 1);
			Direction facing = rotation.rotate(Direction.NORTH);
			assertEquals(Integer.signum(front[0] - back[0]), facing.getStepX(), rotation + " en x");
			assertEquals(Integer.signum(front[1] - back[1]), facing.getStepZ(), rotation + " en z");
		}
	}
}
