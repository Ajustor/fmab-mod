package com.ajustor.fmab.training;

import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.item.Tomes;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Épreuves d'Izumi. Chacune se réussit en jouant (le serveur la constate), puis se récompense en
 * venant lui en rendre compte.
 *
 * @param mastery maîtrise accordée, par école
 * @param nodes   nœuds de savoir accordés
 * @param tome    tome offert par Izumi, s'il y en a un
 */
public enum Trial {
	/** Lever un mur de pierre. */
	FIRST_WALL(Map.of("earth", 10), List.of(), Tomes.STONE_AND_METAL),
	/** Réussir un cercle sur un mur ou un plafond. */
	SURFACE(Map.of(), List.of("fmab:earth/thick_wall"), null),
	/** Réussir un cercle à deux étages. */
	TWO_STAGES(Map.of("earth", 15, "metal", 15), List.of(), null),
	/** Réussir une transmutation avec un gant, sans rien tracer. */
	GLOVES(Map.of(), List.of("fmab:metal/docile_iron"), Tomes.FORMS),
	/** Tenir tête à Izumi en combat d'entraînement. */
	SPAR(Map.of(), List.of("fmab:earth/stone_lance", "fmab:metal/arm_blade"), null),
	/** Survivre un jour entier sur l'île de Yock, sans la quitter : un est tout, tout est un. */
	ISLAND(Map.of("earth", 20, "metal", 20, "fire", 10), List.of(), null);

	private final Map<String, Integer> mastery;
	private final List<String> nodes;
	private final Tome tome;

	Trial(Map<String, Integer> mastery, List<String> nodes, Tome tome) {
		this.mastery = mastery;
		this.nodes = nodes;
		this.tome = tome;
	}

	public Optional<Tome> tome() {
		return Optional.ofNullable(tome);
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
