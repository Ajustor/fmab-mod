// Portage de com.ajustor.fmab.alchemy.drawing (Vec2, Primitive, Drawing). Le Java fait
// référence : chaque calcul suit le même ordre pour donner les mêmes résultats.

export interface Vec2 {
  readonly x: number;
  readonly y: number;
}

export const vec = (x: number, y: number): Vec2 => ({ x, y });
export const add = (a: Vec2, b: Vec2): Vec2 => vec(a.x + b.x, a.y + b.y);
export const sub = (a: Vec2, b: Vec2): Vec2 => vec(a.x - b.x, a.y - b.y);
export const scale = (a: Vec2, k: number): Vec2 => vec(a.x * k, a.y * k);
export const length = (a: Vec2): number => Math.hypot(a.x, a.y);
export const distance = (a: Vec2, b: Vec2): number => Math.hypot(a.x - b.x, a.y - b.y);
/** Angle polaire en radians, y vers le bas. */
export const angle = (a: Vec2): number => Math.atan2(a.y, a.x);
export const polar = (r: number, radians: number): Vec2 => vec(r * Math.cos(radians), r * Math.sin(radians));

export function rotate(a: Vec2, radians: number): Vec2 {
  const c = Math.cos(radians);
  const s = Math.sin(radians);
  return vec(a.x * c - a.y * s, a.x * s + a.y * c);
}

/** Mêmes constantes que Math.toRadians / Math.toDegrees de Java. */
export const toRadians = (deg: number): number => deg * 0.017453292519943295;
export const toDegrees = (rad: number): number => rad * 57.29577951308232;

export type Primitive =
  | { type: "line"; from: Vec2; to: Vec2 }
  | { type: "polyline"; points: Vec2[] }
  | { type: "polygon"; points: Vec2[] }
  | { type: "circle"; center: Vec2; radius: number }
  | { type: "arc"; center: Vec2; radius: number; startDeg: number; sweepDeg: number }
  | { type: "dot"; at: Vec2 };

export const GRID = 32;
export const CENTER = GRID / 2;
export const CENTER_POINT = vec(CENTER, CENTER);
export const MAX_PRIMITIVES = 256;

function segment(a: Vec2, b: Vec2, step: number, out: Vec2[], includeEnd: boolean): void {
  const n = Math.max(1, Math.ceil(distance(a, b) / step));
  const last = includeEnd ? n : n - 1;
  for (let i = 0; i <= last; i++) {
    const t = i / n;
    out.push(vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
  }
}

function sampleArc(center: Vec2, radius: number, startDeg: number, sweepDeg: number, step: number): Vec2[] {
  const len = Math.abs(toRadians(sweepDeg)) * radius;
  const n = Math.max(8, Math.ceil(len / step));
  const out: Vec2[] = [];
  const closed = Math.abs(sweepDeg) >= 360;
  const last = closed ? n - 1 : n;
  for (let i = 0; i <= last; i++) {
    const a = toRadians(startDeg + (sweepDeg * i) / n);
    out.push(add(center, polar(radius, a)));
  }
  return out;
}

/** Points répartis le long du trait, à peu près tous les `step`. */
export function sample(p: Primitive, step: number): Vec2[] {
  const out: Vec2[] = [];
  switch (p.type) {
    case "line":
      segment(p.from, p.to, step, out, true);
      return out;
    case "polyline":
      for (let i = 0; i + 1 < p.points.length; i++) {
        segment(p.points[i], p.points[i + 1], step, out, i + 2 === p.points.length);
      }
      return out;
    case "polygon":
      for (let i = 0; i < p.points.length; i++) {
        segment(p.points[i], p.points[(i + 1) % p.points.length], step, out, false);
      }
      return out;
    case "circle":
      return sampleArc(p.center, p.radius, 0, 360, step);
    case "arc":
      return sampleArc(p.center, p.radius, p.startDeg, p.sweepDeg, step);
    case "dot":
      return [p.at];
  }
}

/** Transforme une primitive ; les transformations utilisées gardent les cercles ronds. */
export function mapPrimitive(p: Primitive, f: (v: Vec2) => Vec2): Primitive {
  switch (p.type) {
    case "line":
      return { type: "line", from: f(p.from), to: f(p.to) };
    case "polyline":
      return { type: "polyline", points: p.points.map(f) };
    case "polygon":
      return { type: "polygon", points: p.points.map(f) };
    case "circle": {
      const c = f(p.center);
      const edge = f(add(p.center, vec(p.radius, 0)));
      return { type: "circle", center: c, radius: distance(c, edge) };
    }
    case "arc": {
      const c = f(p.center);
      const start = f(add(p.center, polar(p.radius, toRadians(p.startDeg))));
      return { type: "arc", center: c, radius: distance(c, start), startDeg: toDegrees(angle(sub(start, c))), sweepDeg: p.sweepDeg };
    }
    case "dot":
      return { type: "dot", at: f(p.at) };
  }
}

export function polygonCentroid(points: Vec2[]): Vec2 {
  let x = 0;
  let y = 0;
  for (const p of points) {
    x += p.x;
    y += p.y;
  }
  return vec(x / points.length, y / points.length);
}

/** Égalité de valeur, comme les records Java. */
export function samePrimitive(a: Primitive, b: Primitive): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

// ---- JSON (même format que GlyphJson) ---------------------------------------------------------------

type JsonVec = [number, number];
const fromJson = (a: JsonVec): Vec2 => vec(a[0], a[1]);
const toJson = (v: Vec2): JsonVec => [v.x, v.y];

export function primitiveFromJson(o: Record<string, unknown>): Primitive {
  switch (o.type) {
    case "line":
      return { type: "line", from: fromJson(o.from as JsonVec), to: fromJson(o.to as JsonVec) };
    case "polyline":
      return { type: "polyline", points: (o.points as JsonVec[]).map(fromJson) };
    case "polygon":
      return { type: "polygon", points: (o.points as JsonVec[]).map(fromJson) };
    case "circle":
      return { type: "circle", center: fromJson(o.center as JsonVec), radius: Number(o.radius) };
    case "arc":
      return {
        type: "arc",
        center: fromJson(o.center as JsonVec),
        radius: Number(o.radius),
        startDeg: Number(o.start_deg),
        sweepDeg: Number(o.sweep_deg),
      };
    case "dot":
      return { type: "dot", at: fromJson(o.at as JsonVec) };
    default:
      throw new Error(`type de primitive inconnu : ${String(o.type)}`);
  }
}

export function primitiveToJson(p: Primitive): Record<string, unknown> {
  switch (p.type) {
    case "line":
      return { type: "line", from: toJson(p.from), to: toJson(p.to) };
    case "polyline":
      return { type: "polyline", points: p.points.map(toJson) };
    case "polygon":
      return { type: "polygon", points: p.points.map(toJson) };
    case "circle":
      return { type: "circle", center: toJson(p.center), radius: p.radius };
    case "arc":
      return { type: "arc", center: toJson(p.center), radius: p.radius, start_deg: p.startDeg, sweep_deg: p.sweepDeg };
    case "dot":
      return { type: "dot", at: toJson(p.at) };
  }
}
