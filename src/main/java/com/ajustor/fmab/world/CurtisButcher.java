package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * La boucherie des Curtis : une maison de brique à deux étages ; au rez-de-chaussée, l'étal (des
 * tonneaux, un fumoir), des chaînes et leurs crochets au plafond ; Izumi derrière son comptoir.
 */
final class CurtisButcher implements Blueprint {
	private static final Palette PALETTE = new Palette(b(Blocks.BRICKS), b(Blocks.DARK_OAK_LOG),
			b(Blocks.SPRUCE_PLANKS), b(Blocks.STONE_BRICKS), Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.BRICKS),
			Blocks.DARK_OAK_DOOR);

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, PALETTE);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside) {
			return shell;
		}
		// L'étal, en travers de la boutique, une ouverture au milieu.
		if (y == 1 && z == 3 && Math.abs(x - sx / 2) > 1) {
			return x % 2 == 0 ? b(Blocks.BARREL) : b(Blocks.SMOKER);
		}
		// Les crochets à viande : des chaînes qui pendent du plafond, au fond.
		if ((y == 2 || y == 3) && z >= 5 && z <= sz - 2 && x % 2 == 1) {
			return b(Blocks.IRON_CHAIN);
		}
		if (y == 1 && z == sz - 2 && x == sx - 2) {
			return b(Blocks.CAULDRON);
		}
		return shell;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), CurtisButcher::stock));
	}

	private static List<ItemStack> stock(Plot p) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(Items.BEEF, 3 + p.noise(1, 0, 0, 5)));
		out.add(new ItemStack(Items.PORKCHOP, 2 + p.noise(2, 0, 0, 5)));
		out.add(new ItemStack(Items.MUTTON, 2 + p.noise(3, 0, 0, 4)));
		out.add(new ItemStack(Items.BONE, 2 + p.noise(4, 0, 0, 4)));
		return out;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(FmabEntities.IZUMI, new BlockPos(p.sizeX() / 2, 1, 4)));
	}
}
