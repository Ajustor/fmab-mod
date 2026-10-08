// Textes de l'éditeur : les fichiers de langue du mod, tels quels. Mêmes clés, mêmes formats
// (%s, %%) que les traductions de Minecraft.

export type Lang = "fr_fr" | "en_us";

let table: Record<string, string> = {};
let current: Lang = "fr_fr";

export async function loadLang(lang: Lang): Promise<void> {
  const res = await fetch(`lang/${lang}.json`);
  table = (await res.json()) as Record<string, string>;
  current = lang;
  document.documentElement.lang = lang === "fr_fr" ? "fr" : "en";
}

export const lang = (): Lang => current;

export function detectLang(): Lang {
  const saved = safeStorage("fmab.lang");
  if (saved === "fr_fr" || saved === "en_us") return saved;
  return navigator.language.toLowerCase().startsWith("fr") ? "fr_fr" : "en_us";
}

export function rememberLang(l: Lang): void {
  try {
    localStorage.setItem("fmab.lang", l);
  } catch {
    // Stockage indisponible (navigation privée) : la langue suivra le navigateur.
  }
}

function safeStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

/** Traduction avec arguments %s, comme Component.translatable. Une clé absente s'affiche telle quelle. */
export function t(key: string, ...args: (string | number)[]): string {
  const pattern = table[key] ?? key;
  let i = 0;
  return pattern.replace(/%(%|s)/g, (_, c: string) => (c === "%" ? "%" : String(args[i++] ?? "")));
}
