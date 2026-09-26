import i18n from '@/i18n';
import { testIds } from '@/lib/testids';

export interface TourStep {
  key: string;
  /**
   * `data-testid` del elemento que este paso senala. `null` para un paso
   * centrado sin objetivo -la bienvenida y el cierre-.
   */
  targetTestId: string | null;
}

/**
 * El tour explica cada pantalla principal desde su entrada en la barra
 * lateral, sin navegar entre rutas: cambiar de pantalla a mitad del tour
 * perderia el paso si la persona ya estaba trabajando en una de ellas.
 * Senalar el elemento de navegacion alcanza para explicar que hace cada
 * pantalla sin salir de la que se esta usando.
 *
 * El texto de cada paso (titulo y descripcion) no vive aqui desde la Fase 15:
 * sigue el idioma de la interfaz, asi que se busca por clave en
 * `tour.steps.<key>` con {@link tourStepTitle} y {@link tourStepDescription}.
 */
export const tourSteps: TourStep[] = [
  { key: 'welcome', targetTestId: null },
  { key: 'dashboard', targetTestId: testIds.layout.navItem('dashboard') },
  { key: 'current-cycle', targetTestId: testIds.layout.navItem('current-cycle') },
  { key: 'incomes', targetTestId: testIds.layout.navItem('incomes') },
  { key: 'expenses', targetTestId: testIds.layout.navItem('expenses') },
  { key: 'savings', targetTestId: testIds.layout.navItem('savings') },
  { key: 'settings', targetTestId: testIds.layout.navItem('settings') },
  { key: 'closing', targetTestId: null },
];

export function tourStepTitle(step: TourStep): string {
  return i18n.t(`tour.steps.${step.key}.title`);
}

export function tourStepDescription(step: TourStep): string {
  return i18n.t(`tour.steps.${step.key}.description`);
}
