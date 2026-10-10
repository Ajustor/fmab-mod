package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.drawing.Vec2;

import java.util.Locale;

/**
 * Problème repéré dans un tracé. {@code where} est en coordonnées de grille du carnet, pour que
 * l'interface puisse l'entourer sur le dessin ; il est nul quand le problème concerne tout le
 * cercle.
 */
public record CircleIssue(Kind kind, Vec2 where, String detail) {
	public enum Kind {
		/** Aucun anneau centré : ce n'est pas un cercle de transmutation. */
		NO_RING,
		/** Un groupe de traits qui ne ressemble à aucun glyphe. */
		UNKNOWN_GLYPH,
		/** Un glyphe tracé hors de tout anneau. */
		OUTSIDE_RING,
		/** Un polygone qui n'est inscrit dans aucun anneau. */
		LOOSE_POLYGON,
		/** Un étage sans glyphe d'action : il ne fait rien. */
		NO_ACTION,
		/** Un étage sans élément : il ne vise aucune matière. */
		NO_ELEMENT,
		/** Plusieurs actions dans un même étage. */
		TOO_MANY_ACTIONS,
		/** Éléments et action qui ne forment aucune combinaison connue : rebond. */
		UNKNOWN_COMBINATION,
		/** Plus de complexité, d'étages ou de côtés que le rang n'en permet. */
		RANK_TOO_LOW,
		/** Un glyphe que l'alchimiste n'a pas encore compris. */
		GLYPH_NOT_LEARNED,
		/** Le polygone ne tient pas la charge : rebond proportionnel. */
		UNSTABLE,
		/** Un étage extérieur qu'aucun trait ne relie au précédent : il ne reçoit rien. */
		UNLINKED_STAGE,
		/** Deux étages reliés par des liaisons de types différents. */
		CONFLICTING_LINKS,
		/** Deux éléments dans un même étage sans hexagramme pour les fusionner. */
		FUSION_NEEDS_HEXAGRAM,
		/** Un satellite qui n'est ni une infusion (éléments seuls) ni un cercle complet. */
		SATELLITE_INCOMPLETE,
		/** Une combinaison qui demande un nœud de savoir pas encore acquis. */
		KNOWLEDGE_MISSING,
		/** Des satellites n'infusent pas tous les éléments que la formule demande : rebond. */
		INCOMPLETE_FORMULA;

		public String translationKey() {
			return "circle.fmab.issue." + name().toLowerCase(Locale.ROOT);
		}
	}

	public static CircleIssue of(Kind kind) {
		return new CircleIssue(kind, null, "");
	}

	public static CircleIssue at(Kind kind, Vec2 where) {
		return new CircleIssue(kind, where, "");
	}
}
