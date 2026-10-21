package com.ajustor.fmab.world;

import java.util.Map;
import java.util.Optional;

/**
 * Les dimensions du mod : des plateaux d'une seule couche de bloc, sans relief ni structure. Leur
 * vue lointaine se décrit sans rien générer (voir l'adaptateur Distant Horizons du module Fabric).
 */
public final class FlatDimensions {
	/** Chaque dimension et le bloc de son unique couche, posée au fond du monde. */
	private static final Map<String, String> FLOORS = Map.of(
			"fmab:white_space", "fmab:white_floor",
			"fmab:gluttony_belly", "fmab:belly_flesh");

	private FlatDimensions() {
	}

	/** Le bloc du sol de cette dimension, si c'en est une du mod. */
	public static Optional<String> floor(String dimension) {
		return Optional.ofNullable(FLOORS.get(dimension));
	}

	public static Map<String, String> all() {
		return FLOORS;
	}
}
