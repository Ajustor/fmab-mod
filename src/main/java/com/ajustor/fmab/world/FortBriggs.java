package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Fort Briggs. Au nord (z 0..12), le glacis : on n'y touche pas, c'est là que rôde Drachma. Puis la
 * muraille, épaisse de cinq blocs et haute de vingt, crénelée, deux tours à ses bouts. Côté
 * Drachma, des contreforts la rythment, des meurtrières la percent sur trois rangs, la grande
 * bannière bleue de Briggs y pend, et une porte voûtée, fermée d'une herse, la traverse. Dos au
 * mur, la caserne de trois niveaux (une échelle monte jusqu'au chemin de ronde), ses fenêtres
 * éclairées, son drapeau et sa cheminée ; derrière, la cour d'arrivée, enneigée, ouverte vers
 * Amestris.
 */
final class FortBriggs implements Blueprint {
	static final int SIZE_X = 41;
	static final int SIZE_Y = 27;
	static final int SIZE_Z = 37;
	private static final int WALL_Z0 = 14;
	private static final int WALL_Z1 = 18;
	private static final int WALL_TOP = 20;
	private static final int KEEP_Z1 = 30;
	private static final int KEEP_TOP = 18;
	private static final int TOWER_TOP = 24;
	/** Hors des tours (x ≤ 4), contre la muraille. */
	private static final int LADDER_X = 5;
	private static final int MID = SIZE_X / 2;
	/** La porte : cinq blocs de large, cinq de haut. */
	private static final int GATE_HALF = 2;
	private static final int GATE_TOP = 5;
	private static final int CHIMNEY_X = SIZE_X - 8;

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		if (z < WALL_Z0 - 1) {
			return null;
		}
		boolean tower = (x <= 4 || x >= SIZE_X - 5) && z >= WALL_Z0 - 1 && z <= WALL_Z1 + 1;
		if (tower) {
			return tower(x, y, z);
		}
		if (z < WALL_Z0) {
			return front(x, y);
		}
		if (y < 0) {
			return b(Blocks.STONE_BRICKS);
		}
		if (z <= WALL_Z1) {
			return wall(x, y, z, p);
		}
		if (z <= KEEP_Z1) {
			return keep(x, y, z, p);
		}
		return yard(x, y, z, p);
	}

	/** Ce qui dépasse de la muraille côté Drachma : les contreforts, la bannière. */
	private static BlockState front(int x, int y) {
		boolean gate = Math.abs(x - MID) <= GATE_HALF + 1;
		if (x % 8 == 0 && !gate && y <= WALL_TOP - 2) {
			return y < 0 ? b(Blocks.STONE_BRICKS) : y == WALL_TOP - 2 ? stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH)
					: b(Blocks.STONE_BRICKS);
		}
		// La bannière de Briggs : du bleu, un liseré blanc, l'aigle d'or au milieu.
		if (Math.abs(x - MID) <= 1 && y >= 9 && y <= 17) {
			if (y == 9) {
				return x == MID ? b(Blocks.WOOL.pick(DyeColor.BLUE)) : null;
			}
			if (x == MID && (y == 14 || y == 15)) {
				return b(Blocks.GOLD_BLOCK);
			}
			return b(Blocks.WOOL.pick(x == MID && y == 12 ? DyeColor.WHITE : DyeColor.BLUE));
		}
		return null;
	}

	private static BlockState tower(int x, int y, int z) {
		if (y < 0) {
			return b(Blocks.STONE_BRICKS);
		}
		boolean rim = x == 0 || x == 4 || x == SIZE_X - 5 || x == SIZE_X - 1 || z == WALL_Z0 - 1 || z == WALL_Z1 + 1;
		boolean corner = (x == 0 || x == 4 || x == SIZE_X - 5 || x == SIZE_X - 1) && (z == WALL_Z0 - 1 || z == WALL_Z1 + 1);
		if (y <= TOWER_TOP) {
			// Des fenêtres sur les faces extérieures, une tous les cinq niveaux.
			boolean outer = x == 0 || x == SIZE_X - 1 || z == WALL_Z0 - 1;
			boolean middle = z == WALL_Z0 - 1 ? x == 2 || x == SIZE_X - 3 : z == WALL_Z0 + 2;
			if (outer && middle && y % 5 == 3 && y < TOWER_TOP - 1) {
				return b(Blocks.POLISHED_BLACKSTONE);
			}
			if (y == TOWER_TOP - 1 && rim) {
				return b(Blocks.CHISELED_STONE_BRICKS);
			}
			return y == TOWER_TOP ? b(FmabBlocks.BRIGGS_PACKED_SNOW) : b(Blocks.STONE_BRICKS);
		}
		if (y == TOWER_TOP + 1) {
			return rim && (x + z) % 2 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.SNOW);
		}
		// Les projecteurs, aux coins des tours.
		if (y == TOWER_TOP + 2 && corner) {
			return b(Blocks.LANTERN);
		}
		return AIR;
	}

	private static BlockState wall(int x, int y, int z, Plot p) {
		boolean gate = Math.abs(x - MID) <= GATE_HALF && y >= 1 && y <= GATE_TOP;
		if (gate) {
			// La voûte : les coins du haut restent pleins ; la herse ferme le passage côté nord.
			if (y == GATE_TOP && Math.abs(x - MID) == GATE_HALF) {
				return b(Blocks.STONE_BRICKS);
			}
			if (z == WALL_Z0 + 1) {
				return b(Blocks.IRON_BARS).setValue(CrossCollisionBlock.EAST, x < MID + GATE_HALF)
						.setValue(CrossCollisionBlock.WEST, x > MID - GATE_HALF);
			}
			return AIR;
		}
		if (y <= WALL_TOP) {
			if (y == WALL_TOP) {
				return b(FmabBlocks.BRIGGS_PACKED_SNOW);
			}
			if (z == WALL_Z0) {
				// Le parement nord : l'arc de la porte, deux bandeaux, trois rangs de meurtrières.
				if (y == GATE_TOP + 1 && Math.abs(x - MID) <= GATE_HALF + 1) {
					return b(Blocks.CHISELED_STONE_BRICKS);
				}
				if (y == 8 || y == 14) {
					return b(Blocks.SMOOTH_STONE);
				}
				if ((y == 11 || y == 17 || y == 5 && Math.abs(x - MID) > GATE_HALF + 3) && x % 4 == 2) {
					return b(Blocks.POLISHED_BLACKSTONE);
				}
			}
			return p.noise(x, y, z, 9) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
		}
		if (y == WALL_TOP + 1 && z == WALL_Z0 && x % 2 == 0) {
			return b(Blocks.STONE_BRICK_WALL);
		}
		if (y == WALL_TOP + 2 && z == WALL_Z0 && x % 2 == 0) {
			return b(Blocks.SNOW);
		}
		if (y == WALL_TOP + 1 && z == WALL_Z0 + 2 && x % 8 == 4) {
			// Les canons de Briggs, pointés vers le nord.
			return b(Blocks.BLACKSTONE);
		}
		if (y == WALL_TOP + 1 && z == WALL_Z0 + 1 && x % 8 == 4) {
			return b(Blocks.POLISHED_BLACKSTONE_WALL);
		}
		if (y == WALL_TOP + 2 && z == WALL_Z0 + 2 && x % 8 == 4) {
			return b(Blocks.LANTERN);
		}
		if (y == WALL_TOP + 1 && z > WALL_Z0 + 2 && p.noise(x, y, z, 3) == 0) {
			return b(Blocks.SNOW);
		}
		return AIR;
	}

	private static BlockState keep(int x, int y, int z, Plot p) {
		if (x <= 1 || x >= SIZE_X - 2) {
			return y <= KEEP_TOP ? b(Blocks.STONE_BRICKS) : y == KEEP_TOP + 1 ? b(Blocks.SNOW) : AIR;
		}
		if (y > KEEP_TOP) {
			return roof(x, y, z, p);
		}
		if (x == LADDER_X && z == WALL_Z1 + 1 && y > 0) {
			return ladder();
		}
		// Le passage de la porte continue jusqu'à la cour, sous la caserne.
		if (Math.abs(x - MID) <= GATE_HALF && y >= 1 && y < GATE_TOP && z <= WALL_Z1 + 2) {
			return AIR;
		}
		boolean shell = x == 2 || x == SIZE_X - 3 || z == KEEP_Z1;
		if (y == 0) {
			return b(Blocks.POLISHED_ANDESITE);
		}
		if (y == KEEP_TOP) {
			return b(Blocks.SMOOTH_STONE);
		}
		if (shell) {
			if (z == KEEP_Z1 && Math.abs(x - MID) <= 1 && y <= 3) {
				return AIR;
			}
			// La façade sur la cour : un bandeau à chaque étage, des fenêtres de deux blocs.
			if (z == KEEP_Z1 && (y == 6 || y == 12)) {
				return b(Blocks.SMOOTH_STONE);
			}
			if (z == KEEP_Z1 && y % 6 >= 2 && y % 6 <= 4 && x % 5 <= 1 && Math.abs(x - MID) > 2) {
				return b(Blocks.GLASS_PANE).setValue(CrossCollisionBlock.EAST, x % 5 == 0)
						.setValue(CrossCollisionBlock.WEST, x % 5 == 1);
			}
			if (x == CHIMNEY_X && z == KEEP_Z1 - 1) {
				return b(Blocks.BRICKS);
			}
			return p.noise(x, y, z, 11) == 0 ? b(Blocks.MOSSY_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
		}
		if (x == CHIMNEY_X && z == KEEP_Z1 - 1) {
			return b(Blocks.BRICKS);
		}
		if (y == 6 || y == 12) {
			return b(Blocks.SPRUCE_PLANKS);
		}
		// Derrière chaque fenêtre, une lanterne : la caserne veille la nuit.
		if ((y == 2 || y == 8 || y == 14) && z == KEEP_Z1 - 1 && x % 5 == 0 && Math.abs(x - MID) > 2) {
			return b(Blocks.LANTERN);
		}
		if (y == 1 && z == WALL_Z1 + 4 && x == MID + 5) {
			return b(Blocks.CAMPFIRE);
		}
		if (y == 1 && z == KEEP_Z1 - 2 && (x == 4 || x == SIZE_X - 5)) {
			return b(Blocks.BARREL);
		}
		return AIR;
	}

	/** Le toit de la caserne : un parapet enneigé, la cheminée, le drapeau de Briggs. */
	private static BlockState roof(int x, int y, int z, Plot p) {
		if (x == LADDER_X && z == WALL_Z1 + 1 && y <= WALL_TOP) {
			// L'échelle continue jusqu'au chemin de ronde, contre la muraille.
			return ladder();
		}
		boolean edge = x == 2 || x == SIZE_X - 3 || z == KEEP_Z1;
		if (y == KEEP_TOP + 1) {
			if (edge) {
				return (x + z) % 2 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.STONE_BRICKS);
			}
			if (x == CHIMNEY_X && z == KEEP_Z1 - 1) {
				return b(Blocks.BRICKS);
			}
			return p.noise(x, y, z, 2) == 0 ? b(Blocks.SNOW) : AIR;
		}
		if (x == CHIMNEY_X && z == KEEP_Z1 - 1 && y <= KEEP_TOP + 3) {
			return y == KEEP_TOP + 3 ? b(Blocks.CAMPFIRE) : b(Blocks.BRICKS);
		}
		// Le mât, et le drapeau bleu qui claque vers l'est.
		if (x == MID && z == KEEP_Z1 - 1 && y <= KEEP_TOP + 7) {
			return b(Blocks.IRON_BARS);
		}
		if (z == KEEP_Z1 - 1 && x > MID && x <= MID + 3 && (y == KEEP_TOP + 6 || y == KEEP_TOP + 7)) {
			return b(Blocks.WOOL.pick(x == MID + 2 && y == KEEP_TOP + 6 ? DyeColor.WHITE : DyeColor.BLUE));
		}
		return AIR;
	}

	private static BlockState yard(int x, int y, int z, Plot p) {
		if (y < 0) {
			return null;
		}
		if (y == 0) {
			return Math.abs(x - MID) <= 1 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS);
		}
		if ((x == 0 || x == SIZE_X - 1) && y <= 2) {
			return y == 2 && z % 3 == 0 ? b(Blocks.LANTERN) : b(Blocks.COBBLESTONE_WALL);
		}
		// L'auvent au-dessus de la porte, et ses deux torches.
		if (z == KEEP_Z1 + 1 && Math.abs(x - MID) <= 2 && y == 4) {
			return b(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
		}
		if (z == KEEP_Z1 + 1 && Math.abs(x - MID) == 2 && y == 3) {
			return b(Blocks.WALL_TORCH).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		}
		// Les réserves déchargées dans la cour : caisses, sacs de sable empilés.
		if (y == 1 && z == KEEP_Z1 + 3 && (x == 6 || x == 7 || x == SIZE_X - 8)) {
			return b(Blocks.BARREL);
		}
		if (y == 2 && z == KEEP_Z1 + 3 && x == 6) {
			return b(Blocks.BARREL);
		}
		if (y == 1 && z == KEEP_Z1 + 4 && x >= SIZE_X - 12 && x <= SIZE_X - 9) {
			return b(Blocks.SANDSTONE_SLAB).setValue(SlabBlock.TYPE, x % 2 == 0 ? SlabType.DOUBLE : SlabType.BOTTOM);
		}
		if (y == 1 && Math.abs(x - MID) > 1 && p.noise(x, y, z, 3) == 0) {
			return b(Blocks.SNOW);
		}
		return y <= 6 ? AIR : null;
	}

	private static BlockState ladder() {
		// Tournée vers le sud : elle s'appuie sur la muraille, au nord.
		return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(
				Chest.loot(new BlockPos(6, 1, KEEP_Z1 - 2), "fort_briggs_armory"),
				Chest.loot(new BlockPos(SIZE_X - 7, 1, KEEP_Z1 - 2), "fort_briggs_armory"),
				Chest.loot(new BlockPos(MID, 7, KEEP_Z1 - 2), "fort_briggs_stores"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(FmabEntities.OLIVIER, new BlockPos(MID, 1, WALL_Z1 + 6)),
				new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(8, 1, WALL_Z1 + 4)),
				new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(SIZE_X - 9, 1, WALL_Z1 + 4)),
				new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(MID, 7, WALL_Z1 + 5)),
				new Spawn(FmabEntities.BRIGGS_SOLDIER, new BlockPos(MID - 6, 1, KEEP_Z1 + 3)));
	}
}
