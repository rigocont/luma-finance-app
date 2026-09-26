import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import SavingsOutlinedIcon from '@mui/icons-material/SavingsOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import TrendingUpOutlinedIcon from '@mui/icons-material/TrendingUpOutlined';
import type { SvgIconComponent } from '@mui/icons-material';

import { paths } from '@/routes/paths';

export interface NavItem {
  key: string;
  /** Clave de traduccion bajo `layout.nav` (Fase 15). No es el texto en si. */
  labelKey: string;
  path: string;
  icon: SvgIconComponent;
  /**
   * La seccion muestra cuantos pendientes tiene.
   *
   * Es una bandera y no un numero porque el numero lo sabe el servidor: ponerlo
   * aqui obligaria a pasarlo por toda la navegacion desde quien lo consulta.
   */
  showsPendingCount?: boolean;
}

export const navItems: NavItem[] = [
  { key: 'dashboard', labelKey: 'dashboard', path: paths.dashboard, icon: DashboardOutlinedIcon },
  {
    key: 'current-cycle',
    labelKey: 'current-cycle',
    path: paths.currentCycle,
    icon: FactCheckOutlinedIcon,
    showsPendingCount: true,
  },
  {
    key: 'incomes',
    labelKey: 'incomes',
    path: paths.incomes,
    icon: TrendingUpOutlinedIcon,
  },
  {
    key: 'expenses',
    labelKey: 'expenses',
    path: paths.expenses,
    icon: ReceiptLongOutlinedIcon,
  },
  { key: 'savings', labelKey: 'savings', path: paths.savings, icon: SavingsOutlinedIcon },
  { key: 'settings', labelKey: 'settings', path: paths.settings, icon: SettingsOutlinedIcon },
];
