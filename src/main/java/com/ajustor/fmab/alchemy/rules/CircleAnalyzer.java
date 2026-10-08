package com.ajustor.fmab.alchemy.rules;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleIssue.Kind;
import com.ajustor.fmab.alchemy.circle.ParsedCircle;
import com.ajustor.fmab.alchemy.circle.PlacedGlyph;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.Rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * Règles de calcul d'un cercle.
 *
 * <ul>
 *   <li>Complexité : somme des glyphes, modificateurs compris ; elle doit tenir sous le plafond du
 *   rang.</li>
 *   <li>Stabilité : un polygone supporte autant de complexité qu'il a de côtés (2 pour un anneau
 *   nu), un quart de plus si les glyphes sont disposés symétriquement.</li>
 *   <li>Concentration : somme des glyphes, multipliée par le nombre d'étages.</li>
 *   <li>Portée : celle de la combinaison, +50 % par point d'intensité.</li>
 * </ul>
 */
public final class CircleAnalyzer {
	private static final int BARE_RING_CAPACITY = 2;
	private static final double SYMMETRY_BONUS = 1.25;
	private static final double INTENSITY_STEP = 0.5;
	/** Distance (cases) sous laquelle deux glyphes sont l'image l'un de l'autre. */
	private static final double MIRROR_TOLERANCE = 1.5;
	private static final double UNKNOWN_COMBINATION_SEVERITY = 0.5;
	private static final double UNKNOWN_GLYPH_SEVERITY = 0.3;

	/** Problèmes qui empêchent toute réaction. */
	private static final Set<Kind> INERT = EnumSet.of(Kind.NO_RING, Kind.NO_ACTION, Kind.NO_ELEMENT,
			Kind.TOO_MANY_ACTIONS, Kind.RANK_TOO_LOW, Kind.GLYPH_NOT_LEARNED);

	private final CombinationTable combinations;
	private final Optional<Glyph> intensityGlyph;
	private final Optional<Glyph> directionGlyph;

	public CircleAnalyzer(CombinationTable combinations, Collection<Glyph> glyphs) {
		this.combinations = combinations;
		this.intensityGlyph = glyphs.stream().filter(g -> g.is(GlyphLayer.MODIFIER, "intensity")).findFirst();
		this.directionGlyph = glyphs.stream().filter(g -> g.is(GlyphLayer.MODIFIER, "direction")).findFirst();
	}

	/**
	 * @param rank  rang de l'alchimiste
	 * @param known glyphes qu'il a compris ; {@code null} pour ne pas vérifier (éditeur web)
	 */
	public Analysis analyze(ParsedCircle parsed, Rank rank, Set<String> known) {
		List<CircleIssue> issues = new ArrayList<>(parsed.issues());
		List<Analysis.StageEffect> effects = new ArrayList<>();
		int complexity = 0;
		int concentration = 0;
		Rank required = Rank.APPRENTICE;
		double stability = Double.POSITIVE_INFINITY;
		double severity = 0;

		for (Stage stage : parsed.stages()) {
			List<Glyph> written = new ArrayList<>();
			stage.glyphs().forEach(g -> written.add(g.glyph()));
			for (int i = 0; i < stage.intensity(); i++) {
				intensityGlyph.ifPresent(written::add);
			}
			if (stage.direction().isPresent()) {
				directionGlyph.ifPresent(written::add);
			}

			int load = 0;
			for (Glyph g : written) {
				load += g.complexity();
				concentration += g.concentration();
				required = max(required, g.rank());
			}
			if (known != null) {
				written.stream().map(Glyph::id).filter(id -> !known.contains(id)).distinct()
						.forEach(id -> issues.add(new CircleIssue(Kind.GLYPH_NOT_LEARNED, null, id)));
			}
			complexity += load;

			if (load > 0) {
				double capacity = stage.sides() == 0 ? BARE_RING_CAPACITY : stage.sides();
				if (symmetric(stage.glyphs())) {
					capacity *= SYMMETRY_BONUS;
				}
				stability = Math.min(stability, capacity / load);
			}
			required = max(required, rankFor(stage.sides(), Rank::maxPolygonSides));

			effect(stage, issues).ifPresent(effects::add);
		}
		concentration *= Math.max(1, parsed.stages().size());
		required = max(required, rankFor(parsed.stages().size(), Rank::maxStages));
		required = max(required, rankFor(complexity, Rank::complexityCap));
		if (!rank.atLeast(required)) {
			issues.add(new CircleIssue(Kind.RANK_TOO_LOW, null, required.serializedName()));
		}
		if (stability == Double.POSITIVE_INFINITY) {
			stability = 0;
		} else if (stability < 1) {
			issues.add(CircleIssue.of(Kind.UNSTABLE));
			severity = Math.max(severity, 1 - stability);
		}

		for (CircleIssue issue : issues) {
			if (issue.kind() == Kind.UNKNOWN_COMBINATION) {
				severity = Math.max(severity, UNKNOWN_COMBINATION_SEVERITY);
			} else if (issue.kind() == Kind.UNKNOWN_GLYPH) {
				severity = Math.min(1, severity + UNKNOWN_GLYPH_SEVERITY);
			}
		}

		Analysis.Outcome outcome;
		if (parsed.stages().isEmpty() || issues.stream().anyMatch(i -> INERT.contains(i.kind()))) {
			outcome = Analysis.Outcome.INERT;
			severity = 0;
		} else if (severity > 0) {
			outcome = Analysis.Outcome.REBOUND;
		} else if (effects.isEmpty()) {
			outcome = Analysis.Outcome.INERT;
		} else {
			outcome = Analysis.Outcome.WORKS;
		}
		return new Analysis(parsed, complexity, required, stability, concentration, effects, issues, outcome,
				severity);
	}

	private Optional<Analysis.StageEffect> effect(Stage stage, List<CircleIssue> issues) {
		List<PlacedGlyph> actions = stage.layer(GlyphLayer.ACTION);
		List<PlacedGlyph> elements = stage.layer(GlyphLayer.ELEMENT);
		if (actions.isEmpty() && elements.isEmpty()) {
			// Un étage vide sert de liaison ou de décor : il ne fait rien et ne gêne pas.
			return Optional.empty();
		}
		if (actions.isEmpty()) {
			issues.add(CircleIssue.of(Kind.NO_ACTION));
			return Optional.empty();
		}
		if (actions.size() > 1) {
			issues.add(CircleIssue.at(Kind.TOO_MANY_ACTIONS, actions.get(1).position()));
			return Optional.empty();
		}
		if (elements.isEmpty()) {
			issues.add(CircleIssue.of(Kind.NO_ELEMENT));
			return Optional.empty();
		}
		Set<String> roles = new LinkedHashSet<>();
		elements.forEach(e -> roles.add(e.glyph().role()));
		String action = actions.getFirst().glyph().role();
		Optional<Combination> combination = combinations.find(roles, action);
		if (combination.isEmpty()) {
			issues.add(new CircleIssue(Kind.UNKNOWN_COMBINATION, null, String.join("+", roles) + "+" + action));
			return Optional.empty();
		}
		double range = combination.get().range() * (1 + INTENSITY_STEP * stage.intensity());
		return Optional.of(new Analysis.StageEffect(stage.index(), combination.get(), range, stage.intensity(),
				stage.direction().orElse(Double.NaN)));
	}

	/** Symétrie miroir (axe vertical ou horizontal) ou demi-tour : chaque glyphe a son image. */
	static boolean symmetric(List<PlacedGlyph> glyphs) {
		return mirrored(glyphs, v -> new Vec2(-v.x(), v.y()))
				|| mirrored(glyphs, v -> new Vec2(v.x(), -v.y()))
				|| mirrored(glyphs, v -> new Vec2(-v.x(), -v.y()));
	}

	private static boolean mirrored(List<PlacedGlyph> glyphs, UnaryOperator<Vec2> image) {
		for (PlacedGlyph g : glyphs) {
			Vec2 target = image.apply(g.position());
			boolean found = glyphs.stream().anyMatch(o -> o.glyph().id().equals(g.glyph().id())
					&& o.position().distance(target) <= MIRROR_TOLERANCE);
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/** Plus petit rang dont la limite admet {@code value}. */
	private static Rank rankFor(int value, ToIntFunction<Rank> limit) {
		for (Rank r : Rank.values()) {
			if (value <= limit.applyAsInt(r)) {
				return r;
			}
		}
		return Rank.GATE;
	}

	private static Rank max(Rank a, Rank b) {
		return a.atLeast(b) ? a : b;
	}
}
