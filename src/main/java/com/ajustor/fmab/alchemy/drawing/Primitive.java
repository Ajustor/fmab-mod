package com.ajustor.fmab.alchemy.drawing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Trait élémentaire d'un dessin : ce que produisent les outils du carnet (règle, compas, polygone,
 * arc, point) et ce que décrivent les JSON de glyphes. Le même type sert aux deux côtés pour que
 * le parseur compare des choses comparables.
 */
public sealed interface Primitive {
	/** Points répartis le long du trait, à peu près tous les {@code step}. */
	List<Vec2> sample(double step);

	Primitive map(UnaryOperator<Vec2> f);

	record Line(Vec2 from, Vec2 to) implements Primitive {
		@Override
		public List<Vec2> sample(double step) {
			List<Vec2> out = new ArrayList<>();
			segment(from, to, step, out, true);
			return out;
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			return new Line(f.apply(from), f.apply(to));
		}
	}

	/** Ligne brisée ouverte (une flèche, par exemple). */
	record Polyline(List<Vec2> points) implements Primitive {
		public Polyline {
			points = List.copyOf(points);
		}

		@Override
		public List<Vec2> sample(double step) {
			List<Vec2> out = new ArrayList<>();
			for (int i = 0; i + 1 < points.size(); i++) {
				segment(points.get(i), points.get(i + 1), step, out, i + 2 == points.size());
			}
			return out;
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			return new Polyline(points.stream().map(f).toList());
		}
	}

	/** Polygone fermé, régulier ou non. */
	record Polygon(List<Vec2> points) implements Primitive {
		public Polygon {
			points = List.copyOf(points);
		}

		@Override
		public List<Vec2> sample(double step) {
			List<Vec2> out = new ArrayList<>();
			for (int i = 0; i < points.size(); i++) {
				segment(points.get(i), points.get((i + 1) % points.size()), step, out, false);
			}
			return out;
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			return new Polygon(points.stream().map(f).toList());
		}


		public Vec2 centroid() {
			double x = 0, y = 0;
			for (Vec2 p : points) {
				x += p.x();
				y += p.y();
			}
			return new Vec2(x / points.size(), y / points.size());
		}
	}

	record Circle(Vec2 center, double radius) implements Primitive {
		@Override
		public List<Vec2> sample(double step) {
			return new Arc(center, radius, 0, 360).sample(step);
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			// Les transformations utilisées (translation, rotation, homothétie uniforme) gardent
			// les cercles ronds : on transporte le centre et un point du bord.
			Vec2 c = f.apply(center);
			Vec2 edge = f.apply(center.add(new Vec2(radius, 0)));
			return new Circle(c, c.distance(edge));
		}
	}

	/** Arc de cercle ; angles en degrés, y vers le bas donc sens horaire à l'écran. */
	record Arc(Vec2 center, double radius, double startDeg, double sweepDeg) implements Primitive {
		@Override
		public List<Vec2> sample(double step) {
			double length = Math.abs(Math.toRadians(sweepDeg)) * radius;
			int n = Math.max(8, (int) Math.ceil(length / step));
			List<Vec2> out = new ArrayList<>(n + 1);
			boolean closed = Math.abs(sweepDeg) >= 360;
			int last = closed ? n - 1 : n;
			for (int i = 0; i <= last; i++) {
				double a = Math.toRadians(startDeg + sweepDeg * i / n);
				out.add(center.add(Vec2.polar(radius, a)));
			}
			return out;
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			Vec2 c = f.apply(center);
			Vec2 start = f.apply(center.add(Vec2.polar(radius, Math.toRadians(startDeg))));
			double r = c.distance(start);
			double newStart = Math.toDegrees(start.sub(c).angle());
			// Une rotation décale le départ ; une symétrie inverserait le sens, mais le parseur ne
			// fait que des rotations et des homothéties positives.
			return new Arc(c, r, newStart, sweepDeg);
		}
	}

	record Dot(Vec2 at) implements Primitive {
		@Override
		public List<Vec2> sample(double step) {
			return List.of(at);
		}

		@Override
		public Primitive map(UnaryOperator<Vec2> f) {
			return new Dot(f.apply(at));
		}
	}

	private static void segment(Vec2 a, Vec2 b, double step, List<Vec2> out, boolean includeEnd) {
		int n = Math.max(1, (int) Math.ceil(a.distance(b) / step));
		int last = includeEnd ? n : n - 1;
		for (int i = 0; i <= last; i++) {
			double t = (double) i / n;
			out.add(new Vec2(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t));
		}
	}
}
