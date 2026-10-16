package com.ajustor.fmab.homunculus;

import java.util.Set;

/**
 * Les règles qui empêchent les homonculus de pulluler : un Envy vaincu ne revient plus hanter son
 * vainqueur, un homonculus errant que personne n'approche finit par partir, et le noyau de Pierre ne
 * tombe que du premier homonculus de chaque sorte qu'on abat. Fonctions pures, appliquées par
 * {@link EnvySpawner} et les homonculus eux-mêmes.
 */
public final class HomunculusRules {
	/** Un homonculus errant laissé seul ce nombre de ticks s'en va (cinq minutes). */
	public static final int VANISH_AFTER = 20 * 60 * 5;
	/** En deçà, un joueur compte comme « proche ». */
	public static final double NEAR = 64;

	private HomunculusRules() {
	}

	/** Envy vient-il encore rôder autour d'un joueur ? Plus une fois qu'il l'a vu tomber. */
	public static boolean envyHaunts(Set<String> slain) {
		return !slain.contains("envy");
	}

	/** Le noyau de Pierre : seulement pour le premier homonculus de cette sorte que le joueur abat. */
	public static boolean dropsCore(Set<String> slainByKiller, String kind) {
		return !slainByKiller.contains(kind);
	}

	/** Le temps passé seul, après une seconde de plus avec ou sans joueur proche. */
	public static int alone(int ticks, boolean playerNear, int step) {
		return playerNear ? 0 : ticks + step;
	}

	/** Seul depuis assez longtemps pour s'en aller. */
	public static boolean vanishes(int aloneTicks) {
		return aloneTicks >= VANISH_AFTER;
	}
}
