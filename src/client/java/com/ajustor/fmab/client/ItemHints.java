package com.ajustor.fmab.client;

import com.ajustor.fmab.FmabClientConfig;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.gate.Restoration;
import com.ajustor.fmab.item.GloveItem;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.registry.FmabItems;
import com.ajustor.fmab.transmutation.StateWatch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Les infobulles des objets clés qui n'expliquent rien d'eux-mêmes : la montre d'Alchimiste d'État,
 * le noyau de Pierre, le sang cristallisé, le fil, l'encre, la cartouche. Les chiffres viennent des
 * constantes du code ; la montre dit quand elle est sans effet pour le joueur.
 */
public final class ItemHints {
	private ItemHints() {
	}

	public static void append(ItemStack stack, List<Component> lines) {
		// Une règle, pas un conseil : affichée même sans les infobulles d'aide.
		var player = Minecraft.getInstance().player;
		if (player != null && stack.getItem() instanceof GloveItem glove) {
			int stable = glove.kind().stableStages(FmabAttachments.ALCHEMIST.getOrCreate(player).rank());
			if (stable < glove.kind().stages()) {
				lines.add(Component.translatable("item.fmab.glove.unstable_above", stable).withStyle(ChatFormatting.RED));
			}
		}
		if (!FmabClientConfig.get().itemHints()) {
			return;
		}
		if (stack.is(FmabItems.STATE_WATCH)) {
			lines.add(hint("item.fmab.state_watch.hint", StateWatch.RANGE_BONUS, Math.round(StateWatch.SAVING * 100)));
			if (player != null && !FmabAttachments.ALCHEMIST.getOrCreate(player).rank().atLeast(Rank.STATE)) {
				lines.add(Component.translatable("item.fmab.state_watch.inert").withStyle(ChatFormatting.RED));
			}
		} else if (stack.is(FmabItems.PHILOSOPHER_STONE_CORE)) {
			lines.add(hint("item.fmab.philosopher_stone_core.hint", Restoration.CORES_PER_STONE, Restoration.STONE_SOULS));
		} else if (stack.is(FmabItems.CRYSTALLIZED_BLOOD)) {
			lines.add(hint("item.fmab.crystallized_blood.hint", Restoration.CORES_PER_STONE));
			lines.add(hint("item.fmab.crystallized_blood.where"));
		} else if (stack.is(FmabItems.ALCHEMICAL_THREAD)) {
			lines.add(hint("item.fmab.alchemical_thread.hint"));
		} else if (stack.is(FmabItems.ALCHEMICAL_INK)) {
			lines.add(hint("item.fmab.alchemical_ink.hint"));
		} else if (stack.is(FmabItems.CARTRIDGE)) {
			lines.add(hint("item.fmab.cartridge.hint"));
		}
	}

	private static Component hint(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
	}
}
