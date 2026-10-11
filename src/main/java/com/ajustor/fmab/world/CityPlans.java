package com.ajustor.fmab.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Central, la ville circulaire : le terrassement, la muraille et ses tours, la voirie, la fontaine,
 * la bibliothèque. Des plans écrits en fonctions, posés par {@link CentralStructure}.
 */
final class CityPlans {
	private CityPlans() {
	}

	static double radius(int x, int z, Blueprint.Plot p) {
		return Math.hypot(x - p.sizeX() / 2, z - p.sizeZ() / 2);
	}

	/** Le plateau de la ville : on dégage au-dessus du sol, on comble dessous. */
	static BlockState cityGround(int x, int y, int z, Blueprint.Plot p) {
		double r = radius(x, z, p);
		if (r > CITY_RADIUS) {
			return null;
		}
		if (y >= 0) {
			return AIR;
		}
		if (y == -1) {
			return b(Blocks.GRASS_BLOCK);
		}
		return y >= -3 ? b(Blocks.DIRT) : b(Blocks.STONE);
	}

	/** Muraille circulaire à créneaux, quatre portes sur les avenues, huit tours. */
	static BlockState cityWalls(int x, int y, int z, Blueprint.Plot p) {
		int dx = x - p.sizeX() / 2, dz = z - p.sizeZ() / 2;
		double r = Math.hypot(dx, dz);
		if (y < 0 || r > CITY_RADIUS) {
			return null;
		}
		boolean gate = Math.abs(dx) <= AVENUE || Math.abs(dz) <= AVENUE;
		// Tours entre les portes.
		for (int k = 0; k < 8; k++) {
			double a = Math.toRadians(22.5 + 45 * k);
			double tx = Math.cos(a) * 69.5, tz = Math.sin(a) * 69.5;
			double d = Math.hypot(dx - tx, dz - tz);
			if (d <= 4) {
				if (y < TOWER_HEIGHT) {
					return d >= 3 ? stone(x, y, z, p) : (y == 0 || y == TOWER_HEIGHT - 4 ? b(Blocks.STONE_BRICKS) : AIR);
				}
				if (y == TOWER_HEIGHT && d >= 3 && (x + z) % 2 == 0) {
					return b(Blocks.STONE_BRICKS);
				}
				return null;
			}
		}
		if (r < WALL_INNER || r >= WALL_OUTER) {
			return null;
		}
		if (gate && y < 6) {
			return AIR;
		}
		if (y < WALL_HEIGHT) {
			return stone(x, y, z, p);
		}
		if (y == WALL_HEIGHT && r >= WALL_OUTER - 1 && Math.floorMod(Math.round(Math.atan2(dz, dx) * 40), 2) == 0) {
			return b(Blocks.STONE_BRICKS);
		}
		return null;
	}

	/** Pierre de taille, un peu moussue et fissurée par endroits. */
	static BlockState stone(int x, int y, int z, Blueprint.Plot p) {
		return switch (p.noise(x, y, z, 12)) {
			case 0 -> b(Blocks.MOSSY_STONE_BRICKS);
			case 1 -> b(Blocks.CRACKED_STONE_BRICKS);
			default -> b(Blocks.STONE_BRICKS);
		};
	}

	/** Avenues en croix, boulevard circulaire, place centrale et réverbères. */
	static BlockState cityRoads(int x, int y, int z, Blueprint.Plot p) {
		int dx = x - p.sizeX() / 2, dz = z - p.sizeZ() / 2;
		double r = Math.hypot(dx, dz);
		if (r >= WALL_INNER) {
			return null;
		}
		int adx = Math.abs(dx), adz = Math.abs(dz);
		boolean avenue = adx <= AVENUE || adz <= AVENUE;
		boolean ring = r >= RING_INNER && r <= RING_OUTER;
		boolean plaza = r <= PLAZA;
		if (y == -1) {
			if (plaza) {
				return (dx + dz) % 4 == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
			}
			if (avenue || ring) {
				boolean edge = adx == AVENUE || adz == AVENUE;
				return edge && !ring ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS);
			}
			return null;
		}
		// Réverbères le long des avenues, tous les dix blocs.
		boolean lampX = adx == AVENUE + 1 && adz > PLAZA + 2 && adz % 10 == 0;
		boolean lampZ = adz == AVENUE + 1 && adx > PLAZA + 2 && adx % 10 == 0;
		if ((lampX || lampZ) && !ring && r < RING_INNER - 1 || (lampX || lampZ) && r > RING_OUTER + 1 && r < WALL_INNER - 2) {
			if (y <= 1) {
				return b(Blocks.STONE_BRICK_WALL);
			}
			if (y == 2) {
				return b(Blocks.LANTERN);
			}
		}
		return null;
	}

	/** Fontaine de la place centrale. */
	static BlockState fountain(int x, int y, int z, Blueprint.Plot p) {
		double r = Math.hypot(x - 4, z - 4);
		if (r <= 0.5) {
			return y <= 2 ? b(Blocks.CHISELED_STONE_BRICKS) : y == 3 ? b(Blocks.LANTERN) : null;
		}
		if (y == 0 && r >= 3 && r <= 4.3) {
			return b(Blocks.STONE_BRICKS);
		}
		if (y == 0 && r < 3) {
			return b(Blocks.WATER);
		}
		if (y == -1 && r <= 4.3) {
			return b(Blocks.STONE_BRICKS);
		}
		return null;
	}

	/** Bibliothèque : murs de brique, hautes fenêtres, rayonnages en rangées. */
	static BlockState library(int x, int y, int z, Blueprint.Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		int h = 9;
		if (y == -1) {
			return b(Blocks.POLISHED_ANDESITE);
		}
		boolean wall = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		boolean corner = (x == 0 || x == sx - 1) && (z == 0 || z == sz - 1);
		if (y >= h) {
			return y == h && wall ? b(Blocks.STONE_BRICK_SLAB) : null;
		}
		if (y == h - 1) {
			return b(Blocks.STONE_BRICKS);
		}
		if (wall) {
			if (corner) {
				return b(Blocks.STONE_BRICKS);
			}
			if (z == 0 && Math.abs(x - sx / 2) <= 1 && y <= 2) {
				return x == sx / 2 && y <= 1 ? door(Blocks.DARK_OAK_DOOR, y, Direction.NORTH) : AIR;
			}
			boolean window = y >= 2 && y <= 5 && (x + z) % 3 == 0;
			return window ? b(Blocks.GLASS_PANE) : b(Blocks.BRICKS);
		}
		// Intérieur : rayonnages sur deux hauteurs, allée centrale.
		if (y <= 2 && z >= 3 && z <= sz - 3 && x % 3 == 1 && Math.abs(x - sx / 2) > 1) {
			return b(Blocks.BOOKSHELF);
		}
		if (y == 0 && z == sz - 3 && x == sx / 2) {
			return b(Blocks.LECTERN);
		}
		if (y == h - 2 && x % 5 == 2 && z % 5 == 2) {
			return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
		}
		return AIR;
	}
}
