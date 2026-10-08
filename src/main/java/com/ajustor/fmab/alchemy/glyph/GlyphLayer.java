package com.ajustor.fmab.alchemy.glyph;

import java.util.Locale;

public enum GlyphLayer {
	/** Matière visée. */
	ELEMENT,
	/** Ce que l'étage fait de la matière. */
	ACTION,
	/** Paramètres posés sur l'anneau : direction, intensité. */
	MODIFIER;

	public static GlyphLayer fromSerializedName(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}
}
