package com.ajustor.fmab.alchemy.exchange;

/**
 * Le prix d'une réparation par l'alchimie : comme à l'enclume, une unité de la matière qui répare
 * l'objet (un lingot, une planche, un diamant…) lui rend un quart de sa durabilité. Le prix se
 * compte en masse d'échange, pour qu'un bloc vaille ses neuf lingots. Calcul en entiers : ce qui est
 * payé répare toujours au moins l'usure.
 */
public final class RepairCost {
	/** Une unité de matière rend un quart de la durabilité. */
	private static final int QUARTERS = 4;

	private RepairCost() {
	}

	/** La masse qui répare toute l'usure, arrondie au-dessus. */
	public static int massFor(int damage, int maxDamage, int unitMass) {
		if (damage <= 0 || maxDamage <= 0) {
			return 0;
		}
		return (int) Math.ceilDiv((long) QUARTERS * damage * unitMass, maxDamage);
	}

	/** La durabilité que rend cette masse, arrondie au-dessous. */
	public static int restored(int mass, int maxDamage, int unitMass) {
		if (mass <= 0 || unitMass <= 0) {
			return 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, (long) mass * maxDamage / ((long) QUARTERS * unitMass));
	}
}
