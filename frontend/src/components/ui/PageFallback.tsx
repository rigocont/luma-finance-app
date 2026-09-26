import Box from '@mui/material/Box';

import { LoadingState } from './LoadingState';

/**
 * Lo que se ve mientras llega el codigo de una pantalla.
 *
 * <p>Se reusan los esqueletos de carga en lugar de un spinner, por la misma
 * razon que en el resto de la aplicacion: la pagina no salta cuando el
 * contenido aparece. Y como cada ruta viaja en su propio trozo, esto se ve una
 * sola vez por seccion, no en cada visita.
 */
export function PageFallback() {
  return (
    <Box sx={{ pt: 8 }}>
      <LoadingState rows={4} />
    </Box>
  );
}
