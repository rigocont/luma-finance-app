import { lazy, Suspense, type ComponentType, type ReactElement } from 'react';
import { createBrowserRouter } from 'react-router';

import { OnboardingGate } from '@/components/auth/OnboardingGate';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { AppShell } from '@/components/layout/AppShell';
import { PageFallback } from '@/components/ui/PageFallback';
import { NotFoundPage } from '@/pages/NotFoundPage';

import { paths } from './paths';

/**
 * Rutas de la aplicacion.
 *
 * <p>Cada pantalla se carga cuando se visita, no al abrir la aplicacion. Sin
 * esto todo viajaba en un solo archivo que crecia una fase a la vez: quien
 * entraba al login descargaba tambien los ahorros, la revision del ciclo y el
 * asistente de alta.
 *
 * <p>Lo que NO es perezoso es deliberado. {@link AppShell} es el marco de casi
 * todas las pantallas, asi que partirlo no ahorra nada y solo agrega una espera
 * al entrar. {@link NotFoundPage} es diminuta y pedirla por separado costaria
 * mas que traerla.
 */

/**
 * Un modulo con export nombrado, presentado como ruta perezosa.
 *
 * <p>El {@code .then} que reetiqueta el export a {@code default} no es adorno:
 * {@code React.lazy} solo acepta modulos con export por omision, y el proyecto
 * usa exports nombrados en todas partes.
 */
function pagina(cargar: () => Promise<{ default: ComponentType }>): ReactElement {
  const Perezosa = lazy(cargar);

  return (
    <Suspense fallback={<PageFallback />}>
      <Perezosa />
    </Suspense>
  );
}

export const router = createBrowserRouter([
  // Publicas: son las unicas rutas accesibles sin sesion.
  {
    path: paths.login,
    element: pagina(() =>
      import('@/features/auth/LoginPage').then((m) => ({ default: m.LoginPage })),
    ),
  },
  {
    path: paths.register,
    element: pagina(() =>
      import('@/features/auth/RegisterPage').then((m) => ({ default: m.RegisterPage })),
    ),
  },
  {
    path: paths.forgotPassword,
    element: pagina(() =>
      import('@/features/auth/ForgotPasswordPage').then((m) => ({
        default: m.ForgotPasswordPage,
      })),
    ),
  },
  {
    path: paths.resetPassword,
    element: pagina(() =>
      import('@/features/auth/ResetPasswordPage').then((m) => ({
        default: m.ResetPasswordPage,
      })),
    ),
  },

  // Todo lo demas exige sesion. ProtectedRoute envuelve al shell, asi que
  // ninguna pantalla de la aplicacion se renderiza sin token.
  {
    element: <ProtectedRoute />,
    children: [
      // Fuera del shell y fuera de la compuerta: es justo la pantalla a la que
      // la compuerta manda, y meterla dentro seria un ciclo de redirecciones.
      {
        path: paths.onboarding.slice(1),
        element: pagina(() =>
          import('@/features/onboarding/OnboardingPage').then((m) => ({
            default: m.OnboardingPage,
          })),
        ),
      },

      {
        element: <OnboardingGate />,
        children: [
          {
            path: paths.dashboard,
            element: <AppShell />,
            children: [
              {
                index: true,
                element: pagina(() =>
                  import('@/pages/DashboardPage').then((m) => ({ default: m.DashboardPage })),
                ),
              },
              {
                path: paths.currentCycle.slice(1),
                element: pagina(() =>
                  import('@/features/review/ReviewPage').then((m) => ({
                    default: m.ReviewPage,
                  })),
                ),
              },
              {
                path: paths.incomes.slice(1),
                element: pagina(() =>
                  import('@/features/incomes/IncomesPage').then((m) => ({
                    default: m.IncomesPage,
                  })),
                ),
              },
              {
                path: paths.expenses.slice(1),
                element: pagina(() =>
                  import('@/features/expenses/ExpensesPage').then((m) => ({
                    default: m.ExpensesPage,
                  })),
                ),
              },
              {
                path: paths.savings.slice(1),
                element: pagina(() =>
                  import('@/features/savings/SavingsPage').then((m) => ({
                    default: m.SavingsPage,
                  })),
                ),
              },
              {
                path: paths.settings.slice(1),
                element: pagina(() =>
                  import('@/pages/SettingsPage').then((m) => ({ default: m.SettingsPage })),
                ),
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
]);
