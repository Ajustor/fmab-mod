package com.ajustor.fmab.stone;

import java.util.Map;
import java.util.TreeMap;

/**
 * La matière qu'une Pierre vivante a tirée des projectiles, élément par élément, sans en avoir
 * encore fait une pièce entière : une flèche, c'est le quart d'un silex, d'un bâton et d'une plume.
 * Les fractions s'additionnent d'un projectile à l'autre ; rien ne se perd.
 */
public final class MassBalance {
	/** Ce qu'un calcul en virgule flottante peut avoir rogné sur une masse entière. */
	private static final double ROUNDING = 1e-9;

	private final Map<String, Double> masses = new TreeMap<>();

	/** Verse la matière de {@code count} objets faits de ces masses. */
	public void add(Map<String, Double> masses, int count) {
		masses.forEach((element, mass) -> add(element, mass * count));
	}

	/** Verse de la matière d'un élément, comme celle qu'on n'a pas pu rendre en pièces. */
	public void add(String element, double mass) {
		masses.merge(element, mass, Double::sum);
	}

	/** Retire et rend la masse entière de chaque élément ; les fractions attendent la suite. */
	public Map<String, Integer> takeWhole() {
		Map<String, Integer> whole = new TreeMap<>();
		masses.replaceAll((element, mass) -> {
			int taken = (int) Math.floor(mass + ROUNDING);
			if (taken <= 0) {
				return mass;
			}
			whole.put(element, taken);
			return Math.max(0, mass - taken);
		});
		return whole;
	}

	/** Ce qui reste d'un élément. */
	public double of(String element) {
		return masses.getOrDefault(element, 0.0);
	}
}
