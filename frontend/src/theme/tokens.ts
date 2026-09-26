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
    honeyGradient: 'linear-gradient(135deg, #E8AE4C 0%, #C98A2B 100%)',

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
    honeyGradient: 'linear-gradient(135deg, #EFB959 0%, #D99930 100%)',

    positive: '#6ABE8E',
    positiveWash: '#1E3128',
    negative: '#E8806C',
    negativeWash: '#3A231E',

    onInk: '#1A1815',
  },
} as const;

/**
 * Paleta de las graficas. VERIFICADA, no elegida a ojo.
 *
 * Los colores de arriba estan afinados para texto y acentos; los rellenos de una
 * grafica tienen otras exigencias, y no son las mismas en claro que en oscuro.
 * Estos valores salieron de correr el validador de la guia de visualizacion
 * —banda de luminosidad, piso de croma, separacion para daltonismo y contraste
 * contra la superficie— y son los que pasaron las cinco comprobaciones.
 *
 * Dos cosas que el validador obligo a cambiar, y conviene no deshacerlas:
 *
 * 1. El modo oscuro NO es el claro aclarado. `honey` y `positive` del tema
 *    oscuro (#EFB959, #6ABE8E) quedan por encima del techo de luminosidad para
 *    un relleno (L 0.82 y 0.74 contra un maximo de 0.67): como texto funcionan,
 *    como area grande brillan. De ahi estos pasos propios.
 *
 * 2. "Disponible" no es un color, es el hueco. Se intento como cuarto segmento
 *    gris y fallo dos comprobaciones: un gris no alcanza el piso de croma, y
 *    contra el verde del ahorro daba una separacion de 2.4 en vision deutan
 *    —indistinguibles—. La barra se dibuja sobre una pista y lo que sobra se
 *    queda sin llenar. Es mejor diseno y ademas pasa.
 *
 * La separacion gasto/ahorro cae en la banda 6-8 en vision protan, que solo es
 * admisible con una segunda codificacion. La hay, y por eso no se puede quitar:
 * cada segmento lleva su etiqueta visible, hay 2px de superficie entre
 * segmentos, y los gastos variables van con trama diagonal.
 */
export const chartPalette = {
  light: {
    /** Gastos. El mismo tono de la marca. */
    expense: '#C98A2B',
    /** Ahorro: dinero que se queda. */
    saving: '#2E7D52',
    /** La pista sobre la que se dibuja. Lo que sobra es lo disponible. */
    track: '#EFE7DA',
    /** Solo para lo que se pasa del ingreso. */
    over: '#B3402F',
  },

  dark: {
    expense: '#C3862A',
    saving: '#2A7A50',
    track: '#2F2A24',
    over: '#BE5239',
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
