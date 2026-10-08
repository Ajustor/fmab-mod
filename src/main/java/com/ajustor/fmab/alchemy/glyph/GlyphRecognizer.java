package com.ajustor.fmab.alchemy.glyph;

import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reconnaît un glyphe dans un groupe de traits.
 *
 * <p>Les deux tracés (celui du joueur et la référence) sont échantillonnés en points, centrés sur
 * leur barycentre et ramenés à un rayon moyen de 1 : la taille et la position dans le cercle ne
 * comptent plus. On cherche ensuite l'orientation, dans la marge du glyphe, qui rapproche le plus
 * les deux nuages de points (distance de chanfrein dans les deux sens). Les deux sens comptent :
 * l'un détecte un trait en trop, l'autre un trait manquant, comme la barre qui distingue la Terre
 * de l'Eau.
 */
public final class GlyphRecognizer {
	/** Pas angulaire de la recherche d'orientation. */
	private static final double ROTATION_STEP_DEG = 3;
	/** Un point isolé du tracé ne peut pas s'écarter de plus de ce multiple de la marge moyenne. */
	private static final double MAX_DEVIATION_FACTOR = 3;

	private final List<Template> templates;

	public GlyphRecognizer(Collection<Glyph> glyphs) {
		// Un glyphe réduit à un point (l'Intensité) n'a pas de forme à comparer : le parseur le
		// reconnaît à sa place sur l'anneau.
		this.templates = glyphs.stream().map(Template::new).filter(t -> t.shape != null).toList();
	}

	public record Match(Glyph glyph, double score, double rotationDeg) {
	}

	/** Meilleur glyphe dans sa marge, ou rien si le tracé ne ressemble à aucun. */
	public Optional<Match> recognize(List<Primitive> strokes) {
		return recognize(strokes, null);
	}

	/** Comme {@link #recognize(List)}, en ne considérant que les glyphes de la couche donnée. */
	public Optional<Match> recognize(List<Primitive> strokes, GlyphLayer onlyLayer) {
		Shape drawn = Shape.of(strokes);
		if (drawn == null) {
			return Optional.empty();
		}
		Match best = null;
		for (Template t : templates) {
			if (onlyLayer != null && t.glyph.layer() != onlyLayer) {
				continue;
			}
			Match m = t.match(drawn);
			if (m != null && (best == null || m.score < best.score)) {
				best = m;
			}
		}
		return Optional.ofNullable(best);
	}

	/**
	 * Écart du tracé à chaque glyphe, sans appliquer les marges : sert au diagnostic (« ça
	 * ressemble à la Terre, mais trop de travers ») et au calibrage des tolérances.
	 */
	public Map<Glyph, Double> distances(List<Primitive> strokes) {
		Shape drawn = Shape.of(strokes);
		Map<Glyph, Double> out = new LinkedHashMap<>();
		if (drawn == null) {
			return out;
		}
		for (Template t : templates) {
			Match m = t.match(drawn, false);
			out.put(t.glyph, m == null ? Double.POSITIVE_INFINITY : m.score);
		}
		return out;
	}

	private static final class Template {
		final Glyph glyph;
		final Shape shape;

		Template(Glyph glyph) {
			this.glyph = glyph;
			this.shape = Shape.of(glyph.primitives());
		}

		Match match(Shape drawn) {
			return match(drawn, true);
		}

		Match match(Shape drawn, boolean enforce) {
			Tolerance tol = glyph.tolerance();
			double range = tol.anyRotation() ? 180 : tol.rotationDeg();
			Match best = null;
			for (double deg = -range; deg <= range + 1e-9; deg += ROTATION_STEP_DEG) {
				// Le tracé du joueur est tourné de −deg pour revenir à la référence : deg est
				// donc l'orientation du glyphe dessiné.
				List<Vec2> rotated = rotate(drawn.points, Math.toRadians(-deg));
				if (enforce && !tol.anyRotation() && !aspectWithin(rotated, tol)) {
					continue;
				}
				double forward = 0, backward = 0, worst = 0;
				for (Vec2 p : rotated) {
					double d = nearest(p, shape.points);
					forward += d;
					worst = Math.max(worst, d);
				}
				for (Vec2 p : shape.points) {
					double d = nearest(p, rotated);
					backward += d;
					worst = Math.max(worst, d);
				}
				double score = (forward / rotated.size() + backward / shape.points.size()) / 2;
				if (enforce && (score > tol.position() || worst > tol.position() * MAX_DEVIATION_FACTOR)) {
					continue;
				}
				if (best == null || score < best.score) {
					best = new Match(glyph, score, deg);
				}
			}
			return best;
		}

		private boolean aspectWithin(List<Vec2> rotated, Tolerance tol) {
			double drawnAspect = aspect(rotated);
			double refAspect = aspect(shape.points);
			double ratio = drawnAspect / refAspect;
			return ratio >= tol.minScale() && ratio <= tol.maxScale();
		}
	}

	/** Nuage de points normalisé : barycentre à l'origine, rayon quadratique moyen de 1. */
	private record Shape(List<Vec2> points) {
		static Shape of(List<Primitive> primitives) {
			double size = extent(primitives);
			if (size <= 0) {
				return null;
			}
			// Échantillonnage proportionnel à la taille : même densité quelle que soit l'échelle.
			double step = size / 48;
			List<Vec2> pts = new ArrayList<>();
			for (Primitive p : primitives) {
				pts.addAll(p.sample(step));
			}
			double cx = 0, cy = 0;
			for (Vec2 p : pts) {
				cx += p.x();
				cy += p.y();
			}
			Vec2 c = new Vec2(cx / pts.size(), cy / pts.size());
			double sq = 0;
			for (Vec2 p : pts) {
				double d = p.distance(c);
				sq += d * d;
			}
			double rms = Math.sqrt(sq / pts.size());
			if (rms <= 1e-9) {
				return null;
			}
			List<Vec2> norm = new ArrayList<>(pts.size());
			for (Vec2 p : pts) {
				norm.add(p.sub(c).scale(1 / rms));
			}
			return new Shape(norm);
		}

		private static double extent(List<Primitive> primitives) {
			double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
			double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
			for (Primitive p : primitives) {
				for (Vec2 v : p.sample(0.25)) {
					minX = Math.min(minX, v.x());
					maxX = Math.max(maxX, v.x());
					minY = Math.min(minY, v.y());
					maxY = Math.max(maxY, v.y());
				}
			}
			return Math.max(maxX - minX, maxY - minY);
		}
	}

	private static List<Vec2> rotate(List<Vec2> pts, double radians) {
		List<Vec2> out = new ArrayList<>(pts.size());
		for (Vec2 p : pts) {
			out.add(p.rotate(radians));
		}
		return out;
	}

	private static double nearest(Vec2 p, List<Vec2> pts) {
		double best = Double.MAX_VALUE;
		for (Vec2 q : pts) {
			double dx = p.x() - q.x(), dy = p.y() - q.y();
			double d = dx * dx + dy * dy;
			if (d < best) {
				best = d;
			}
		}
		return Math.sqrt(best);
	}

	/** Largeur sur hauteur, bornée pour qu'un trait plat ne donne pas l'infini. */
	private static double aspect(List<Vec2> pts) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (Vec2 p : pts) {
			minX = Math.min(minX, p.x());
			maxX = Math.max(maxX, p.x());
			minY = Math.min(minY, p.y());
			maxY = Math.max(maxY, p.y());
		}
		return Math.max(maxX - minX, 0.2) / Math.max(maxY - minY, 0.2);
	}
}
