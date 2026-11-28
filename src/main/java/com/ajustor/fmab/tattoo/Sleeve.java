package com.ajustor.fmab.tattoo;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Le tour d'un bras, de l'épaule au poignet, déplié en une page ({@link Canvas#SLEEVE}) : on y tatoue
 * tout autour du bras, comme le bras de Scar.
 *
 * <p>La page fait le tour dans le sens où chaque face se lit sans miroir vue de l'extérieur (celui
 * des autres emplacements, {@link BodySpot}). La couture tombe au milieu de la face intérieure, contre
 * le corps, là où on la voit le moins ; la face extérieure est au milieu de la page. Pour le bras
 * droit : intérieur, arrière, extérieur, avant, intérieur ; pour le gauche, son miroir : intérieur,
 * avant, extérieur, arrière, intérieur.
 *
 * <p>Chaque face a seize cases de large, quelle que soit sa largeur sur le modèle : un bras fin (trois
 * pixels de large au lieu de quatre) serre un peu ses faces avant et arrière.
 *
 * <p>Repère du modèle de joueur, comme {@link BodySpot} : en pixels de peau divisés par 16, +Y vers le
 * bas, l'avant du corps vers −Z.
 */
public final class Sleeve {
	private static final float PX = 1f / 16;
	/** Le haut de la page : l'épaule. */
	public static final float TOP = -2 * PX;
	/** Le bas de la page : le poignet. La main garde ses emplacements (paume, dos du gant). */
	public static final float BOTTOM = 6 * PX;
	/** Cases par face. */
	public static final int FACE = Drawing.GRID / 2;

	/** Les faces du bras, dans l'ordre de la page. */
	public enum Face {
		INNER, BACK, OUTER, FRONT
	}

	/**
	 * Un morceau de la page posé à plat sur une face : de ({@code x0}, {@code z0}) à ({@code x1},
	 * {@code z1}) à l'horizontale, de {@link #TOP} à {@link #BOTTOM} à la verticale, de {@code u0} à
	 * {@code u1} sur la largeur de la page (de 0 à 1), sa normale vers l'extérieur.
	 */
	public record Piece(Face face, float x0, float z0, float x1, float z1, float u0, float u1, float nx, float nz) {
	}

	private Sleeve() {
	}

	/** Les faces de la page, dans l'ordre, de gauche à droite : la face intérieure, coupée en deux, aux bords. */
	public static List<Face> order(boolean leftArm) {
		return leftArm ? List.of(Face.INNER, Face.FRONT, Face.OUTER, Face.BACK, Face.INNER)
				: List.of(Face.INNER, Face.BACK, Face.OUTER, Face.FRONT, Face.INNER);
	}

	/** La face sous cette colonne de la page. */
	public static Face faceAt(boolean leftArm, double x) {
		double shifted = x + FACE / 2.0;
		int index = (int) Math.floor(shifted / FACE);
		return order(leftArm).get(Math.clamp(index, 0, 4));
	}

	/**
	 * Les cinq morceaux de la page autour d'un bras (fin ou large), décollés de {@code lift} de la peau.
	 */
	public static List<Piece> pieces(boolean leftArm, boolean thin, float lift) {
		float side = leftArm ? 1 : -1;
		float outer = side * (thin ? 2 : 3) * PX;
		float inner = -side * PX;
		// Le sens de la marche : vers +Z depuis la face intérieure du bras droit, vers −Z pour le gauche.
		float sz = -side;
		float near = 2 * PX * sz, far = -2 * PX * sz;
		float[][] corners = {
				{inner, 0}, {inner, near}, {outer, near}, {outer, far}, {inner, far}, {inner, 0}};
		float[][] normals = {{-side, 0}, {0, sz}, {side, 0}, {0, -sz}, {-side, 0}};
		float[] bounds = {0, FACE / 2f, FACE * 1.5f, FACE * 2.5f, FACE * 3.5f, FACE * 4};
		List<Face> faces = order(leftArm);
		List<Piece> out = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			float nx = normals[i][0], nz = normals[i][1];
			out.add(new Piece(faces.get(i),
					corners[i][0] + nx * lift, corners[i][1] + nz * lift,
					corners[i + 1][0] + nx * lift, corners[i + 1][1] + nz * lift,
					bounds[i] / (FACE * 4), bounds[i + 1] / (FACE * 4), nx, nz));
		}
		return out;
	}

	/**
	 * Où poser un cercle de plus autour du bras : au milieu de la face extérieure, puis de l'arrière et
	 * de l'avant, la première qui n'a pas déjà un cercle ; toutes prises, la face extérieure.
	 */
	public static Vec2 freeSpot(List<BodyArt.Stamp> taken) {
		double y = Canvas.SLEEVE.height() / 2.0;
		List<Vec2> spots = List.of(new Vec2(FACE * 2, y), new Vec2(FACE, y), new Vec2(FACE * 3, y));
		for (Vec2 spot : spots) {
			if (taken.stream().allMatch(st -> st.center().distance(spot) >= FACE / 2.0)) {
				return spot;
			}
		}
		return spots.getFirst();
	}

	/** Cases de la page par pixel de peau, à la verticale comme sur une face large. */
	private static final double CELLS_PER_PIXEL = Canvas.SLEEVE.height() / ((BOTTOM - TOP) / PX);

	/**
	 * Un dessin de l'ancien emplacement de l'avant-bras (un carré de la page du carnet, 3,2 pixels de
	 * côté, centré à 3,5 pixels sous l'épaule, sur la face extérieure), reporté au même endroit du tour
	 * du bras. Ses tampons trop petits pour se lire encore grandissent jusqu'au plus petit lisible.
	 */
	public static BodyArt fromForearm(BodyArt forearm) {
		double oldPixel = 3.2 / Drawing.GRID;
		double k = oldPixel * CELLS_PER_PIXEL;
		Vec2 to = new Vec2(Canvas.SLEEVE.width() / 2.0, (3.5 - TOP / PX) * CELLS_PER_PIXEL);
		BodyArt moved = forearm.moved(v -> to.add(v.sub(Drawing.CENTER_POINT).scale(k)), k);
		List<BodyArt.Stamp> stamps = moved.stamps().stream()
				.map(st -> st.half() >= BodyArt.MIN_STAMP_HALF ? st
						: new BodyArt.Stamp(st.circle(), st.center(), BodyArt.MIN_STAMP_HALF, st.color()))
				.toList();
		return new BodyArt(moved.strokes(), stamps);
	}
}
