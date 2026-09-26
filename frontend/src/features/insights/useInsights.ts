import { useQuery } from '@tanstack/react-query';

import { fetchInsights, insightsKeys } from './api';

/**
 * El analisis financiero del usuario.
 *
 * No depende de que exista un ciclo abierto ni de su estado: el servidor
 * decide que senales aplican (deficit, remanente, crecimiento) y aqui solo se
 * presenta lo que llega.
 */
export function useInsights() {
  return useQuery({
    queryKey: insightsKeys.current,
    queryFn: fetchInsights,
  });
}
