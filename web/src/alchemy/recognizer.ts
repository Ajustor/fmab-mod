// Portage de GlyphRecognizer : nuages de points normalisés, distance de chanfrein dans les deux
// sens, recherche de l'orientation dans la marge du glyphe.
//
// Les résultats sont exactement ceux du Java (mêmes calculs, dans le même ordre) ; seul le travail
// inutile est évité, car l'éditeur relit le cercle à chaque trait :
// - le plus proche voisin passe par une grille, au lieu de parcourir tous les points ;
// - une orientation est abandonnée dès qu'elle ne peut plus être retenue (hors marge, ou déjà pire
//   que la meilleure) ;
// - un groupe de traits déjà reconnu n'est pas relu (cache, par tracé).

import { type Primitive, type Vec2, distance, sample, scale, sub, toRadians, vec } from "./geometry";
import { type Glyph, type Layer, anyRotation } from "./glyph";

const ROTATION_STEP_DEG = 3;
const MAX_DEVIATION_FACTOR = 3;
/** Au-delà, le cache repart de zéro : un éditeur ouvert longtemps n'y accumule pas tout. */
const CACHE_LIMIT = 4096;

export interface Match {
  readonly glyph: Glyph;
  readonly score: number;
  readonly rotationDeg: number;
}

/** Un nuage de points, coordonnées à part. */
interface Shape {
  readonly xs: Float64Array;
  readonly ys: Float64Array;
}

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
  const xs = new Float64Array(pts.length);
  const ys = new Float64Array(pts.length);
  pts.forEach((p, i) => {
    const v = scale(sub(p, c), 1 / rms);
    xs[i] = v.x;
    ys[i] = v.y;
  });
  return { xs, ys };
}

function aspect(xs: Float64Array, ys: Float64Array): number {
  let minX = Number.MAX_VALUE;
  let minY = Number.MAX_VALUE;
  let maxX = -Number.MAX_VALUE;
  let maxY = -Number.MAX_VALUE;
  for (let i = 0; i < xs.length; i++) {
    minX = Math.min(minX, xs[i]);
    maxX = Math.max(maxX, xs[i]);
    minY = Math.min(minY, ys[i]);
    maxY = Math.max(maxY, ys[i]);
  }
  return Math.max(maxX - minX, 0.2) / Math.max(maxY - minY, 0.2);
}

/**
 * Les points rangés par case, pour trouver le plus proche sans tout parcourir. La distance rendue
 * est calculée comme en parcourant tout (même carré, même racine) : seul l'ordre de visite change,
 * et le minimum n'en dépend pas.
 */
export class Grid {
  private static readonly CELL = 0.25;
  private readonly minX: number;
  private readonly minY: number;
  private readonly cols: number;
  private readonly rows: number;
  private readonly start: Int32Array;
  private readonly order: Int32Array;

  constructor(
    private readonly xs: Float64Array,
    private readonly ys: Float64Array,
  ) {
    let minX = Number.MAX_VALUE;
    let minY = Number.MAX_VALUE;
    let maxX = -Number.MAX_VALUE;
    let maxY = -Number.MAX_VALUE;
    for (let i = 0; i < xs.length; i++) {
      minX = Math.min(minX, xs[i]);
      maxX = Math.max(maxX, xs[i]);
      minY = Math.min(minY, ys[i]);
      maxY = Math.max(maxY, ys[i]);
    }
    this.minX = minX;
    this.minY = minY;
    this.cols = Math.floor((maxX - minX) / Grid.CELL) + 1;
    this.rows = Math.floor((maxY - minY) / Grid.CELL) + 1;
    const counts = new Int32Array(this.cols * this.rows + 1);
    const cellOf = new Int32Array(xs.length);
    for (let i = 0; i < xs.length; i++) {
      const c = this.col(xs[i]) + this.row(ys[i]) * this.cols;
      cellOf[i] = c;
      counts[c + 1]++;
    }
    for (let c = 0; c < this.cols * this.rows; c++) counts[c + 1] += counts[c];
    this.start = counts.slice();
    this.order = new Int32Array(xs.length);
    const fill = counts.slice();
    for (let i = 0; i < xs.length; i++) this.order[fill[cellOf[i]]++] = i;
  }

  private col(x: number): number {
    return Math.min(this.cols - 1, Math.max(0, Math.floor((x - this.minX) / Grid.CELL)));
  }

  private row(y: number): number {
    return Math.min(this.rows - 1, Math.max(0, Math.floor((y - this.minY) / Grid.CELL)));
  }

  /** Distance au point le plus proche. */
  nearest(px: number, py: number): number {
    const cx = this.col(px);
    const cy = this.row(py);
    let best = Number.MAX_VALUE;
    const maxRing = Math.max(this.cols, this.rows);
    for (let ring = 0; ring <= maxRing; ring++) {
      // Tout point hors des anneaux déjà vus est au moins à cette distance de (px, py).
      if (ring > 0) {
        const reach = this.reach(px, py, cx, cy, ring);
        if (reach * reach > best) break;
      }
      const x0 = cx - ring;
      const x1 = cx + ring;
      const y0 = cy - ring;
      const y1 = cy + ring;
      for (let y = Math.max(0, y0); y <= Math.min(this.rows - 1, y1); y++) {
        const edgeRow = y === y0 || y === y1;
        for (let x = Math.max(0, x0); x <= Math.min(this.cols - 1, x1); x++) {
          if (!edgeRow && x !== x0 && x !== x1) continue;
          const c = x + y * this.cols;
          for (let k = this.start[c]; k < this.start[c + 1]; k++) {
            const i = this.order[k];
            const dx = px - this.xs[i];
            const dy = py - this.ys[i];
            const d = dx * dx + dy * dy;
            if (d < best) best = d;
          }
        }
      }
    }
    return Math.sqrt(best);
  }

  /** La plus courte distance de (px, py) à une case hors du carré de {@code ring - 1} cases autour de la sienne. */
  private reach(px: number, py: number, cx: number, cy: number, ring: number): number {
    const left = px - (this.minX + (cx - ring + 1) * Grid.CELL);
    const right = this.minX + (cx + ring) * Grid.CELL - px;
    const top = py - (this.minY + (cy - ring + 1) * Grid.CELL);
    const bottom = this.minY + (cy + ring) * Grid.CELL - py;
    return Math.max(0, Math.min(left, right, top, bottom));
  }
}

class Template {
  readonly shape: Shape | null;
  private readonly grid: Grid | null;
  private readonly aspect: number;

  constructor(readonly glyph: Glyph) {
    this.shape = shapeOf(glyph.primitives);
    this.grid = this.shape === null ? null : new Grid(this.shape.xs, this.shape.ys);
    this.aspect = this.shape === null ? 1 : aspect(this.shape.xs, this.shape.ys);
  }

  match(drawn: Shape, enforce = true): Match | null {
    const shape = this.shape!;
    const grid = this.grid!;
    const tol = this.glyph.tolerance;
    const any = anyRotation(tol);
    const range = any ? 180 : tol.rotationDeg;
    const n = drawn.xs.length;
    const m = shape.xs.length;
    const rx = new Float64Array(n);
    const ry = new Float64Array(n);
    const worstLimit = tol.position * MAX_DEVIATION_FACTOR;
    let best: Match | null = null;
    for (let deg = -range; deg <= range + 1e-9; deg += ROTATION_STEP_DEG) {
      // Comme rotate(p, toRadians(-deg)), point par point.
      const radians = toRadians(-deg);
      const c = Math.cos(radians);
      const s = Math.sin(radians);
      for (let i = 0; i < n; i++) {
        rx[i] = drawn.xs[i] * c - drawn.ys[i] * s;
        ry[i] = drawn.xs[i] * s + drawn.ys[i] * c;
      }
      if (enforce && !any) {
        const ratio = aspect(rx, ry) / this.aspect;
        if (!(ratio >= tol.minScale && ratio <= tol.maxScale)) continue;
      }
      // Le score ne peut que croître en ajoutant des distances : dès qu'il dépasse ce qu'on peut
      // retenir, l'orientation est perdue.
      const ceiling = Math.min(enforce ? tol.position : Number.POSITIVE_INFINITY, best === null ? Number.POSITIVE_INFINITY : best.score);
      let forward = 0;
      let backward = 0;
      let worst = 0;
      let lost = false;
      for (let i = 0; i < n; i++) {
        const d = grid.nearest(rx[i], ry[i]);
        forward += d;
        worst = Math.max(worst, d);
        if ((enforce && worst > worstLimit) || forward / n / 2 > ceiling) {
          lost = true;
          break;
        }
      }
      if (lost) continue;
      const back = new Grid(rx, ry);
      for (let i = 0; i < m; i++) {
        const d = back.nearest(shape.xs[i], shape.ys[i]);
        backward += d;
        worst = Math.max(worst, d);
        if ((enforce && worst > worstLimit) || (forward / n + backward / m) / 2 > ceiling) {
          lost = true;
          break;
        }
      }
      if (lost) continue;
      const score = (forward / n + backward / m) / 2;
      if (enforce && (score > tol.position || worst > worstLimit)) continue;
      if (best === null || score < best.score) best = { glyph: this.glyph, score, rotationDeg: deg };
    }
    return best;
  }
}

/** La clé d'un groupe de traits : deux groupes de mêmes traits, dans le même ordre, se reconnaissent pareil. */
function keyOf(strokes: Primitive[], layer: string): string {
  return layer + JSON.stringify(strokes);
}

export class GlyphRecognizer {
  private readonly templates: Template[];
  private readonly matches = new Map<string, Match | null>();
  private readonly gaps = new Map<string, Map<Glyph, number>>();

  constructor(glyphs: readonly Glyph[]) {
    // Un glyphe réduit à un point (l'Intensité) n'a pas de forme : le parseur le reconnaît à sa
    // place sur l'anneau.
    this.templates = glyphs.map((g) => new Template(g)).filter((t) => t.shape !== null);
  }

  recognize(strokes: Primitive[], onlyLayer?: Layer): Match | null {
    const key = keyOf(strokes, onlyLayer ?? "");
    const known = this.matches.get(key);
    if (known !== undefined) return known;
    const found = this.compute(strokes, onlyLayer);
    if (this.matches.size >= CACHE_LIMIT) this.matches.clear();
    this.matches.set(key, found);
    return found;
  }

  private compute(strokes: Primitive[], onlyLayer?: Layer): Match | null {
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
    const key = keyOf(strokes, "*");
    const known = this.gaps.get(key);
    if (known !== undefined) return known;
    const drawn = shapeOf(strokes);
    const out = new Map<Glyph, number>();
    if (drawn !== null) {
      for (const t of this.templates) {
        const m = t.match(drawn, false);
        out.set(t.glyph, m === null ? Number.POSITIVE_INFINITY : m.score);
      }
    }
    if (this.gaps.size >= CACHE_LIMIT) this.gaps.clear();
    this.gaps.set(key, out);
    return out;
  }
}
