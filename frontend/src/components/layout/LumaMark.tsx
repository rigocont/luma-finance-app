import Box from '@mui/material/Box';
import { useTheme } from '@mui/material/styles';

import { palette } from '@/theme/tokens';

/**
 * Marca de LUMA: una fuente de luz calida.
 *
 * Es el simbolo provisional de la Fase 1; el logotipo definitivo se refina mas
 * adelante. Todo lo demas del sistema visual es independiente de el.
 */
export function LumaMark({ size = 28 }: { size?: number }) {
  const theme = useTheme();
  const c = palette[theme.palette.mode];

  return (
    <Box
      role="img"
      aria-label="LUMA"
      sx={{
        width: size,
        height: size,
        borderRadius: '50%',
        flex: 'none',
        background: `radial-gradient(circle at 34% 30%, ${c.honeyFill} 0%, ${c.honey} 52%, ${c.ink} 100%)`,
        boxShadow: `0 0 0 1px ${c.lineStrong}`,
      }}
    />
  );
}
