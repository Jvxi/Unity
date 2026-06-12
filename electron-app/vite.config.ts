import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

function manualChunks(id: string) {
  if (!id.includes("node_modules")) return undefined;
  if (id.includes("@element-plus/icons-vue")) return "vendor-element-icons";
  if (id.includes("element-plus") || id.includes("@popperjs") || id.includes("async-validator") || id.includes("dayjs") || id.includes("lodash-unified")) {
    return "vendor-element-plus";
  }
  if (id.includes("@vue") || id.includes("vue-router") || id.includes("pinia")) return "vendor-vue";
  if (id.includes("axios") || id.includes("@stomp") || id.includes("sockjs-client")) return "vendor-network";
  return "vendor";
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  clearScreen: false,
  server: {
    port: 5173,
    strictPort: true,
  },
  envPrefix: ["VITE_", "TAURI_"],
  build: {
    target: ["es2021", "chrome100", "safari13"],
    minify: !process.env.TAURI_DEBUG ? "esbuild" : false,
    sourcemap: !!process.env.TAURI_DEBUG,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks,
      },
      onwarn(warning, warn) {
        if (
          warning.code === "INVALID_ANNOTATION" &&
          typeof warning.id === "string" &&
          warning.id.includes("@vueuse/core")
        ) {
          return;
        }
        warn(warning);
      },
    },
  },
});
