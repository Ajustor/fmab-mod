// Les cercles de test communs au mod et à l'éditeur : le portage TypeScript doit trouver
// exactement ce que trouve le Java (testdata/circles, écrits par CircleFixturesTest).

import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { CircleAnalyzer } from "../src/alchemy/analyzer";
import { decodeDrawing, encodeDrawing } from "../src/alchemy/code";
import { primitiveFromJson } from "../src/alchemy/geometry";
import { CombinationTable, parseCombination, parseGlyph } from "../src/alchemy/glyph";
import { CircleParser } from "../src/alchemy/parser";
import { GlyphRecognizer } from "../src/alchemy/recognizer";
import { summary } from "../src/alchemy/summary";

const root = join(__dirname, "..", "..");
const data = join(root, "src", "main", "resources", "data", "fmab");
const readJson = (path: string): Record<string, unknown> => JSON.parse(readFileSync(path, "utf-8"));
const jsonFiles = (dir: string): string[] => readdirSync(dir).filter((f) => f.endsWith(".json")).sort();

const glyphs = jsonFiles(join(data, "glyph")).map((f) => parseGlyph(readJson(join(data, "glyph", f))));
const combinations = new CombinationTable(
  jsonFiles(join(data, "combination")).map((f) =>
    parseCombination(`fmab:${f.replace(".json", "")}`, readJson(join(data, "combination", f))),
  ),
);
const parser = new CircleParser(new GlyphRecognizer(glyphs));
const analyzer = new CircleAnalyzer(combinations, glyphs);
const fixtures = join(root, "testdata", "circles");

describe("cercles de test partagés avec le mod", () => {
  for (const file of jsonFiles(fixtures)) {
    it(file, () => {
      const fixture = readJson(join(fixtures, file)) as {
        rank: string;
        primitives: Record<string, unknown>[];
        expected: Record<string, unknown>;
      };
      const drawing = fixture.primitives.map(primitiveFromJson);
      expect(summary(analyzer.analyze(parser.parse(drawing), fixture.rank, null))).toEqual(fixture.expected);
    });
  }
});

describe("codes de tracé", () => {
  it("un code redonne le même tracé", () => {
    const drawing = (readJson(join(fixtures, "series.json")).primitives as Record<string, unknown>[]).map(
      primitiveFromJson,
    );
    expect(decodeDrawing(encodeDrawing(drawing))).toEqual(drawing);
  });

  it("refuse ce qui n'est pas un code", () => {
    expect(() => decodeDrawing("pas-un-code")).toThrow();
    expect(() => decodeDrawing("")).toThrow();
  });
});
