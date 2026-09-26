import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '@/lib/api/types';
import { showToast } from '@/store/toastStore';

import {
  budgetKeys,
  closeCycle,
  fetchCurrentSummary,
  fetchCycleItems,
  fetchDeficitAdvice,
  fetchTrends,
  openNextCycle,
  skipItem,
} from './api';

/** Cuantos ciclos se comparan. Seis: tres meses de quincenas. */
export const CICLOS_A_COMPARAR = 6;

/**
 * Sin ciclo abierto el servidor responde 404, y no es un fallo: es una cuenta
 * que todavia no abrio el primero. La pantalla lo distingue para poder ofrecer
 * abrirlo en lugar de mostrar un error.
 */
export function isNoCycleYet(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

export function useCurrentSummary() {
  return useQuery({
    queryKey: budgetKeys.current,
    queryFn: fetchCurrentSummary,
    retry: (intentos, error) => !isNoCycleYet(error) && intentos < 2,
  });
}

export function useCycleItems(cycleId: string | undefined) {
  return useQuery({
    queryKey: budgetKeys.items(cycleId ?? ''),
    queryFn: () => fetchCycleItems(cycleId as string),
    enabled: cycleId !== undefined,
  });
}

/**
 * El consejo de deficit.
 *
 * Solo se pide cuando de verdad hay deficit: preguntar "que hago" con el ciclo
 * en orden gasta una peticion para recibir una lista vacia.
 */
export function useDeficitAdvice(cycleId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: budgetKeys.advice(cycleId ?? ''),
    queryFn: () => fetchDeficitAdvice(cycleId as string),
    enabled: cycleId !== undefined && enabled,
  });
}

export function useTrends(howMany: number = CICLOS_A_COMPARAR) {
  return useQuery({
    queryKey: budgetKeys.trends(howMany),
    queryFn: () => fetchTrends(howMany),
  });
}

export function useOpenNextCycle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: openNextCycle,
    onSuccess: async () => {
      // Abrir un ciclo materializa renglones a partir de ingresos, gastos y
      // metas: casi todo lo que la aplicacion tenia en cache quedo viejo.
      await queryClient.invalidateQueries();
      showToast('Tu siguiente ciclo esta abierto.');
    },
  });
}

export function useCloseCycle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (cycleId: string) => closeCycle(cycleId),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      showToast('Ciclo cerrado. Queda como registro y ya no se puede cambiar.');
    },
  });
}

export function useSkipItem(cycleId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (itemId: string) => skipItem(cycleId as string, itemId),
    onSuccess: async (item) => {
      await queryClient.invalidateQueries({ queryKey: budgetKeys.all });
      showToast(`"${item.name}" ya no cuenta en este ciclo.`);
    },
  });
}
