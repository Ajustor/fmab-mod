package com.ajustor.fmab.xing;

import java.util.ArrayList;
import java.util.List;

/**
 * La géométrie d'un cercle de kunaï, vu de haut : des points (x, z) autour d'un centre. Ils
 * referment un cercle s'ils sont assez nombreux, chacun à bonne distance du centre, et sans trou
 * trop large dans le tour.
 */
public final class KunaiRing {
	/** Les kunaï d'un cercle. */
	public static final int POINTS = 5;
	static final double MIN_RADIUS = 1.5;
	static final double MAX_RADIUS = 9;
	/** Plus grand écart d'angle entre deux kunaï voisins : au-delà, le cercle ne se referme pas. */
	static final double MAX_GAP = Math.toRadians(150);

	private KunaiRing() {
	}

	/**
	 * @param points les kunaï, en {x, z}
	 * @return le rayon du cercle, ou −1 s'il ne se referme pas
	 */
	public static double radius(List<double[]> points, double cx, double cz) {
		if (points.size() < POINTS) {
			return -1;
		}
		List<Double> angles = new ArrayList<>();
		double radius = 0;
		for (double[] p : points) {
			double d = Math.hypot(p[0] - cx, p[1] - cz);
			if (d < MIN_RADIUS || d > MAX_RADIUS) {
				return -1;
			}
			radius = Math.max(radius, d);
			angles.add(Math.atan2(p[1] - cz, p[0] - cx));
		}
		angles.sort(Double::compare);
		for (int i = 0; i < angles.size(); i++) {
			double next = i + 1 < angles.size() ? angles.get(i + 1) : angles.getFirst() + 2 * Math.PI;
			if (next - angles.get(i) > MAX_GAP) {
				return -1;
			}
		}
		return radius;
	}
}
