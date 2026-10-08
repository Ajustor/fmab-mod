// Résumé d'une analyse, dans le format exact des cercles de test partagés avec le mod
// (CircleFixturesTest.summary côté Java).

import type { Analysis } from "./analyzer";

const round3 = (x: number): number => Math.round(x * 1000) / 1000;

export function summary(a: Analysis): Record<string, unknown> {
  return {
    outcome: a.outcome,
    complexity: a.complexity,
    concentration: a.concentration,
    stability: round3(a.stability),
    required_rank: a.requiredRank,
    rebound: round3(a.reboundSeverity),
    stages: a.parsed.stages.map((s) => ({
      sides: s.sides,
      capacity: s.capacity,
      hexagram: s.hexagram,
      intensity: s.intensity,
      direction: s.direction !== null,
      link: s.link,
      glyphs: s.glyphs.map((g) => g.glyph.id),
      satellites: s.satellites.length,
    })),
    effects: a.effects.map(
      (e) =>
        e.combination.effect +
        (e.satellite >= 0 ? "@satellite" : "") +
        (e.infusions.size === 0 ? "" : "+" + [...e.infusions].sort().join("+")),
    ),
    issues: a.issues.map((i) => i.kind),
  };
}
