import { createBrowserRouter } from 'react-router';

import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { AppShell } from '@/components/layout/AppShell';
import { ForgotPasswordPage } from '@/features/auth/ForgotPasswordPage';
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { PlaceholderPage } from '@/pages/PlaceholderPage';
import { SettingsPage } from '@/pages/SettingsPage';

import { paths } from './paths';

/**
 * Rutas de la Fase 1.
 *
 * Todo lo que no sea el dashboard es un marcador de posicion a proposito: las
 * secciones reales llegan en sus propias fases y comparten este mismo shell.
 */
export const router = createBrowserRouter([
  // Publicas: son las unicas rutas accesibles sin sesion.
  { path: paths.login, element: <LoginPage /> },
  { path: paths.register, element: <RegisterPage /> },
  { path: paths.forgotPassword, element: <ForgotPasswordPage /> },
  { path: paths.resetPassword, element: <ResetPasswordPage /> },

  // Todo lo demas exige sesion. ProtectedRoute envuelve al shell, asi que
  // ninguna pantalla de la aplicacion se renderiza sin token.
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: paths.dashboard,
        element: <AppShell />,
        children: [
          { index: true, element: <DashboardPage /> },
          {
            path: paths.incomes.slice(1),
            element: (
              <PlaceholderPage
                title="Ingresos"
                phase="Fase 5"
                description="Aqui vas a registrar tu sueldo y cualquier otro ingreso, recurrente o extraordinario."
              />
            ),
          },
          {
            path: paths.fixedExpenses.slice(1),
            element: (
              <PlaceholderPage
                title="Gastos fijos"
                phase="Fase 6"
                description="Renta, servicios, seguros: lo que se repite cada ciclo con un monto estable."
              />
            ),
          },
          {
            path: paths.variableExpenses.slice(1),
            element: (
              <PlaceholderPage
                title="Gastos variables"
                phase="Fase 7"
                description="Tarjetas, deudas y gastos que cambian de monto en cada ciclo y hay que revisar."
              />
            ),
          },
          {
            path: paths.savings.slice(1),
            element: (
              <PlaceholderPage
                title="Ahorros"
                phase="Fase 8"
                description="Tus metas, cuanto llevas y cuanto necesitas apartar por ciclo para llegar a tiempo."
              />
            ),
          },
          { path: paths.settings.slice(1), element: <SettingsPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]);
