package com.ajustor.fmab.client.render;

import com.ajustor.fmab.tattoo.BodyArt;
import com.ajustor.fmab.tattoo.TattooSlot;
import org.jspecify.annotations.Nullable;

/**
 * Le dessin en cours dans l'éditeur, montré sur le joueur avant d'être encré : tant que l'éditeur est
 * ouvert, l'emplacement qu'on dessine porte ce dessin-là au rendu, et rien d'autre ne change. Lu et
 * écrit depuis le fil du client seulement.
 */
public final class TattooPreview {
	private static @Nullable TattooSlot slot;
	private static BodyArt art = BodyArt.EMPTY;
	private static boolean engraved;

	private TattooPreview() {
	}

	public static void show(TattooSlot value, BodyArt drawn, boolean steel) {
		slot = value;
		art = drawn;
		engraved = steel;
	}

	public static void clear() {
		slot = null;
		art = BodyArt.EMPTY;
	}

	/** L'emplacement qu'on dessine, ou null si l'éditeur est fermé. */
	public static @Nullable TattooSlot slot() {
		return slot;
	}

	public static BodyArt art() {
		return art;
	}

	public static boolean engraved() {
		return engraved;
	}
}
