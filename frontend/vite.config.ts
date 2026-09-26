import { readFileSync } from 'node:fs';
import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
// defineConfig de vitest/config extiende el de Vite y tipa la seccion `test`.
import { defineConfig } from 'vitest/config';

/**
 * La version sale de package.json y no de una constante escrita a mano.
 *
 * El pie del menu la mostraba como texto fijo, y decia "Fase 1" nueve fases
 * despues. Un dato con dos fuentes acaba mintiendo en una de ellas.
 */
const { version } = JSON.parse(
  readFileSync(new URL('./package.json', import.meta.url), 'utf8'),
) as { version: string };

export default defineConfig({
  plugins: [react()],

  define: {
    __APP_VERSION__: JSON.stringify(version),
  },

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
