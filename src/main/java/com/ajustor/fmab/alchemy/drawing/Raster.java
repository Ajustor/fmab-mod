package com.ajustor.fmab.alchemy.drawing;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Rastérise un tracé en masque de pixels. Le même masque sert à l'aperçu du carnet et à la
 * texture posée sur le sol : ce que le joueur voit est exactement ce qu'il a dessiné.
 */
public final class Raster {
	private Raster() {
	}

	/**
	 * @param size      côté de l'image en pixels
	 * @param thickness épaisseur du trait en pixels
	 * @return masque ligne par ligne, {@code mask[y * size + x]}
	 */
	public static boolean[] rasterize(Drawing drawing, int size, double thickness) {
		boolean[] mask = new boolean[size * size];
		trace(drawing.primitives(), size, thickness, i -> mask[i] = true);
		return mask;
	}

	/**
	 * Le même tracé, pixel par pixel : {@code pixel} reçoit l'indice ({@code y * size + x}) de chaque
	 * pixel couvert (parfois plusieurs fois). Sert aux dessins en couleurs, trait par trait.
	 */
	public static void trace(List<Primitive> primitives, int size, double thickness, IntConsumer pixel) {
		double scale = size / (double) Drawing.GRID;
		double radius = Math.max(0.5, thickness / 2);
		for (Primitive p : primitives) {
			if (p instanceof Primitive.Dot dot) {
				stamp(pixel, size, dot.at().scale(scale), radius * 2.2);
				continue;
			}
			// Un pas d'un demi-pixel suffit pour qu'un trait continu ne laisse pas de trou.
			Vec2 prev = null;
			for (Vec2 v : p.sample(0.5 / scale)) {
				Vec2 px = v.scale(scale);
				if (prev != null && prev.distance(px) > radius) {
					line(pixel, size, prev, px, radius);
				} else {
					stamp(pixel, size, px, radius);
				}
				prev = px;
			}
		}
	}

	private static void line(IntConsumer pixel, int size, Vec2 a, Vec2 b, double radius) {
		int n = Math.max(1, (int) Math.ceil(a.distance(b) / 0.5));
		for (int i = 0; i <= n; i++) {
			double t = (double) i / n;
			stamp(pixel, size, new Vec2(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t), radius);
		}
	}

	private static void stamp(IntConsumer pixel, int size, Vec2 c, double radius) {
		int x0 = (int) Math.floor(c.x() - radius), x1 = (int) Math.ceil(c.x() + radius);
		int y0 = (int) Math.floor(c.y() - radius), y1 = (int) Math.ceil(c.y() + radius);
		double r2 = radius * radius;
		for (int y = Math.max(0, y0); y <= Math.min(size - 1, y1); y++) {
			for (int x = Math.max(0, x0); x <= Math.min(size - 1, x1); x++) {
				double dx = x + 0.5 - c.x(), dy = y + 0.5 - c.y();
				if (dx * dx + dy * dy <= r2) {
					pixel.accept(y * size + x);
				}
			}
		}
	}
}
