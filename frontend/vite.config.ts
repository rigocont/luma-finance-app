import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
// defineConfig de vitest/config extiende el de Vite y tipa la seccion `test`.
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 5173,
    // En desarrollo el frontend habla con el backend a traves de este proxy.
    // Asi el navegador solo ve un origen y CORS deja de ser un problema local.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: true,
  },

  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
  },
});
