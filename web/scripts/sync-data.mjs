// Recopie dans public/ les données du mod dont l'éditeur a besoin : glyphes, combinaisons,
// fichiers de langue et version. L'éditeur affiche ainsi exactement ce que le jeu connaît.

import { mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const web = join(dirname(fileURLToPath(import.meta.url)), "..");
const repo = join(web, "..");
const resources = join(repo, "src", "main", "resources");
const data = join(resources, "data", "fmab");

const readJson = (path) => JSON.parse(readFileSync(path, "utf-8"));
const jsonFiles = (dir) => readdirSync(dir).filter((f) => f.endsWith(".json")).sort();

const glyphs = jsonFiles(join(data, "glyph")).map((f) => readJson(join(data, "glyph", f)));
const combinations = jsonFiles(join(data, "combination")).map((f) => ({
  id: `fmab:${f.replace(".json", "")}`,
  ...readJson(join(data, "combination", f)),
}));

const properties = readFileSync(join(repo, "gradle.properties"), "utf-8");
const field = (name) => properties.match(new RegExp(`^${name}=(.*)$`, "m"))?.[1]?.trim() ?? "?";
const meta = {
  // En CI, le workflow de release passe la version du tag.
  modVersion: process.env.FMAB_VERSION ?? field("mod_version"),
  minecraftVersion: field("minecraft_version"),
};

mkdirSync(join(web, "public", "data"), { recursive: true });
mkdirSync(join(web, "public", "lang"), { recursive: true });
writeFileSync(join(web, "public", "data", "glyphs.json"), JSON.stringify(glyphs));
writeFileSync(join(web, "public", "data", "combinations.json"), JSON.stringify(combinations));
writeFileSync(join(web, "public", "data", "meta.json"), JSON.stringify(meta));
for (const lang of ["fr_fr", "en_us"]) {
  const source = join(resources, "assets", "fmab", "lang", `${lang}.json`);
  writeFileSync(join(web, "public", "lang", `${lang}.json`), readFileSync(source, "utf-8"));
}
console.log(`éditeur : ${glyphs.length} glyphes, ${combinations.length} combinaisons, mod ${meta.modVersion}`);
