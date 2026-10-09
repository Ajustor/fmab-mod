package com.ajustor.fmab.alchemy.drawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Le sceau de sang d'un joueur : un dessin qui n'appartient qu'à lui, tiré de son identifiant.
 * Double anneau, une étoile inscrite, des marques entre les anneaux et un cœur au centre. On le
 * trace dans un plastron pour y fixer une âme ; qui connaît le sceau d'un ami peut le tracer pour
 * lui.
 */
public final class SoulSeal {
	private static final double OUTER = 14;
	private static final double INNER = 11;

	private SoulSeal() {
	}

	public static Drawing of(UUID owner) {
		Random random = new Random(owner.getMostSignificantBits() ^ Long.rotateLeft(owner.getLeastSignificantBits(), 17));
		List<Primitive> out = new ArrayList<>();
		out.add(new Primitive.Circle(Drawing.CENTER_POINT, OUTER));
		out.add(new Primitive.Circle(Drawing.CENTER_POINT, INNER));

		// L'étoile : n branches, reliées une sur k (k premier avec n pour qu'elle se referme d'un trait).
		int n = 5 + random.nextInt(4);
		List<Integer> steps = new ArrayList<>();
		for (int step = 2; step <= (n - 1) / 2; step++) {
			if (gcd(n, step) == 1) {
				steps.add(step);
			}
		}
		// Sans pas possible (six branches), l'étoile devient un hexagone.
		int k = steps.isEmpty() ? 1 : steps.get(random.nextInt(steps.size()));
		double start = -Math.PI / 2 + random.nextInt(4) * Math.PI / (2 * n);
		List<Vec2> star = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			star.add(polar(INNER, start + 2 * Math.PI * ((long) i * k % n) / n));
		}
		out.add(new Primitive.Polygon(star));

		// Les marques entre les anneaux : des points ou de courts traits, à intervalles réguliers.
		int marks = 4 + random.nextInt(5);
		boolean dots = random.nextBoolean();
		double offset = random.nextDouble() * Math.PI;
		for (int i = 0; i < marks; i++) {
			double a = offset + 2 * Math.PI * i / marks;
			if (dots) {
				out.add(new Primitive.Dot(polar((OUTER + INNER) / 2, a)));
			} else {
				out.add(new Primitive.Line(polar(INNER + 0.5, a), polar(OUTER - 0.5, a)));
			}
		}

		// Le cœur : un petit cercle, barré d'une croix ou d'un trait selon le sceau.
		double heart = 2 + random.nextInt(3) * 0.5;
		out.add(new Primitive.Circle(Drawing.CENTER_POINT, heart));
		switch (random.nextInt(3)) {
			case 0 -> {
				out.add(new Primitive.Line(polar(heart + 2, Math.PI / 4), polar(heart + 2, 5 * Math.PI / 4)));
				out.add(new Primitive.Line(polar(heart + 2, 3 * Math.PI / 4), polar(heart + 2, 7 * Math.PI / 4)));
			}
			case 1 -> out.add(new Primitive.Line(polar(heart + 2, Math.PI / 2), polar(heart + 2, -Math.PI / 2)));
			default -> out.add(new Primitive.Dot(Drawing.CENTER_POINT));
		}
		return new Drawing(out);
	}

	private static Vec2 polar(double r, double angle) {
		return new Vec2(snap(16 + r * Math.cos(angle)), snap(16 + r * Math.sin(angle)));
	}

	private static double snap(double d) {
		return Math.round(d * 2) / 2.0;
	}

	private static int gcd(int a, int b) {
		return b == 0 ? a : gcd(b, a % b);
	}
}
