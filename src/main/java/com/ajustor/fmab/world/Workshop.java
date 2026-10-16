package com.ajustor.fmab.world;

import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Un atelier d'automail de Rush Valley : grès, enclumes et établis contre le mur du fond, des
 * pièces dans le coffre. La boutique de Garfiel accueille Winry.
 */
final class Workshop implements Blueprint {
	private final boolean winry;

	Workshop(boolean winry) {
		this.winry = winry;
	}

	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		BlockState shell = house(x, y, z, p, RUSH_VALLEY_PALETTE);
		int sx = p.sizeX(), sz = p.sizeZ();
		boolean inside = x > 0 && x < sx - 1 && z > 0 && z < sz - 1;
		if (!inside || y != 1 || z != sz - 2 || x == 1) {
			return shell;
		}
		return switch ((x + p.variant(3)) % 4) {
			case 0 -> b(Blocks.ANVIL);
			case 1 -> b(FmabBlocks.AUTOMAIL_BENCH);
			case 2 -> b(Blocks.GRINDSTONE);
			default -> b(Blocks.SMITHING_TABLE);
		};
	}

	@Override
	public List<Chest> chests(Plot p) {
		return List.of(new Chest(new BlockPos(1, 1, p.sizeZ() - 2), Workshop::stock));
	}

	/** Des pièces de rechange, du fer, et parfois un automail complet. */
	private static List<ItemStack> stock(Plot p) {
		Item[] parts = {FmabItems.RUSH_VALLEY_AUTOMAIL_ARM, FmabItems.RUSH_VALLEY_AUTOMAIL_LEG,
				FmabItems.IRON_AUTOMAIL_ARM, FmabItems.IRON_AUTOMAIL_LEG};
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(Items.IRON_INGOT, 2 + p.noise(3, 0, 0, 6)));
		out.add(new ItemStack(Items.REDSTONE, 1 + p.noise(4, 0, 0, 4)));
		if (p.noise(5, 0, 0, 3) == 0) {
			out.add(new ItemStack(parts[p.noise(6, 0, 0, parts.length)]));
		}
		return out;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		Spawn resident = new Spawn(winry ? FmabEntities.WINRY : EntityTypes.VILLAGER,
				new BlockPos(p.sizeX() / 2, 1, p.sizeZ() / 2));
		return List.of(resident);
	}
}
