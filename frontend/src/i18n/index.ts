import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import en from './locales/en.json';
import es from './locales/es.json';

/**
 * Idiomas de interfaz.
 *
 * Fase 15. Un solo namespace por defecto y dos catalogos planos, en vez de
 * uno por seccion: con este tamano de aplicacion, partirlo en dieciseis
 * archivos costaria mas coordinacion de la que ahorra.
 *
 * `uiLanguage` (esto) y `locale` (formato de numeros y fechas, en
 * preferences/types.ts) son cosas distintas que casi siempre coinciden: la
 * pantalla se lee en un idioma, pero el formato de "$1,100.00" contra
 * "1.100,00 $" no depende de en que idioma se leen las palabras alrededor.
 */
export const SUPPORTED_LANGUAGES = ['es', 'en'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];
export const DEFAULT_LANGUAGE: SupportedLanguage = 'es';

/**
 * Deteccion manual del idioma del sistema.
 *
 * Se evita `i18next-browser-languagedetector` -una dependencia mas- para algo
 * que aqui es una sola comparacion: si el navegador pide ingles se usa
 * ingles, cualquier otro caso cae en espanol. Esto solo decide el arranque:
 * en cuanto haya sesion, `useLanguageSync` (features/preferences) sobreescribe
 * con lo que la cuenta tenga guardado.
 */
export function detectSystemLanguage(): SupportedLanguage {
  const preferido = typeof navigator !== 'undefined' ? navigator.language : DEFAULT_LANGUAGE;
  return preferido.toLowerCase().startsWith('en') ? 'en' : DEFAULT_LANGUAGE;
}

void i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    en: { translation: en },
  },
  lng: detectSystemLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: SUPPORTED_LANGUAGES,
  interpolation: { escapeValue: false },
});

export default i18n;
