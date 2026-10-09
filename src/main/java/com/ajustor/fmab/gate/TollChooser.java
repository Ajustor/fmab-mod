package com.ajustor.fmab.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Le péage de la Vérité dépend de ce qui a été tenté. Un échec grave (ingrédients incomplets) ou un
 * corps à qui il ne reste rien à prendre coûte le corps entier ; sinon, tirage pondéré parmi ce qui
 * est encore intact, d'autant plus lourd que l'ambition est grande.
 */
public final class TollChooser {
	/** Ordre du tirage : le même à chaque appel, pour qu'un tirage se rejoue à l'identique. */
	private static final List<BodyPart> CANDIDATES = List.of(BodyPart.ORGANS, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG,
			BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.SIGHT);

	private TollChooser() {
	}

	/**
	 * @param lost     ce que la Vérité a déjà pris
	 * @param ambition ce qui a été tenté : ingrédients en surplus, humains visés, ouvertures passées
	 * @param severe   la transmutation a échoué gravement
	 * @param roll     tirage uniforme dans [0, 1)
	 */
	public static BodyPart choose(Set<BodyPart> lost, int ambition, boolean severe, double roll) {
		if (severe) {
			return BodyPart.BODY;
		}
		List<BodyPart> intact = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (BodyPart part : CANDIDATES) {
			if (lost.contains(part)) {
				continue;
			}
			int weight = weight(part, ambition);
			intact.add(part);
			weights.add(weight);
			total += weight;
		}
		if (intact.isEmpty()) {
			return BodyPart.BODY;
		}
		double target = roll * total;
		for (int i = 0; i < intact.size(); i++) {
			target -= weights.get(i);
			if (target < 0) {
				return intact.get(i);
			}
		}
		return intact.getLast();
	}

	public static int weight(BodyPart part, int ambition) {
		return part.baseWeight() + Math.max(0, ambition) * part.severity();
	}
}
