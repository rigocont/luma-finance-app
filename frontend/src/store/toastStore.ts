import { create } from 'zustand';

export type ToastTone = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  message: string;
  tone: ToastTone;
}

interface ToastState {
  toasts: Toast[];
  show: (message: string, tone?: ToastTone) => void;
  dismiss: (id: string) => void;
}

/**
 * Avisos efimeros.
 *
 * Una cola y no un solo aviso: dos acciones seguidas no deben pisarse. Se
 * muestra el primero y los demas esperan su turno.
 *
 * Regla de la interfaz: un boton dice exactamente que pasa y el aviso lo
 * confirma en los mismos terminos. "Registrar pago" -> "Pago registrado".
 */
export const useToastStore = create<ToastState>()((set) => ({
  toasts: [],

  show: (message, tone = 'success') =>
    set((state) => ({
      toasts: [...state.toasts, { id: crypto.randomUUID(), message, tone }],
    })),

  dismiss: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
}));

/** Acceso sin hook, para usarlo dentro de mutaciones. */
export function showToast(message: string, tone: ToastTone = 'success'): void {
  useToastStore.getState().show(message, tone);
}
