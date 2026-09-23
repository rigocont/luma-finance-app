import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
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
}

export const navItems: NavItem[] = [
  { key: 'dashboard', label: 'Resumen', path: paths.dashboard, icon: DashboardOutlinedIcon },
  { key: 'incomes', label: 'Ingresos', path: paths.incomes, icon: TrendingUpOutlinedIcon },
  {
    key: 'fixed-expenses',
    label: 'Gastos fijos',
    path: paths.fixedExpenses,
    icon: ReceiptLongOutlinedIcon,
  },
  {
    key: 'variable-expenses',
    label: 'Gastos variables',
    path: paths.variableExpenses,
    icon: PaymentsOutlinedIcon,
  },
  { key: 'savings', label: 'Ahorros', path: paths.savings, icon: SavingsOutlinedIcon },
  { key: 'settings', label: 'Ajustes', path: paths.settings, icon: SettingsOutlinedIcon },
];
