package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.gate.BodyPart;

import java.util.Locale;
import java.util.Optional;

/**
 * Emplacements de tatouage. Les paumes portent un cercle qu'on lance comme celui d'un gant ; les
 * autres portent des cercles passifs.
 *
 * @param stages étages que la peau peut porter à cet endroit
 * @param active le cercle se lance (paumes) au lieu d'agir en permanence
 * @param left   pour une paume : la main gauche
 */
public enum TattooSlot {
	PALM_LEFT(2, true, true),
	PALM_RIGHT(2, true, false),
	FOREARM_LEFT(1, false, true),
	FOREARM_RIGHT(1, false, false),
	BACK(5, false, false),
	CHEST(3, false, false);

	private final int stages;
	private final boolean active;
	private final boolean left;

	TattooSlot(int stages, boolean active, boolean left) {
		this.stages = stages;
		this.active = active;
		this.left = left;
	}

	public int stages() {
		return stages;
	}

	public boolean active() {
		return active;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "tattoo.fmab.slot." + id();
	}

	/** La paume d'une main. */
	public static TattooSlot palm(boolean leftHand) {
		return leftHand ? PALM_LEFT : PALM_RIGHT;
	}

	public boolean isLeft() {
		return left;
	}

	/** Le bras qui porte cet emplacement ; aucun pour le dos et le torse. */
	public Optional<BodyPart> arm() {
		return switch (this) {
			case PALM_LEFT, FOREARM_LEFT -> Optional.of(BodyPart.LEFT_ARM);
			case PALM_RIGHT, FOREARM_RIGHT -> Optional.of(BodyPart.RIGHT_ARM);
			case BACK, CHEST -> Optional.empty();
		};
	}

	/** L'endroit du bras, sans son côté ({@code palm}, {@code forearm}) : là où se grave un automail. */
	public Optional<String> spot() {
		return switch (this) {
			case PALM_LEFT, PALM_RIGHT -> Optional.of("palm");
			case FOREARM_LEFT, FOREARM_RIGHT -> Optional.of("forearm");
			case BACK, CHEST -> Optional.empty();
		};
	}

	public static Optional<TattooSlot> byId(String id) {
		for (TattooSlot s : values()) {
			if (s.id().equals(id)) {
				return Optional.of(s);
			}
		}
		return Optional.empty();
	}
}
