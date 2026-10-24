package com.ajustor.fmab.alchemy.rules;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.circle.ParsedCircle;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Rank;

import java.util.List;
import java.util.Set;

/**
 * Verdict sur un cercle : ce qu'il coûte, s'il tient, ce qu'il fera. C'est ce que le carnet affiche
 * avant validation et ce que le serveur recalcule à l'activation.
 *
 * @param stability       capacité du polygone rapportée à la charge, sur l'étage le plus faible ;
 *                        en dessous de 1, le cercle rebondit
 * @param reboundSeverity de 0 (rien) à 1 (le support est détruit)
 * @param risk            probabilité qu'un cercle qui devrait fonctionner rebondisse quand même,
 *                        parce que l'alchimiste y trace des glyphes qu'il ne comprend pas
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
		double reboundSeverity,
		double risk
) {
	public Analysis {
		effects = List.copyOf(effects);
		issues = List.copyOf(issues);
	}

	public enum Outcome {
		/** Le cercle ne réagit pas : rien à faire, ou au-delà du rang de l'alchimiste. */
		INERT,
		/** L'énergie revient sur l'alchimiste. */
		REBOUND,
		/** La transmutation a lieu. */
		WORKS
	}

	/**
	 * Effet d'un étage, ou d'un satellite complet posé sur l'un de ses sommets.
	 *
	 * @param satellite  −1 pour l'effet de l'étage lui-même, sinon l'indice du satellite
	 * @param origin     d'où part l'effet, par rapport au centre du carnet (zéro pour l'étage)
	 * @param direction  angle (radians, y vers le bas) donné par la flèche, NaN sans flèche
	 * @param infusions  éléments apportés par les satellites d'infusion ({@code fire}...)
	 * @param link       liaison qui relie l'étage au précédent
	 * @param shape      la forme de l'effet : {@code line}, {@code circle} ou {@code cone}
	 * @param duration   glyphes de durée : chacun ajoute une fois la durée de base
	 * @param targets    glyphes de cibles : chacun ajoute une cible
	 * @param aims       ce que visent les glyphes Objet et Bloc ({@code item}, {@code block}) ; vide, tout
	 */
	public record StageEffect(int stage, int satellite, Combination combination, double range, int intensity,
			Vec2 origin, double direction, Set<String> infusions, LinkKind link, String shape, int duration,
			int targets, Set<String> aims) {
		public StageEffect {
			infusions = Set.copyOf(infusions);
			aims = Set.copyOf(aims);
		}

		/** L'effet touche-t-il les objets ? Oui sans Objet ni Bloc, ou avec Objet (Créature n'y change rien). */
		public boolean aimsItems() {
			return aims.contains("item") || !aims.contains("block");
		}

		/** L'effet touche-t-il les blocs du monde ? Oui sans Objet ni Bloc, ou avec Bloc. */
		public boolean aimsBlocks() {
			return aims.contains("block") || !aims.contains("item");
		}

		/** Le glyphe Créature est-il tracé ? Il ajoute à l'effet ce qu'il fait aux créatures. */
		public boolean aimsEntities() {
			return aims.contains("entity");
		}

		public boolean hasDirection() {
			return !Double.isNaN(direction);
		}

		public boolean infused(String element) {
			return infusions.contains(element);
		}
	}
}
