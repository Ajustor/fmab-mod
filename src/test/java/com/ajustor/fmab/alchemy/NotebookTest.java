package com.ajustor.fmab.alchemy;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleParser;
import com.ajustor.fmab.alchemy.drawing.Drawing;
import com.ajustor.fmab.alchemy.drawing.Primitive;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphRecognizer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.rules.Analysis;
import com.ajustor.fmab.alchemy.rules.CircleAnalyzer;
import com.ajustor.fmab.data.NotebookContents;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le carnet porté par le joueur et le cercle interdit qu'on y recopie. */
class NotebookTest {
	private static final CircleParser PARSER = new CircleParser(new GlyphRecognizer(TestGlyphs.all().values()));
	private static final CircleAnalyzer ANALYZER = new CircleAnalyzer(TestGlyphs.combinations(), TestGlyphs.all().values());

	/** Les notes interdites donnent un cercle qui fonctionne pour un Alchimiste d'État qui en a les glyphes. */
	@Test
	void theForbiddenNotesGiveAWorkingHumanTransmutationCircle() {
		Set<String> studied = TestGlyphs.all().values().stream()
				.filter(g -> Rank.STATE.atLeast(g.rank()))
				.map(Glyph::id)
				.collect(Collectors.toSet());
		Analysis a = ANALYZER.analyze(PARSER.parse(ForbiddenCircles.HUMAN_TRANSMUTATION), Rank.STATE, studied);
		assertEquals(Analysis.Outcome.WORKS, a.outcome(), () -> a.issues().toString());
		assertEquals(ForbiddenCircles.HUMAN_TRANSMUTATION_EFFECT, a.effects().getFirst().combination().effect());
	}

	/** Il manque un satellite à la formule du corps (le fer) : le cercle rebondit. */
	@Test
	void aHumanTransmutationMissingOneIngredientRebounds() {
		Set<String> studied = TestGlyphs.all().values().stream().map(Glyph::id).collect(Collectors.toSet());
		List<Primitive> strokes = new ArrayList<>(ForbiddenCircles.HUMAN_TRANSMUTATION.primitives());
		// Le satellite du Fer, à la pointe en bas à droite, et son glyphe.
		Vec2 iron = new Vec2(24.66, 21.0);
		strokes.removeIf(p -> p.sample(0.5).stream().allMatch(v -> v.distance(iron) <= 3.6));
		Analysis a = ANALYZER.analyze(PARSER.parse(new Drawing(strokes)), Rank.STATE, studied);
		assertEquals(Analysis.Outcome.REBOUND, a.outcome(), () -> a.issues().toString());
		assertTrue(a.issues().stream().anyMatch(i -> i.kind() == CircleIssue.Kind.INCOMPLETE_FORMULA
				&& i.detail().equals("iron")), () -> a.issues().toString());
	}

	/** Le Traité décrit la transmutation humaine sans en donner le cercle tout fait. */
	@Test
	void theTreatiseDoesNotHandOutTheHumanTransmutationCircle() {
		for (Treatise.Chapter chapter : TreatiseTest.load().chapters()) {
			for (Treatise.Page page : chapter.pages()) {
				if (page instanceof Treatise.ExamplePage example) {
					Analysis a = ANALYZER.analyze(PARSER.parse(example.drawing()), Rank.GATE, null);
					assertFalse(a.effects().stream().anyMatch(
									e -> e.combination().effect().equals(ForbiddenCircles.HUMAN_TRANSMUTATION_EFFECT)),
							example::textKey);
				}
			}
		}
	}

	/** Ranger une page la déplace d'un cran et la garde sélectionnée ; aux bords, rien ne bouge. */
	@Test
	void movingAPageSwapsItWithItsNeighbourAndKeepsItSelected() {
		NotebookContents.Page a = new NotebookContents.Page("a", Drawing.EMPTY);
		NotebookContents.Page b = new NotebookContents.Page("b", ForbiddenCircles.HUMAN_TRANSMUTATION);
		NotebookContents.Page c = new NotebookContents.Page("c", Drawing.EMPTY);
		NotebookContents notebook = new NotebookContents(List.of(a, b, c), 0);

		NotebookContents moved = notebook.moved(1, -1);
		assertEquals(List.of(b, a, c), moved.pages());
		assertEquals(0, moved.selected());
		assertSame(notebook, notebook.moved(0, -1));
		assertSame(notebook, notebook.moved(2, 1));
		assertEquals(1, notebook.indexOf(ForbiddenCircles.HUMAN_TRANSMUTATION));
	}
}
