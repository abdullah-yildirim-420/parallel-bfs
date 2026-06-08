import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// base: "./" so the built assets load when Javalin serves the SPA from the jar root.
export default defineConfig({
  plugins: [react()],
  base: "./",
  build: { outDir: "dist", emptyOutDir: true },
  server: { port: 5173 },
});
