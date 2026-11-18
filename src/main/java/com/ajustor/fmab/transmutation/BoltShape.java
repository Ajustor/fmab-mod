package com.ajustor.fmab.transmutation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * La forme des éclairs d'une décharge, autour de son centre : chaque éclair part d'un point du cercle
 * et monte en zigzag vers l'extérieur, en lançant parfois une branche plus fine. La même graine
 * donne toujours la même forme ; le rendu en change toutes les deux ticks, et l'éclair crépite.
 *
 * @param segments les traits de tous les éclairs, relatifs au centre de la décharge
 */
public record BoltShape(List<Segment> segments) {
	/** Un trait d'éclair ; {@code width} est sa demi-largeur, en blocs. */
	public record Segment(Vec3 from, Vec3 to, float width, boolean branch) {
	}

	/** Demi-largeur du tronc à sa racine ; il s'affine vers la pointe. */
	static final float ROOT_WIDTH = 0.05f;
	static final float TIP_WIDTH = 0.015f;
	static final int MIN_SEGMENTS = 4;
	static final int MAX_SEGMENTS = 7;
	/** Ce qu'un trait du tronc gagne en hauteur au plus. */
	static final double MAX_RISE = 0.55;

	/**
	 * @param seed   la graine de la forme
	 * @param radius rayon du cercle d'où partent les éclairs ; 0 pour un éclair qui jaillit d'un point
	 * @param bolts  le nombre d'éclairs
	 */
	public static BoltShape of(long seed, double radius, int bolts) {
		RandomSource random = RandomSource.create(seed);
		List<Segment> segments = new ArrayList<>();
		for (int b = 0; b < bolts; b++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double r = random.nextDouble() * radius;
			Vec3 point = new Vec3(Math.cos(angle) * r, 0, Math.sin(angle) * r);
			int count = MIN_SEGMENTS + random.nextInt(MAX_SEGMENTS - MIN_SEGMENTS + 1);
			int fork = random.nextFloat() < 0.6f ? 1 + random.nextInt(count - 1) : -1;
			for (int s = 0; s < count; s++) {
				Vec3 next = point.add(
						(random.nextDouble() - 0.5) * 0.8 + Math.cos(angle) * 0.25,
						0.15 + random.nextDouble() * (MAX_RISE - 0.15),
						(random.nextDouble() - 0.5) * 0.8 + Math.sin(angle) * 0.25);
				float width = ROOT_WIDTH + (TIP_WIDTH - ROOT_WIDTH) * s / (count - 1f);
				segments.add(new Segment(point, next, width, false));
				if (s == fork) {
					branch(segments, point, angle + (random.nextBoolean() ? 1 : -1) * (0.6 + random.nextDouble() * 0.6),
							width * 0.6f, random);
				}
				point = next;
			}
		}
		return new BoltShape(List.copyOf(segments));
	}

	/** Une branche : deux ou trois traits plus fins, qui s'écartent du tronc. */
	private static void branch(List<Segment> segments, Vec3 from, double angle, float width, RandomSource random) {
		Vec3 point = from;
		for (int s = 0, n = 2 + random.nextInt(2); s < n; s++) {
			Vec3 next = point.add(
					(random.nextDouble() - 0.5) * 0.5 + Math.cos(angle) * 0.3,
					0.05 + random.nextDouble() * 0.3,
					(random.nextDouble() - 0.5) * 0.5 + Math.sin(angle) * 0.3);
			segments.add(new Segment(point, next, Math.max(TIP_WIDTH, width), true));
			point = next;
		}
	}
}
