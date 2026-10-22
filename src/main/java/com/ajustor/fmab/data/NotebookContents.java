package com.ajustor.fmab.data;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pages du Carnet de cercles d'un joueur : les cercles enregistrés, celui qui est sélectionné (celui
 * que la craie trace au sol et que les mains jointes lancent), et la place de chacun dans les roues des
 * cercles. Une roue tient {@link #WHEEL} places : la place {@code p} est la case {@code p % 9} de la
 * roue {@code p / 9}. On range les cercles comme on veut, roue après roue, en laissant des trous.
 *
 * @param slots la place de chaque page, dans l'ordre des pages ; une place manquante, en double ou hors
 *              bornes devient la première libre (un carnet d'avant les roues les prend dans l'ordre)
 */
public record NotebookContents(List<Page> pages, int selected, List<Integer> slots) {
	public static final int MAX_PAGES = 32;
	public static final int MAX_NAME_LENGTH = 32;
	/** Les places d'une roue. */
	public static final int WHEEL = 9;
	/** Autant de roues que de pages : de quoi ranger chaque cercle seul sur la sienne. */
	public static final int MAX_SLOTS = MAX_PAGES * WHEEL;
	public static final NotebookContents EMPTY = new NotebookContents(List.of(), 0);

	public record Page(String name, Drawing drawing) {
		public static final Codec<Page> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.string(0, MAX_NAME_LENGTH).fieldOf("name").forGetter(Page::name),
				FmabCodecs.DRAWING.fieldOf("drawing").forGetter(Page::drawing)
		).apply(i, Page::new));
	}

	public static final Codec<NotebookContents> CODEC = RecordCodecBuilder.create(i -> i.group(
			Page.CODEC.listOf(0, MAX_PAGES).fieldOf("pages").forGetter(NotebookContents::pages),
			Codec.INT.optionalFieldOf("selected", 0).forGetter(NotebookContents::selected),
			Codec.INT.listOf(0, MAX_PAGES).optionalFieldOf("slots", List.of()).forGetter(NotebookContents::slots)
	).apply(i, NotebookContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, NotebookContents> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public NotebookContents {
		pages = List.copyOf(pages);
		selected = pages.isEmpty() ? 0 : Math.clamp(selected, 0, pages.size() - 1);
		Set<Integer> used = new HashSet<>();
		List<Integer> placed = new ArrayList<>(pages.size());
		for (int i = 0; i < pages.size(); i++) {
			Integer slot = i < slots.size() ? slots.get(i) : null;
			if (slot == null || slot < 0 || slot >= MAX_SLOTS || used.contains(slot)) {
				slot = firstFree(used);
			}
			used.add(slot);
			placed.add(slot);
		}
		slots = List.copyOf(placed);
	}

	/** Les pages rangées dans l'ordre, une place chacune. */
	public NotebookContents(List<Page> pages, int selected) {
		this(pages, selected, List.of());
	}

	private static int firstFree(Set<Integer> used) {
		int slot = 0;
		while (used.contains(slot)) {
			slot++;
		}
		return slot;
	}

	public Drawing selectedDrawing() {
		return pages.isEmpty() ? Drawing.EMPTY : pages.get(selected).drawing();
	}

	/** Remplace la page {@code index}, à sa place ; ou en ajoute une, à la première place libre, si {@code index} vaut le nombre de pages. */
	public NotebookContents withPage(int index, Page page) {
		List<Page> out = new ArrayList<>(pages);
		if (index >= out.size()) {
			out.add(page);
			index = out.size() - 1;
		} else {
			out.set(index, page);
		}
		return new NotebookContents(out, index, slots);
	}

	/** Retire la page {@code index} : sa place reste vide. */
	public NotebookContents withoutPage(int index) {
		List<Page> out = new ArrayList<>(pages);
		out.remove(index);
		List<Integer> places = new ArrayList<>(slots);
		places.remove(index);
		return new NotebookContents(out, Math.min(selected, out.size() - 1), places);
	}

	public NotebookContents select(int index) {
		return new NotebookContents(pages, index, slots);
	}

	/**
	 * Déplace la page {@code index} d'un cran ({@code delta} vaut −1 ou 1) ; elle devient la sélection.
	 * Les deux pages échangent aussi leur place dans les roues.
	 */
	public NotebookContents moved(int index, int delta) {
		int to = index + delta;
		if (index < 0 || index >= pages.size() || to < 0 || to >= pages.size()) {
			return this;
		}
		List<Page> out = new ArrayList<>(pages);
		Collections.swap(out, index, to);
		return new NotebookContents(out, to, slots);
	}

	/** La place de la page {@code index} dans les roues. */
	public int slot(int index) {
		return slots.get(index);
	}

	/** La page rangée à cette place, ou −1 si elle est vide. */
	public int at(int slot) {
		return slots.indexOf(slot);
	}

	/** Les roues qui portent au moins un cercle, ou les trous entre elles : jusqu'à la dernière occupée. */
	public int wheels() {
		return slots.stream().mapToInt(slot -> slot / WHEEL + 1).max().orElse(0);
	}

	/**
	 * Range la page {@code index} à cette place. Si une autre page l'occupait, elle prend la place
	 * laissée : les deux échangent. Une place hors des roues ne change rien.
	 */
	public NotebookContents placed(int index, int slot) {
		if (index < 0 || index >= pages.size() || slot < 0 || slot >= MAX_SLOTS || slots.get(index) == slot) {
			return this;
		}
		List<Integer> places = new ArrayList<>(slots);
		int other = at(slot);
		if (other >= 0) {
			places.set(other, slots.get(index));
		}
		places.set(index, slot);
		return new NotebookContents(pages, selected, places);
	}

	/** La première page qui porte exactement ce tracé, ou −1. */
	public int indexOf(Drawing drawing) {
		for (int i = 0; i < pages.size(); i++) {
			if (pages.get(i).drawing().equals(drawing)) {
				return i;
			}
		}
		return -1;
	}
}
