package com.ajustor.fmab.item;

import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.registry.FmabAttachments;
import com.ajustor.fmab.transmutation.AlchemyRules;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.compat.EntityCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Consigner son savoir : un alchimiste choisit des glyphes qu'il comprend et les écrit dans un livre
 * vierge de son sac, qui devient des notes à son nom. Qui les lit apprend ces glyphes, une fois, comme
 * dans un tome ; l'auteur, lui, ne perd rien de ce qu'il sait.
 */
public final class Notes {
	/** Un livre de notes ne tient pas plus de glyphes. */
	public static final int MAX_GLYPHS = 16;

	public enum Result { WRITTEN, NOTHING, NO_BOOK }

	private Notes() {
	}

	/**
	 * Ce qui s'écrit vraiment, de ce que l'alchimiste a demandé : les glyphes qui existent et qu'il
	 * comprend, sans doublon, dans l'ordre demandé, {@link #MAX_GLYPHS} au plus.
	 */
	public static List<String> chosen(List<String> asked, Set<String> known, Set<String> existing) {
		Set<String> out = new LinkedHashSet<>();
		for (String id : asked) {
			if (known.contains(id) && existing.contains(id) && out.size() < MAX_GLYPHS) {
				out.add(id);
			}
		}
		return List.copyOf(out);
	}

	/** Écrit les notes : un livre vierge du sac devient un tome à son nom (un alchimiste en créatif n'en a pas besoin). */
	public static Result write(ServerPlayer author, List<String> asked) {
		Set<String> existing = new LinkedHashSet<>();
		for (Glyph g : AlchemyRules.of(author.level().registryAccess()).glyphs()) {
			existing.add(g.id());
		}
		List<String> glyphs = chosen(asked, FmabAttachments.ALCHEMIST.getOrCreate(author).known(), existing);
		if (glyphs.isEmpty()) {
			return Result.NOTHING;
		}
		if (!author.isCreative() && !takeBook(author)) {
			author.sendOverlayMessage(Component.translatable("tome.fmab.consign.no_book"));
			return Result.NO_BOOK;
		}
		ItemStack notes = Tomes.stack(Tome.notes(author.getName().getString(), new ArrayList<>(glyphs)));
		if (!author.getInventory().add(notes)) {
			EntityCompat.drop(author, notes);
		}
		author.level().playSound(null, author.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1, 1);
		author.sendSystemMessage(Component.translatable("tome.fmab.consigned", glyphs.size()));
		return Result.WRITTEN;
	}

	private static boolean takeBook(ServerPlayer player) {
		for (ItemStack stack : player.getInventory()) {
			if (stack.is(Items.BOOK) && !stack.isEmpty()) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}
}
