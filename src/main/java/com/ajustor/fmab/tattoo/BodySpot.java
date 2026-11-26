package com.ajustor.fmab.tattoo;

/**
 * Où se pose un vrai cercle sur le corps d'un joueur, dans le repère du membre qui le porte (celui du
 * modèle de joueur : en pixels de peau divisés par 16, +Y vers le bas, l'avant du corps vers −Z).
 *
 * <p>Un gant le brode au-dessus de tout, même de la manche. Un tatouage est à fleur de peau, sous la
 * couche des vêtements de la peau du joueur (gonflée d'un quart de pixel) : là où elle est opaque, elle
 * le cache, comme un vrai vêtement ; là où elle est vide, il se voit.
 *
 * <p>Le cercle se lit sans miroir vu de l'extérieur : {@link Placement#u} va de la gauche vers la droite
 * de qui le regarde, le bas de la page suit +Y.
 */
public enum BodySpot {
	/** Le dos de la main, brodé sur le gant : la face extérieure du bras, au niveau de la main. */
	HAND_BACK,
	/** La paume, tatouée : la face avant du bras, au niveau de la main. */
	PALM,
	/** L'avant-bras, tatoué : la face extérieure du bras, au-dessus de la main et du gant. */
	FOREARM,
	/** La poitrine, tatouée : la face avant du buste, sous les clavicules. */
	CHEST,
	/** Le dos, tatoué : la face arrière du buste, entre les omoplates. */
	BACK;

	/** Un pixel de peau, dans le repère du modèle. */
	private static final float PX = 1f / 16;
	/** Au-dessus de la manche (un quart de pixel) : la broderie d'un gant. */
	public static final float GLOVE_LIFT = 0.3f * PX;
	/** À fleur de peau, sous la couche des vêtements : un tatouage. */
	public static final float SKIN_LIFT = 0.1f * PX;

	/**
	 * Le carré où se dessine le cercle : son centre, l'axe horizontal de la page vu de l'extérieur, la
	 * normale de la face, et le demi-côté du carré.
	 */
	public record Placement(float x, float y, float z, float ux, float uz, float nx, float nz, float half) {
	}

	public boolean tattoo() {
		return this != HAND_BACK;
	}

	/**
	 * Le carré de ce cercle sur un bras (gauche ou non, fin ou large) ou sur le buste. Les bras du modèle
	 * de joueur : le droit de x = −3 à 1 (−2 à 1 s'il est fin), le gauche de −1 à 3 (−1 à 2), de y = −2
	 * (l'épaule) à 10 (le bout des doigts), de z = −2 à 2. Le buste : de x = −4 à 4, de y = 0 à 12.
	 */
	public Placement place(boolean leftArm, boolean thin) {
		return place(leftArm, thin, tattoo() ? SKIN_LIFT : GLOVE_LIFT);
	}

	/**
	 * Le même carré, à {@code lift} de la face : un cercle gravé dans un automail se pose au-dessus de
	 * l'acier ({@link #GLOVE_LIFT}), pas sous des vêtements qu'un automail n'a pas.
	 */
	public Placement place(boolean leftArm, boolean thin, float lift) {
		float outer = (thin ? 2 : 3) * PX;
		// La face extérieure du bras : −X pour le droit, +X pour le gauche. Vue de l'extérieur, la page
		// part de l'arrière du bras droit (+Z), de l'avant du bras gauche (−Z).
		float side = leftArm ? 1 : -1;
		return switch (this) {
			case HAND_BACK -> new Placement(side * (outer + lift), 8 * PX, 0, 0, side, side, 0, 2 * PX);
			case FOREARM -> new Placement(side * (outer + lift), 3.5f * PX, 0, 0, side, side, 0, 1.6f * PX);
			// La face avant du bras, vue de devant : la page suit +X. Son milieu dépend de la largeur du bras.
			case PALM -> new Placement(side * (thin ? 0.5f : 1f) * PX, 8 * PX, -2 * PX - lift, 1, 0, 0, -1,
					(thin ? 1.5f : 2f) * PX);
			case CHEST -> new Placement(0, 4 * PX, -2 * PX - lift, 1, 0, 0, -1, 3 * PX);
			// Vue de dos, la droite de qui regarde est la droite du joueur : la page suit −X.
			case BACK -> new Placement(0, 5.5f * PX, 2 * PX + lift, -1, 0, 0, 1, 3.5f * PX);
		};
	}

	/** L'emplacement de ce tatouage. */
	public static BodySpot of(TattooSlot slot) {
		return switch (slot) {
			case PALM_LEFT, PALM_RIGHT -> PALM;
			case FOREARM_LEFT, FOREARM_RIGHT -> FOREARM;
			case CHEST -> CHEST;
			case BACK -> BACK;
		};
	}
}
