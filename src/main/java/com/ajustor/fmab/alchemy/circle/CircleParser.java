package com.ajustor.fmab.alchemy.circle;

import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Découpe un tracé en étages et y reconnaît les glyphes.
 *
 * <ol>
 *   <li>Les anneaux sont les cercles centrés sur le carnet et assez grands ; chacun ferme un étage,
 *   lu du centre vers l'extérieur.</li>
 *   <li>Un polygone dont tous les sommets touchent un anneau appartient à cet étage ; on peut en
 *   superposer plusieurs.</li>
 *   <li>Un petit cercle centré sur un sommet de polygone est un satellite ; ce qu'il contient lui
 *   appartient.</li>
 *   <li>Un trait qui va d'un anneau au suivant est une liaison : simple, doublé d'un trait parallèle,
 *   ou brisé (plusieurs segments alignés).</li>
 *   <li>Un point posé sur un anneau est un point d'intensité.</li>
 *   <li>Le reste est regroupé en glyphes : deux traits dont les boîtes englobantes se touchent
 *   appartiennent au même glyphe. Un groupe collé à un anneau est essayé comme modificateur
 *   (la flèche de direction), sinon comme élément ou action.</li>
 * </ol>
 */
public final class CircleParser {
	/** Un anneau fait au moins ce rayon (en cases) : en dessous, c'est un trait de glyphe. */
	public static final double MIN_RING_RADIUS = 5;
	/** Un satellite fait au moins ce rayon : en dessous, il n'y a pas la place d'y écrire. */
	public static final double MIN_SATELLITE_RADIUS = 2.5;
	/** Écart accepté entre le centre d'un anneau et le centre du carnet. */
	private static final double CENTER_TOLERANCE = 0.75;
	/** Distance à l'anneau en deçà de laquelle un sommet, un point ou une flèche est « sur » lui. */
	private static final double ON_RING = 1.0;
	/** Deux anneaux plus proches que ça sont le même, tracé deux fois. */
	private static final double SAME_RING = 1.0;
	/** Marge des boîtes englobantes pour regrouper les traits d'un même glyphe. */
	private static final double GROUP_MARGIN = 0.5;
	/** Deux traits d'une liaison doublée : presque parallèles et proches. */
	private static final double DOUBLE_LINK_ANGLE = Math.toRadians(15);
	private static final double DOUBLE_LINK_DISTANCE = 2.5;
	/** Segments d'un trait brisé : alignés à ce point près. */
	private static final double DASH_ANGLE = Math.toRadians(8);
	private static final double DASH_OFFSET = 0.6;

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
		int[] polygons = new int[n];
		int[] capacity = new int[n];
		int[] intensity = new int[n];
		Double[] direction = new Double[n];
		List<List<Vec2>> vertices = new ArrayList<>();
		List<List<PlacedGlyph>> glyphs = new ArrayList<>();
		List<List<Satellite>> satellites = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			vertices.add(new ArrayList<>());
			glyphs.add(new ArrayList<>());
			satellites.add(new ArrayList<>());
		}

		// 2. Polygones inscrits.
		List<Primitive> afterPolygons = new ArrayList<>();
		for (Primitive p : rest) {
			if (p instanceof Primitive.Polygon poly) {
				int ring = inscribedIn(poly, rings);
				if (ring >= 0) {
					sides[ring] = Math.max(sides[ring], poly.points().size());
					polygons[ring]++;
					capacity[ring] += poly.points().size();
					if (poly.points().size() == 3) {
						triangles[ring]++;
					}
					vertices.get(ring).addAll(poly.points());
					continue;
				}
				if (isLarge(poly)) {
					issues.add(CircleIssue.at(CircleIssue.Kind.LOOSE_POLYGON, poly.centroid()));
					continue;
				}
			}
			afterPolygons.add(p);
		}

		// 3. Satellites : un petit cercle sur un sommet, et tout ce qu'il entoure.
		List<SatelliteDraft> drafts = new ArrayList<>();
		List<Primitive> afterSatellites = new ArrayList<>();
		for (Primitive p : afterPolygons) {
			if (p instanceof Primitive.Circle c && c.radius() >= MIN_SATELLITE_RADIUS && c.radius() < MIN_RING_RADIUS) {
				int ring = vertexRing(c.center(), vertices);
				if (ring >= 0) {
					drafts.add(new SatelliteDraft(ring, c));
					continue;
				}
			}
			afterSatellites.add(p);
		}

		// 4. Liaisons entre anneaux voisins.
		LinkKind[] links = new LinkKind[n];
		Arrays.fill(links, LinkKind.NONE);
		List<Primitive> afterLinks = links(afterSatellites, rings, links, issues);

		// 5. Points d'intensité.
		List<Primitive> strokes = new ArrayList<>();
		for (Primitive p : afterLinks) {
			if (p instanceof Primitive.Dot dot && !insideAny(dot.at(), drafts)) {
				int ring = onRing(dot.at(), rings);
				if (ring >= 0) {
					intensity[ring]++;
					continue;
				}
			}
			strokes.add(p);
		}

		// 6. Contenu des satellites. S'il est illisible, ce n'était pas un satellite mais un
		// glyphe qui en avait l'air (le cercle du sel posé sur un sommet, par exemple).
		for (SatelliteDraft draft : drafts) {
			List<Primitive> inside = new ArrayList<>();
			for (Primitive p : strokes) {
				if (centroid(List.of(p)).distance(draft.circle.center()) < draft.circle.radius()) {
					inside.add(p);
				}
			}
			List<PlacedGlyph> found = new ArrayList<>();
			boolean readable = true;
			for (List<Primitive> group : group(inside)) {
				Optional<GlyphRecognizer.Match> m = recognizer.recognize(group)
						.filter(x -> x.glyph().layer() != GlyphLayer.MODIFIER);
				if (m.isEmpty()) {
					readable = false;
					break;
				}
				found.add(new PlacedGlyph(m.get().glyph(), centroid(group).sub(Drawing.CENTER_POINT),
						m.get().rotationDeg()));
			}
			if (readable) {
				strokes.removeAll(inside);
				satellites.get(draft.ring).add(new Satellite(draft.circle.center().sub(Drawing.CENTER_POINT),
						draft.circle.radius(), found));
			} else {
				strokes.add(draft.circle);
			}
		}

		// 7. Glyphes.
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
			stages.add(new Stage(i, rings.get(i), Math.max(sides[i], hexagram ? 6 : 0), hexagram, polygons[i],
					capacity[i], glyphs.get(i), intensity[i],
					direction[i] == null ? OptionalDouble.empty() : OptionalDouble.of(direction[i]),
					links[i], satellites.get(i)));
		}
		return new ParsedCircle(stages, issues);
	}

	private record SatelliteDraft(int ring, Primitive.Circle circle) {
	}

	private record Segment(Primitive.Line line, Vec2 inner, Vec2 outer) {
		double angle() {
			return outer.sub(inner).angle();
		}

		Vec2 middle() {
			return inner.add(outer).scale(0.5);
		}
	}

	/**
	 * Repère les liaisons entre chaque anneau et le suivant, note leur type dans {@code links}
	 * (indexé par l'étage extérieur) et rend les traits qui n'en sont pas.
	 */
	private static List<Primitive> links(List<Primitive> strokes, List<Double> rings, LinkKind[] links,
			List<CircleIssue> issues) {
		List<Primitive> rest = new ArrayList<>(strokes);
		for (int i = 0; i + 1 < rings.size(); i++) {
			double inner = rings.get(i), outer = rings.get(i + 1);
			Set<LinkKind> kinds = EnumSet.noneOf(LinkKind.class);
			Vec2 where = null;

			// Traits continus d'un anneau à l'autre.
			List<Segment> whole = new ArrayList<>();
			for (Primitive p : rest) {
				if (p instanceof Primitive.Line line) {
					Segment s = oriented(line);
					if (Math.abs(radius(s.inner) - inner) <= ON_RING && Math.abs(radius(s.outer) - outer) <= ON_RING) {
						whole.add(s);
					}
				}
			}
			boolean[] paired = new boolean[whole.size()];
			for (int a = 0; a < whole.size(); a++) {
				for (int b = a + 1; b < whole.size() && !paired[a]; b++) {
					if (!paired[b] && angleBetween(whole.get(a).angle(), whole.get(b).angle()) <= DOUBLE_LINK_ANGLE
							&& whole.get(a).middle().distance(whole.get(b).middle()) <= DOUBLE_LINK_DISTANCE) {
						paired[a] = paired[b] = true;
						kinds.add(LinkKind.PARALLEL);
					}
				}
				if (!paired[a]) {
					kinds.add(LinkKind.SERIES);
				}
				where = whole.get(a).middle();
				rest.remove(whole.get(a).line);
			}

			// Traits brisés : plusieurs segments alignés qui, ensemble, vont d'un anneau à l'autre.
			List<Segment> pieces = new ArrayList<>();
			for (Primitive p : rest) {
				if (p instanceof Primitive.Line line) {
					Segment s = oriented(line);
					if (radius(s.inner) >= inner - ON_RING && radius(s.outer) <= outer + ON_RING) {
						pieces.add(s);
					}
				}
			}
			boolean[] used = new boolean[pieces.size()];
			for (int a = 0; a < pieces.size(); a++) {
				if (used[a]) {
					continue;
				}
				List<Segment> dash = new ArrayList<>(List.of(pieces.get(a)));
				for (int b = a + 1; b < pieces.size(); b++) {
					if (!used[b] && aligned(pieces.get(a), pieces.get(b))) {
						dash.add(pieces.get(b));
					}
				}
				if (dash.size() < 2) {
					continue;
				}
				double from = dash.stream().mapToDouble(s -> radius(s.inner)).min().orElseThrow();
				double to = dash.stream().mapToDouble(s -> radius(s.outer)).max().orElseThrow();
				if (Math.abs(from - inner) <= ON_RING && Math.abs(to - outer) <= ON_RING) {
					kinds.add(LinkKind.CONDITIONAL);
					where = dash.getFirst().middle();
					for (int b = a; b < pieces.size(); b++) {
						if (dash.contains(pieces.get(b))) {
							used[b] = true;
							rest.remove(pieces.get(b).line);
						}
					}
				}
			}

			if (kinds.size() > 1) {
				issues.add(CircleIssue.at(CircleIssue.Kind.CONFLICTING_LINKS, where));
			}
			links[i + 1] = kinds.isEmpty() ? LinkKind.NONE : kinds.iterator().next();
		}
		return rest;
	}

	private static Segment oriented(Primitive.Line line) {
		return radius(line.from()) <= radius(line.to())
				? new Segment(line, line.from(), line.to())
				: new Segment(line, line.to(), line.from());
	}

	/** Même droite, à peu de chose près. */
	private static boolean aligned(Segment a, Segment b) {
		if (angleBetween(a.angle(), b.angle()) > DASH_ANGLE) {
			return false;
		}
		Vec2 dir = a.outer.sub(a.inner);
		double len = dir.length();
		if (len < 1e-9) {
			return false;
		}
		Vec2 rel = b.middle().sub(a.inner);
		double cross = Math.abs(dir.x() * rel.y() - dir.y() * rel.x()) / len;
		return cross <= DASH_OFFSET;
	}

	private static double angleBetween(double a, double b) {
		double d = Math.abs(a - b) % (2 * Math.PI);
		return Math.min(d, 2 * Math.PI - d);
	}

	private static double radius(Vec2 p) {
		return p.distance(Drawing.CENTER_POINT);
	}

	/** Anneau dont le polygone touche le bord par tous ses sommets, ou −1. */
	private static int inscribedIn(Primitive.Polygon poly, List<Double> rings) {
		for (int i = 0; i < rings.size(); i++) {
			double r = rings.get(i);
			boolean all = poly.points().stream().allMatch(v -> Math.abs(radius(v) - r) <= ON_RING);
			if (all) {
				return i;
			}
		}
		return -1;
	}

	/** Étage dont un sommet de polygone est sous ce point, ou −1. */
	private static int vertexRing(Vec2 p, List<List<Vec2>> vertices) {
		for (int i = 0; i < vertices.size(); i++) {
			for (Vec2 v : vertices.get(i)) {
				if (v.distance(p) <= ON_RING) {
					return i;
				}
			}
		}
		return -1;
	}

	private static boolean insideAny(Vec2 p, List<SatelliteDraft> drafts) {
		return drafts.stream().anyMatch(d -> d.circle.center().distance(p) < d.circle.radius());
	}

	/** Un polygone qui couvre une bonne part du carnet ne peut pas être un glyphe. */
	private static boolean isLarge(Primitive.Polygon poly) {
		Vec2 c = poly.centroid();
		return poly.points().stream().anyMatch(v -> v.distance(c) >= MIN_RING_RADIUS);
	}

	private static int onRing(Vec2 p, List<Double> rings) {
		double d = radius(p);
		for (int i = 0; i < rings.size(); i++) {
			if (Math.abs(d - rings.get(i)) <= ON_RING) {
				return i;
			}
		}
		return -1;
	}

	/** L'étage d'un point : le premier anneau qui le contient, en partant du centre. */
	private static int stageOf(Vec2 p, List<Double> rings) {
		double d = radius(p);
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
		Arrays.fill(index, -1);
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
