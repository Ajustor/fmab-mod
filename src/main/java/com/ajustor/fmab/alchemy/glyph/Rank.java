package com.ajustor.fmab.alchemy.glyph;

import java.util.Locale;

/**
 * Rangs d'alchimiste. Chacun fixe ce qu'un cercle a le droit de contenir : plafond de complexité,
 * nombre d'étages, polygone le plus riche, polygones superposés et nombre de satellites.
 */
public enum Rank {
	APPRENTICE(6, 1, 4, 0, 1),
	ALCHEMIST(14, 2, 6, 2, 3),
	STATE(25, 3, 12, 6, 4),
	GATE(40, 5, 12, 12, 6);

	private final int complexityCap;
	private final int maxStages;
	private final int maxPolygonSides;
	private final int maxSatellites;
	private final int maxPolygonsPerStage;

	Rank(int complexityCap, int maxStages, int maxPolygonSides, int maxSatellites, int maxPolygonsPerStage) {
		this.complexityCap = complexityCap;
		this.maxStages = maxStages;
		this.maxPolygonSides = maxPolygonSides;
		this.maxSatellites = maxSatellites;
		this.maxPolygonsPerStage = maxPolygonsPerStage;
	}

	/** Polygones superposés dans un même anneau ; un hexagramme en compte deux. */
	public int maxPolygonsPerStage() {
		return maxPolygonsPerStage;
	}

	public int maxSatellites() {
		return maxSatellites;
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
