package com.ajustor.fmab.gate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que coûte, en âmes de Pierre philosophale, de reprendre à la Vérité ce qu'elle a pris : plus
 * c'est grave, plus c'est cher. Quand la Pierre n'a pas assez d'âmes pour tout, elle rend ce qu'elle
 * peut, le plus grave d'abord.
 */
public final class Restoration {
	/** Les âmes d'une Pierre neuve. */
	public static final int STONE_SOULS = 100;

	private Restoration() {
	}

	public static int cost(BodyPart part) {
		return switch (part) {
			case ORGANS -> 15;
			case LEFT_LEG, RIGHT_LEG -> 20;
			case LEFT_ARM, RIGHT_ARM -> 25;
			case SIGHT -> 30;
			case BODY -> 60;
		};
	}

	/**
	 * Ce que la Pierre rend, et ce que ça lui coûte.
	 *
	 * @param restored les parties rendues
	 * @param souls    les âmes dépensées
	 */
	public record Plan(Set<BodyPart> restored, int souls) {
		public Plan {
			restored = restored.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(restored));
		}
	}

	/** Le plus grave d'abord (le corps, puis la vue, les bras…), tant qu'il reste des âmes. */
	public static Plan plan(Set<BodyPart> lost, int souls) {
		List<BodyPart> order = new ArrayList<>(lost);
		order.sort(Comparator.comparingInt(Restoration::cost).reversed().thenComparing(Enum::ordinal));
		Set<BodyPart> restored = EnumSet.noneOf(BodyPart.class);
		int spent = 0;
		for (BodyPart part : order) {
			if (spent + cost(part) <= souls) {
				restored.add(part);
				spent += cost(part);
			}
		}
		return new Plan(restored, spent);
	}
}
