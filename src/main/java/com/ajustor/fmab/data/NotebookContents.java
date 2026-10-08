package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages du Carnet de cercles : les cercles enregistrés et celui qui est sélectionné (celui que la
 * craie trace au sol).
 */
public record NotebookContents(List<Page> pages, int selected) {
	public static final int MAX_PAGES = 16;
	public static final int MAX_NAME_LENGTH = 32;
	public static final NotebookContents EMPTY = new NotebookContents(List.of(), 0);

	public record Page(String name, Drawing drawing) {
		public static final Codec<Page> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.string(0, MAX_NAME_LENGTH).fieldOf("name").forGetter(Page::name),
				FmabCodecs.DRAWING.fieldOf("drawing").forGetter(Page::drawing)
		).apply(i, Page::new));
	}

	public static final Codec<NotebookContents> CODEC = RecordCodecBuilder.create(i -> i.group(
			Page.CODEC.listOf(0, MAX_PAGES).fieldOf("pages").forGetter(NotebookContents::pages),
			Codec.INT.optionalFieldOf("selected", 0).forGetter(NotebookContents::selected)
	).apply(i, NotebookContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, NotebookContents> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public NotebookContents {
		pages = List.copyOf(pages);
		selected = pages.isEmpty() ? 0 : Math.clamp(selected, 0, pages.size() - 1);
	}

	public Drawing selectedDrawing() {
		return pages.isEmpty() ? Drawing.EMPTY : pages.get(selected).drawing();
	}

	/** Remplace la page {@code index}, ou en ajoute une si {@code index} vaut le nombre de pages. */
	public NotebookContents withPage(int index, Page page) {
		List<Page> out = new ArrayList<>(pages);
		if (index >= out.size()) {
			out.add(page);
			index = out.size() - 1;
		} else {
			out.set(index, page);
		}
		return new NotebookContents(out, index);
	}

	public NotebookContents withoutPage(int index) {
		List<Page> out = new ArrayList<>(pages);
		out.remove(index);
		return new NotebookContents(out, Math.min(selected, out.size() - 1));
	}

	public NotebookContents select(int index) {
		return new NotebookContents(pages, index);
	}
}
