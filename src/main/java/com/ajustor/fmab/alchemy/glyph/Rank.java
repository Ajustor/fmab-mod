package com.ajustor.fmab.alchemy.glyph;

import java.util.Locale;

/**
 * Rangs d'alchimiste. Chacun fixe ce qu'un cercle a le droit de contenir : plafond de complexité,
 * nombre d'étages et polygone le plus riche.
 */
public enum Rank {
	APPRENTICE(6, 1, 4),
	ALCHEMIST(14, 2, 6),
	STATE(25, 3, 12),
	GATE(40, 5, 12);

	private final int complexityCap;
	private final int maxStages;
	private final int maxPolygonSides;

	Rank(int complexityCap, int maxStages, int maxPolygonSides) {
		this.complexityCap = complexityCap;
		this.maxStages = maxStages;
		this.maxPolygonSides = maxPolygonSides;
	}

	public int complexityCap() {
		return complexityCap;
	}

	public int maxStages() {
		return maxStages;
	}

	public int maxPolygonSides() {
		return maxPolygonSides;
	}

	public boolean atLeast(Rank other) {
		return ordinal() >= other.ordinal();
	}

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "rank.fmab." + serializedName();
	}

	public static Rank fromSerializedName(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}
}
