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
  label: string;
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
  { key: 'dashboard', label: 'Resumen', path: paths.dashboard, icon: DashboardOutlinedIcon },
  {
    key: 'current-cycle',
    label: 'Este ciclo',
    path: paths.currentCycle,
    icon: FactCheckOutlinedIcon,
    showsPendingCount: true,
  },
  { key: 'incomes', label: 'Ingresos', path: paths.incomes, icon: TrendingUpOutlinedIcon },
  { key: 'expenses', label: 'Gastos', path: paths.expenses, icon: ReceiptLongOutlinedIcon },
  { key: 'savings', label: 'Ahorros', path: paths.savings, icon: SavingsOutlinedIcon },
  { key: 'settings', label: 'Ajustes', path: paths.settings, icon: SettingsOutlinedIcon },
];
