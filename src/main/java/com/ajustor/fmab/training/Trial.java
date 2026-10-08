package com.ajustor.fmab.training;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Épreuves d'Izumi. Chacune se réussit en jouant (le serveur la constate), puis se récompense en
 * venant lui en rendre compte.
 *
 * @param mastery maîtrise accordée, par école
 * @param nodes   nœuds de savoir accordés
 */
public enum Trial {
	/** Lever un mur de pierre. */
	FIRST_WALL(Map.of("earth", 10), List.of()),
	/** Réussir un cercle sur un mur ou un plafond. */
	SURFACE(Map.of(), List.of("fmab:earth/thick_wall")),
	/** Réussir un cercle à deux étages. */
	TWO_STAGES(Map.of("earth", 15, "metal", 15), List.of()),
	/** Réussir une transmutation avec un gant, sans rien tracer. */
	GLOVES(Map.of(), List.of("fmab:metal/docile_iron")),
	/** Tenir tête à Izumi en combat d'entraînement. */
	SPAR(Map.of(), List.of("fmab:earth/stone_lance", "fmab:metal/arm_blade"));

	private final Map<String, Integer> mastery;
	private final List<String> nodes;

	Trial(Map<String, Integer> mastery, List<String> nodes) {
		this.mastery = mastery;
		this.nodes = nodes;
	}

	public Map<String, Integer> mastery() {
		return mastery;
	}

	public List<String> nodes() {
		return nodes;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "trial.fmab." + id();
	}

	public static Trial byId(String id) {
		return valueOf(id.toUpperCase(Locale.ROOT));
	}
}
