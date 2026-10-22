package com.ajustor.fmab.homunculus;

import com.ajustor.fmab.stone.Recruitment;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Les règles des homonculus : ils ne s'en prennent qu'à qui menace les plans de Père, un Envy vaincu
 * ne revient plus hanter son vainqueur, un homonculus errant que personne n'approche finit par partir,
 * et le noyau de Pierre ne tombe que du premier homonculus de chaque sorte qu'on abat. Fonctions
 * pures, appliquées par {@link EnvySpawner} et les homonculus eux-mêmes.
 */
public final class HomunculusRules {
	/** Un homonculus errant laissé seul ce nombre de ticks s'en va (cinq minutes). */
	public static final int VANISH_AFTER = 20 * 60 * 5;
	/** En deçà, un joueur compte comme « proche ». */
	public static final double NEAR = 64;

	private HomunculusRules() {
	}

	/**
	 * Ce joueur menace-t-il les plans de Père ? Les homonculus ne perdent pas leur temps avec les
	 * humains ordinaires : ils ne s'en prennent qu'à qui les a frappés, porte une Pierre vivante, a
	 * refusé l'offre de Père, a vu tomber l'un des leurs ou a scellé un point de sang du cercle
	 * national. Une recrue de Père n'est menacée que si elle frappe.
	 *
	 * @param answer   sa réponse à l'offre de Père ({@link Recruitment#ACCEPTED}, {@link Recruitment#REFUSED} ou rien)
	 * @param souls    les âmes de sa Pierre vivante
	 * @param slain    les homonculus qu'il a vus tomber
	 * @param saboteur il a scellé un point de sang
	 * @param struckUs il a frappé cet homonculus
	 */
	public static boolean threatens(@Nullable String answer, int souls, Set<String> slain, boolean saboteur,
			boolean struckUs) {
		if (struckUs) {
			return true;
		}
		if (Recruitment.ACCEPTED.equals(answer)) {
			return false;
		}
		return souls > 0 || Recruitment.REFUSED.equals(answer) || !slain.isEmpty() || saboteur;
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
