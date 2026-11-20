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

/** Ce que le rituel joue : le cercle interdit à tracer, et les mots qui l'accompagnent. */
export interface Ritual {
  primitives: unknown[];
  title: () => string;
  motto: () => string;
}

/** Le rituel partagé avec le site (public/pact-ritual.js, recopié de site/pact-ritual.js). */
type RitualPlayer = (options: { primitives: unknown[]; title: string; motto: string; onSealed: () => void }) => void;

function apply(on: boolean): void {
  document.documentElement.classList.toggle("pact", on);
  try {
    localStorage.setItem(STORAGE_KEY, on ? "1" : "0");
  } catch {
    // Stockage refusé (navigation privée) : le pacte tient jusqu'au rechargement.
  }
}

/**
 * Scelle ou rompt le pacte ; {@code quiet} n'anime rien (au chargement de la page). Scellé à la
 * main, le rituel se joue et passe la page au rouge au plus fort de l'éclair, puis {@code then}
 * est appelé ; sans le script du rituel, un simple éclair.
 */
export function seal(on: boolean, quiet = false, ritual?: Ritual, then?: () => void): void {
  if (!on || quiet) {
    apply(on);
    return;
  }
  const play = (window as unknown as { fmabPactRitual?: RitualPlayer }).fmabPactRitual;
  if (play && ritual) {
    play({
      primitives: ritual.primitives,
      title: ritual.title(),
      motto: ritual.motto(),
      onSealed: () => {
        apply(true);
        then?.();
      },
    });
    return;
  }
  apply(true);
  then?.();
  const flash = document.createElement("div");
  flash.className = "pact-flash";
  document.body.append(flash);
  window.setTimeout(() => flash.remove(), 1100);
}

/** Écoute le code au clavier ; {@code onSealed} reçoit chaque scellement ou rupture. */
export function listenForPact(onSealed: (on: boolean) => void, ritual?: Ritual): void {
  let typed = 0;
  window.addEventListener("keydown", (e) => {
    // Le B et le A tapés dans le champ d'import ne comptent pas.
    const tag = (e.target as HTMLElement | null)?.tagName;
    if (tag === "TEXTAREA" || tag === "INPUT") return;
    typed = advance(typed, e.key ?? "");
    if (typed === KONAMI.length) {
      typed = 0;
      const on = !document.documentElement.classList.contains("pact");
      seal(on, false, ritual, () => onSealed(on));
      if (!on) onSealed(on);
    }
  });
}
