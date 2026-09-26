/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

/** La version de package.json, inyectada al compilar. Ver vite.config.ts. */
declare const __APP_VERSION__: string;
