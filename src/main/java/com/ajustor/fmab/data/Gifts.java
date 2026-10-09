package com.ajustor.fmab.data;

import com.ajustor.fmab.registry.FmabAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/** Les cadeaux qu'un PNJ ne fait qu'une fois à chaque joueur. */
public final class Gifts {
	private Gifts() {
	}

	public static boolean received(ServerPlayer player, String gift) {
		return player.getAttachedOrCreate(FmabAttachments.GIFTS).contains(gift);
	}

	/**
	 * Donne le cadeau s'il n'a pas déjà été reçu.
	 *
	 * @return vrai s'il vient d'être donné
	 */
	public static boolean give(ServerPlayer player, String gift, ItemStack stack) {
		if (received(player, gift)) {
			return false;
		}
		Set<String> gifts = new HashSet<>(player.getAttachedOrCreate(FmabAttachments.GIFTS));
		gifts.add(gift);
		player.setAttached(FmabAttachments.GIFTS, Set.copyOf(gifts));
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		return true;
	}
}
