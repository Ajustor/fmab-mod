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
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

	/** Le progrès « Premier dessin » : un cercle neuf enregistré, pas une page rangée ou effacée. */
	@Test
	void savingANewCircleCountsAsDrawingOne() {
		NotebookContents.Page blank = new NotebookContents.Page("a", Drawing.EMPTY);
		NotebookContents.Page human = new NotebookContents.Page("b", ForbiddenCircles.HUMAN_TRANSMUTATION);
		NotebookContents before = new NotebookContents(List.of(blank), 0);
		NotebookContents drawn = new NotebookContents(List.of(blank, human), 1);
		assertTrue(com.ajustor.fmab.data.Notebooks.drewNew(before, drawn));
		assertFalse(com.ajustor.fmab.data.Notebooks.drewNew(drawn, drawn.moved(1, -1)), "ranger n'est pas dessiner");
		assertFalse(com.ajustor.fmab.data.Notebooks.drewNew(drawn, before), "effacer n'est pas dessiner");
		assertFalse(com.ajustor.fmab.data.Notebooks.drewNew(before,
				new NotebookContents(List.of(blank, new NotebookContents.Page("c", Drawing.EMPTY)), 0)),
				"une page blanche n'est pas un cercle");
	}

	/** Le carnet tient trente-deux cercles, et les garde tels quels à l'enregistrement. */
	@Test
	void theNotebookHoldsThirtyTwoCircles() {
		List<NotebookContents.Page> pages = IntStream.range(0, 32)
				.mapToObj(i -> new NotebookContents.Page("Cercle " + (i + 1),
						new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 2 + i * 0.25)))))
				.toList();
		NotebookContents full = new NotebookContents(pages, 31);
		var json = NotebookContents.CODEC.encodeStart(JsonOps.INSTANCE, full).getOrThrow();
		NotebookContents read = NotebookContents.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(full, read);
		assertEquals(31, read.selected());
		assertTrue(NotebookContents.MAX_PAGES >= 32);
	}

	/** Un carnet d'avant les roues range ses cercles dans l'ordre, neuf par roue. */
	@Test
	void anOldNotebookFillsItsWheelsInOrder() {
		NotebookContents notebook = new NotebookContents(pages(11), 0);
		assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), notebook.slots());
		assertEquals(2, notebook.wheels());
		assertEquals(9, notebook.at(9), "la dixième page ouvre la deuxième roue");
		assertEquals(-1, notebook.at(11));
	}

	/** Poser un cercle sur une place libre l'y range ; sur une place prise, les deux échangent. */
	@Test
	void placingACircleMovesItOrSwapsIt() {
		NotebookContents notebook = new NotebookContents(pages(3), 2);
		NotebookContents far = notebook.placed(0, 4 * NotebookContents.WHEEL + 3);
		assertEquals(List.of(39, 1, 2), far.slots(), "une roue lointaine, des roues vides entre deux");
		assertEquals(5, far.wheels());
		assertEquals(2, far.selected(), "ranger ne change pas la sélection");
		NotebookContents swapped = notebook.placed(0, 2);
		assertEquals(List.of(2, 1, 0), swapped.slots());
		assertSame(notebook, notebook.placed(0, 0));
		assertSame(notebook, notebook.placed(0, NotebookContents.MAX_SLOTS));
		assertSame(notebook, notebook.placed(5, 1));
	}

	/** Une page effacée laisse sa place vide ; une page nouvelle prend la première libre. */
	@Test
	void aNewPageTakesTheFirstFreePlace() {
		NotebookContents notebook = new NotebookContents(pages(3), 0).withoutPage(1);
		assertEquals(List.of(0, 2), notebook.slots());
		NotebookContents added = notebook.withPage(2, new NotebookContents.Page("d", Drawing.EMPTY));
		assertEquals(List.of(0, 2, 1), added.slots());
		assertEquals(List.of(0, 2, 1), added.withPage(0, new NotebookContents.Page("e", Drawing.EMPTY)).slots(),
				"redessiner une page la laisse à sa place");
	}

	/** Les places se gardent à l'enregistrement ; doublons et places hors des roues sont réparés. */
	@Test
	void placesSurviveSavingAndAreRepaired() {
		NotebookContents arranged = new NotebookContents(pages(3), 1).placed(1, 20);
		var json = NotebookContents.CODEC.encodeStart(JsonOps.INSTANCE, arranged).getOrThrow();
		assertEquals(arranged, NotebookContents.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		assertEquals(List.of(5, 0, 1), new NotebookContents(pages(3), 0, List.of(5, 5, -1)).slots());
		assertEquals(List.of(0, 1), new NotebookContents(pages(2), 0, List.of(NotebookContents.MAX_SLOTS)).slots());
	}

	private static List<NotebookContents.Page> pages(int n) {
		return IntStream.range(0, n)
				.mapToObj(i -> new NotebookContents.Page("p" + i,
						new Drawing(List.of(new Primitive.Circle(Drawing.CENTER_POINT, 2 + i * 0.25)))))
				.toList();
	}
}
