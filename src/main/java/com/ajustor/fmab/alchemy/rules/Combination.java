package com.ajustor.fmab.alchemy.rules;

import com.google.gson.JsonObject;

import java.util.Optional;
import java.util.Set;

/**
 * Une ligne de la table des combinaisons ({@code data/<ns>/combination/<nom>.json}) : tels éléments
 * et telle action produisent tel effet. Un pack peut en ajouter sans recompiler.
 *
 * <pre>{@code
 * { "elements": ["earth"], "action": "fix", "effect": "fmab:wall", "range": 2,
 *   "school": "earth", "requires": "fmab:earth/stone_lance", "passive": "fmab:reinforce" }
 * }</pre>
 *
 * <p>L'effet est soit un effet codé dans le mod ({@code fmab:wall}...), soit décrit en données par
 * {@code spec} : une zone, un filtre et une opération (voir {@code DataEffects}). Une combinaison
 * dont les éléments sont {@code ["*"]} est un joker : elle vaut pour tout élément seul qui n'a pas
 * sa propre combinaison avec cette action, sauf ceux de {@code except}.
 *
 * @param range    portée de base en blocs, avant les points d'intensité
 * @param school   école dont la maîtrise progresse, et dont les bonus s'appliquent
 * @param requires nœud de savoir sans lequel la combinaison est inconnue
 * @param passive  effet permanent du cercle quand il est porté (vêtement brodé, tatouage)
 * @param formula  éléments que des satellites doivent infuser dans l'étage : la formule complète
 *                 de ce qu'on veut créer (la transmutation humaine écrit tout ce dont un corps est
 *                 fait). Il en manque un, et le cercle rebondit.
 * @param except   pour un joker, les éléments auxquels il ne s'applique pas
 * @param spec     l'effet décrit en données, à la place d'un effet codé
 */
public record Combination(String id, Set<String> elements, String action, String effect, double range, String school,
		Optional<String> requires, Optional<String> passive, Set<String> formula, Set<String> except,
		Optional<JsonObject> spec) {
	/** Les éléments d'un joker. */
	public static final String ANY = "*";

	public Combination {
		elements = Set.copyOf(elements);
		formula = Set.copyOf(formula);
		except = Set.copyOf(except);
	}

	public Combination(String id, Set<String> elements, String action, String effect, double range, String school,
			Optional<String> requires, Optional<String> passive, Set<String> formula) {
		this(id, elements, action, effect, range, school, requires, passive, formula, Set.of(), Optional.empty());
	}

	public Combination(String id, Set<String> elements, String action, String effect, double range, String school,
			Optional<String> requires, Optional<String> passive) {
		this(id, elements, action, effect, range, school, requires, passive, Set.of());
	}

	public Combination withId(String newId) {
		return new Combination(newId, elements, action, effect, range, school, requires, passive, formula, except, spec);
	}

	/** Un joker, qui vaut pour tout élément seul ? */
	public boolean wildcard() {
		return elements.contains(ANY);
	}

	/** Le joker appliqué à cet élément : la combinaison que le cercle obtient vraiment. */
	public Combination forElement(String element, String defaultSchool) {
		return new Combination(id, Set.of(element), action, effect, range, school.isEmpty() ? defaultSchool : school,
				requires, passive, formula, Set.of(), spec);
	}
}
