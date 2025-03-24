import { defineConfig } from "vite";
import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";

export default defineConfig({
    plugins: [react()],
    root: "./resources/frontend",
    publicDir: "./public",
        build: {
	    outDir: "../../build/dist",
	    assetsDir: "./public/assets",
    },
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
