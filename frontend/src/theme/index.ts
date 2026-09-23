import { createTheme, type Theme } from '@mui/material/styles';

import { buildComponents } from './components';
import { FONT_FAMILY, palette, radius, shadows, SPACING_UNIT, type ThemeMode } from './tokens';

/**
 * Construye el tema de LUMA para un modo de color.
 *
 * Se generan dos temas completos y se intercambian desde `uiStore`, en lugar de
 * usar la API de esquemas de color de MUI. Es menos magico y mas facil de seguir.
 */
export function createLumaTheme(mode: ThemeMode): Theme {
  const c = palette[mode];
  const s = shadows[mode];

  const base = createTheme({
    spacing: SPACING_UNIT,

    shape: {
      // Radio por defecto: botones, inputs, filas. Las tarjetas lo suben a `md`.
      borderRadius: radius.sm,
    },

    palette: {
      mode,
      // La accion primaria siempre es tinta. Nunca hay que elegir color de boton.
      primary: { main: c.ink, contrastText: c.onInk },
      // La miel es identidad, no accion: foco, progreso, marca.
      secondary: { main: c.honey, contrastText: c.onInk },
      success: { main: c.positive, contrastText: c.onInk },
      error: { main: c.negative, contrastText: c.onInk },
      warning: { main: c.honey, contrastText: c.onInk },
      info: { main: c.muted, contrastText: c.onInk },
      background: { default: c.ground, paper: c.surface },
      text: { primary: c.text, secondary: c.muted, disabled: c.faint },
      divider: c.line,
    },

    typography: {
      fontFamily: FONT_FAMILY,
      h1: { fontSize: '2.125rem', fontWeight: 800, letterSpacing: '-0.025em', lineHeight: 1.15 },
      h2: { fontSize: '1.5625rem', fontWeight: 700, letterSpacing: '-0.018em', lineHeight: 1.2 },
      h3: { fontSize: '1.1875rem', fontWeight: 600, letterSpacing: '-0.01em', lineHeight: 1.3 },
      h4: { fontSize: '1rem', fontWeight: 600, lineHeight: 1.4 },
      body1: { fontSize: '0.9375rem', fontWeight: 400, lineHeight: 1.6 },
      body2: { fontSize: '0.8125rem', fontWeight: 500, lineHeight: 1.55 },
      caption: { fontSize: '0.75rem', fontWeight: 500, lineHeight: 1.45 },
      overline: {
        fontSize: '0.6875rem',
        fontWeight: 700,
        letterSpacing: '0.1em',
        textTransform: 'uppercase',
        lineHeight: 1.45,
      },
      button: { fontSize: '0.875rem', fontWeight: 600, textTransform: 'none' },
    },
  });

  return createTheme(base, {
    components: buildComponents(c, s, base),
  });
}

export { palette, radius, shadows, type ThemeMode } from './tokens';
