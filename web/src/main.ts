import { CircleAnalyzer } from "./alchemy/analyzer";
import { decodeDrawing } from "./alchemy/code";
import { primitiveFromJson } from "./alchemy/geometry";
import { type Combination, CombinationTable, type Glyph, RANKS, parseCombination, parseGlyph } from "./alchemy/glyph";
import { CircleParser } from "./alchemy/parser";
import { GlyphRecognizer } from "./alchemy/recognizer";
import { CircleCanvas, type Tool } from "./editor/canvas";
import { type Lang, detectLang, loadLang, rememberLang, t } from "./editor/i18n";
import {
  type Example,
  type PanelContext,
  SUPPORTS,
  copyCode,
  copyShareLink,
  downloadJson,
  downloadPng,
  importCode,
  renderAnalysis,
  renderExamples,
  renderPalette,
  starterDrawing,
  supportName,
} from "./editor/panel";
import "./style.css";

const $ = <T extends HTMLElement>(id: string): T => document.getElementById(id) as T;

async function json<T>(path: string): Promise<T> {
  return (await fetch(path)).json() as Promise<T>;
}

async function main(): Promise<void> {
  const [rawGlyphs, rawCombinations, meta, rawExamples] = await Promise.all([
    json<Record<string, unknown>[]>("data/glyphs.json"),
    json<(Record<string, unknown> & { id: string })[]>("data/combinations.json"),
    json<{ modVersion: string; minecraftVersion: string }>("data/meta.json"),
    json<{ textKey: string; primitives: Record<string, unknown>[] }[]>("data/examples.json"),
  ]);
  const examples: Example[] = rawExamples.map((e) => ({ textKey: e.textKey, primitives: e.primitives.map(primitiveFromJson) }));
  const glyphs: Glyph[] = rawGlyphs.map(parseGlyph);
  const combinations: Combination[] = rawCombinations.map((c) => parseCombination(c.id, c));
  const parser = new CircleParser(new GlyphRecognizer(glyphs));
  const analyzer = new CircleAnalyzer(new CombinationTable(combinations), glyphs);

  await loadLang(detectLang());

  const supportSelect = $<HTMLSelectElement>("support");
  const rankSelect = $<HTMLSelectElement>("rank");
  const status = $<HTMLElement>("status");
  const note = $<HTMLElement>("example-note");
  let statusTimer = 0;

  const canvas = new CircleCanvas($<HTMLCanvasElement>("canvas"), () => refresh());
  const ctx: PanelContext = {
    canvas,
    glyphs,
    combinations,
    support: () => SUPPORTS.find((s) => s.id === supportSelect.value) ?? SUPPORTS[0],
    rank: () => rankSelect.value || "alchemist",
    status: (message) => {
      status.textContent = message;
      window.clearTimeout(statusTimer);
      statusTimer = window.setTimeout(() => (status.textContent = ""), 3000);
    },
  };

  function refresh(): void {
    canvas.analysis = canvas.primitives.length === 0 ? null : analyzer.analyze(parser.parse(canvas.primitives), ctx.rank(), null);
    renderAnalysis($("analysis"), canvas.analysis, ctx);
    canvas.draw();
  }

  function selectTool(tool: Tool): void {
    canvas.tool = tool;
    canvas.resetGesture();
    document.querySelectorAll<HTMLButtonElement>("[data-tool]").forEach((b) => b.classList.toggle("active", b.dataset.tool === tool));
    $("stamp-options").hidden = tool !== "stamp";
    canvas.draw();
  }

  const requiredRank = (example: Example): string =>
    analyzer.analyze(parser.parse(example.primitives), "gate", null).requiredRank;

  /** Ouvre un exemple : son tracé, le rang qu'il demande, et son explication sous le canevas. */
  function openExample(example: Example): void {
    canvas.set(example.primitives);
    rankSelect.value = requiredRank(example);
    note.textContent = t(example.textKey);
    note.hidden = false;
    refresh();
    canvas.canvas.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  function translate(): void {
    document.querySelectorAll<HTMLElement>("[data-i18n]").forEach((e) => (e.textContent = t(e.dataset.i18n!)));
    document.querySelectorAll<HTMLElement>("[data-i18n-title]").forEach((e) => (e.title = t(e.dataset.i18nTitle!)));
    $("version").textContent = t("editor.fmab.version", meta.modVersion, meta.minecraftVersion);
    supportSelect.replaceChildren(
      ...SUPPORTS.map((s) => new Option(supportName(s.id), s.id)),
    );
    const rank = rankSelect.value || "alchemist";
    rankSelect.replaceChildren(...RANKS.map((r) => new Option(t(`rank.fmab.${r.id}`), r.id)));
    rankSelect.value = rank;
    $<HTMLButtonElement>("sides").textContent = t("notebook.fmab.sides", canvas.sides);
    $<HTMLButtonElement>("symmetry").textContent = t(canvas.symmetry ? "notebook.fmab.symmetry.on" : "notebook.fmab.symmetry.off");
    renderExamples($("examples"), examples, requiredRank, openExample);
    note.hidden = true;
    renderPalette($("palette"), $("sheet"), ctx, (g) => {
      canvas.stampGlyph = g;
      $("stamp-name").textContent = t(g.nameKey);
      selectTool("stamp");
    });
    refresh();
  }

  // Outils.
  document.querySelectorAll<HTMLButtonElement>("[data-tool]").forEach((b) =>
    b.addEventListener("click", () => selectTool(b.dataset.tool as Tool)),
  );
  $("sides").addEventListener("click", () => {
    canvas.sides = canvas.sides >= 12 ? 3 : canvas.sides + 1;
    selectTool("polygon");
    translate();
  });
  $("symmetry").addEventListener("click", () => {
    canvas.symmetry = !canvas.symmetry;
    translate();
  });
  $("stamp-size").addEventListener("input", (e) => {
    canvas.stampSize = Number((e.target as HTMLInputElement).value);
    canvas.draw();
  });
  $("undo").addEventListener("click", () => canvas.undo());
  $("redo").addEventListener("click", () => canvas.redo());
  $("clear").addEventListener("click", () => {
    note.hidden = true;
    canvas.set([]);
  });
  supportSelect.addEventListener("change", refresh);
  rankSelect.addEventListener("change", translate);

  // Export et import.
  $("copy").addEventListener("click", () => void copyCode(ctx));
  $("share").addEventListener("click", () => void copyShareLink(ctx));
  $("json").addEventListener("click", () => downloadJson(ctx));
  $("png").addEventListener("click", () => downloadPng(ctx));
  $("import").addEventListener("click", () => {
    const code = $<HTMLTextAreaElement>("import-code").value;
    importCode(ctx, code);
  });

  // Langue.
  document.querySelectorAll<HTMLButtonElement>("[data-lang]").forEach((b) =>
    b.addEventListener("click", async () => {
      const l = b.dataset.lang as Lang;
      rememberLang(l);
      await loadLang(l);
      translate();
    }),
  );

  // Raccourcis : Ctrl+Z / Ctrl+Y, et 1 à 5 pour les outils.
  window.addEventListener("keydown", (e) => {
    if ((e.target as HTMLElement).tagName === "TEXTAREA") return;
    if (e.ctrlKey && e.key.toLowerCase() === "z") {
      e.preventDefault();
      canvas.undo();
    } else if (e.ctrlKey && e.key.toLowerCase() === "y") {
      e.preventDefault();
      canvas.redo();
    } else if (!e.ctrlKey && "12345".includes(e.key)) {
      selectTool((["line", "circle", "polygon", "arc", "dot"] as Tool[])[Number(e.key) - 1]);
    }
  });

  // Un lien partagé porte le tracé dans l'adresse : #c=<code>.
  const shared = new URLSearchParams(window.location.hash.slice(1)).get("c");
  let start = starterDrawing();
  if (shared) {
    try {
      start = decodeDrawing(shared);
    } catch {
      ctx.status(t("notebook.fmab.import_failed"));
    }
  }
  canvas.set(start, false);
  selectTool("line");
  translate();
}

void main();
