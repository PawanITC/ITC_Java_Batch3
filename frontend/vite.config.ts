import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Standard proxy for all your fetch() calls
      '/api': {
        target: 'http://localhost:8060',
        changeOrigin: true,
        secure: false,
        // No need for bypass if we call the Gateway directly for login
      },
    },
  },
});