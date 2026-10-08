package com.ajustor.fmab.alchemy.rules;

import java.util.Optional;
import java.util.Set;

/**
 * Une ligne de la table des combinaisons ({@code data/<ns>/combination/<nom>.json}) : tels éléments
 * et telle action produisent tel effet. Les effets eux-mêmes sont codés dans le mod ; la table
 * dit seulement quel effet un cercle déclenche, donc un pack peut en ajouter sans recompiler.
 *
 * <pre>{@code
 * { "elements": ["earth"], "action": "fix", "effect": "fmab:wall", "range": 2,
 *   "school": "earth", "requires": "fmab:earth/stone_lance" }
 * }</pre>
 *
 * @param range    portée de base en blocs, avant les points d'intensité
 * @param school   école dont la maîtrise progresse, et dont les bonus s'appliquent
 * @param requires nœud de savoir sans lequel la combinaison est inconnue
 */
public record Combination(String id, Set<String> elements, String action, String effect, double range, String school,
		Optional<String> requires) {
	public Combination {
		elements = Set.copyOf(elements);
	}

	public Combination withId(String newId) {
		return new Combination(newId, elements, action, effect, range, school, requires);
	}
}
