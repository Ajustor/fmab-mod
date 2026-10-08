package com.ajustor.fmab.alchemy.drawing;

import com.ajustor.fmab.alchemy.glyph.Glyph;

import java.util.ArrayList;
import java.util.List;

/**
 * Cercles simples à un étage, construits comme le ferait un joueur dans le carnet : un anneau, un
 * polygone inscrit pointe en haut, l'élément en haut et l'action en bas. Ils servent au carnet de
 * départ et aux exemples du Traité.
 */
public final class SimpleCircles {
	private static final double RING = 14;
	private static final double GLYPH_HALF_SIZE = 3;
	private static final Vec2 TOP = new Vec2(16, 11);
	private static final Vec2 BOTTOM = new Vec2(16, 20);

	private SimpleCircles() {
	}

	/**
	 * @param sides côtés du polygone inscrit : assez pour porter la complexité des deux glyphes
	 */
	public static Drawing of(Glyph element, Glyph action, int sides) {
		List<Primitive> out = new ArrayList<>();
		out.add(new Primitive.Circle(Drawing.CENTER_POINT, RING));
		List<Vec2> vertices = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			double a = -Math.PI / 2 + 2 * Math.PI * i / sides;
			vertices.add(new Vec2(Math.round(16 + RING * Math.cos(a)), Math.round(16 + RING * Math.sin(a))));
		}
		out.add(new Primitive.Polygon(vertices));
		out.addAll(place(element, TOP));
		out.addAll(place(action, BOTTOM));
		return new Drawing(out);
	}

	/** Polygone le plus simple qui porte les deux glyphes sans rebond : un triangle si possible. */
	public static int sidesFor(Glyph element, Glyph action) {
		int load = element.complexity() + action.complexity();
		return Math.max(3, Math.min(12, load));
	}

	/** Le tracé de référence d'un glyphe, posé et accroché à la demi-case comme dans le carnet. */
	public static List<Primitive> place(Glyph glyph, Vec2 at) {
		return place(glyph, at, GLYPH_HALF_SIZE, 0);
	}

	/**
	 * Le tracé d'un glyphe à la taille et l'orientation voulues : c'est le tampon du carnet.
	 *
	 * @param halfSize    demi-largeur en cases (le glyphe de référence tient dans −1..1)
	 * @param rotationDeg rotation dans le sens horaire (y vers le bas)
	 */
	public static List<Primitive> place(Glyph glyph, Vec2 at, double halfSize, double rotationDeg) {
		double rad = Math.toRadians(rotationDeg);
		List<Primitive> out = new ArrayList<>();
		for (Primitive p : glyph.primitives()) {
			Primitive placed = p.map(v -> v.rotate(rad).scale(halfSize).add(at));
			if (placed instanceof Primitive.Arc a) {
				// Un arc tourne aussi par ses angles, pas seulement par son centre.
				placed = new Primitive.Arc(a.center(), a.radius(), a.startDeg() + rotationDeg, a.sweepDeg());
			}
			out.add(snap(placed));
		}
		return out;
	}

	private static Primitive snap(Primitive p) {
		return switch (p) {
			case Primitive.Circle c -> new Primitive.Circle(snap(c.center()), Math.max(0.5, half(c.radius())));
			case Primitive.Arc a -> new Primitive.Arc(snap(a.center()), Math.max(0.5, half(a.radius())),
					Math.round(a.startDeg() / 15) * 15.0, Math.round(a.sweepDeg() / 15) * 15.0);
			default -> p.map(SimpleCircles::snap);
		};
	}

	private static Vec2 snap(Vec2 v) {
		return new Vec2(half(v.x()), half(v.y()));
	}

	private static double half(double d) {
		return Math.round(d * 2) / 2.0;
	}
}
