package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Le Devil's Nest, le bar de Greed à Dublith : un comptoir le long du mur du fond, des tonneaux,
 * des tables, une lanterne au plafond, et la réserve d'or du patron.
 */
final class DevilsNest implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, DEVILS_NEST_PALETTE);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside) {
			return shell;
		}
		if (y == 1) {
			// Le comptoir : des tonneaux contre le mur, le zinc devant.
			if (z == sz - 2) {
				return x % 3 == 0 ? b(Blocks.BREWING_STAND) : b(Blocks.BARREL);
			}
			if (z == sz - 3 && x > 1 && x < sx - 2) {
				return b(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
			}
			// Les tables : un piquet et un plateau (pas dans l'axe de la porte).
			if (z == 2 && x % 4 == 2 && x != sx / 2) {
				return b(Blocks.DARK_OAK_FENCE);
			}
		}
		if (y == 2 && z == 2 && x % 4 == 2 && x != p.sizeX() / 2) {
			return b(Blocks.DARK_OAK_PRESSURE_PLATE);
		}
		if (y == 1 && x == 1 && z == 1) {
			return b(Blocks.LANTERN);
		}
		return shell;
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), plot -> List.of(
				new ItemStack(Items.GOLD_INGOT, 6 + plot.noise(1, 0, 0, 10)),
				new ItemStack(Items.EMERALD, 2 + plot.noise(2, 0, 0, 6)),
				new ItemStack(Items.GLASS_BOTTLE, 3 + plot.noise(3, 0, 0, 5)))));
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		return List.of(new Spawn(FmabEntities.GREED, new BlockPos(p.sizeX() / 2, 1, p.sizeZ() - 4)));
	}
}
