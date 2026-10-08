// Portage de DrawingCode : le JSON {"v":1,"primitives":[...]} compressé (deflate brut) puis encodé
// en base64 URL, sans remplissage. Le même code se colle dans le Carnet de cercles.

import { deflateSync, inflateSync, strFromU8, strToU8 } from "fflate";
import { MAX_PRIMITIVES, type Primitive, primitiveFromJson, primitiveToJson } from "./geometry";

export const CODE_VERSION = 1;
export const MAX_CODE_LENGTH = 16_384;
const MAX_JSON_BYTES = 256 * 1024;

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function fromBase64Url(text: string): Uint8Array {
  const base64 = text.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "===".slice((base64.length + 3) % 4);
  const binary = atob(padded);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

export function encodeDrawing(primitives: Primitive[]): string {
  const json = JSON.stringify({ v: CODE_VERSION, primitives: primitives.map(primitiveToJson) });
  return toBase64Url(deflateSync(strToU8(json), { level: 9 }));
}

/** @throws Error si le code est illisible, trop gros ou d'une version future */
export function decodeDrawing(code: string): Primitive[] {
  const trimmed = code.trim();
  if (trimmed.length === 0 || trimmed.length > MAX_CODE_LENGTH) throw new Error("code vide ou trop long");
  const json = inflateSync(fromBase64Url(trimmed));
  if (json.length > MAX_JSON_BYTES) throw new Error("code trop gros");
  const root = JSON.parse(strFromU8(json)) as { v: number; primitives: Record<string, unknown>[] };
  if (root.v > CODE_VERSION) throw new Error(`code d'une version plus récente (${root.v})`);
  const prims = root.primitives.map(primitiveFromJson);
  if (prims.length > MAX_PRIMITIVES) throw new Error("trop de traits");
  return prims;
}
