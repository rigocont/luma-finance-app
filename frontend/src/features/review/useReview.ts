import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '@/lib/api/types';
import { showToast } from '@/store/toastStore';

import {
  confirmAmounts,
  fetchCurrentCycle,
  fetchItemHistory,
  fetchPendingReview,
  fetchSavingItems,
  reviewKeys,
  settleItem,
} from './api';
import type { ConfirmAmountsPayload, SettlePayload } from './types';

/**
 * Sin ciclo abierto el servidor responde 404, y eso NO es un error de la
 * aplicacion: es un estado normal de una cuenta nueva. Se distingue aqui para
 * que la pantalla pueda decir "abre tu primer ciclo" en vez de "algo fallo".
 */
export function isNoCycleYet(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

export function useCurrentCycle() {
  return useQuery({
    queryKey: reviewKeys.cycle,
    queryFn: fetchCurrentCycle,
    // Un 404 aqui significa "todavia no hay ciclo": reintentarlo solo retrasa
    // el mensaje que la persona necesita ver.
    retry: (intentos, error) => !isNoCycleYet(error) && intentos < 2,
  });
}

export function usePendingReview() {
  return useQuery({
    queryKey: reviewKeys.pending,
    queryFn: fetchPendingReview,
    retry: (intentos, error) => !isNoCycleYet(error) && intentos < 2,
  });
}

/**
 * Cuantos renglones esperan revision. Lo usa la insignia del menu.
 *
 * Comparte la misma clave que la pantalla, asi que verla no cuesta una peticion
 * extra: TanStack Query reutiliza la respuesta.
 */
export function usePendingReviewCount(): number {
  const { data } = usePendingReview();
  return data?.length ?? 0;
}

export function useSavingItems(cycleId: string | undefined) {
  return useQuery({
    queryKey: reviewKeys.savings(cycleId ?? ''),
    queryFn: () => fetchSavingItems(cycleId as string),
    enabled: cycleId !== undefined,
  });
}

export function useItemHistory(cycleId: string | undefined, itemId: string | null) {
  return useQuery({
    queryKey: reviewKeys.history(cycleId ?? '', itemId ?? ''),
    queryFn: () => fetchItemHistory(cycleId as string, itemId as string),
    enabled: cycleId !== undefined && itemId !== null,
  });
}

export function useConfirmAmounts(cycleId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ConfirmAmountsPayload) => confirmAmounts(cycleId as string, payload),
    onSuccess: async (confirmados) => {
      await queryClient.invalidateQueries({ queryKey: reviewKeys.all });
      showToast(
        confirmados.length === 1
          ? `Listo: "${confirmados[0]!.name}" queda en ${confirmados[0]!.plannedAmount.amount}.`
          : `Listo: ${confirmados.length} gastos confirmados.`,
      );
    },
  });
}

export function useSettleItem(cycleId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ itemId, payload }: { itemId: string; payload: SettlePayload }) =>
      settleItem(cycleId as string, itemId, payload),
    onSuccess: async (item, { payload }) => {
      await queryClient.invalidateQueries({ queryKey: reviewKeys.all });
      // El progreso de la meta cambio: la pantalla de ahorros tiene que
      // enterarse aunque no sea la que disparo la peticion.
      await queryClient.invalidateQueries({ queryKey: ['savings'] });

      showToast(
        payload.registerInGoal === false
          ? `"${item.name}" registrado. No se sumo a la meta.`
          : `"${item.name}" registrado y sumado a tu meta.`,
      );
    },
  });
}
