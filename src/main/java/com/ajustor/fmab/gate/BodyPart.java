package com.ajustor.fmab.gate;

import java.util.Locale;

/**
 * Ce que la Vérité peut prendre. Le poids de base rend les péages légers plus fréquents ; la gravité
 * fait pencher la balance vers les péages lourds quand l'ambition grandit.
 */
public enum BodyPart {
	ORGANS(3, 1),
	LEFT_LEG(3, 1),
	RIGHT_LEG(3, 1),
	LEFT_ARM(2, 2),
	RIGHT_ARM(2, 2),
	SIGHT(1, 3),
	/** Le corps entier : l'âme survit, fixée dans une armure par un sceau de sang. */
	BODY(0, 0);

	private final int baseWeight;
	private final int severity;

	BodyPart(int baseWeight, int severity) {
		this.baseWeight = baseWeight;
		this.severity = severity;
	}

	public int baseWeight() {
		return baseWeight;
	}

	public int severity() {
		return severity;
	}

	public boolean arm() {
		return this == LEFT_ARM || this == RIGHT_ARM;
	}

	public boolean leg() {
		return this == LEFT_LEG || this == RIGHT_LEG;
	}

	/** Un membre que l'automail peut remplacer. */
	public boolean limb() {
		return arm() || leg();
	}

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "body.fmab." + serializedName();
	}

	public static BodyPart fromSerializedName(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}
}
