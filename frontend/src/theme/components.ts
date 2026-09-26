import type { Components, Theme } from '@mui/material/styles';

import { radius, type LumaColors } from './tokens';

type ShadowSet = { level1: string; level2: string };

/**
 * Overrides de MUI que convierten Material Design en LUMA.
 *
 * Las dos reglas que gobiernan casi todo: separar con borde antes que con sombra,
 * y que la accion primaria sea siempre tinta.
 */
export function buildComponents(c: LumaColors, s: ShadowSet, theme: Theme): Components<Theme> {
  return {
    MuiCssBaseline: {
      styleOverrides: {
        ':root': {
          colorScheme: theme.palette.mode,
        },
        body: {
          backgroundColor: c.ground,
          WebkitFontSmoothing: 'antialiased',
        },
        // Las cifras se alinean en columnas en toda la aplicacion.
        '.luma-tabular': {
          fontVariantNumeric: 'tabular-nums',
        },
      },
    },

    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: radius.sm,
          paddingInline: theme.spacing(4),
          paddingBlock: theme.spacing(2.25),
        },
        // No hace falta un override para el boton primario: la paleta ya
        // define primary.main = tinta y contrastText = onInk.
        outlined: {
          borderColor: c.line,
          color: c.ink,
          '&:hover': { borderColor: c.lineStrong, backgroundColor: c.surface2 },
        },
      },
    },

    MuiPaper: {
      defaultProps: {
        // Por defecto nada flota: se separa con borde.
        variant: 'outlined',
        elevation: 0,
      },
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          borderColor: c.line,
        },
        outlined: {
          borderColor: c.line,
        },
      },
    },

    MuiCard: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: {
          borderRadius: radius.md,
          borderColor: c.line,
          backgroundColor: c.surface,
        },
      },
    },

    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: theme.spacing(4),
          '&:last-child': { paddingBottom: theme.spacing(4) },
        },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: radius.full,
          fontWeight: 600,
          fontSize: '0.75rem',
        },
        // La miel destaca en foco/identidad; en un chip destacado (color="warning")
        // se ve como una gradiente sutil de dos tonos en vez de un relleno flat.
        colorWarning: {
          background: c.honeyGradient,
          color: c.onInk,
        },
      },
    },

    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        size: 'small',
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: radius.sm,
          backgroundColor: c.surface,
          '& .MuiOutlinedInput-notchedOutline': { borderColor: c.line },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: c.lineStrong },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: c.honey,
            borderWidth: 2,
          },
          // El borde del fieldset es un trazo SVG (notch para el label): no admite
          // una gradiente literal. El acento de foco se resuelve con un resplandor
          // que mezcla los dos tonos de la miel, en el mismo espiritu de la gradiente.
          '&.Mui-focused': {
            boxShadow: `0 0 0 3px ${theme.alpha(c.honeyFill, 0.28)}, 0 0 10px 1px ${theme.alpha(c.honey, 0.22)}`,
          },
        },
      },
    },

    MuiAppBar: {
      defaultProps: {
        elevation: 0,
        color: 'transparent',
      },
      styleOverrides: {
        root: {
          backgroundColor: c.ground,
          borderBottom: `1px solid ${c.line}`,
          backgroundImage: 'none',
        },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: c.ground,
          borderRight: `1px solid ${c.line}`,
          backgroundImage: 'none',
        },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: radius.sm,
          marginInline: theme.spacing(2),
          paddingBlock: theme.spacing(2),
          '&.Mui-selected': {
            backgroundColor: c.honeyWash,
            color: c.ink,
            '&:hover': { backgroundColor: c.honeyWash },
          },
        },
      },
    },

    MuiListItemIcon: {
      styleOverrides: {
        root: {
          minWidth: 34,
          color: 'inherit',
        },
      },
    },

    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: c.ink,
          color: c.onInk,
          fontSize: '0.75rem',
          borderRadius: radius.sm,
        },
      },
    },

    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: radius.lg,
          boxShadow: s.level2,
          border: `1px solid ${c.line}`,
        },
      },
    },

    MuiSnackbarContent: {
      styleOverrides: {
        root: {
          backgroundColor: c.ink,
          color: c.onInk,
          borderRadius: radius.sm,
          boxShadow: s.level2,
        },
      },
    },

    MuiLinearProgress: {
      styleOverrides: {
        root: {
          height: 7,
          borderRadius: radius.full,
          backgroundColor: c.surface2,
        },
        bar: {
          borderRadius: radius.full,
          backgroundColor: c.honey,
        },
      },
    },

    MuiSkeleton: {
      defaultProps: {
        animation: 'wave',
      },
      styleOverrides: {
        root: {
          backgroundColor: c.surface2,
          borderRadius: radius.sm,
        },
      },
    },

    MuiDivider: {
      styleOverrides: {
        root: { borderColor: c.line },
      },
    },
  };
}
