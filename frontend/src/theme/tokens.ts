/**
 * Tokens del sistema de diseno de LUMA.
 *
 * Fuente unica de verdad del color, el radio y el espacio. El tema de MUI se
 * construye a partir de aqui; ningun componente define un color literal.
 *
 * Ver docs/design-system.md para el razonamiento detras de cada decision.
 */

export const palette = {
  light: {
    ground: '#FBF8F3',
    surface: '#FFFFFF',
    surface2: '#F5F0E8',
    line: '#E8E0D4',
    lineStrong: '#D6CCBC',

    ink: '#1B1917',
    text: '#2C2825',
    muted: '#6F6862',
    faint: '#9A928A',

    honey: '#C98A2B',
    honeyFill: '#E8AE4C',
    honeyWash: '#FBF0DC',

    positive: '#2E7D52',
    positiveWash: '#E4F2E9',
    negative: '#B3402F',
    negativeWash: '#FAE8E4',

    onInk: '#FBF8F3',
  },

  dark: {
    ground: '#1A1815',
    surface: '#221F1A',
    surface2: '#2A2620',
    line: '#35302A',
    lineStrong: '#464037',

    ink: '#F0EBE3',
    text: '#E7E1D8',
    muted: '#A69E94',
    faint: '#7C746B',

    honey: '#EFB959',
    honeyFill: '#D99930',
    honeyWash: '#33291A',

    positive: '#6ABE8E',
    positiveWash: '#1E3128',
    negative: '#E8806C',
    negativeWash: '#3A231E',

    onInk: '#1A1815',
  },
} as const;

export type ThemeMode = 'light' | 'dark';
/** Ancho a `string` a proposito: `palette.light` y `palette.dark` tienen
 *  literales distintos y `palette[mode]` debe encajar en ambos. */
export type LumaColors = Record<keyof (typeof palette)['light'], string>;

export const radius = {
  sm: 10,
  md: 14,
  lg: 20,
  full: 999,
} as const;

/** Escala de espacio con base 4. MUI multiplica por este valor. */
export const SPACING_UNIT = 4;

export const shadows = {
  light: {
    level1: '0 1px 2px rgba(43, 33, 20, 0.05)',
    level2: '0 6px 20px -6px rgba(43, 33, 20, 0.14)',
  },
  dark: {
    level1: '0 1px 2px rgba(0, 0, 0, 0.3)',
    level2: '0 6px 20px -6px rgba(0, 0, 0, 0.5)',
  },
} as const;

export const FONT_FAMILY = [
  'Figtree',
  'ui-sans-serif',
  'system-ui',
  '-apple-system',
  'Segoe UI',
  'Roboto',
  'sans-serif',
].join(', ');
