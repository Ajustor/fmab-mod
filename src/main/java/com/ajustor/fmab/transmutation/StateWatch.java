package com.ajustor.fmab.transmutation;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * La montre d'Alchimiste d'État amplifie l'alchimie de son titulaire : elle ne sert qu'à un
 * Alchimiste d'État, et seulement s'il la porte sur lui.
 */
public final class StateWatch {
	/** Portée ajoutée à chaque effet. */
	public static final int RANGE_BONUS = 1;
	/** Part de la concentration épargnée. */
	private static final double SAVING = 0.2;

	private StateWatch() {
	}

	public static boolean empowers(ServerPlayer player) {
		if (!player.getAttachedOrCreate(FmabAttachments.ALCHEMIST).rank().atLeast(Rank.STATE)) {
			return false;
		}
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(FmabItems.STATE_WATCH)) {
				return true;
			}
		}
		return false;
	}

	/** Coût en concentration, arrondi au-dessus pour qu'un cercle coûte toujours au moins 1. */
	public static int cost(int base, boolean watch) {
		return watch ? Math.max(1, (int) Math.ceil(base * (1 - SAVING))) : base;
	}
}
