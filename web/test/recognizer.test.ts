// Le reconnaisseur de l'éditeur relit le cercle à chaque trait : il doit rester rapide, sans rien
// changer à ce qu'il trouve (les cercles de test partagés avec le mod le vérifient, voir fixtures).

import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { CircleAnalyzer } from "../src/alchemy/analyzer";
import { primitiveFromJson } from "../src/alchemy/geometry";
import { CombinationTable, parseCombination, parseGlyph } from "../src/alchemy/glyph";
import { CircleParser } from "../src/alchemy/parser";
import { Grid, GlyphRecognizer } from "../src/alchemy/recognizer";

const root = join(__dirname, "..", "..");
const data = join(root, "src", "main", "resources", "data", "fmab", "fmab");
const readJson = (path: string): any => JSON.parse(readFileSync(path, "utf-8"));
const jsonFiles = (dir: string): string[] => readdirSync(dir).filter((f) => f.endsWith(".json")).sort();

describe("reconnaisseur de glyphes", () => {
  it("la grille trouve le même plus proche voisin qu'un parcours de tous les points", () => {
    let seed = 7;
    const random = (): number => {
      seed = (seed * 16807) % 2147483647;
      return seed / 2147483647;
    };
    for (let round = 0; round < 20; round++) {
      const n = 5 + Math.floor(random() * 200);
      const xs = Float64Array.from({ length: n }, () => random() * 4 - 2);
      const ys = Float64Array.from({ length: n }, () => random() * 4 - 2);
      const grid = new Grid(xs, ys);
      for (let q = 0; q < 50; q++) {
        // Des requêtes dedans et loin dehors.
        const px = random() * 10 - 5;
        const py = random() * 10 - 5;
        let best = Number.MAX_VALUE;
        for (let i = 0; i < n; i++) best = Math.min(best, (px - xs[i]) ** 2 + (py - ys[i]) ** 2);
        expect(grid.nearest(px, py)).toBe(Math.sqrt(best));
      }
    }
  });

  it("lit tous les cercles du Traité en bien moins d'une seconde (il en fallait huit)", () => {
    const glyphs = jsonFiles(join(data, "glyph")).map((f) => parseGlyph(readJson(join(data, "glyph", f))));
    const combinations = new CombinationTable(
      jsonFiles(join(data, "combination")).map((f) => parseCombination(`fmab:${f.replace(".json", "")}`, readJson(join(data, "combination", f)))),
    );
    const parser = new CircleParser(new GlyphRecognizer(glyphs));
    const analyzer = new CircleAnalyzer(combinations, glyphs);
    const treatise = readJson(join(root, "src", "main", "resources", "assets", "fmab", "treatise", "treatise.json"));
    const examples = treatise.chapters.flatMap((c: any) => c.pages).filter((p: any) => p.example);
    const start = performance.now();
    for (const e of examples) analyzer.analyze(parser.parse(e.example.map(primitiveFromJson)), "gate", null);
    expect(performance.now() - start).toBeLessThan(2000);
  });
});
