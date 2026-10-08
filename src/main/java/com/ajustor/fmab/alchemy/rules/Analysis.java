package com.ajustor.fmab.alchemy.rules;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.ParsedCircle;
import com.ajustor.fmab.alchemy.glyph.Rank;

import java.util.List;

/**
 * Verdict sur un cercle : ce qu'il coûte, s'il tient, ce qu'il fera. C'est ce que le carnet affiche
 * avant validation et ce que le serveur recalcule à l'activation.
 *
 * @param stability       capacité du polygone rapportée à la charge, sur l'étage le plus faible ;
 *                        en dessous de 1, le cercle rebondit
 * @param reboundSeverity de 0 (rien) à 1 (le support est détruit)
 */
public record Analysis(
		ParsedCircle parsed,
		int complexity,
		Rank requiredRank,
		double stability,
		int concentration,
		List<StageEffect> effects,
		List<CircleIssue> issues,
		Outcome outcome,
		double reboundSeverity
) {
	public Analysis {
		effects = List.copyOf(effects);
		issues = List.copyOf(issues);
	}

	public enum Outcome {
		/** Le cercle ne réagit pas : rien à faire, ou l'alchimiste ne le comprend pas. */
		INERT,
		/** L'énergie revient sur l'alchimiste. */
		REBOUND,
		/** La transmutation a lieu. */
		WORKS
	}

	/**
	 * Effet d'un étage.
	 *
	 * @param direction angle (radians, y vers le bas) donné par la flèche, NaN sans flèche
	 */
	public record StageEffect(int stage, Combination combination, double range, int intensity, double direction) {
		public boolean hasDirection() {
			return !Double.isNaN(direction);
		}
	}
}
