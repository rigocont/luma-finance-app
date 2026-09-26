import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import type { ThemeMode } from '@/theme/tokens';

export type ThemePreference = ThemeMode | 'system';

interface UiState {
  themePreference: ThemePreference;
  sidebarOpen: boolean;
  setThemePreference: (preference: ThemePreference) => void;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
}

/**
 * Estado de cliente unicamente: preferencias de interfaz.
 *
 * Los datos del servidor NO viven aqui. Para eso esta TanStack Query, que ya
 * resuelve cache, reintentos e invalidacion sin duplicar el estado.
 */
export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      themePreference: 'system',
      sidebarOpen: false,
      setThemePreference: (themePreference) => set({ themePreference }),
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setSidebarOpen: (sidebarOpen) => set({ sidebarOpen }),
    }),
    { name: 'luma-ui' },
  ),
);

/** Resuelve la preferencia a un modo concreto usando el ajuste del sistema. */
export function resolveThemeMode(
  preference: ThemePreference,
  systemPrefersDark: boolean,
): ThemeMode {
  if (preference === 'system') {
    return systemPrefersDark ? 'dark' : 'light';
  }
  return preference;
}
