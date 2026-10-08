// Portage de GlyphRecognizer : nuages de points normalisés, distance de chanfrein dans les deux
// sens, recherche de l'orientation dans la marge du glyphe.

import { type Primitive, type Vec2, distance, rotate, sample, scale, sub, toRadians, vec } from "./geometry";
import { type Glyph, type Layer, anyRotation } from "./glyph";

const ROTATION_STEP_DEG = 3;
const MAX_DEVIATION_FACTOR = 3;

export interface Match {
  readonly glyph: Glyph;
  readonly score: number;
  readonly rotationDeg: number;
}

type Shape = Vec2[];

function extent(primitives: Primitive[]): number {
  let minX = Number.MAX_VALUE;
  let minY = Number.MAX_VALUE;
  let maxX = -Number.MAX_VALUE;
  let maxY = -Number.MAX_VALUE;
  for (const p of primitives) {
    for (const v of sample(p, 0.25)) {
      minX = Math.min(minX, v.x);
      maxX = Math.max(maxX, v.x);
      minY = Math.min(minY, v.y);
      maxY = Math.max(maxY, v.y);
    }
  }
  return Math.max(maxX - minX, maxY - minY);
}

function shapeOf(primitives: Primitive[]): Shape | null {
  const size = extent(primitives);
  if (size <= 0) return null;
  const step = size / 48;
  const pts: Vec2[] = [];
  for (const p of primitives) pts.push(...sample(p, step));
  let cx = 0;
  let cy = 0;
  for (const p of pts) {
    cx += p.x;
    cy += p.y;
  }
  const c = vec(cx / pts.length, cy / pts.length);
  let sq = 0;
  for (const p of pts) {
    const d = distance(p, c);
    sq += d * d;
  }
  const rms = Math.sqrt(sq / pts.length);
  if (rms <= 1e-9) return null;
  return pts.map((p) => scale(sub(p, c), 1 / rms));
}

function nearest(p: Vec2, pts: Vec2[]): number {
  let best = Number.MAX_VALUE;
  for (const q of pts) {
    const dx = p.x - q.x;
    const dy = p.y - q.y;
    const d = dx * dx + dy * dy;
    if (d < best) best = d;
  }
  return Math.sqrt(best);
}

function aspect(pts: Vec2[]): number {
  let minX = Number.MAX_VALUE;
  let minY = Number.MAX_VALUE;
  let maxX = -Number.MAX_VALUE;
  let maxY = -Number.MAX_VALUE;
  for (const p of pts) {
    minX = Math.min(minX, p.x);
    maxX = Math.max(maxX, p.x);
    minY = Math.min(minY, p.y);
    maxY = Math.max(maxY, p.y);
  }
  return Math.max(maxX - minX, 0.2) / Math.max(maxY - minY, 0.2);
}

class Template {
  readonly shape: Shape | null;

  constructor(readonly glyph: Glyph) {
    this.shape = shapeOf(glyph.primitives);
  }

  match(drawn: Shape, enforce = true): Match | null {
    const shape = this.shape!;
    const tol = this.glyph.tolerance;
    const range = anyRotation(tol) ? 180 : tol.rotationDeg;
    let best: Match | null = null;
    for (let deg = -range; deg <= range + 1e-9; deg += ROTATION_STEP_DEG) {
      const rotated = drawn.map((p) => rotate(p, toRadians(-deg)));
      if (enforce && !anyRotation(tol)) {
        const ratio = aspect(rotated) / aspect(shape);
        if (!(ratio >= tol.minScale && ratio <= tol.maxScale)) continue;
      }
      let forward = 0;
      let backward = 0;
      let worst = 0;
      for (const p of rotated) {
        const d = nearest(p, shape);
        forward += d;
        worst = Math.max(worst, d);
      }
      for (const p of shape) {
        const d = nearest(p, rotated);
        backward += d;
        worst = Math.max(worst, d);
      }
      const score = (forward / rotated.length + backward / shape.length) / 2;
      if (enforce && (score > tol.position || worst > tol.position * MAX_DEVIATION_FACTOR)) continue;
      if (best === null || score < best.score) best = { glyph: this.glyph, score, rotationDeg: deg };
    }
    return best;
  }
}

export class GlyphRecognizer {
  private readonly templates: Template[];

  constructor(glyphs: readonly Glyph[]) {
    // Un glyphe réduit à un point (l'Intensité) n'a pas de forme : le parseur le reconnaît à sa
    // place sur l'anneau.
    this.templates = glyphs.map((g) => new Template(g)).filter((t) => t.shape !== null);
  }

  recognize(strokes: Primitive[], onlyLayer?: Layer): Match | null {
    const drawn = shapeOf(strokes);
    if (drawn === null) return null;
    let best: Match | null = null;
    for (const t of this.templates) {
      if (onlyLayer !== undefined && t.glyph.layer !== onlyLayer) continue;
      const m = t.match(drawn);
      if (m !== null && (best === null || m.score < best.score)) best = m;
    }
    return best;
  }

  /** Écart à chaque glyphe sans appliquer les marges : pour les fiches de l'éditeur. */
  distances(strokes: Primitive[]): Map<Glyph, number> {
    const drawn = shapeOf(strokes);
    const out = new Map<Glyph, number>();
    if (drawn === null) return out;
    for (const t of this.templates) {
      const m = t.match(drawn, false);
      out.set(t.glyph, m === null ? Number.POSITIVE_INFINITY : m.score);
    }
    return out;
  }
}
