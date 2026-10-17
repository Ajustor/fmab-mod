package com.ajustor.fmab.world;

import com.ajustor.fmab.entity.character.StoryCharacters;
import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;
import static com.ajustor.fmab.world.CityPlans.*;

/**
 * Quartier général de l'armée : un bâtiment carré autour d'une cour à ciel ouvert, l'arène de
 * l'examen d'État. L'examinateur s'y tient.
 */
final class Headquarters implements Blueprint {
	private static final int H = 12;

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean courtyard = x >= 6 && x <= sx - 7 && z >= 7 && z <= sz - 7;
		if (y == -1) {
			return courtyard ? b(Blocks.SAND) : b(Blocks.POLISHED_ANDESITE);
		}
		if (y > H) {
			return null;
		}
		boolean outer = x == 0 || x == sx - 1 || z == 0 || z == sz - 1;
		boolean inner = (x == 5 || x == sx - 6) && z >= 6 && z <= sz - 6 || (z == 6 || z == sz - 6) && x >= 5 && x <= sx - 6;
		if (courtyard) {
			return y == 0 && (x == 6 || x == sx - 7 || z == 7 || z == sz - 7) ? b(Blocks.STONE_BRICK_SLAB) : AIR;
		}
		if (y == H) {
			return outer ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.STONE_BRICK_SLAB);
		}
		if (y == H - 1 || y == 5) {
			return outer || inner || y == H - 1 ? b(Blocks.STONE_BRICKS) : b(Blocks.SPRUCE_PLANKS);
		}
		// Portail : au milieu de la façade et vers la cour.
		if ((z == 0 || z == 6) && Math.abs(x - sx / 2) <= 1 && y <= 3) {
			return AIR;
		}
		if (outer) {
			// Étendards bleus de l'armée de part et d'autre du portail.
			if (z == 0 && Math.abs(x - sx / 2) == 3 && y >= 6 && y <= 9) {
				return b(Blocks.WOOL.pick(DyeColor.BLUE));
			}
			boolean window = (y == 2 || y == 3 || y == 7 || y == 8) && x % 3 == 1 && z == 0
					|| (y == 2 || y == 3 || y == 7 || y == 8) && z % 3 == 1 && (x == 0 || x == sx - 1);
			return window ? b(Blocks.GLASS_PANE) : stone(x, y, z, p);
		}
		if (inner) {
			return (y <= 3 && (x + z) % 4 == 0) ? AIR : b(Blocks.STONE_BRICKS);
		}
		if (y == 4 && x % 6 == 3 && z % 6 == 3) {
			return b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
		}
		return AIR;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		// L'examinateur attend dans la cour, face au portail ; la garde se tient au portail et dans la cour.
		int sx = p.sizeX(), sz = p.sizeZ();
		return List.of(new Spawn(FmabEntities.STATE_EXAMINER, new BlockPos(sx / 2, 0, sz - 8)),
				new Spawn(FmabEntities.character(StoryCharacters.MUSTANG), new BlockPos(sx / 2 - 3, 0, sz - 6)),
				new Spawn(FmabEntities.character(StoryCharacters.HAWKEYE), new BlockPos(sx / 2 - 4, 0, sz - 6)),
				new Spawn(FmabEntities.character(StoryCharacters.ARMSTRONG), new BlockPos(sx / 2 + 3, 0, sz - 6)),
				new Spawn(FmabEntities.character(StoryCharacters.HUGHES), new BlockPos(sx / 2 + 4, 0, sz - 6)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx / 2 - 2, 0, 2)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx / 2 + 2, 0, 2)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(8, 0, sz - 9)),
				new Spawn(FmabEntities.AMESTRIAN_SOLDIER, new BlockPos(sx - 9, 0, sz - 9)));
	}
}
