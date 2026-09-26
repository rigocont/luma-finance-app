import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { useEffect, useRef } from 'react';

import { testIds } from '@/lib/testids';
import { palette } from '@/theme/tokens';

const ADSENSE_CLIENT_ID = import.meta.env.VITE_ADSENSE_CLIENT_ID as string | undefined;
const ADSENSE_SLOT_ID = import.meta.env.VITE_ADSENSE_SLOT_ID as string | undefined;

declare global {
  interface Window {
    adsbygoogle?: unknown[];
  }
}

let scriptRequested = false;

function cargarScriptDeAdSense(clientId: string) {
  if (scriptRequested) return;
  scriptRequested = true;

  const script = document.createElement('script');
  script.async = true;
  script.crossOrigin = 'anonymous';
  script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${clientId}`;
  document.head.appendChild(script);
}

/**
 * Un espacio de anuncio, listo para Google AdSense el dia que exista una
 * cuenta aprobada y un dominio publico -algo que este entorno de desarrollo
 * no tiene.
 *
 * Sin VITE_ADSENSE_CLIENT_ID y VITE_ADSENSE_SLOT_ID configurados, no hace
 * ninguna peticion externa: reserva el espacio con un marcador visible, para
 * que la pantalla no salte el dia que el anuncio real empiece a cargar.
 *
 * LUMA no monetiza con un plan de pago (esa idea se descarto para la Fase
 * 12): el uso es gratuito y este es el unico mecanismo de ingreso. No hay
 * nada que desbloquear ni ocultar segun el usuario.
 */
export function AdSlot() {
  const theme = useTheme();
  const c = palette[theme.palette.mode];
  const solicitado = useRef(false);

  useEffect(() => {
    if (!ADSENSE_CLIENT_ID || !ADSENSE_SLOT_ID || solicitado.current) {
      return;
    }
    solicitado.current = true;

    cargarScriptDeAdSense(ADSENSE_CLIENT_ID);

    try {
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch {
      // La red no respondio o un bloqueador de anuncios la detuvo: el
      // espacio se queda vacio, no rompe la pantalla.
    }
  }, []);

  if (!ADSENSE_CLIENT_ID || !ADSENSE_SLOT_ID) {
    return (
      <Box
        data-testid={testIds.ads.placeholder}
        aria-hidden
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: 90,
          mt: 8,
          borderRadius: 3,
          border: `1px dashed ${c.lineStrong}`,
          backgroundColor: c.surface2,
        }}
      >
        <Typography variant="caption" sx={{ color: c.faint }}>
          Espacio publicitario
        </Typography>
      </Box>
    );
  }

  return (
    <Box data-testid={testIds.ads.slot} sx={{ minHeight: 90, mt: 8 }}>
      <ins
        className="adsbygoogle"
        style={{ display: 'block' }}
        data-ad-client={ADSENSE_CLIENT_ID}
        data-ad-slot={ADSENSE_SLOT_ID}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </Box>
  );
}
