import { testIds } from '@/lib/testids';

export interface TourStep {
  key: string;
  title: string;
  description: string;
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
 */
export const tourSteps: TourStep[] = [
  {
    key: 'welcome',
    title: 'Bienvenido a LUMA',
    description:
      'Un recorrido rapido por las pantallas principales. Se puede omitir en cualquier momento, y se puede volver a tomar despues desde Ajustes.',
    targetTestId: null,
  },
  {
    key: 'dashboard',
    title: 'Resumen',
    description: 'Cuanto dinero te queda este ciclo, y por que.',
    targetTestId: testIds.layout.navItem('dashboard'),
  },
  {
    key: 'current-cycle',
    title: 'Este ciclo',
    description:
      'Los pagos que vienen y los que ya vencieron. Aqui confirmas las cifras reales de lo que gastaste.',
    targetTestId: testIds.layout.navItem('current-cycle'),
  },
  {
    key: 'incomes',
    title: 'Ingresos',
    description: 'Lo que entra cada ciclo: tu sueldo y cualquier otro ingreso recurrente.',
    targetTestId: testIds.layout.navItem('incomes'),
  },
  {
    key: 'expenses',
    title: 'Gastos',
    description: 'Tus gastos fijos y variables, con su categoria y su fecha de vencimiento.',
    targetTestId: testIds.layout.navItem('expenses'),
  },
  {
    key: 'savings',
    title: 'Ahorros',
    description: 'Tus metas de ahorro y cuanto llevas de cada una.',
    targetTestId: testIds.layout.navItem('savings'),
  },
  {
    key: 'settings',
    title: 'Ajustes',
    description:
      'Tu cuenta, tu tipo de ciclo, el idioma y la apariencia. Este tour tambien vive aqui, por si lo quieres repetir.',
    targetTestId: testIds.layout.navItem('settings'),
  },
  {
    key: 'closing',
    title: 'Listo',
    description: 'Eso es todo. Ya puedes empezar a usar LUMA.',
    targetTestId: null,
  },
];
