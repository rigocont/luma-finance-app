import '@testing-library/jest-dom/vitest';

import i18n from '@/i18n';

// Las pruebas existentes (Fases 1-14) afirman texto en espanol tal cual
// aparecia antes de la Fase 15. jsdom no siempre reporta el mismo
// navigator.language que un navegador real, asi que el idioma de arranque se
// fija aqui en vez de depender de la deteccion automatica: una prueba de
// interfaz no deberia poder fallar solo porque el entorno de pruebas detecto
// otro idioma del sistema.
await i18n.changeLanguage('es');
