package com.ajustor.fmab.block;

import java.util.Locale;

/**
 * Ce qui allume un cercle inscrit. On le règle accroupi, main nue, d'un clic droit sur le cercle ;
 * celui qui l'arme en devient l'auteur : c'est sa concentration (et son savoir) que le cercle
 * consume quand il part tout seul.
 */
public enum CircleTrigger {
	/** La paume contre le cercle, comme toujours. */
	HAND,
	/** La paume l'arme ; il part trois secondes plus tard, le temps de s'écarter. */
	DELAY,
	/** Un piège : il part quand une créature (autre que son auteur) marche dessus. */
	PRESSURE,
	/** Il part quand un signal de redstone l'atteint. */
	REDSTONE;

	/** Le délai d'un cercle à retardement, en ticks. */
	public static final int DELAY_TICKS = 60;
	/** Un piège ou un cercle à redstone ne repart pas avant ce délai, en ticks. */
	public static final int REARM_TICKS = 40;

	public CircleTrigger next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public String translationKey() {
		return "circle.fmab.trigger." + name().toLowerCase(Locale.ROOT);
	}

	public static CircleTrigger byOrdinal(int i) {
		return values()[Math.floorMod(i, values().length)];
	}
}
