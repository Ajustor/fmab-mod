package com.ajustor.fmab.training;

import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.data.Tome;
import com.ajustor.fmab.item.Tomes;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Épreuves d'Izumi, dans l'ordre où elle les présente. Chacune se réussit en jouant (le serveur la
 * constate), puis se récompense en venant lui en rendre compte. La plupart sont à la portée d'un
 * Apprenti ; celle des deux étages attend l'Alchimiste, qui seul les tient.
 *
 * @param mastery maîtrise accordée, par école
 * @param nodes   nœuds de savoir accordés
 * @param tome    tome offert par Izumi, s'il y en a un
 * @param rank    le rang à partir duquel l'épreuve est à portée (Izumi le dit à qui ne l'a pas)
 */
public enum Trial {
	/** Lever un mur de pierre. */
	FIRST_WALL(Map.of("earth", 10), List.of(), Tomes.STONE_AND_METAL, Rank.APPRENTICE),
	/** Réussir un cercle sur un mur ou un plafond. */
	SURFACE(Map.of(), List.of("fmab:earth/thick_wall"), null, Rank.APPRENTICE),
	/** Comprendre un glyphe à force de le tracer, sans tome. */
	PRACTICE(Map.of("earth", 10, "metal", 10), List.of(), null, Rank.APPRENTICE),
	/** Réussir une transmutation avec un gant, sans rien tracer. */
	GLOVES(Map.of(), List.of("fmab:metal/docile_iron"), Tomes.FORMS, Rank.APPRENTICE),
	/** Tenir tête à Izumi en combat d'entraînement. */
	SPAR(Map.of(), List.of("fmab:earth/stone_lance", "fmab:metal/arm_blade"), null, Rank.APPRENTICE),
	/** Réussir un cercle à deux étages : un Apprenti n'en tient qu'un. */
	TWO_STAGES(Map.of("earth", 15, "metal", 15), List.of(), null, Rank.ALCHEMIST),
	/** Survivre un jour entier sur l'île de Yock, sans la quitter : un est tout, tout est un. */
	ISLAND(Map.of("earth", 20, "metal", 20, "fire", 10), List.of(), null, Rank.APPRENTICE);

	private final Map<String, Integer> mastery;
	private final List<String> nodes;
	private final Tome tome;
	private final Rank rank;

	Trial(Map<String, Integer> mastery, List<String> nodes, Tome tome, Rank rank) {
		this.mastery = mastery;
		this.nodes = nodes;
		this.tome = tome;
		this.rank = rank;
	}

	public Rank rank() {
		return rank;
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

	/** Comment la réussir : ce qu'Izumi en dit quand on la survole. */
	public String hintKey() {
		return translationKey() + ".hint";
	}

	public static Trial byId(String id) {
		return valueOf(id.toUpperCase(Locale.ROOT));
	}
}
