package com.ajustor.fmab.world;

import com.ajustor.fmab.item.CipheredNotesItem;
import com.ajustor.fmab.registry.FmabBlocks;
import com.ajustor.fmab.registry.FmabEntities;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.ajustor.fmab.world.Blueprints.*;

/**
 * Les ruines d'Ishval : des maisons de terre écroulées, un temple au toit effondré, et au centre le
 * point de sang de la guerre d'extermination. Scar y erre.
 */
final class IshvalRuins implements Blueprint {
	@Override
	public BlockState at(int x, int y, int z, Plot p) {
		int s = p.sizeX(), c = s / 2;
		if (y < 0) {
			return b(Blocks.SANDSTONE);
		}
		if (y == 0) {
			int n = p.noise(x, 0, z, 6);
			return n == 0 ? b(Blocks.COARSE_DIRT) : n == 1 ? b(Blocks.RED_SAND) : b(Blocks.SAND);
		}
		if (y == 1 && x == c && z == c) {
			return b(FmabBlocks.BLOOD_CREST);
		}
		// Le temple au centre : des murs à hauteur variable, sans toit.
		int dx = Math.abs(x - c), dz = Math.abs(z - c);
		if (Math.max(dx, dz) == 5 && y <= 2 + p.noise(x, 7, z, 5) && !(dz == 5 && dx <= 1 && z > c)) {
			return p.noise(x, y, z, 3) == 0 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.CUT_RED_SANDSTONE);
		}
		// Les maisons de terre, en ruine, en cercle autour.
		for (int k = 0; k < 6; k++) {
			double a = Math.PI / 3 * k + 0.3;
			int hx = c + (int) Math.round(Math.cos(a) * 11), hz = c + (int) Math.round(Math.sin(a) * 11);
			int ex = Math.abs(x - hx), ez = Math.abs(z - hz);
			if (Math.max(ex, ez) == 2 && y <= 1 + p.noise(x, k, z, 4)) {
				return p.noise(x, y, z, 4) == 0 ? b(Blocks.PACKED_MUD) : b(Blocks.TERRACOTTA);
			}
		}
		if (y == 1 && p.noise(x, 3, z, 40) == 0) {
			return b(Blocks.DEAD_BUSH);
		}
		return y <= 6 ? AIR : null;
	}

	@Override
	public List<Chest> chests(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(new Chest(new BlockPos(c + 3, 1, c - 3), IshvalRuins::remains));
	}

	private static List<ItemStack> remains(Plot p) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(Items.BONE, 3 + p.noise(1, 0, 0, 5)));
		out.add(new ItemStack(Items.CLAY_BALL, 2 + p.noise(2, 0, 0, 4)));
		out.add(new ItemStack(Items.PAPER, 1 + p.noise(3, 0, 0, 3)));
		if (p.noise(4, 0, 0, 2) == 0) {
			out.add(CipheredNotesItem.of(FmabItems.CIPHERED_NOTES, p.noise(5, 0, 0, 4)));
		}
		return out;
	}

	@Override
	public List<Spawn> spawns(Plot p) {
		int c = p.sizeX() / 2;
		return List.of(new Spawn(FmabEntities.SCAR, new BlockPos(c - 2, 1, c + 2)));
	}
}
