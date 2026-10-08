// Le canevas : la même grille de 32 cases que le Carnet de cercles, les mêmes outils, la même
// accroche à la demi-case. Le tracé produit est exactement celui que le jeu lira.

import type { Analysis } from "../alchemy/analyzer";
import {
  CENTER_POINT,
  GRID,
  MAX_PRIMITIVES,
  type Primitive,
  type Vec2,
  add,
  angle,
  distance,
  mapPrimitive,
  polar,
  sample,
  samePrimitive,
  scale,
  sub,
  toDegrees,
  toRadians,
  vec,
} from "../alchemy/geometry";
import type { Glyph } from "../alchemy/glyph";

export type Tool = "line" | "circle" | "polygon" | "arc" | "dot" | "stamp";

const PAPER = "#ede3c8";
const GRID_LINE = "rgba(0,0,0,0.13)";
const AXIS_LINE = "rgba(0,0,0,0.27)";
const INK = "#2b2b40";
const PREVIEW = "#3a6fd8";
const GLYPH_MARK = "#2e9e4f";
const ISSUE_MARK = "#d03a2f";

const snap = (v: number): number => Math.round(v * 2) / 2;
const snapVec = (v: Vec2): Vec2 => vec(snap(v.x), snap(v.y));

/** Pose un glyphe comme le ferait un joueur dans le carnet : chaque point accroché à la demi-case. */
export function stampGlyph(glyph: Glyph, at: Vec2, halfSize: number): Primitive[] {
  return glyph.primitives.map((p) => {
    const placed = mapPrimitive(p, (v) => add(scale(v, halfSize), at));
    switch (placed.type) {
      case "circle":
        return { ...placed, center: snapVec(placed.center), radius: Math.max(0.5, snap(placed.radius)) };
      case "arc":
        return {
          ...placed,
          center: snapVec(placed.center),
          radius: Math.max(0.5, snap(placed.radius)),
          startDeg: Math.round(placed.startDeg / 15) * 15,
          sweepDeg: Math.round(placed.sweepDeg / 15) * 15,
        };
      default:
        return mapPrimitive(placed, snapVec);
    }
  });
}

/** Image d'une primitive par la symétrie d'axe vertical du carnet. */
export function mirror(p: Primitive): Primitive {
  if (p.type === "arc") {
    return { ...p, center: vec(GRID - p.center.x, p.center.y), startDeg: 180 - (p.startDeg + p.sweepDeg) };
  }
  return mapPrimitive(p, (v) => vec(GRID - v.x, v.y));
}

export class CircleCanvas {
  primitives: Primitive[] = [];
  tool: Tool = "line";
  sides = 3;
  symmetry = false;
  stampGlyph: Glyph | null = null;
  stampSize = 3;
  analysis: Analysis | null = null;
  highlight: Vec2 | null = null;

  private readonly ctx: CanvasRenderingContext2D;
  private undoStack: Primitive[][] = [];
  private redoStack: Primitive[][] = [];
  private anchor: Vec2 | null = null;
  private arcStart: Vec2 | null = null;
  private cursor: Vec2 = CENTER_POINT;
  private inside = false;

  constructor(
    readonly canvas: HTMLCanvasElement,
    private readonly onChange: () => void,
  ) {
    this.ctx = canvas.getContext("2d")!;
    canvas.addEventListener("pointerdown", (e) => this.down(e));
    canvas.addEventListener("pointermove", (e) => this.move(e));
    canvas.addEventListener("pointerup", (e) => this.up(e));
    canvas.addEventListener("pointerleave", () => {
      this.inside = false;
      this.draw();
    });
    canvas.addEventListener("contextmenu", (e) => e.preventDefault());
    new ResizeObserver(() => this.resize()).observe(canvas);
    this.resize();
  }

  // ---- Modifications ------------------------------------------------------------------------------

  set(primitives: Primitive[], remember = true): void {
    if (remember) {
      this.undoStack.push(this.primitives);
      this.redoStack = [];
    }
    this.primitives = primitives.slice(0, MAX_PRIMITIVES);
    this.resetGesture();
    this.onChange();
  }

  undo(): void {
    const prev = this.undoStack.pop();
    if (prev) {
      this.redoStack.push(this.primitives);
      this.primitives = prev;
      this.resetGesture();
      this.onChange();
    }
  }

  redo(): void {
    const next = this.redoStack.pop();
    if (next) {
      this.undoStack.push(this.primitives);
      this.primitives = next;
      this.onChange();
    }
  }

  private commit(added: Primitive[]): void {
    const out = [...this.primitives];
    for (const p of added) {
      out.push(p);
      const m = mirror(p);
      if (this.symmetry && !samePrimitive(m, p)) out.push(m);
    }
    this.set(out);
  }

  private erase(at: Vec2): void {
    let best = -1;
    let bestDistance = 1;
    this.primitives.forEach((p, i) => {
      for (const v of sample(p, 0.25)) {
        const d = distance(v, at);
        if (d < bestDistance) {
          bestDistance = d;
          best = i;
        }
      }
    });
    if (best >= 0) this.set(this.primitives.filter((_, i) => i !== best));
  }

  resetGesture(): void {
    this.anchor = null;
    this.arcStart = null;
  }

  // ---- Saisie --------------------------------------------------------------------------------------

  private toGrid(e: PointerEvent): Vec2 {
    const r = this.canvas.getBoundingClientRect();
    const cell = r.width / GRID;
    const gx = Math.min(GRID, Math.max(0, (e.clientX - r.left) / cell));
    const gy = Math.min(GRID, Math.max(0, (e.clientY - r.top) / cell));
    return vec(snap(gx), snap(gy));
  }

  private down(e: PointerEvent): void {
    const p = this.toGrid(e);
    if (e.button === 2) {
      this.resetGesture();
      this.erase(p);
      return;
    }
    if (e.button !== 0) return;
    this.canvas.setPointerCapture(e.pointerId);
    switch (this.tool) {
      case "dot":
        this.commit([{ type: "dot", at: p }]);
        break;
      case "stamp":
        if (this.stampGlyph) this.commit(stampGlyph(this.stampGlyph, p, this.stampSize));
        break;
      case "arc":
        if (this.arcStart && this.anchor) this.commit([arcFrom(this.anchor, this.arcStart, p)]);
        else this.anchor = p;
        break;
      default:
        this.anchor = p;
    }
    this.draw();
  }

  private move(e: PointerEvent): void {
    this.cursor = this.toGrid(e);
    this.inside = true;
    this.draw();
  }

  private up(e: PointerEvent): void {
    if (!this.anchor || e.button !== 0) return;
    const p = this.toGrid(e);
    const a = this.anchor;
    switch (this.tool) {
      case "line":
        if (distance(a, p) > 0) this.commit([{ type: "line", from: a, to: p }]);
        else this.resetGesture();
        break;
      case "circle": {
        const r = snap(distance(a, p));
        if (r >= 0.5) this.commit([{ type: "circle", center: a, radius: r }]);
        else this.resetGesture();
        break;
      }
      case "polygon":
        if (distance(a, p) >= 0.5) this.commit([polygonFrom(a, p, this.sides)]);
        else this.resetGesture();
        break;
      case "arc":
        if (!this.arcStart) {
          if (distance(a, p) >= 0.5) this.arcStart = p;
          else this.resetGesture();
        }
        break;
    }
    this.draw();
  }

  private preview(): Primitive | null {
    const a = this.anchor;
    if (this.tool === "stamp" && this.stampGlyph && this.inside) return null;
    if (!a) return null;
    switch (this.tool) {
      case "line":
        return { type: "line", from: a, to: this.cursor };
      case "circle":
        return { type: "circle", center: a, radius: Math.max(0.5, snap(distance(a, this.cursor))) };
      case "polygon":
        return distance(a, this.cursor) >= 0.5 ? polygonFrom(a, this.cursor, this.sides) : null;
      case "arc":
        return this.arcStart ? arcFrom(a, this.arcStart, this.cursor) : { type: "line", from: a, to: this.cursor };
      default:
        return null;
    }
  }

  // ---- Rendu -----------------------------------------------------------------------------------------

  private resize(): void {
    const size = this.canvas.getBoundingClientRect().width;
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = Math.round(size * dpr);
    this.canvas.height = Math.round(size * dpr);
    this.draw();
  }

  draw(): void {
    const ctx = this.ctx;
    const px = this.canvas.width;
    const cell = px / GRID;
    ctx.save();
    ctx.fillStyle = PAPER;
    ctx.fillRect(0, 0, px, px);
    for (let i = 0; i <= GRID; i++) {
      ctx.strokeStyle = i === GRID / 2 ? AXIS_LINE : GRID_LINE;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(i * cell + 0.5, 0);
      ctx.lineTo(i * cell + 0.5, px);
      ctx.moveTo(0, i * cell + 0.5);
      ctx.lineTo(px, i * cell + 0.5);
      ctx.stroke();
    }
    const stroke = Math.max(1.5, cell * 0.14);
    for (const p of this.primitives) this.trace(p, INK, stroke, cell);
    const pv = this.preview();
    if (pv) {
      this.trace(pv, PREVIEW, stroke, cell);
      if (this.symmetry) this.trace(mirror(pv), PREVIEW, stroke, cell);
    }
    if (this.tool === "stamp" && this.stampGlyph && this.inside) {
      for (const p of stampGlyph(this.stampGlyph, this.cursor, this.stampSize)) this.trace(p, PREVIEW, stroke, cell);
    }
    if (this.analysis) {
      for (const s of this.analysis.parsed.stages) {
        for (const g of s.glyphs) this.mark(add(g.position, CENTER_POINT), GLYPH_MARK, cell);
        for (const sat of s.satellites) for (const g of sat.glyphs) this.mark(add(g.position, CENTER_POINT), GLYPH_MARK, cell);
      }
      for (const i of this.analysis.issues) if (i.where) this.mark(i.where, ISSUE_MARK, cell);
    }
    if (this.highlight) {
      ctx.strokeStyle = ISSUE_MARK;
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(this.highlight.x * cell, this.highlight.y * cell, cell * 2.2, 0, Math.PI * 2);
      ctx.stroke();
    }
    if (this.inside && this.tool !== "stamp") {
      ctx.fillStyle = PREVIEW;
      ctx.fillRect(this.cursor.x * cell - 2.5, this.cursor.y * cell - 2.5, 5, 5);
    }
    ctx.restore();
  }

  private trace(p: Primitive, color: string, width: number, cell: number, ctx = this.ctx): void {
    ctx.strokeStyle = color;
    ctx.fillStyle = color;
    ctx.lineWidth = width;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    switch (p.type) {
      case "dot":
        ctx.arc(p.at.x * cell, p.at.y * cell, width * 1.2, 0, Math.PI * 2);
        ctx.fill();
        return;
      case "circle":
        ctx.arc(p.center.x * cell, p.center.y * cell, p.radius * cell, 0, Math.PI * 2);
        break;
      case "arc": {
        const a0 = toRadians(p.startDeg);
        ctx.arc(p.center.x * cell, p.center.y * cell, p.radius * cell, a0, a0 + toRadians(p.sweepDeg), p.sweepDeg < 0);
        break;
      }
      case "line":
        ctx.moveTo(p.from.x * cell, p.from.y * cell);
        ctx.lineTo(p.to.x * cell, p.to.y * cell);
        break;
      case "polyline":
      case "polygon":
        p.points.forEach((v, i) => (i === 0 ? ctx.moveTo(v.x * cell, v.y * cell) : ctx.lineTo(v.x * cell, v.y * cell)));
        if (p.type === "polygon") ctx.closePath();
        break;
    }
    ctx.stroke();
  }

  private mark(at: Vec2, color: string, cell: number): void {
    const ctx = this.ctx;
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    const s = cell * 0.8;
    ctx.strokeRect(at.x * cell - s, at.y * cell - s, s * 2, s * 2);
  }

  /** Image PNG du tracé seul, sur fond transparent. */
  toPng(size = 512): string {
    const c = document.createElement("canvas");
    c.width = size;
    c.height = size;
    const ctx = c.getContext("2d")!;
    const cell = size / GRID;
    for (const p of this.primitives) this.trace(p, INK, Math.max(2, cell * 0.14), cell, ctx);
    return c.toDataURL("image/png");
  }
}

/** Polygone régulier de centre `c` dont un sommet est en `vertex`, sommets accrochés à la demi-case. */
export function polygonFrom(c: Vec2, vertex: Vec2, sides: number): Primitive {
  const r = distance(c, vertex);
  const start = angle(sub(vertex, c));
  const points: Vec2[] = [];
  for (let i = 0; i < sides; i++) points.push(snapVec(add(c, polar(r, start + (2 * Math.PI * i) / sides))));
  return { type: "polygon", points };
}

/** Arc de centre `c`, partant de `start`, tournant dans le sens horaire jusqu'à `end`. */
export function arcFrom(c: Vec2, start: Vec2, end: Vec2): Primitive {
  const a0 = toDegrees(angle(sub(start, c)));
  const a1 = toDegrees(angle(sub(end, c)));
  let sweep = (((a1 - a0) % 360) + 360) % 360;
  if (sweep < 1) sweep = 360;
  return { type: "arc", center: c, radius: snap(distance(c, start)), startDeg: Math.round(a0), sweepDeg: Math.round(sweep) };
}

