// Le pacte alchimique : ↑ ↑ ↓ ↓ ← → ← → B A, comme sur le reste du site. L'éditeur passe au sang
// et à la craie rouge et propose les deux cercles interdits. L'état est partagé avec le site
// (même origine, même clé), si bien qu'un pacte scellé sur l'accueil l'est aussi ici.

export const KONAMI = [
  "arrowup",
  "arrowup",
  "arrowdown",
  "arrowdown",
  "arrowleft",
  "arrowright",
  "arrowleft",
  "arrowright",
  "b",
  "a",
] as const;

const STORAGE_KEY = "fmab-pact";

/** Où en est la séquence après la touche {@code key} : un ↑ de trop après « ↑ ↑ » ne la casse pas. */
export function advance(typed: number, key: string): number {
  const k = key.toLowerCase();
  if (k === KONAMI[typed]) return typed + 1;
  if (k === "arrowup") return typed === 2 ? 2 : 1;
  return 0;
}

export function isSealed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === "1";
  } catch {
    return false;
  }
}

/** Scelle ou rompt le pacte ; {@code quiet} évite l'éclair (au chargement de la page). */
export function seal(on: boolean, quiet = false): void {
  document.documentElement.classList.toggle("pact", on);
  try {
    localStorage.setItem(STORAGE_KEY, on ? "1" : "0");
  } catch {
    // Stockage refusé (navigation privée) : le pacte tient jusqu'au rechargement.
  }
  if (on && !quiet) {
    const flash = document.createElement("div");
    flash.className = "pact-flash";
    document.body.append(flash);
    window.setTimeout(() => flash.remove(), 1100);
  }
}

/** Écoute le code au clavier ; {@code onSealed} reçoit chaque scellement ou rupture. */
export function listenForPact(onSealed: (on: boolean) => void): void {
  let typed = 0;
  window.addEventListener("keydown", (e) => {
    // Le B et le A tapés dans le champ d'import ne comptent pas.
    const tag = (e.target as HTMLElement | null)?.tagName;
    if (tag === "TEXTAREA" || tag === "INPUT") return;
    typed = advance(typed, e.key ?? "");
    if (typed === KONAMI.length) {
      typed = 0;
      const on = !document.documentElement.classList.contains("pact");
      seal(on);
      onSealed(on);
    }
  });
}
