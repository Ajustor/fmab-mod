package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Découpe un tracé en étages et y reconnaît les glyphes.
 *
 * <ol>
 *   <li>Les anneaux sont les cercles centrés sur le carnet et assez grands ; chacun ferme un étage,
 *   lu du centre vers l'extérieur.</li>
 *   <li>Un polygone dont tous les sommets touchent un anneau est le polygone de cet étage.</li>
 *   <li>Un point posé sur un anneau est un point d'intensité.</li>
 *   <li>Le reste est regroupé en glyphes : deux traits dont les boîtes englobantes se touchent
 *   appartiennent au même glyphe. Un groupe collé à un anneau est essayé comme modificateur
 *   (la flèche de direction), sinon comme élément ou action.</li>
 * </ol>
 */
public final class CircleParser {
	/** Un anneau fait au moins ce rayon (en cases) : en dessous, c'est un trait de glyphe. */
	public static final double MIN_RING_RADIUS = 5;
	/** Écart accepté entre le centre d'un anneau et le centre du carnet. */
	private static final double CENTER_TOLERANCE = 0.75;
	/** Distance à l'anneau en deçà de laquelle un sommet, un point ou une flèche est « sur » lui. */
	private static final double ON_RING = 1.0;
	/** Deux anneaux plus proches que ça sont le même, tracé deux fois. */
	private static final double SAME_RING = 1.0;
	/** Marge des boîtes englobantes pour regrouper les traits d'un même glyphe. */
	private static final double GROUP_MARGIN = 0.5;

	private final GlyphRecognizer recognizer;

	public CircleParser(GlyphRecognizer recognizer) {
		this.recognizer = recognizer;
	}

	public ParsedCircle parse(Drawing drawing) {
		List<CircleIssue> issues = new ArrayList<>();
		List<Primitive> rest = new ArrayList<>();
		List<Double> rings = new ArrayList<>();

		// 1. Anneaux.
		for (Primitive p : drawing.primitives()) {
			if (p instanceof Primitive.Circle c
					&& c.center().distance(Drawing.CENTER_POINT) <= CENTER_TOLERANCE
					&& c.radius() >= MIN_RING_RADIUS) {
				if (rings.stream().noneMatch(r -> Math.abs(r - c.radius()) < SAME_RING)) {
					rings.add(c.radius());
				}
			} else {
				rest.add(p);
			}
		}
		if (rings.isEmpty()) {
			issues.add(CircleIssue.of(CircleIssue.Kind.NO_RING));
			return new ParsedCircle(List.of(), issues);
		}
		rings.sort(Comparator.naturalOrder());

		int n = rings.size();
		int[] sides = new int[n];
		int[] triangles = new int[n];
		int[] intensity = new int[n];
		Double[] direction = new Double[n];
		List<List<PlacedGlyph>> glyphs = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			glyphs.add(new ArrayList<>());
		}

		// 2 et 3. Polygones inscrits et points d'intensité.
		List<Primitive> strokes = new ArrayList<>();
		for (Primitive p : rest) {
			if (p instanceof Primitive.Polygon poly) {
				int ring = inscribedIn(poly, rings);
				if (ring >= 0) {
					sides[ring] = Math.max(sides[ring], poly.points().size());
					if (poly.points().size() == 3) {
						triangles[ring]++;
					}
					continue;
				}
				if (isLarge(poly)) {
					issues.add(CircleIssue.at(CircleIssue.Kind.LOOSE_POLYGON, poly.centroid()));
					continue;
				}
			}
			if (p instanceof Primitive.Dot dot) {
				int ring = onRing(dot.at(), rings);
				if (ring >= 0) {
					intensity[ring]++;
					continue;
				}
			}
			strokes.add(p);
		}

		// 4. Glyphes.
		for (List<Primitive> group : group(strokes)) {
			Vec2 at = centroid(group);
			int ring = onRing(at, rings);
			if (ring >= 0) {
				Optional<GlyphRecognizer.Match> modifier = recognizer.recognize(group, GlyphLayer.MODIFIER);
				if (modifier.isPresent() && modifier.get().glyph().role().equals("direction")) {
					direction[ring] = at.sub(Drawing.CENTER_POINT).angle();
					continue;
				}
			}
			Optional<GlyphRecognizer.Match> match = recognizer.recognize(group)
					.filter(m -> m.glyph().layer() != GlyphLayer.MODIFIER);
			if (match.isEmpty()) {
				issues.add(CircleIssue.at(CircleIssue.Kind.UNKNOWN_GLYPH, at));
				continue;
			}
			int stage = stageOf(at, rings);
			if (stage < 0) {
				issues.add(CircleIssue.at(CircleIssue.Kind.OUTSIDE_RING, at));
				continue;
			}
			glyphs.get(stage).add(new PlacedGlyph(match.get().glyph(), at.sub(Drawing.CENTER_POINT),
					match.get().rotationDeg()));
		}

		List<Stage> stages = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			boolean hexagram = triangles[i] >= 2;
			stages.add(new Stage(i, rings.get(i), hexagram ? 6 : sides[i], hexagram, glyphs.get(i), intensity[i],
					direction[i] == null ? OptionalDouble.empty() : OptionalDouble.of(direction[i])));
		}
		return new ParsedCircle(stages, issues);
	}

	/** Anneau dont le polygone touche le bord par tous ses sommets, ou −1. */
	private static int inscribedIn(Primitive.Polygon poly, List<Double> rings) {
		for (int i = 0; i < rings.size(); i++) {
			double r = rings.get(i);
			boolean all = poly.points().stream()
					.allMatch(v -> Math.abs(v.distance(Drawing.CENTER_POINT) - r) <= ON_RING);
			if (all) {
				return i;
			}
		}
		return -1;
	}

	/** Un polygone qui couvre une bonne part du carnet ne peut pas être un glyphe. */
	private static boolean isLarge(Primitive.Polygon poly) {
		Vec2 c = poly.centroid();
		return poly.points().stream().anyMatch(v -> v.distance(c) >= MIN_RING_RADIUS);
	}

	private static int onRing(Vec2 p, List<Double> rings) {
		double d = p.distance(Drawing.CENTER_POINT);
		for (int i = 0; i < rings.size(); i++) {
			if (Math.abs(d - rings.get(i)) <= ON_RING) {
				return i;
			}
		}
		return -1;
	}

	/** L'étage d'un point : le premier anneau qui le contient, en partant du centre. */
	private static int stageOf(Vec2 p, List<Double> rings) {
		double d = p.distance(Drawing.CENTER_POINT);
		for (int i = 0; i < rings.size(); i++) {
			if (d < rings.get(i)) {
				return i;
			}
		}
		return -1;
	}

	private static Vec2 centroid(List<Primitive> group) {
		double x = 0, y = 0;
		int count = 0;
		for (Primitive p : group) {
			for (Vec2 v : p.sample(0.25)) {
				x += v.x();
				y += v.y();
				count++;
			}
		}
		return new Vec2(x / count, y / count);
	}

	/** Regroupe les traits dont les boîtes englobantes (élargies) se chevauchent. */
	static List<List<Primitive>> group(List<Primitive> strokes) {
		int n = strokes.size();
		double[][] boxes = new double[n][];
		for (int i = 0; i < n; i++) {
			boxes[i] = box(strokes.get(i));
		}
		int[] parent = new int[n];
		for (int i = 0; i < n; i++) {
			parent[i] = i;
		}
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (overlap(boxes[i], boxes[j])) {
					parent[find(parent, i)] = find(parent, j);
				}
			}
		}
		List<List<Primitive>> groups = new ArrayList<>();
		int[] index = new int[n];
		java.util.Arrays.fill(index, -1);
		for (int i = 0; i < n; i++) {
			int root = find(parent, i);
			if (index[root] < 0) {
				index[root] = groups.size();
				groups.add(new ArrayList<>());
			}
			groups.get(index[root]).add(strokes.get(i));
		}
		return groups;
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}
		return i;
	}

	private static double[] box(Primitive p) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (Vec2 v : p.sample(0.25)) {
			minX = Math.min(minX, v.x());
			maxX = Math.max(maxX, v.x());
			minY = Math.min(minY, v.y());
			maxY = Math.max(maxY, v.y());
		}
		return new double[]{minX - GROUP_MARGIN, minY - GROUP_MARGIN, maxX + GROUP_MARGIN, maxY + GROUP_MARGIN};
	}

	private static boolean overlap(double[] a, double[] b) {
		return a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3];
	}
}
