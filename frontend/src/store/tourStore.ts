import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface TourState {
  /**
   * Si ya se vio el tour completo, o se omitio en cualquier paso.
   *
   * Se guarda en este navegador, no en la cuenta: el tour es una preferencia
   * de presentacion, no un dato de negocio, y no hay endpoint que la sostenga
   * (ver docs/testing/16-tour-guiado.md). Omitirlo cuenta igual que
   * terminarlo: en ambos casos no debe reaparecer solo, y desde Ajustes se
   * puede volver a tomar cuando se quiera.
   */
  completed: boolean;
  /** Si el tour esta corriendo ahora mismo. No se persiste: es efimero. */
  running: boolean;
  /** Paso actual, mientras corre. */
  stepIndex: number;

  /** Arranca desde el primer paso. Lo usa el auto-inicio y "Ajustes". */
  start: () => void;
  next: () => void;
  prev: () => void;
  /** Omitir en cualquier paso cuenta como completado: no reaparece solo. */
  skip: () => void;
  /** Terminar el ultimo paso tambien cuenta como completado. */
  finish: () => void;
}

export const useTourStore = create<TourState>()(
  persist(
    (set) => ({
      completed: false,
      running: false,
      stepIndex: 0,

      start: () => set({ running: true, stepIndex: 0 }),
      next: () => set((state) => ({ stepIndex: state.stepIndex + 1 })),
      prev: () => set((state) => ({ stepIndex: Math.max(0, state.stepIndex - 1) })),
      skip: () => set({ running: false, completed: true }),
      finish: () => set({ running: false, completed: true }),
    }),
    {
      name: 'luma-tour',
      // running y stepIndex son de una sesion en curso: persistirlos dejaria
      // el tour "a medias" congelado si se recarga la pagina a mitad de paso.
      partialize: (state) => ({ completed: state.completed }),
    },
  ),
);
