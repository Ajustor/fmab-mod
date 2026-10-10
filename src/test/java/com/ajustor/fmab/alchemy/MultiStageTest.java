package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleIssue.Kind;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.circle.ParsedCircle;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cercles d'Alchimiste : deux étages (anneaux de rayon 6 et 12), liaisons, satellites, fusion de
 * deux éléments.
 */
class MultiStageTest {
	private static final double INNER = 6;
	private static final double OUTER = 12;
	/** Sommet haut-droite de l'hexagone extérieur, là où l'on pose les satellites. */
	private static final Vec2 VERTEX = new Vec2(26, 10);

	private final CircleParser parser = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private final CircleAnalyzer analyzer = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	@Test
	void aSimpleStrokeChainsTheStagesInSeries() {
		Analysis a = analyze(twoStages(seriesLink()));
		assertEquals(LinkKind.SERIES, a.parsed().stages().get(1).link());
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals(List.of("fmab:decompose", "fmab:spike"),
				a.effects().stream().map(e -> e.combination().effect()).toList());
		assertEquals(Rank.ALCHEMIST, a.requiredRank());
	}

	@Test
	void aDoubledStrokeRunsThemInParallel() {
		List<Primitive> link = new ArrayList<>(seriesLink());
		link.add(new Primitive.Line(new Vec2(12, 21), new Vec2(8, 25)));
		Analysis a = analyze(twoStages(link));
		assertEquals(LinkKind.PARALLEL, a.parsed().stages().get(1).link(), () -> a.issues().toString());
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
	}

	@Test
	void aBrokenStrokeIsAFallback() {
		List<Primitive> dashes = List.of(
				new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(10, 22)),
				new Primitive.Line(new Vec2(9, 23), new Vec2(7.5, 24.5)));
		Analysis a = analyze(twoStages(dashes));
		assertEquals(LinkKind.CONDITIONAL, a.parsed().stages().get(1).link(), () -> a.issues().toString());
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
	}

	@Test
	void anOuterStageWithoutLinkIsReported() {
		Analysis a = analyze(twoStages(List.of()));
		assertEquals(LinkKind.NONE, a.parsed().stages().get(1).link());
		assertTrue(kinds(a).contains(Kind.UNLINKED_STAGE));
	}

	@Test
	void twoStagesWaverInAnApprenticesHands() {
		Analysis a = analyzer.analyze(parser.parse(twoStages(seriesLink())), Rank.APPRENTICE, null);
		Analysis alchemist = analyzer.analyze(parser.parse(twoStages(seriesLink())), Rank.ALCHEMIST, null);
		assertEquals(Outcome.REBOUND, a.outcome(), () -> a.issues().toString());
		assertTrue(kinds(a).contains(Kind.RANK_TOO_LOW));
		assertTrue(kinds(a).contains(Kind.UNSTABLE));
		assertTrue(a.stability() < alchemist.stability());
	}

	@Test
	void aSatelliteHoldingOnlyFireInfusesItsStage() {
		List<Primitive> extra = new ArrayList<>(seriesLink());
		extra.add(new Primitive.Circle(VERTEX, 3));
		extra.addAll(TestGlyphs.drawn("feu", VERTEX, 2, 0));
		ParsedCircle parsed = parser.parse(twoStages(extra));
		assertEquals(1, parsed.stages().get(1).satellites().size(), () -> parsed.issues().toString());
		Analysis a = analyzer.analyze(parsed, Rank.ALCHEMIST, null);
		Analysis.StageEffect spike = a.effects().stream()
				.filter(e -> e.combination().effect().equals("fmab:spike")).findFirst().orElseThrow();
		assertTrue(spike.infused("fire"), () -> a.issues().toString());
		assertFalse(a.effects().getFirst().infused("fire"), "seul l'étage du satellite est infusé");
	}

	@Test
	void aCompleteSatelliteCastsItsOwnEffectFromItsVertex() {
		List<Primitive> extra = new ArrayList<>(seriesLink());
		extra.add(new Primitive.Circle(VERTEX, 4.5));
		extra.addAll(TestGlyphs.drawn("terre", new Vec2(26, 8), 2, 0));
		extra.addAll(TestGlyphs.drawn("fixer", new Vec2(26, 12.5), 1.5, 0));
		ParsedCircle parsed = parser.parse(twoStages(extra));
		assertEquals(1, parsed.stages().get(1).satellites().size(), () -> parsed.issues().toString());
		Analysis a = analyzer.analyze(parsed, Rank.ALCHEMIST, null);
		Analysis.StageEffect wall = a.effects().stream()
				.filter(e -> e.satellite() >= 0).findFirst().orElseThrow(() -> new AssertionError(a.issues()));
		assertEquals("fmab:wall", wall.combination().effect());
		assertEquals(VERTEX.sub(Drawing.CENTER_POINT), wall.origin());
	}

	@Test
	void twoElementsFuseOnlyInAHexagram() {
		List<Primitive> base = new ArrayList<>();
		base.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		base.addAll(TestGlyphs.drawn("feu", new Vec2(12, 12), 2.5, 0));
		base.addAll(TestGlyphs.drawn("air", new Vec2(20, 12), 2.5, 0));
		base.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 20), 2.5, 0));

		List<Primitive> hexagram = new ArrayList<>(base);
		hexagram.add(polygon(14, 3, -90));
		hexagram.add(polygon(14, 3, 90));
		Analysis fused = analyzer.analyze(parser.parse(new Drawing(hexagram)), Rank.ALCHEMIST, null);
		assertTrue(fused.parsed().stages().getFirst().hexagram());
		assertEquals("fmab:flame_burst", fused.effects().getFirst().combination().effect(),
				() -> fused.issues().toString());

		List<Primitive> square = new ArrayList<>(base);
		square.add(polygon(14, 4, -90));
		Analysis unfused = analyzer.analyze(parser.parse(new Drawing(square)), Rank.ALCHEMIST, null);
		assertTrue(kinds(unfused).contains(Kind.FUSION_NEEDS_HEXAGRAM));
		assertEquals(Outcome.REBOUND, unfused.outcome());
	}

	@Test
	void layeredPolygonsAddTheirSidesSoFireAndAirHoldInAHexagramAndAHexagon() {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		all.add(polygon(14, 3, -90));
		all.add(polygon(14, 3, 90));
		all.add(polygon(14, 6, 0));
		all.addAll(TestGlyphs.drawn("feu", new Vec2(12, 12), 2.5, 0));
		all.addAll(TestGlyphs.drawn("air", new Vec2(20, 12), 2.5, 0));
		all.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 20), 2.5, 0));
		Analysis a = analyze(new Drawing(all));
		assertEquals(12, a.parsed().stages().getFirst().capacity());
		assertEquals(Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals(Rank.ALCHEMIST, a.requiredRank());
	}

	@Test
	void twoPolygonsInOneRingWaverForAnApprentice() {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, 14));
		all.add(polygon(14, 3, -90));
		all.add(polygon(14, 4, -90));
		all.addAll(TestGlyphs.drawn("fer", new Vec2(16, 11), 3, 0));
		all.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 20), 3, 0));
		Analysis a = analyzer.analyze(parser.parse(new Drawing(all)), Rank.APPRENTICE, null);
		Analysis alchemist = analyzer.analyze(parser.parse(new Drawing(all)), Rank.ALCHEMIST, null);
		assertEquals(Rank.ALCHEMIST, a.requiredRank());
		assertTrue(kinds(a).contains(Kind.RANK_TOO_LOW));
		assertTrue(a.stability() < alchemist.stability());
	}

	private Analysis analyze(Drawing d) {
		return analyzer.analyze(parser.parse(d), Rank.ALCHEMIST, null);
	}

	private static Set<Kind> kinds(Analysis a) {
		return a.issues().stream().map(CircleIssue::kind).collect(Collectors.toSet());
	}

	/** Trait simple, en bas à gauche, de l'anneau intérieur à l'anneau extérieur. */
	private static List<Primitive> seriesLink() {
		return List.of(new Primitive.Line(new Vec2(11.5, 20.5), new Vec2(7.5, 24.5)));
	}

	/**
	 * Centre : Terre + Décomposer dans un carré. Extérieur : Terre + Projeter dans un hexagone.
	 */
	private static Drawing twoStages(List<Primitive> extra) {
		List<Primitive> all = new ArrayList<>();
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, INNER));
		all.add(polygon(INNER, 4, -90));
		all.addAll(TestGlyphs.drawn("terre", new Vec2(16, 13.5), 2, 0));
		all.addAll(TestGlyphs.drawn("decomposer", new Vec2(16, 18.5), 1.8, 0));
		all.add(new Primitive.Circle(Drawing.CENTER_POINT, OUTER));
		all.add(polygon(OUTER, 6, -90));
		all.addAll(TestGlyphs.drawn("terre", new Vec2(16, 7), 2, 0));
		all.addAll(TestGlyphs.drawn("projeter", new Vec2(16, 25), 2, 0));
		all.addAll(extra);
		return new Drawing(all);
	}

	private static Primitive polygon(double radius, int sides, double startDeg) {
		List<Vec2> vertices = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			double a = Math.toRadians(startDeg + 360.0 * i / sides);
			vertices.add(new Vec2(Math.round(16 + radius * Math.cos(a)), Math.round(16 + radius * Math.sin(a))));
		}
		return new Primitive.Polygon(vertices);
	}
}
