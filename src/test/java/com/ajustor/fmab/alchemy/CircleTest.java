package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleIssue.Kind;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.Analysis.Outcome;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cercles complets, dessinés comme dans le carnet, de la lecture au verdict. */
class CircleTest {
	private static final Vec2 TOP = new Vec2(16, 11);
	private static final Vec2 BOTTOM = new Vec2(16, 20);

	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@Test
	void theFartherItReachesTheMoreItCosts() {
		Analysis wall = analyze(circle(3, glyph("terre", TOP), glyph("fixer", BOTTOM)));
		double far = wall.effects().getFirst().combination().range();
		assertEquals(1 + CircleAnalyzer.RANGE_COST * (far - 1), CircleAnalyzer.reach(wall.effects()), 1e-9);
		assertEquals(1, CircleAnalyzer.reach(java.util.List.of()), 1e-9, "sans effet, pas de surcoût");
	}

	@Test
	void earthAndSaltInATriangleRaiseAWall() {
		Analysis a = analyze(circle(3, glyph("terre", TOP), glyph("fixer", BOTTOM)));
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals("fmab:wall", a.effects().getFirst().combination().effect());
		assertEquals(2, a.complexity());
		// Terre (2) + Fixer (3), un mur de portée 2 : un quart de plus pour le bloc au-delà du premier.
		assertEquals(6, a.concentration());
		// Triangle (3) chargé à 2, glyphes alignés sur l'axe vertical : 3 × 1,25 / 2.
		assertEquals(1.875, a.stability(), 1e-9);
	}

	@Test
	void everyStartingCombinationWorks() {
		assertEffect("terre", "projeter", "fmab:spike");
		assertEffect("fer", "projeter", "fmab:blade");
		assertEffect("eau", "fixer", "fmab:ice_platform");
		assertEffect("terre", "decomposer", "fmab:decompose");
		assertEffect("fer", "reparer", "fmab:repair");
	}

	/** Chaque matière de réparation de l'enclume a son cercle : son élément et Réparer. */
	@Test
	void everyRepairMaterialHasItsCircle() {
		// L'Or et le Netherite ne se tracent qu'aux rangs élevés : un Alchimiste d'État les tient tous.
		for (String element : new String[]{"fer", "cuivre", "bois", "terre", "carbone", "netherite", "or", "fibre",
				"chair", "chaux"}) {
			Analysis a = analyzer.analyze(parser.parse(circle(4, glyph(element, TOP), glyph("reparer", BOTTOM))),
					Rank.STATE, null);
			assertEquals(Outcome.WORKS, a.outcome(), () -> element + " et Réparer : " + a.issues());
			assertEquals("fmab:repair", a.effects().getFirst().combination().effect(), element);
		}
	}

	@Test
	void noRingNoCircle() {
		Analysis a = analyze(new Drawing(glyph("terre", TOP)));
		assertEquals(Outcome.INERT, a.outcome());
		assertTrue(kinds(a).contains(Kind.NO_RING));
	}

	@Test
	void aCombinationMissingFromTheTableRebounds() {
		Analysis a = analyze(circle(4, glyph("eau", TOP), glyph("decomposer", BOTTOM)));
		assertEquals(Outcome.REBOUND, a.outcome());
		assertTrue(kinds(a).contains(Kind.UNKNOWN_COMBINATION));
	}

	@Test
	void anOverloadedTriangleReboundsInProportion() {
		// Fer (2) + Projeter (2) = 4 sur un triangle : 3 × 1,25 / 4 = 0,9375.
		Analysis triangle = analyze(circle(3, glyph("fer", TOP), glyph("projeter", BOTTOM)));
		assertEquals(Outcome.REBOUND, triangle.outcome());
		assertEquals(1 - 0.9375, triangle.reboundSeverity(), 1e-9);
		Analysis square = analyze(circle(4, glyph("fer", TOP), glyph("projeter", BOTTOM)));
		assertEquals(Outcome.WORKS, square.outcome(), () -> square.issues().toString());
	}

	@Test
	void dotsOnTheRingWidenTheEffect() {
		List<Primitive> strokes = new ArrayList<>(glyph("terre", TOP));
		strokes.addAll(glyph("fixer", BOTTOM));
		strokes.add(new Primitive.Dot(new Vec2(2, 16)));
		strokes.add(new Primitive.Dot(new Vec2(30, 16)));
		Analysis a = analyze(circle(4, strokes));
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals(2, a.effects().getFirst().intensity());
		assertEquals(4, a.effects().getFirst().range(), 1e-9);
		assertEquals(4, a.complexity());
	}

	@Test
	void theArrowOnTheRingGivesTheDirection() {
		List<Primitive> strokes = new ArrayList<>(glyph("terre", TOP));
		strokes.addAll(glyph("fixer", BOTTOM));
		strokes.addAll(TestGlyphs.drawn("direction", new Vec2(30, 16), 1.5, 90));
		Analysis a = analyze(circle(4, strokes));
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertTrue(a.effects().getFirst().hasDirection());
		assertEquals(0, a.effects().getFirst().direction(), 0.05);
	}

	/** Les modificateurs posés sur l'anneau règlent l'étage : forme, durée, nombre de cibles. */
	@Test
	void modifiersOnTheRingShapeTheStage() {
		List<Primitive> strokes = new ArrayList<>(glyph("terre", TOP));
		strokes.addAll(glyph("projeter", BOTTOM));
		strokes.addAll(TestGlyphs.drawn("forme_cone", new Vec2(26, 26), 1.5, 0));
		strokes.addAll(TestGlyphs.drawn("duree", new Vec2(6, 26), 1.5, 0));
		strokes.addAll(TestGlyphs.drawn("cibles", new Vec2(6, 6), 1.5, 0));
		strokes.addAll(TestGlyphs.drawn("cibles", new Vec2(26, 6), 1.5, 0));
		var stage = parser.parse(circle(4, strokes)).stages().getFirst();
		assertEquals("cone", stage.shape());
		assertEquals(1, stage.duration());
		assertEquals(2, stage.targets());
		Analysis a = analyze(circle(4, strokes));
		assertTrue(a.issues().stream().noneMatch(i -> i.kind() == CircleIssue.Kind.UNKNOWN_GLYPH), () -> a.issues().toString());
		assertEquals("cone", a.effects().getFirst().shape());
		assertEquals(2, a.effects().getFirst().targets());
		// Chaque modificateur se paie, comme la flèche ou les points d'intensité.
		Analysis bare = analyze(circle(4, glyph("terre", TOP), glyph("projeter", BOTTOM)));
		assertTrue(a.concentration() > bare.concentration());
	}

	@Test
	void aGlyphNotYetUnderstoodStillWorksButMayRebound() {
		Drawing d = circle(3, glyph("terre", TOP), glyph("fixer", BOTTOM));
		Analysis one = analyzer.analyze(parser.parse(d), Rank.APPRENTICE, Set.of("fmab:terre"));
		assertEquals(Outcome.WORKS, one.outcome());
		assertTrue(kinds(one).contains(Kind.GLYPH_NOT_LEARNED));
		assertEquals(0.25, one.risk(), 1e-9);

		Analysis none = analyzer.analyze(parser.parse(d), Rank.APPRENTICE, Set.of());
		assertEquals(0.5, none.risk(), 1e-9);
		Analysis all = analyzer.analyze(parser.parse(d), Rank.APPRENTICE, Set.of("fmab:terre", "fmab:fixer"));
		assertEquals(0, all.risk(), 1e-9);
	}

	@Test
	void anApprenticesHexagonHoldsLessThanTheirSquare() {
		// Le rang ne bloque pas : l'hexagone dépasse les quatre côtés d'un Apprenti, il vacille.
		Analysis hexagon = analyze(circle(6, glyph("terre", TOP), glyph("fixer", BOTTOM)));
		Analysis square = analyze(circle(4, glyph("terre", TOP), glyph("fixer", BOTTOM)));
		assertTrue(kinds(hexagon).contains(Kind.RANK_TOO_LOW));
		assertEquals(Rank.ALCHEMIST, hexagon.requiredRank());
		assertTrue(hexagon.effects().size() == 1, () -> hexagon.issues().toString());
		assertTrue(hexagon.stability() < square.stability(),
				() -> "hexagone " + hexagon.stability() + ", carré " + square.stability());
		Analysis alchemist = analyzer.analyze(parser.parse(circle(6, glyph("terre", TOP), glyph("fixer", BOTTOM))),
				Rank.ALCHEMIST, null);
		assertTrue(hexagon.stability() < alchemist.stability());
	}

	@Test
	void anUnreadableGlyphIsReportedWhereItIs() {
		List<Primitive> strokes = new ArrayList<>(glyph("terre", TOP));
		strokes.addAll(glyph("fixer", BOTTOM));
		strokes.add(new Primitive.Line(new Vec2(6, 14), new Vec2(8, 18)));
		Analysis a = analyze(circle(4, strokes));
		assertEquals(Outcome.REBOUND, a.outcome());
		assertTrue(a.issues().stream().anyMatch(i -> i.kind() == Kind.UNKNOWN_GLYPH
				&& i.where().distance(new Vec2(7, 16)) < 0.5));
	}

	@Test
	void anUnreadableGlyphNamesTheGlyphItComesClosestTo() {
		// La Terre tournée bien au-delà de sa marge de 15° : illisible, mais on dit de quoi elle se rapproche.
		List<Primitive> strokes = new ArrayList<>();
		for (Primitive p : glyph("terre", TOP)) {
			strokes.add(p.map(v -> v.sub(TOP).rotate(Math.toRadians(40)).add(TOP)));
		}
		strokes.addAll(glyph("fixer", BOTTOM));
		Analysis a = analyze(circle(4, strokes));
		CircleIssue issue = a.issues().stream().filter(i -> i.kind() == Kind.UNKNOWN_GLYPH).findFirst().orElseThrow();
		assertTrue(issue.detail().matches("fmab:[a-z_]+\\|\\d+\\.\\d{3}"), issue.detail());
	}

	private void assertEffect(String element, String action, String effect) {
		Analysis a = analyze(circle(4, glyph(element, TOP), glyph(action, BOTTOM)));
		assertEquals(Outcome.WORKS, a.outcome(), () -> element + "+" + action + " : " + a.issues());
		assertEquals(effect, a.effects().getFirst().combination().effect());
	}

	private Analysis analyze(Drawing d) {
		return analyzer.analyze(parser.parse(d), Rank.APPRENTICE, null);
	}

	private static Set<Kind> kinds(Analysis a) {
		return a.issues().stream().map(CircleIssue::kind).collect(Collectors.toSet());
	}

	private static List<Primitive> glyph(String name, Vec2 at) {
		return TestGlyphs.drawn(name, at, 3, 0);
	}

	@SafeVarargs
	private static Drawing circle(int sides, List<Primitive>... glyphs) {
		List<Primitive> all = new ArrayList<>();
		for (List<Primitive> g : glyphs) {
			all.addAll(g);
		}
		return circle(sides, all);
	}

	/** Anneau de rayon 14 et polygone régulier inscrit, pointe en haut, sommets sur la grille. */
	private static Drawing circle(int sides, List<Primitive> inside) {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		List<Vec2> vertices = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			double a = -Math.PI / 2 + 2 * Math.PI * i / sides;
			vertices.add(new Vec2(Math.round(16 + 14 * Math.cos(a)), Math.round(16 + 14 * Math.sin(a))));
		}
		all.add(new Primitive.Polygon(vertices));
		all.addAll(inside);
		return new Drawing(all);
	}
}
