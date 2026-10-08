import { defineConfig } from "vite";

// Le site est publié sous /<dépôt>/editor/ sur GitHub Pages : chemins relatifs partout.
export default defineConfig({
  base: "./",
  build: { outDir: "dist", emptyOutDir: true },
});
