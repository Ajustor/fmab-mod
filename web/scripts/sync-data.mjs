// Recopie dans public/ les données du mod dont l'éditeur a besoin : glyphes, combinaisons,
// exemples du Traité, fichiers de langue et version. L'éditeur affiche ainsi exactement ce que
// le jeu connaît.

import { execSync } from "node:child_process";
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const web = join(dirname(fileURLToPath(import.meta.url)), "..");
const repo = join(web, "..");
const resources = join(repo, "src", "main", "resources");
const data = join(resources, "data", "fmab", "fmab");

const readJson = (path) => JSON.parse(readFileSync(path, "utf-8"));
const jsonFiles = (dir) => readdirSync(dir).filter((f) => f.endsWith(".json")).sort();

const glyphs = jsonFiles(join(data, "glyph")).map((f) => readJson(join(data, "glyph", f)));
const combinations = jsonFiles(join(data, "combination")).map((f) => ({
  id: `fmab:${f.replace(".json", "")}`,
  ...readJson(join(data, "combination", f)),
}));

// Les cercles d'exemple du Traité d'alchimie : les mêmes que dans le jeu, testés côté Java.
const treatise = readJson(join(resources, "assets", "fmab", "treatise", "treatise.json"));
const examples = treatise.chapters
  .flatMap((chapter) => chapter.pages)
  .filter((page) => page.example)
  .map((page) => ({ textKey: page.text, primitives: page.example }));

// Les deux cercles interdits (pacte alchimique, code Konami), tels que le jeu les dessine.
const forbiddenSource = readFileSync(
  join(repo, "src", "main", "java", "com", "ajustor", "fmab", "alchemy", "ForbiddenCircles.java"),
  "utf-8",
);
const forbidden = Object.fromEntries(
  [
    ["stone", "PHILOSOPHER_STONE"],
    ["human", "HUMAN_TRANSMUTATION"],
  ].map(([key, name]) => {
    const m = forbiddenSource.match(new RegExp(`${name} = parse\\("""([\\s\\S]*?)"""\\);`));
    if (!m) throw new Error(`cercle ${name} introuvable dans ForbiddenCircles.java`);
    return [key, JSON.parse(m[1])];
  }),
);

const properties = readFileSync(join(repo, "gradle.properties"), "utf-8");
const field = (name) => properties.match(new RegExp(`^${name}=(.*)$`, "m"))?.[1]?.trim() ?? "?";
// La dernière version publiée (tag vX.Y.Z), si le dépôt a ses tags ; sinon celle de gradle.properties.
const lastTag = () => {
  try {
    return execSync("git describe --tags --abbrev=0 --match v*", { cwd: repo, stdio: ["ignore", "pipe", "ignore"] })
      .toString()
      .trim()
      .replace(/^v/, "");
  } catch {
    return null;
  }
};
// Les versions de Minecraft prises en charge : une section [« 26.2 »] par version dans Stonecutter.
const minecraftVersions = [
  ...readFileSync(join(repo, "stonecutter.properties.toml"), "utf-8").matchAll(/^\["([^"]+)"\]/gm),
].map((m) => m[1]);
const meta = {
  // En CI, le workflow de release passe la version du tag.
  modVersion: process.env.FMAB_VERSION ?? lastTag() ?? field("mod_version"),
  minecraftVersion: minecraftVersions.join(", ") || "?",
};

mkdirSync(join(web, "public", "data"), { recursive: true });
mkdirSync(join(web, "public", "lang"), { recursive: true });
writeFileSync(join(web, "public", "data", "glyphs.json"), JSON.stringify(glyphs));
writeFileSync(join(web, "public", "data", "combinations.json"), JSON.stringify(combinations));
writeFileSync(join(web, "public", "data", "meta.json"), JSON.stringify(meta));
writeFileSync(join(web, "public", "data", "examples.json"), JSON.stringify(examples));
writeFileSync(join(web, "public", "data", "forbidden.json"), JSON.stringify(forbidden));
for (const lang of ["fr_fr", "en_us"]) {
  const source = join(resources, "assets", "fmab", "lang", `${lang}.json`);
  writeFileSync(join(web, "public", "lang", `${lang}.json`), readFileSync(source, "utf-8"));
}
console.log(
  `éditeur : ${glyphs.length} glyphes, ${combinations.length} combinaisons, ${examples.length} exemples, mod ${meta.modVersion}`,
);
