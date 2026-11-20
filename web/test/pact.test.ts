import { describe, expect, it } from "vitest";
import { KONAMI, advance } from "../src/editor/pact";

const type = (keys: string[]): number => keys.reduce(advance, 0);

describe("code Konami", () => {
  it("la séquence complète scelle le pacte", () => {
    expect(type(["ArrowUp", "ArrowUp", "ArrowDown", "ArrowDown", "ArrowLeft", "ArrowRight", "ArrowLeft", "ArrowRight", "b", "a"])).toBe(
      KONAMI.length,
    );
  });

  it("B et A majuscules comptent aussi", () => {
    expect(type([...KONAMI.slice(0, 8), "B", "A"])).toBe(KONAMI.length);
  });

  it("un ↑ de trop au début ne casse pas la séquence", () => {
    expect(type(["ArrowUp", ...KONAMI])).toBe(KONAMI.length);
  });

  it("une fausse touche la remet à zéro", () => {
    expect(type(["ArrowUp", "ArrowUp", "ArrowDown", "x"])).toBe(0);
    expect(type(["ArrowUp", "ArrowUp", "ArrowDown", "ArrowUp"])).toBe(1);
  });
});
