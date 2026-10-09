package com.ajustor.fmab.alchemy.rules;

import com.ajustor.fmab.alchemy.circle.CircleIssue;
import com.ajustor.fmab.alchemy.circle.CircleIssue.Kind;
import com.ajustor.fmab.alchemy.circle.LinkKind;
import com.ajustor.fmab.alchemy.circle.ParsedCircle;
import com.ajustor.fmab.alchemy.circle.PlacedGlyph;
import com.ajustor.fmab.alchemy.circle.Satellite;
import com.ajustor.fmab.alchemy.circle.Stage;
import com.ajustor.fmab.alchemy.drawing.Vec2;
import com.ajustor.fmab.alchemy.glyph.Glyph;
import com.ajustor.fmab.alchemy.glyph.GlyphLayer;
import com.ajustor.fmab.alchemy.glyph.Rank;
import com.ajustor.fmab.alchemy.knowledge.Knowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
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
 *   <li>Complexité : somme des glyphes, modificateurs et satellites compris ; elle doit tenir sous
 *   le plafond du rang.</li>
 *   <li>Stabilité : un étage supporte autant de complexité que ses polygones ont de côtés en tout
 *   (2 pour un anneau nu), un quart de plus si les glyphes sont disposés symétriquement.</li>
 *   <li>Concentration : somme des glyphes, multipliée par le nombre d'étages.</li>
 *   <li>Portée : celle de la combinaison, +50 % par point d'intensité.</li>
 *   <li>Deux éléments dans un étage ne fusionnent que dans un hexagramme.</li>
 *   <li>Un satellite qui ne porte que des éléments infuse l'effet de son étage ; un satellite
 *   complet (élément et action) lance son propre effet depuis son sommet.</li>
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
			Kind.TOO_MANY_ACTIONS, Kind.RANK_TOO_LOW, Kind.KNOWLEDGE_MISSING);
	/**
	 * Chaque glyphe tracé sans être compris ajoute ce risque de rebond : on peut recopier un cercle
	 * qu'on ne comprend pas, à ses risques.
	 */
	public static final double UNLEARNED_RISK = 0.25;
	public static final double MAX_UNLEARNED_RISK = 0.75;
	/** Problèmes qui font rebondir le cercle, avec la gravité d'une combinaison inconnue. */
	private static final Set<Kind> MISCOMPOSED = EnumSet.of(Kind.UNKNOWN_COMBINATION, Kind.CONFLICTING_LINKS,
			Kind.FUSION_NEEDS_HEXAGRAM, Kind.SATELLITE_INCOMPLETE);

	private final CombinationTable combinations;
	private final Optional<Glyph> intensityGlyph;
	private final Optional<Glyph> directionGlyph;

	public CircleAnalyzer(CombinationTable combinations, Collection<Glyph> glyphs) {
		this.combinations = combinations;
		this.intensityGlyph = glyphs.stream().filter(g -> g.is(GlyphLayer.MODIFIER, "intensity")).findFirst();
		this.directionGlyph = glyphs.stream().filter(g -> g.is(GlyphLayer.MODIFIER, "direction")).findFirst();
	}

	/** Analyse sans savoir : ni bonus, ni vérification des nœuds demandés (éditeur web, tests). */
	public Analysis analyze(ParsedCircle parsed, Rank rank, Set<String> known) {
		return analyze(parsed, rank, known, null);
	}

	/**
	 * @param rank      rang de l'alchimiste
	 * @param known     glyphes qu'il a compris ; {@code null} pour ne pas vérifier
	 * @param knowledge son savoir ; {@code null} pour ignorer bonus et nœuds demandés
	 */
	public Analysis analyze(ParsedCircle parsed, Rank rank, Set<String> known, Knowledge knowledge) {
		List<CircleIssue> issues = new ArrayList<>(parsed.issues());
		List<Analysis.StageEffect> effects = new ArrayList<>();
		int complexity = 0;
		int concentration = 0;
		int satelliteCount = 0;
		Rank required = Rank.APPRENTICE;
		double stability = Double.POSITIVE_INFINITY;
		double severity = 0;
		Set<String> unlearned = new LinkedHashSet<>();

		for (Stage stage : parsed.stages()) {
			List<Glyph> written = new ArrayList<>();
			stage.glyphs().forEach(g -> written.add(g.glyph()));
			stage.satellites().forEach(s -> s.glyphs().forEach(g -> written.add(g.glyph())));
			for (int i = 0; i < stage.intensity(); i++) {
				intensityGlyph.ifPresent(written::add);
			}
			if (stage.direction().isPresent()) {
				directionGlyph.ifPresent(written::add);
			}
			satelliteCount += stage.satellites().size();

			int load = 0;
			for (Glyph g : written) {
				load += g.complexity();
				concentration += g.concentration();
				required = max(required, g.rank());
			}
			if (known != null) {
				written.stream().map(Glyph::id).filter(id -> !known.contains(id)).forEach(unlearned::add);
			}
			complexity += load;

			if (load > 0) {
				double capacity = stage.capacity() == 0 ? BARE_RING_CAPACITY : stage.capacity();
				if (symmetric(stage.glyphs())) {
					capacity *= SYMMETRY_BONUS;
				}
				stability = Math.min(stability, capacity / load);
			}
			required = max(required, rankFor(stage.sides(), Rank::maxPolygonSides));
			required = max(required, rankFor(stage.polygons(), Rank::maxPolygonsPerStage));
			if (stage.index() > 0 && stage.link() == LinkKind.NONE && load > 0) {
				issues.add(new CircleIssue(Kind.UNLINKED_STAGE, null, Integer.toString(stage.index())));
			}

			effects.addAll(effects(stage, issues, knowledge));
		}
		concentration *= Math.max(1, parsed.stages().size());
		if (knowledge != null) {
			double discount = 0;
			for (Analysis.StageEffect e : effects) {
				discount += knowledge.perk(e.combination().school(), "concentration_discount");
			}
			concentration = Math.max(1, concentration - (int) discount);
		}
		unlearned.forEach(id -> issues.add(new CircleIssue(Kind.GLYPH_NOT_LEARNED, null, id)));
		double risk = Math.min(MAX_UNLEARNED_RISK, UNLEARNED_RISK * unlearned.size());
		required = max(required, rankFor(parsed.stages().size(), Rank::maxStages));
		required = max(required, rankFor(complexity, Rank::complexityCap));
		required = max(required, rankFor(satelliteCount, Rank::maxSatellites));
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
			if (MISCOMPOSED.contains(issue.kind())) {
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
				severity, outcome == Analysis.Outcome.WORKS ? risk : 0);
	}

	/** L'effet de l'étage, puis ceux de ses satellites complets. */
	private List<Analysis.StageEffect> effects(Stage stage, List<CircleIssue> issues, Knowledge knowledge) {
		List<Analysis.StageEffect> out = new ArrayList<>();
		double direction = stage.direction().orElse(Double.NaN);

		Set<String> infusions = new LinkedHashSet<>();
		List<Satellite> complete = new ArrayList<>();
		for (Satellite s : stage.satellites()) {
			boolean hasElement = !s.layer(GlyphLayer.ELEMENT).isEmpty();
			int actions = s.layer(GlyphLayer.ACTION).size();
			if (hasElement && actions == 0) {
				s.layer(GlyphLayer.ELEMENT).forEach(g -> infusions.add(g.glyph().role()));
			} else if (hasElement && actions == 1) {
				complete.add(s);
			} else {
				issues.add(CircleIssue.at(Kind.SATELLITE_INCOMPLETE, s.center()));
			}
		}

		combination(stage.layer(GlyphLayer.ELEMENT), stage.layer(GlyphLayer.ACTION), stage.hexagram(), issues, knowledge)
				.ifPresent(c -> out.add(new Analysis.StageEffect(stage.index(), -1, c, range(c, stage.intensity(), knowledge),
						stage.intensity(), Vec2.ZERO, direction, infusions, stage.link())));

		for (int i = 0; i < stage.satellites().size(); i++) {
			Satellite s = stage.satellites().get(i);
			if (!complete.contains(s)) {
				continue;
			}
			int index = i;
			// Un satellite n'a pas de polygone à lui : il ne fusionne pas deux éléments.
			combination(s.layer(GlyphLayer.ELEMENT), s.layer(GlyphLayer.ACTION), false, issues, knowledge)
					.ifPresent(c -> out.add(new Analysis.StageEffect(stage.index(), index, c, range(c, 0, knowledge), 0,
							s.center(), Double.isNaN(direction) ? s.center().angle() : direction, Set.of(),
							stage.link())));
		}
		return out;
	}

	private Optional<Combination> combination(List<PlacedGlyph> elements, List<PlacedGlyph> actions, boolean hexagram,
			List<CircleIssue> issues, Knowledge knowledge) {
		if (actions.isEmpty() && elements.isEmpty()) {
			// Un étage vide sert de relais ou de décor : il ne fait rien et ne gêne pas.
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
		if (roles.size() > 1 && !hexagram) {
			issues.add(CircleIssue.at(Kind.FUSION_NEEDS_HEXAGRAM, elements.get(1).position()));
			return Optional.empty();
		}
		String action = actions.getFirst().glyph().role();
		Optional<Combination> combination = combinations.find(roles, action);
		if (combination.isEmpty()) {
			issues.add(new CircleIssue(Kind.UNKNOWN_COMBINATION, null, String.join("+", roles) + "+" + action));
			return combination;
		}
		Optional<String> missing = combination.get().requires().filter(n -> knowledge != null && !knowledge.has(n));
		if (missing.isPresent()) {
			issues.add(new CircleIssue(Kind.KNOWLEDGE_MISSING, null, missing.get()));
			return Optional.empty();
		}
		return combination;
	}

	private static double range(Combination c, int intensity, Knowledge knowledge) {
		double bonus = knowledge == null ? 0 : knowledge.perk(c.school(), "range_bonus");
		return (c.range() + bonus) * (1 + INTENSITY_STEP * intensity);
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
