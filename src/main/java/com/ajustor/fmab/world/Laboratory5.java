package com.ajustor.fmab.world;

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
 * Le Laboratoire 5 : en surface, un complexe de l'armée laissé à l'abandon. Une enceinte coiffée
 * de barbelés, sa porte entre deux piliers, une guérite ; dans la cour, le grand bâtiment de
 * briques à étage, vitres brisées, toiture crevée, et dedans, derrière les toiles d'araignée, le
 * puits à échelle qui descend dans la grande salle souterraine. Des cellules le long du mur du
 * fond, un cercle tracé au sang au milieu du sol, des coffres de recherche, et ses gardiens : Lust
 * et Gluttony.
 *
 * <p>Coordonnées : la façade du bâtiment regarde l'entrée (z 0) ; il occupe x 4..20, z 7..17, du
 * sol (y 0) au toit (y 10). Le puits descend en (5, z 14) ; la salle couvre toute la parcelle, sol
 * en y = −18, plafond en y = −12.
 */
final class Laboratory5 implements Blueprint {
	static final int HALL_FLOOR = -18;
	static final int HALL_CEILING = -12;
	/** Jusqu'où montent le bâtiment et sa cheminée au-dessus du sol. */
	static final int SURFACE_TOP = 12;
	private static final int SHAFT_X = 5;
	private static final int SHAFT_Z = 14;
	private static final int B_X0 = 4;
	private static final int B_X1 = 20;
	private static final int B_Z0 = 7;
	private static final int B_Z1 = 17;
	private static final int UPPER = 5;
	private static final int ROOF = 10;
	private static final int CHIMNEY_X = 18;
	private static final int CHIMNEY_Z = 15;
	private static final int DOOR_X = 12;

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		if (y >= HALL_FLOOR && y <= HALL_CEILING) {
			return hall(x, y, z, p);
		}
		if (y > HALL_CEILING && y < -1) {
			return shaft(x, y, z);
		}
		return surface(x, y, z, p);
	}

	private BlockState hall(int x, int y, int z, Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean wall = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		if (y == HALL_FLOOR) {
			double d = Math.hypot(x - sx / 2.0 + 0.5, z - 9);
			if (Math.abs(d - 5) < 0.6 || (d < 5 && (x == sx / 2 || z == 9))) {
				return b(Blocks.CONCRETE.pick(DyeColor.RED));
			}
			return (x + z) % 2 == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.STONE_BRICKS);
		}
		if (y == HALL_CEILING) {
			return x == SHAFT_X && z == SHAFT_Z ? ladder() : b(Blocks.STONE_BRICKS);
		}
		if (wall) {
			return p.noise(x, y, z, 7) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
		}
		// Le puits continue jusqu'au sol, son échelle contre un pilier.
		if (x == SHAFT_X && z == SHAFT_Z) {
			return ladder();
		}
		if (x == SHAFT_X && z == SHAFT_Z + 1) {
			return b(Blocks.STONE_BRICKS);
		}
		// Les cellules : des barreaux, des murs tous les cinq blocs.
		if (z == sz - 5) {
			return x % 5 == 0 ? b(Blocks.STONE_BRICKS) : y < HALL_CEILING - 1 ? ironBars(x, sx) : b(Blocks.STONE_BRICKS);
		}
		if (z > sz - 5 && x % 5 == 0) {
			return b(Blocks.STONE_BRICKS);
		}
		// Lanternes d'âmes aux coins, chaînes qui pendent du plafond.
		if (y == HALL_FLOOR + 1 && (x == 1 || x == sx - 2) && (z == 1 || z == sz - 6)) {
			return b(Blocks.SOUL_LANTERN);
		}
		if (y == HALL_CEILING - 1 && x % 6 == 3 && z % 6 == 3) {
			return b(Blocks.IRON_CHAIN);
		}
		return AIR;
	}

	private static BlockState ironBars(int x, int sx) {
		return b(Blocks.IRON_BARS).setValue(CrossCollisionBlock.WEST, x % 5 != 1).setValue(CrossCollisionBlock.EAST,
				x % 5 != 4);
	}

	private BlockState shaft(int x, int y, int z) {
		if (Math.abs(x - SHAFT_X) > 1 || Math.abs(z - SHAFT_Z) > 1) {
			return null;
		}
		if (x == SHAFT_X && z == SHAFT_Z) {
			return ladder();
		}
		return b(Blocks.STONE_BRICKS);
	}

	private static BlockState ladder() {
		// Tournée vers le nord : elle s'appuie sur le bloc au sud.
		return b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
	}

	/** La surface : le sol de la cour, l'enceinte, le bâtiment. */
	private BlockState surface(int x, int y, int z, Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		if (y > SURFACE_TOP) {
			return null;
		}
		boolean inside = x >= B_X0 && x <= B_X1 && z >= B_Z0 && z <= B_Z1;
		if (y == -1) {
			if (x == SHAFT_X && z == SHAFT_Z) {
				return ladder();
			}
			if (inside) {
				return p.noise(x, y, z, 5) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			}
			// La cour : du gravier et des pavés, l'allée de l'entrée jusqu'à la porte.
			if (Math.abs(x - DOOR_X) <= 1 && z < B_Z0) {
				return b(Blocks.COBBLESTONE);
			}
			return p.noise(x, y, z, 3) == 0 ? b(Blocks.GRAVEL) : p.noise(x, y, z, 4) == 0 ? b(Blocks.COARSE_DIRT) : null;
		}
		boolean rim = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		if (rim) {
			return enclosure(x, y, z, sx, p);
		}
		if (inside) {
			return building(x, y, z, p);
		}
		return yard(x, y, z, p);
	}

	/** L'enceinte : un mur de pierre usé, des barbelés dessus, la porte entre deux piliers. */
	private static BlockState enclosure(int x, int y, int z, int sx, Plot p) {
		boolean pillar = z == 0 && (x == DOOR_X - 2 || x == DOOR_X + 2);
		if (pillar) {
			return y <= 3 ? b(Blocks.CHISELED_STONE_BRICKS) : y == 4 ? b(Blocks.LANTERN) : null;
		}
		if (z == 0 && Math.abs(x - DOOR_X) <= 1) {
			return y <= 3 ? AIR : null;
		}
		if (y <= 2) {
			if (y == 2 && p.noise(x, y, z, 5) == 0) {
				return AIR;
			}
			return p.noise(x, y, z, 4) == 0 ? b(Blocks.MOSSY_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
		}
		if (y == 3) {
			return p.noise(x, y, z, 3) == 0 ? AIR : b(Blocks.IRON_BARS);
		}
		return null;
	}

	/** Le grand bâtiment : briques, chaînages de pierre, deux niveaux, l'abandon. */
	private BlockState building(int x, int y, int z, Plot p) {
		boolean shellX = x == B_X0 || x == B_X1;
		boolean shellZ = z == B_Z0 || z == B_Z1;
		boolean corner = shellX && shellZ;
		if (x == CHIMNEY_X && z == CHIMNEY_Z) {
			return y == SURFACE_TOP ? b(Blocks.CAMPFIRE) : b(Blocks.BRICKS);
		}
		if (y > ROOF + 1) {
			return AIR;
		}
		// Le toit, crevé par endroits ; le parapet tout autour.
		if (y == ROOF + 1) {
			return (shellX || shellZ) && (x + z) % 2 == 0 && p.noise(x, y, z, 3) != 0 ? b(Blocks.STONE_BRICK_WALL) : AIR;
		}
		if (y == ROOF) {
			return p.noise(x, y, z, 6) == 0 && !shellX && !shellZ ? AIR : b(Blocks.STONE_BRICKS);
		}
		if (shellX || shellZ) {
			return facade(x, y, z, corner, shellZ, p);
		}
		// L'étage : un plancher qui a cédé ici et là.
		if (y == UPPER) {
			return p.noise(x, y, z, 5) == 0 ? AIR : b(Blocks.SPRUCE_PLANKS);
		}
		// Le puits, et le pilier contre lequel son échelle s'appuie.
		if (x == SHAFT_X && z == SHAFT_Z && y == 0) {
			return ladder();
		}
		if (x == SHAFT_X && z == SHAFT_Z + 1 && y <= 1) {
			return b(Blocks.STONE_BRICKS);
		}
		// Ce qui reste : des tonneaux renversés, un bureau, des étagères, des toiles d'araignée.
		if (y == 0 && z == B_Z1 - 1 && x >= 14 && x <= 16) {
			return b(Blocks.BOOKSHELF);
		}
		if (y == 0 && x == B_X1 - 2 && (z == B_Z0 + 2 || z == B_Z0 + 3)) {
			return b(Blocks.BARREL).setValue(BlockStateProperties.FACING, z == B_Z0 + 2 ? Direction.UP : Direction.EAST);
		}
		if (y == 0 && x == 9 && z == 12) {
			return b(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
		}
		if ((y == UPPER - 1 || y == ROOF - 1) && p.noise(x, y, z, 9) == 0) {
			return b(Blocks.COBWEB);
		}
		return AIR;
	}

	/** Les murs du bâtiment : fenêtres à barreaux ou brisées, la porte, des briques qui manquent. */
	private static BlockState facade(int x, int y, int z, boolean corner, boolean front, Plot p) {
		if (corner || y == 0 || y == UPPER) {
			return b(Blocks.STONE_BRICKS);
		}
		// La porte, sous un linteau de pierre.
		if (z == B_Z0 && Math.abs(x - DOOR_X) <= 1 && y <= 3) {
			return y == 3 ? b(Blocks.CHISELED_STONE_BRICKS) : AIR;
		}
		int along = front ? x : z;
		boolean window = along % 3 == 0 && (y == 2 || y == 3 || y == 7 || y == 8);
		if (window) {
			int state = p.noise(x, y, z, 4);
			return state == 0 ? AIR : state == 1 ? b(Blocks.IRON_BARS) : b(Blocks.GLASS_PANE);
		}
		// Plus haut, des briques ont lâché.
		if (y >= ROOF - 2 && p.noise(x, y, z, 7) == 0) {
			return AIR;
		}
		return p.noise(x, y, z, 6) == 0 ? b(Blocks.MUD_BRICKS) : b(Blocks.BRICKS);
	}

	/** La cour : la guérite près de la porte, des caisses, les drapeaux de l'armée sur la façade. */
	private static BlockState yard(int x, int y, int z, Plot p) {
		// La guérite : quatre poteaux, un toit de dalles.
		if (x >= 16 && x <= 18 && z >= 1 && z <= 3) {
			boolean post = (x == 16 || x == 18) && (z == 1 || z == 3);
			if (y <= 2 && post) {
				return b(Blocks.SPRUCE_FENCE);
			}
			if (y == 3) {
				return b(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
			}
			return y <= 3 ? AIR : null;
		}
		// Les drapeaux d'Amestris, de part et d'autre de la porte.
		if (z == B_Z0 - 1 && (x == DOOR_X - 4 || x == DOOR_X + 4) && y >= 4 && y <= 8) {
			return b(Blocks.WOOL.pick(y == 6 ? DyeColor.YELLOW : DyeColor.BLUE));
		}
		if (y == 0 && ((x == 2 && (z == 3 || z == 4)) || (x == 21 && z == 18))) {
			return b(Blocks.BARREL);
		}
		if (y == 1 && x == 2 && z == 3) {
			return b(Blocks.BARREL);
		}
		return y <= SURFACE_TOP ? AIR : null;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(
				Chest.loot(new BlockPos(2, HALL_FLOOR + 1, 2), "laboratory_5_research"),
				Chest.loot(new BlockPos(p.sizeX() - 3, HALL_FLOOR + 1, 2), "laboratory_5_archives"));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		return List.of(
				// Barry le Boucher rôde dans le bâtiment abandonné, près du puits.
				new Spawn(FmabEntities.BARRY, new BlockPos(10, 0, 11)),
				new Spawn(FmabEntities.LUST, new BlockPos(sx / 2 + 3, HALL_FLOOR + 1, 12)),
				// Les frères Slicer gardent le hall, Numéro 48.
				new Spawn(FmabEntities.SLICER, new BlockPos(sx / 2 - 3, HALL_FLOOR + 1, 12)),
				new Spawn(FmabEntities.GLUTTONY, new BlockPos(sx / 2 - 4, HALL_FLOOR + 1, 10)),
				new Spawn(FmabEntities.HAUNTED_ARMOR, new BlockPos(4, HALL_FLOOR + 1, 6)),
				new Spawn(FmabEntities.HAUNTED_ARMOR, new BlockPos(sx - 5, HALL_FLOOR + 1, 6)),
				new Spawn(FmabEntities.IMMORTAL_SOLDIER, new BlockPos(sx / 2, HALL_FLOOR + 1, 7)),
				// Dans les cellules, les essais ratés de l'armée.
				new Spawn(FmabEntities.CHIMERA_CRAWLER, new BlockPos(2, HALL_FLOOR + 1, sz - 3)),
				// Au milieu d'une cellule : la bête est large.
				new Spawn(FmabEntities.CHIMERA_BEAST, new BlockPos(12, HALL_FLOOR + 1, sz - 3)),
				new Spawn(FmabEntities.CHIMERA_CRAWLER, new BlockPos(sx - 3, HALL_FLOOR + 1, sz - 3)));
	}
}
