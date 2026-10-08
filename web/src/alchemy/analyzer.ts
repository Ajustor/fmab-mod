// Portage de CircleAnalyzer et Analysis. L'éditeur n'a pas de savoir : ni bonus, ni nœuds requis.

import { type Vec2, angle, vec } from "./geometry";
import { type Combination, type CombinationTable, type Glyph, RANKS, type RankInfo, rankIndex } from "./glyph";
import {
  type CircleIssue,
  type IssueKind,
  type LinkKind,
  type ParsedCircle,
  type PlacedGlyph,
  type Satellite,
  type Stage,
  inLayer,
  issue,
} from "./parser";

export type Outcome = "inert" | "rebound" | "works";

export interface StageEffect {
  readonly stage: number;
  /** −1 pour l'effet de l'étage lui-même. */
  readonly satellite: number;
  readonly combination: Combination;
  readonly range: number;
  readonly intensity: number;
  readonly origin: Vec2;
  /** Angle de la flèche, ou NaN. */
  readonly direction: number;
  readonly infusions: ReadonlySet<string>;
  readonly link: LinkKind;
}

export interface Analysis {
  readonly parsed: ParsedCircle;
  readonly complexity: number;
  readonly requiredRank: string;
  readonly stability: number;
  readonly concentration: number;
  readonly effects: StageEffect[];
  readonly issues: CircleIssue[];
  readonly outcome: Outcome;
  readonly reboundSeverity: number;
}

const BARE_RING_CAPACITY = 2;
const SYMMETRY_BONUS = 1.25;
const INTENSITY_STEP = 0.5;
const MIRROR_TOLERANCE = 1.5;
const UNKNOWN_COMBINATION_SEVERITY = 0.5;
const UNKNOWN_GLYPH_SEVERITY = 0.3;

const INERT: ReadonlySet<IssueKind> = new Set<IssueKind>([
  "no_ring",
  "no_action",
  "no_element",
  "too_many_actions",
  "rank_too_low",
  "glyph_not_learned",
  "knowledge_missing",
]);
const MISCOMPOSED: ReadonlySet<IssueKind> = new Set<IssueKind>([
  "unknown_combination",
  "conflicting_links",
  "fusion_needs_hexagram",
  "satellite_incomplete",
]);

function rankFor(value: number, limit: (r: RankInfo) => number): number {
  for (let i = 0; i < RANKS.length; i++) {
    if (value <= limit(RANKS[i])) return i;
  }
  return RANKS.length - 1;
}

export class CircleAnalyzer {
  private readonly intensityGlyph?: Glyph;
  private readonly directionGlyph?: Glyph;

  constructor(
    private readonly combinations: CombinationTable,
    glyphs: readonly Glyph[],
  ) {
    this.intensityGlyph = glyphs.find((g) => g.layer === "modifier" && g.role === "intensity");
    this.directionGlyph = glyphs.find((g) => g.layer === "modifier" && g.role === "direction");
  }

  /**
   * @param rank  identifiant du rang de l'alchimiste
   * @param known glyphes compris ; null pour ne pas vérifier
   */
  analyze(parsed: ParsedCircle, rank: string, known: ReadonlySet<string> | null): Analysis {
    const issues: CircleIssue[] = [...parsed.issues];
    const effects: StageEffect[] = [];
    let complexity = 0;
    let concentration = 0;
    let satelliteCount = 0;
    let required = 0;
    let stability = Number.POSITIVE_INFINITY;
    let severity = 0;

    for (const stage of parsed.stages) {
      const written: Glyph[] = stage.glyphs.map((g) => g.glyph);
      for (const s of stage.satellites) written.push(...s.glyphs.map((g) => g.glyph));
      for (let i = 0; i < stage.intensity; i++) if (this.intensityGlyph) written.push(this.intensityGlyph);
      if (stage.direction !== null && this.directionGlyph) written.push(this.directionGlyph);
      satelliteCount += stage.satellites.length;

      let load = 0;
      for (const g of written) {
        load += g.complexity;
        concentration += g.concentration;
        required = Math.max(required, rankIndex(g.rank));
      }
      if (known !== null) {
        for (const id of new Set(written.map((g) => g.id))) {
          if (!known.has(id)) issues.push(issue("glyph_not_learned", null, id));
        }
      }
      complexity += load;

      if (load > 0) {
        let capacity = stage.capacity === 0 ? BARE_RING_CAPACITY : stage.capacity;
        if (symmetric(stage.glyphs)) capacity *= SYMMETRY_BONUS;
        stability = Math.min(stability, capacity / load);
      }
      required = Math.max(required, rankFor(stage.sides, (r) => r.maxPolygonSides));
      required = Math.max(required, rankFor(stage.polygons, (r) => r.maxPolygonsPerStage));
      if (stage.index > 0 && stage.link === "none" && load > 0) {
        issues.push(issue("unlinked_stage", null, String(stage.index)));
      }
      effects.push(...this.effectsOf(stage, issues));
    }
    concentration *= Math.max(1, parsed.stages.length);
    required = Math.max(required, rankFor(parsed.stages.length, (r) => r.maxStages));
    required = Math.max(required, rankFor(complexity, (r) => r.complexityCap));
    required = Math.max(required, rankFor(satelliteCount, (r) => r.maxSatellites));
    if (rankIndex(rank) < required) issues.push(issue("rank_too_low", null, RANKS[required].id));
    if (stability === Number.POSITIVE_INFINITY) {
      stability = 0;
    } else if (stability < 1) {
      issues.push(issue("unstable"));
      severity = Math.max(severity, 1 - stability);
    }

    for (const i of issues) {
      if (MISCOMPOSED.has(i.kind)) severity = Math.max(severity, UNKNOWN_COMBINATION_SEVERITY);
      else if (i.kind === "unknown_glyph") severity = Math.min(1, severity + UNKNOWN_GLYPH_SEVERITY);
    }

    let outcome: Outcome;
    if (parsed.stages.length === 0 || issues.some((i) => INERT.has(i.kind))) {
      outcome = "inert";
      severity = 0;
    } else if (severity > 0) {
      outcome = "rebound";
    } else if (effects.length === 0) {
      outcome = "inert";
    } else {
      outcome = "works";
    }
    return {
      parsed,
      complexity,
      requiredRank: RANKS[required].id,
      stability,
      concentration,
      effects,
      issues,
      outcome,
      reboundSeverity: severity,
    };
  }

  private effectsOf(stage: Stage, issues: CircleIssue[]): StageEffect[] {
    const out: StageEffect[] = [];
    const direction = stage.direction ?? Number.NaN;
    const infusions = new Set<string>();
    const complete: Satellite[] = [];
    for (const s of stage.satellites) {
      const hasElement = inLayer(s.glyphs, "element").length > 0;
      const actions = inLayer(s.glyphs, "action").length;
      if (hasElement && actions === 0) {
        for (const g of inLayer(s.glyphs, "element")) infusions.add(g.glyph.role);
      } else if (hasElement && actions === 1) {
        complete.push(s);
      } else {
        issues.push(issue("satellite_incomplete", s.center));
      }
    }

    const main = this.combination(inLayer(stage.glyphs, "element"), inLayer(stage.glyphs, "action"), stage.hexagram, issues);
    if (main) {
      out.push({
        stage: stage.index,
        satellite: -1,
        combination: main,
        range: main.range * (1 + INTENSITY_STEP * stage.intensity),
        intensity: stage.intensity,
        origin: vec(0, 0),
        direction,
        infusions,
        link: stage.link,
      });
    }
    stage.satellites.forEach((s, index) => {
      if (!complete.includes(s)) return;
      // Un satellite n'a pas de polygone à lui : il ne fusionne pas deux éléments.
      const c = this.combination(inLayer(s.glyphs, "element"), inLayer(s.glyphs, "action"), false, issues);
      if (c) {
        out.push({
          stage: stage.index,
          satellite: index,
          combination: c,
          range: c.range,
          intensity: 0,
          origin: s.center,
          direction: Number.isNaN(direction) ? angle(s.center) : direction,
          infusions: new Set(),
          link: stage.link,
        });
      }
    });
    return out;
  }

  private combination(
    elements: PlacedGlyph[],
    actions: PlacedGlyph[],
    hexagram: boolean,
    issues: CircleIssue[],
  ): Combination | undefined {
    if (actions.length === 0 && elements.length === 0) return undefined;
    if (actions.length === 0) {
      issues.push(issue("no_action"));
      return undefined;
    }
    if (actions.length > 1) {
      issues.push(issue("too_many_actions", actions[1].position));
      return undefined;
    }
    if (elements.length === 0) {
      issues.push(issue("no_element"));
      return undefined;
    }
    const roles = new Set(elements.map((e) => e.glyph.role));
    if (roles.size > 1 && !hexagram) {
      issues.push(issue("fusion_needs_hexagram", elements[1].position));
      return undefined;
    }
    const action = actions[0].glyph.role;
    const c = this.combinations.find(roles, action);
    if (!c) issues.push(issue("unknown_combination", null, `${[...roles].join("+")}+${action}`));
    return c;
  }
}

/** Symétrie miroir (axe vertical ou horizontal) ou demi-tour : chaque glyphe a son image. */
export function symmetric(glyphs: PlacedGlyph[]): boolean {
  const mirrored = (image: (v: Vec2) => Vec2): boolean =>
    glyphs.every((g) => {
      const target = image(g.position);
      return glyphs.some(
        (o) => o.glyph.id === g.glyph.id && Math.hypot(o.position.x - target.x, o.position.y - target.y) <= MIRROR_TOLERANCE,
      );
    });
  return mirrored((v) => vec(-v.x, v.y)) || mirrored((v) => vec(v.x, -v.y)) || mirrored((v) => vec(-v.x, -v.y));
}
