// Portage de CircleParser, Stage, Satellite, PlacedGlyph, CircleIssue et LinkKind.

import {
  CENTER_POINT,
  type Primitive,
  type Vec2,
  angle,
  distance,
  length,
  polygonCentroid,
  sample,
  samePrimitive,
  sub,
  toRadians,
  vec,
} from "./geometry";
import type { Glyph, Layer } from "./glyph";
import type { GlyphRecognizer } from "./recognizer";

export type LinkKind = "none" | "series" | "parallel" | "conditional";
/** Ordre de l'énumération Java : en cas de conflit, la première trouvée dans cet ordre l'emporte. */
const LINK_ORDER: LinkKind[] = ["series", "parallel", "conditional"];

export type IssueKind =
  | "no_ring"
  | "unknown_glyph"
  | "outside_ring"
  | "loose_polygon"
  | "no_action"
  | "no_element"
  | "too_many_actions"
  | "unknown_combination"
  | "rank_too_low"
  | "glyph_not_learned"
  | "unstable"
  | "unlinked_stage"
  | "conflicting_links"
  | "fusion_needs_hexagram"
  | "satellite_incomplete"
  | "knowledge_missing"
  | "incomplete_formula";

export interface CircleIssue {
  readonly kind: IssueKind;
  /** Coordonnées de grille du carnet, ou null quand le problème concerne tout le cercle. */
  readonly where: Vec2 | null;
  readonly detail: string;
}

export const issue = (kind: IssueKind, where: Vec2 | null = null, detail = ""): CircleIssue => ({ kind, where, detail });

export interface PlacedGlyph {
  readonly glyph: Glyph;
  /** Par rapport au centre du carnet. */
  readonly position: Vec2;
  readonly rotationDeg: number;
}

export interface Satellite {
  readonly center: Vec2;
  readonly radius: number;
  readonly glyphs: PlacedGlyph[];
}

export interface Stage {
  readonly index: number;
  readonly ringRadius: number;
  readonly sides: number;
  readonly hexagram: boolean;
  readonly polygons: number;
  readonly capacity: number;
  readonly glyphs: PlacedGlyph[];
  readonly intensity: number;
  /** Angle de la flèche de direction (radians, y vers le bas), ou null. */
  readonly direction: number | null;
  readonly link: LinkKind;
  readonly satellites: Satellite[];
}

export interface ParsedCircle {
  readonly stages: Stage[];
  readonly issues: CircleIssue[];
}

export const inLayer = (glyphs: PlacedGlyph[], layer: Layer): PlacedGlyph[] =>
  glyphs.filter((g) => g.glyph.layer === layer);

export const MIN_RING_RADIUS = 5;
export const MIN_SATELLITE_RADIUS = 2.5;
const CENTER_TOLERANCE = 0.75;
const ON_RING = 1.0;
const SAME_RING = 1.0;
const GROUP_MARGIN = 0.5;
const DOUBLE_LINK_ANGLE = toRadians(15);
const DOUBLE_LINK_DISTANCE = 2.5;
const DASH_ANGLE = toRadians(8);
const DASH_OFFSET = 0.6;

const radius = (p: Vec2): number => distance(p, CENTER_POINT);

/** Retire la première primitive égale (List.remove(Object) en Java). */
function removeFirst(list: Primitive[], p: Primitive): void {
  const i = list.findIndex((q) => samePrimitive(q, p));
  if (i >= 0) list.splice(i, 1);
}

export class CircleParser {
  constructor(private readonly recognizer: GlyphRecognizer) {}

  parse(primitives: Primitive[]): ParsedCircle {
    const issues: CircleIssue[] = [];
    const rest: Primitive[] = [];
    const rings: number[] = [];

    // 1. Anneaux.
    for (const p of primitives) {
      if (p.type === "circle" && distance(p.center, CENTER_POINT) <= CENTER_TOLERANCE && p.radius >= MIN_RING_RADIUS) {
        if (!rings.some((r) => Math.abs(r - p.radius) < SAME_RING)) rings.push(p.radius);
      } else {
        rest.push(p);
      }
    }
    if (rings.length === 0) {
      issues.push(issue("no_ring"));
      return { stages: [], issues };
    }
    rings.sort((a, b) => a - b);

    const n = rings.length;
    const sides = new Array<number>(n).fill(0);
    const triangles = new Array<number>(n).fill(0);
    const polygons = new Array<number>(n).fill(0);
    const capacity = new Array<number>(n).fill(0);
    const intensity = new Array<number>(n).fill(0);
    const direction = new Array<number | null>(n).fill(null);
    const vertices: Vec2[][] = Array.from({ length: n }, () => []);
    const glyphs: PlacedGlyph[][] = Array.from({ length: n }, () => []);
    const satellites: Satellite[][] = Array.from({ length: n }, () => []);

    // 2. Polygones inscrits.
    const afterPolygons: Primitive[] = [];
    for (const p of rest) {
      if (p.type === "polygon") {
        const ring = inscribedIn(p.points, rings);
        if (ring >= 0) {
          sides[ring] = Math.max(sides[ring], p.points.length);
          polygons[ring]++;
          capacity[ring] += p.points.length;
          if (p.points.length === 3) triangles[ring]++;
          vertices[ring].push(...p.points);
          continue;
        }
        if (isLarge(p.points)) {
          issues.push(issue("loose_polygon", polygonCentroid(p.points)));
          continue;
        }
      }
      afterPolygons.push(p);
    }

    // 3. Satellites.
    const drafts: { ring: number; circle: Extract<Primitive, { type: "circle" }> }[] = [];
    const afterSatellites: Primitive[] = [];
    for (const p of afterPolygons) {
      if (p.type === "circle" && p.radius >= MIN_SATELLITE_RADIUS && p.radius < MIN_RING_RADIUS) {
        const ring = vertexRing(p.center, vertices);
        if (ring >= 0) {
          drafts.push({ ring, circle: p });
          continue;
        }
      }
      afterSatellites.push(p);
    }

    // 4. Liaisons.
    const links = new Array<LinkKind>(n).fill("none");
    const afterLinks = findLinks(afterSatellites, rings, links, issues);

    // 5. Points d'intensité.
    const strokes: Primitive[] = [];
    for (const p of afterLinks) {
      if (p.type === "dot" && !drafts.some((d) => distance(d.circle.center, p.at) < d.circle.radius)) {
        const ring = onRing(p.at, rings);
        if (ring >= 0) {
          intensity[ring]++;
          continue;
        }
      }
      strokes.push(p);
    }

    // 6. Contenu des satellites.
    for (const draft of drafts) {
      const inside = strokes.filter((p) => distance(centroid([p]), draft.circle.center) < draft.circle.radius);
      const found: PlacedGlyph[] = [];
      let readable = true;
      for (const g of group(inside)) {
        const m = this.recognizer.recognize(g);
        if (m === null || m.glyph.layer === "modifier") {
          readable = false;
          break;
        }
        found.push({ glyph: m.glyph, position: sub(centroid(g), CENTER_POINT), rotationDeg: m.rotationDeg });
      }
      if (readable) {
        for (const p of inside) {
          // removeAll : toutes les occurrences égales.
          for (let i = strokes.length - 1; i >= 0; i--) {
            if (samePrimitive(strokes[i], p)) strokes.splice(i, 1);
          }
        }
        satellites[draft.ring].push({ center: sub(draft.circle.center, CENTER_POINT), radius: draft.circle.radius, glyphs: found });
      } else {
        strokes.push(draft.circle);
      }
    }

    // 7. Glyphes.
    for (const g of group(strokes)) {
      const at = centroid(g);
      const ring = onRing(at, rings);
      if (ring >= 0) {
        const modifier = this.recognizer.recognize(g, "modifier");
        if (modifier !== null && modifier.glyph.role === "direction") {
          direction[ring] = angle(sub(at, CENTER_POINT));
          continue;
        }
      }
      const m = this.recognizer.recognize(g);
      if (m === null || m.glyph.layer === "modifier") {
        issues.push(issue("unknown_glyph", at, this.closest(g)));
        continue;
      }
      const stage = stageOf(at, rings);
      if (stage < 0) {
        issues.push(issue("outside_ring", at));
        continue;
      }
      glyphs[stage].push({ glyph: m.glyph, position: sub(at, CENTER_POINT), rotationDeg: m.rotationDeg });
    }

    const stages: Stage[] = [];
    for (let i = 0; i < n; i++) {
      const hexagram = triangles[i] >= 2;
      stages.push({
        index: i,
        ringRadius: rings[i],
        sides: Math.max(sides[i], hexagram ? 6 : 0),
        hexagram,
        polygons: polygons[i],
        capacity: capacity[i],
        glyphs: glyphs[i],
        intensity: intensity[i],
        direction: direction[i],
        link: links[i],
        satellites: satellites[i],
      });
    }
    return { stages, issues };
  }

  /** Le glyphe le plus proche d'un tracé non reconnu, « id|écart », comme côté Java. */
  private closest(strokes: Primitive[]): string {
    let best: Glyph | null = null;
    let score = Number.POSITIVE_INFINITY;
    for (const [glyph, d] of this.recognizer.distances(strokes)) {
      if (glyph.layer !== "modifier" && d < score) {
        best = glyph;
        score = d;
      }
    }
    return best === null ? "" : `${best.id}|${score.toFixed(3)}`;
  }
}

interface Segment {
  readonly line: Extract<Primitive, { type: "line" }>;
  readonly inner: Vec2;
  readonly outer: Vec2;
}

const segAngle = (s: Segment): number => angle(sub(s.outer, s.inner));
const segMiddle = (s: Segment): Vec2 => vec((s.inner.x + s.outer.x) * 0.5, (s.inner.y + s.outer.y) * 0.5);

function oriented(line: Extract<Primitive, { type: "line" }>): Segment {
  return radius(line.from) <= radius(line.to)
    ? { line, inner: line.from, outer: line.to }
    : { line, inner: line.to, outer: line.from };
}

function angleBetween(a: number, b: number): number {
  const d = Math.abs(a - b) % (2 * Math.PI);
  return Math.min(d, 2 * Math.PI - d);
}

function aligned(a: Segment, b: Segment): boolean {
  if (angleBetween(segAngle(a), segAngle(b)) > DASH_ANGLE) return false;
  const dir = sub(a.outer, a.inner);
  const len = length(dir);
  if (len < 1e-9) return false;
  const rel = sub(segMiddle(b), a.inner);
  return Math.abs(dir.x * rel.y - dir.y * rel.x) / len <= DASH_OFFSET;
}

function findLinks(strokes: Primitive[], rings: number[], links: LinkKind[], issues: CircleIssue[]): Primitive[] {
  const rest = [...strokes];
  for (let i = 0; i + 1 < rings.length; i++) {
    const inner = rings[i];
    const outer = rings[i + 1];
    const kinds = new Set<LinkKind>();
    let where: Vec2 | null = null;

    const whole: Segment[] = [];
    for (const p of rest) {
      if (p.type === "line") {
        const s = oriented(p);
        if (Math.abs(radius(s.inner) - inner) <= ON_RING && Math.abs(radius(s.outer) - outer) <= ON_RING) whole.push(s);
      }
    }
    const paired = new Array<boolean>(whole.length).fill(false);
    for (let a = 0; a < whole.length; a++) {
      for (let b = a + 1; b < whole.length && !paired[a]; b++) {
        if (
          !paired[b] &&
          angleBetween(segAngle(whole[a]), segAngle(whole[b])) <= DOUBLE_LINK_ANGLE &&
          distance(segMiddle(whole[a]), segMiddle(whole[b])) <= DOUBLE_LINK_DISTANCE
        ) {
          paired[a] = paired[b] = true;
          kinds.add("parallel");
        }
      }
      if (!paired[a]) kinds.add("series");
      where = segMiddle(whole[a]);
      removeFirst(rest, whole[a].line);
    }

    const pieces: Segment[] = [];
    for (const p of rest) {
      if (p.type === "line") {
        const s = oriented(p);
        if (radius(s.inner) >= inner - ON_RING && radius(s.outer) <= outer + ON_RING) pieces.push(s);
      }
    }
    const used = new Array<boolean>(pieces.length).fill(false);
    for (let a = 0; a < pieces.length; a++) {
      if (used[a]) continue;
      const dash: Segment[] = [pieces[a]];
      for (let b = a + 1; b < pieces.length; b++) {
        if (!used[b] && aligned(pieces[a], pieces[b])) dash.push(pieces[b]);
      }
      if (dash.length < 2) continue;
      const from = Math.min(...dash.map((s) => radius(s.inner)));
      const to = Math.max(...dash.map((s) => radius(s.outer)));
      if (Math.abs(from - inner) <= ON_RING && Math.abs(to - outer) <= ON_RING) {
        kinds.add("conditional");
        where = segMiddle(dash[0]);
        for (let b = a; b < pieces.length; b++) {
          if (dash.some((d) => samePrimitive(d.line, pieces[b].line))) {
            used[b] = true;
            removeFirst(rest, pieces[b].line);
          }
        }
      }
    }

    if (kinds.size > 1) issues.push(issue("conflicting_links", where));
    links[i + 1] = LINK_ORDER.find((k) => kinds.has(k)) ?? "none";
  }
  return rest;
}

function inscribedIn(points: Vec2[], rings: number[]): number {
  for (let i = 0; i < rings.length; i++) {
    if (points.every((v) => Math.abs(radius(v) - rings[i]) <= ON_RING)) return i;
  }
  return -1;
}

function vertexRing(p: Vec2, vertices: Vec2[][]): number {
  for (let i = 0; i < vertices.length; i++) {
    if (vertices[i].some((v) => distance(v, p) <= ON_RING)) return i;
  }
  return -1;
}

function isLarge(points: Vec2[]): boolean {
  const c = polygonCentroid(points);
  return points.some((v) => distance(v, c) >= MIN_RING_RADIUS);
}

function onRing(p: Vec2, rings: number[]): number {
  const d = radius(p);
  for (let i = 0; i < rings.length; i++) {
    if (Math.abs(d - rings[i]) <= ON_RING) return i;
  }
  return -1;
}

function stageOf(p: Vec2, rings: number[]): number {
  const d = radius(p);
  for (let i = 0; i < rings.length; i++) {
    if (d < rings[i]) return i;
  }
  return -1;
}

export function centroid(g: Primitive[]): Vec2 {
  let x = 0;
  let y = 0;
  let count = 0;
  for (const p of g) {
    for (const v of sample(p, 0.25)) {
      x += v.x;
      y += v.y;
      count++;
    }
  }
  return vec(x / count, y / count);
}

/** Regroupe les traits dont les boîtes englobantes (élargies) se chevauchent. */
export function group(strokes: Primitive[]): Primitive[][] {
  const n = strokes.length;
  const boxes = strokes.map(box);
  const parent = Array.from({ length: n }, (_, i) => i);
  const find = (i: number): number => {
    while (parent[i] !== i) {
      parent[i] = parent[parent[i]];
      i = parent[i];
    }
    return i;
  };
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      if (overlap(boxes[i], boxes[j])) parent[find(i)] = find(j);
    }
  }
  const groups: Primitive[][] = [];
  const index = new Array<number>(n).fill(-1);
  for (let i = 0; i < n; i++) {
    const root = find(i);
    if (index[root] < 0) {
      index[root] = groups.length;
      groups.push([]);
    }
    groups[index[root]].push(strokes[i]);
  }
  return groups;
}

function box(p: Primitive): [number, number, number, number] {
  let minX = Number.MAX_VALUE;
  let minY = Number.MAX_VALUE;
  let maxX = -Number.MAX_VALUE;
  let maxY = -Number.MAX_VALUE;
  for (const v of sample(p, 0.25)) {
    minX = Math.min(minX, v.x);
    maxX = Math.max(maxX, v.x);
    minY = Math.min(minY, v.y);
    maxY = Math.max(maxY, v.y);
  }
  return [minX - GROUP_MARGIN, minY - GROUP_MARGIN, maxX + GROUP_MARGIN, maxY + GROUP_MARGIN];
}

const overlap = (a: number[], b: number[]): boolean => a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3];
