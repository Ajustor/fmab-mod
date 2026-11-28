package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.gate.BodyPart;

import java.util.Locale;
import java.util.Optional;

/**
 * Emplacements de tatouage. Les paumes portent un cercle qu'on lance comme celui d'un gant ; les
 * autres portent des cercles passifs. Le tour de chaque bras, de l'épaule au poignet ({@link Sleeve}),
 * porte plusieurs cercles : c'est là que se tatoue le motif d'Ishval, le bras de Scar.
 *
 * @param stages étages que la peau peut porter à cet endroit, par cercle
 * @param active le cercle se lance (paumes) au lieu d'agir en permanence
 * @param left   pour un bras : le gauche
 */
public enum TattooSlot {
	PALM_LEFT(2, true, true),
	PALM_RIGHT(2, true, false),
	ARM_LEFT(2, false, true),
	ARM_RIGHT(2, false, false),
	BACK(5, false, false),
	CHEST(3, false, false);

	/** L'identifiant des bras avant qu'on les tatoue tout autour : seul l'avant-bras se tatouait. */
	private static final String LEGACY_FOREARM = "forearm_";

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

	/** Cercles que l'emplacement porte au plus : plusieurs autour d'un bras, un ailleurs. */
	public int maxStamps() {
		return sleeve() ? BodyArt.MAX_STAMPS : 1;
	}

	/** Le tour d'un bras ? */
	public boolean sleeve() {
		return this == ARM_LEFT || this == ARM_RIGHT;
	}

	/** La page où l'on dessine cet emplacement. */
	public Canvas canvas() {
		return sleeve() ? Canvas.SLEEVE : Canvas.SQUARE;
	}

	/**
	 * Ce dessin, avec un cercle de plus posé en tampon : il remplace le cercle d'un emplacement qui
	 * n'en porte qu'un (au milieu de la page, aussi grand qu'elle) ; autour d'un bras, il s'ajoute aux
	 * autres, sur une face où il n'y en a pas encore.
	 */
	public BodyArt stamp(BodyArt before, Drawing circle, int color) {
		if (!sleeve()) {
			return before.withStamp(BodyArt.fullStamp(circle, color, canvas()));
		}
		return before.plusStamp(new BodyArt.Stamp(circle, Sleeve.freeSpot(before.stamps()), Sleeve.FACE / 2.0, color));
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

	/** Le tour d'un bras. */
	public static TattooSlot sleeve(boolean leftArm) {
		return leftArm ? ARM_LEFT : ARM_RIGHT;
	}

	public boolean isLeft() {
		return left;
	}

	/** Le bras qui porte cet emplacement ; aucun pour le dos et le torse. */
	public Optional<BodyPart> arm() {
		return switch (this) {
			case PALM_LEFT, ARM_LEFT -> Optional.of(BodyPart.LEFT_ARM);
			case PALM_RIGHT, ARM_RIGHT -> Optional.of(BodyPart.RIGHT_ARM);
			case BACK, CHEST -> Optional.empty();
		};
	}

	/** L'endroit du bras, sans son côté ({@code palm}, {@code arm}) : là où se grave un automail. */
	public Optional<String> spot() {
		return switch (this) {
			case PALM_LEFT, PALM_RIGHT -> Optional.of("palm");
			case ARM_LEFT, ARM_RIGHT -> Optional.of("arm");
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

	/**
	 * L'emplacement d'avant le tour des bras que désignait cet identifiant : {@code forearm_left} est
	 * devenu le tour du bras gauche. Rien pour un identifiant d'aujourd'hui.
	 */
	public static Optional<TattooSlot> legacy(String id) {
		if (!id.startsWith(LEGACY_FOREARM)) {
			return Optional.empty();
		}
		return switch (id.substring(LEGACY_FOREARM.length())) {
			case "left" -> Optional.of(ARM_LEFT);
			case "right" -> Optional.of(ARM_RIGHT);
			default -> Optional.empty();
		};
	}
}
