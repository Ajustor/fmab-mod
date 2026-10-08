// Panneau de droite : support et rang, analyse en direct, palette de glyphes, export et import.

import type { Analysis } from "../alchemy/analyzer";
import { decodeDrawing, encodeDrawing } from "../alchemy/code";
import { CENTER_POINT, GRID, type Primitive, primitiveToJson, sample } from "../alchemy/geometry";
import { type Combination, type Glyph, RANKS, rankIndex } from "../alchemy/glyph";
import type { CircleCanvas } from "./canvas";
import { t } from "./i18n";

/** Les supports et le nombre d'étages qu'ils portent ; Infinity pour une surface. */
export const SUPPORTS: { id: string; stages: number }[] = [
  { id: "surface", stages: Infinity },
  { id: "cloth_gloves", stages: 1 },
  { id: "leather_gloves", stages: 2 },
  { id: "spark_gloves", stages: 2 },
  { id: "iron_gauntlets", stages: 2 },
  { id: "state_gloves", stages: 5 },
];

const el = <K extends keyof HTMLElementTagNameMap>(
  tag: K,
  attrs: Record<string, string> = {},
  ...children: (Node | string)[]
): HTMLElementTagNameMap[K] => {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  e.append(...children);
  return e;
};

const effectName = (effect: string): string => t(`effect.${effect.replace(":", ".")}`);
const rankName = (rank: string): string => t(`rank.fmab.${rank}`);

/** Petite image d'un glyphe pour la palette et les fiches. */
export function glyphThumbnail(glyph: Glyph, size = 44): HTMLCanvasElement {
  const c = el("canvas", { width: String(size * 2), height: String(size * 2), class: "thumb" });
  c.style.width = `${size}px`;
  c.style.height = `${size}px`;
  const ctx = c.getContext("2d")!;
  const k = size * 0.8;
  ctx.strokeStyle = "#2b2b40";
  ctx.fillStyle = "#2b2b40";
  ctx.lineWidth = 3;
  ctx.lineCap = "round";
  for (const p of glyph.primitives) {
    const pts = sample(p, 0.02);
    if (p.type === "dot") {
      ctx.beginPath();
      ctx.arc(size + p.at.x * k, size + p.at.y * k, 5, 0, Math.PI * 2);
      ctx.fill();
      continue;
    }
    ctx.beginPath();
    pts.forEach((v, i) => (i === 0 ? ctx.moveTo(size + v.x * k, size + v.y * k) : ctx.lineTo(size + v.x * k, size + v.y * k)));
    if (p.type === "polygon" || p.type === "circle") ctx.closePath();
    ctx.stroke();
  }
  return c;
}

export interface PanelContext {
  canvas: CircleCanvas;
  glyphs: readonly Glyph[];
  combinations: readonly Combination[];
  support: () => { id: string; stages: number };
  rank: () => string;
  status: (message: string) => void;
}

/** Analyse lisible : les mêmes informations que le carnet, plus le support choisi. */
export function renderAnalysis(root: HTMLElement, a: Analysis | null, ctx: PanelContext): void {
  root.replaceChildren();
  if (!a) {
    root.append(el("p", { class: "muted" }, t("notebook.fmab.empty")));
    return;
  }
  const support = ctx.support();
  const tooBig = a.parsed.stages.length > support.stages;
  const outcome = tooBig ? "inert" : a.outcome;
  root.append(
    el(
      "p",
      { class: `verdict ${outcome}` },
      tooBig
        ? t("transmutation.fmab.support_too_small", support.stages)
        : t(`notebook.fmab.outcome.${a.outcome}`, Math.round(a.reboundSeverity * 100)),
    ),
  );
  const list = el("ul", { class: "facts" });
  a.parsed.stages.forEach((s) => {
    const names = s.glyphs.map((g) => t(g.glyph.nameKey)).join(", ") || "—";
    const poly = s.capacity === 0 ? t("notebook.fmab.no_polygon") : t("notebook.fmab.polygon", s.sides);
    list.append(el("li", {}, `${t("notebook.fmab.link", s.index + 1, poly)} — ${names}`));
    if (s.index > 0) {
      list.append(el("li", { class: "sub" }, t("notebook.fmab.link", s.index + 1, t(`notebook.fmab.link.${s.link}`))));
    }
    if (s.satellites.length > 0) list.append(el("li", { class: "sub" }, t("notebook.fmab.satellites", s.index + 1, s.satellites.length)));
  });
  for (const e of a.effects) {
    const infusions = [...e.infusions].map((i) => t(`editor.fmab.infusion.${i}`)).join(", ");
    list.append(
      el("li", { class: "effect" }, t("notebook.fmab.effect", effectName(e.combination.effect), e.range.toFixed(1)) + (infusions ? ` — ${infusions}` : "")),
    );
  }
  const cap = RANKS[rankIndex(ctx.rank())].complexityCap;
  list.append(el("li", {}, t("notebook.fmab.complexity", a.complexity, cap, rankName(a.requiredRank))));
  const stability = el("li", {}, t("notebook.fmab.stability", Math.round(a.stability * 100)));
  const bar = el("span", { class: "bar" });
  const fill = el("span", { class: a.stability >= 1 ? "fill ok" : "fill bad" });
  fill.style.width = `${Math.min(100, Math.round((a.stability / 1.5) * 100))}%`;
  bar.append(fill);
  stability.append(bar);
  list.append(stability);
  list.append(el("li", {}, t("editor.fmab.concentration", a.concentration)));
  root.append(list);
  if (a.issues.length > 0) {
    const issues = el("ul", { class: "issues" });
    for (const i of a.issues) {
      const li = el("li", {}, t(`circle.fmab.issue.${i.kind}`));
      if (i.where) {
        li.classList.add("located");
        li.addEventListener("mouseenter", () => {
          ctx.canvas.highlight = i.where;
          ctx.canvas.draw();
        });
        li.addEventListener("mouseleave", () => {
          ctx.canvas.highlight = null;
          ctx.canvas.draw();
        });
      }
      issues.append(li);
    }
    root.append(issues);
  }
}

/** Fiche d'un glyphe : ce qu'il vise ou fait, son coût, son rang, ses combinaisons connues. */
export function glyphSheet(g: Glyph, combinations: readonly Combination[]): HTMLElement {
  const known = combinations.filter((c) => (g.layer === "element" ? c.elements.has(g.role) : c.action === g.role));
  const sheet = el(
    "div",
    { class: "sheet" },
    el("strong", {}, t(g.nameKey)),
    el("p", { class: "muted" }, `${t(`treatise.fmab.layer.${g.layer}`)} — ${t("treatise.fmab.rank", rankName(g.rank))}`),
    el("p", {}, t(g.descriptionKey)),
    el("p", { class: "muted" }, t("treatise.fmab.cost", g.complexity, g.concentration)),
  );
  if (known.length > 0) {
    sheet.append(el("p", { class: "muted" }, t("editor.fmab.combinations")));
    const ul = el("ul", {});
    for (const c of known) ul.append(el("li", {}, effectName(c.effect)));
    sheet.append(ul);
  }
  return sheet;
}

/** Palette filtrée par couche et par rang : un clic arme l'outil « tampon ». */
export function renderPalette(root: HTMLElement, sheet: HTMLElement, ctx: PanelContext, onPick: (g: Glyph) => void): void {
  root.replaceChildren();
  const rank = rankIndex(ctx.rank());
  for (const layer of ["element", "action", "modifier"] as const) {
    const row = el("div", { class: "palette-row" });
    row.append(el("span", { class: "layer" }, t(`treatise.fmab.layer.${layer}`)));
    for (const g of ctx.glyphs.filter((x) => x.layer === layer)) {
      const locked = rankIndex(g.rank) > rank;
      const button = el("button", { class: locked ? "glyph locked" : "glyph", title: t(g.nameKey), type: "button" });
      button.append(glyphThumbnail(g));
      button.addEventListener("mouseenter", () => sheet.replaceChildren(glyphSheet(g, ctx.combinations)));
      button.addEventListener("click", () => onPick(g));
      row.append(button);
    }
    root.append(row);
  }
}

// ---- Export et import ------------------------------------------------------------------------------

function download(name: string, href: string): void {
  const a = el("a", { href, download: name });
  document.body.append(a);
  a.click();
  a.remove();
}

export async function copyCode(ctx: PanelContext): Promise<void> {
  await navigator.clipboard.writeText(encodeDrawing(ctx.canvas.primitives));
  ctx.status(t("notebook.fmab.exported"));
}

export function importCode(ctx: PanelContext, code: string): void {
  try {
    ctx.canvas.set(decodeDrawing(code));
    ctx.status(t("notebook.fmab.imported"));
  } catch {
    ctx.status(t("notebook.fmab.import_failed"));
  }
}

export function downloadJson(ctx: PanelContext): void {
  const json = JSON.stringify({ v: 1, primitives: ctx.canvas.primitives.map(primitiveToJson) }, null, 2);
  download("cercle.json", URL.createObjectURL(new Blob([json], { type: "application/json" })));
}

export function downloadPng(ctx: PanelContext): void {
  download("cercle.png", ctx.canvas.toPng());
}

export async function copyShareLink(ctx: PanelContext): Promise<void> {
  const url = new URL(window.location.href);
  url.hash = `c=${encodeDrawing(ctx.canvas.primitives)}`;
  await navigator.clipboard.writeText(url.toString());
  ctx.status(t("editor.fmab.link_copied"));
}

/** Tracé de départ : un anneau et un triangle, comme au premier chapitre du Traité. */
export function starterDrawing(): Primitive[] {
  const pts = [0, 1, 2].map((i) => {
    const a = -Math.PI / 2 + (2 * Math.PI * i) / 3;
    return { x: Math.round(CENTER_POINT.x + 14 * Math.cos(a)), y: Math.round(CENTER_POINT.y + 14 * Math.sin(a)) };
  });
  return [
    { type: "circle", center: CENTER_POINT, radius: GRID / 2 - 2 },
    { type: "polygon", points: pts },
  ];
}
