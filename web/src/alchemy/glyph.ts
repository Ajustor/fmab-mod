// Portage de Glyph, GlyphJson, Rank, Combination et CombinationTable.

import { type Primitive, primitiveFromJson } from "./geometry";

export type Layer = "element" | "action" | "modifier";

export interface RankInfo {
  readonly id: string;
  readonly complexityCap: number;
  readonly maxStages: number;
  readonly maxPolygonSides: number;
  readonly maxSatellites: number;
  readonly maxPolygonsPerStage: number;
}

/** Dans l'ordre de progression, comme l'énumération Java. */
export const RANKS: readonly RankInfo[] = [
  { id: "apprentice", complexityCap: 6, maxStages: 1, maxPolygonSides: 4, maxSatellites: 0, maxPolygonsPerStage: 1 },
  { id: "alchemist", complexityCap: 14, maxStages: 2, maxPolygonSides: 6, maxSatellites: 2, maxPolygonsPerStage: 3 },
  { id: "state", complexityCap: 25, maxStages: 3, maxPolygonSides: 12, maxSatellites: 6, maxPolygonsPerStage: 4 },
  { id: "gate", complexityCap: 40, maxStages: 5, maxPolygonSides: 12, maxSatellites: 12, maxPolygonsPerStage: 6 },
];

export const rankIndex = (id: string): number => {
  const i = RANKS.findIndex((r) => r.id === id);
  if (i < 0) throw new Error(`rang inconnu : ${id}`);
  return i;
};

export interface Tolerance {
  readonly rotationDeg: number;
  readonly position: number;
  readonly minScale: number;
  readonly maxScale: number;
}

export const anyRotation = (t: Tolerance): boolean => t.rotationDeg >= 180;

export interface Glyph {
  readonly id: string;
  readonly layer: Layer;
  readonly role: string;
  readonly primitives: Primitive[];
  readonly complexity: number;
  readonly concentration: number;
  readonly rank: string;
  readonly tolerance: Tolerance;
  readonly nameKey: string;
  readonly descriptionKey: string;
  readonly since: string;
}

export function parseGlyph(json: Record<string, unknown>): Glyph {
  const layer = String(json.layer) as Layer;
  const t = json.tolerance as { rotation_deg: number; position: number; scale: [number, number] } | undefined;
  return {
    id: String(json.id),
    layer,
    role: String(json[layer]),
    primitives: (json.primitives as Record<string, unknown>[]).map(primitiveFromJson),
    complexity: Number(json.complexity),
    concentration: Number(json.concentration),
    rank: String(json.rank),
    tolerance: t
      ? { rotationDeg: t.rotation_deg, position: t.position, minScale: t.scale[0], maxScale: t.scale[1] }
      : { rotationDeg: 15, position: 0.1, minScale: 0.7, maxScale: 1.4 },
    nameKey: String(json.name_key),
    descriptionKey: String(json.description_key),
    since: json.since ? String(json.since) : "0.1",
  };
}

export interface Combination {
  readonly id: string;
  readonly elements: ReadonlySet<string>;
  readonly action: string;
  readonly effect: string;
  readonly range: number;
  readonly school: string;
  readonly requires?: string;
  /** Éléments que des satellites doivent infuser dans l'étage, sinon le cercle rebondit. */
  readonly formula: ReadonlySet<string>;
}

const SCHOOLS: Record<string, string> = {
  earth: "earth",
  iron: "metal",
  copper: "metal",
  water: "water",
  fire: "fire",
  air: "fire",
};

export function parseCombination(id: string, json: Record<string, unknown>): Combination {
  const elements = new Set((json.elements as string[]).map(String));
  const first = [...elements].sort()[0] ?? "";
  return {
    id,
    elements,
    action: String(json.action),
    effect: String(json.effect),
    range: json.range === undefined ? 1 : Number(json.range),
    school: json.school ? String(json.school) : (SCHOOLS[first] ?? "earth"),
    requires: json.requires ? String(json.requires) : undefined,
    formula: new Set(((json.formula as string[] | undefined) ?? []).map(String)),
  };
}

const sameSet = (a: ReadonlySet<string>, b: ReadonlySet<string>): boolean =>
  a.size === b.size && [...a].every((x) => b.has(x));

export class CombinationTable {
  constructor(readonly all: readonly Combination[]) {}

  find(elements: ReadonlySet<string>, action: string): Combination | undefined {
    return this.all.find((c) => c.action === action && sameSet(c.elements, elements));
  }
}
