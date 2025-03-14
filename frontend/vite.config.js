import { defineConfig } from "vite";
import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    watch: {
	// Exclude .cljs files
	// so changes dont trigger multiple reloads
	// ignored: "**/*.js",
	usePolling: true,
	hmr: false
    },
  },
});
