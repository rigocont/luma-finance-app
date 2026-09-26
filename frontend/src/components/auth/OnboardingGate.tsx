import { Navigate, Outlet } from 'react-router';

import { useCurrentUser } from '@/features/auth/useAuth';
import { paths } from '@/routes/paths';

/**
 * Manda al asistente hasta que el alta este terminada.
 *
 * <p>Envuelve al shell, no a las rutas publicas: quien no ha configurado su
 * cuenta no tiene nada que hacer en el resumen, y dejarlo entrar a secciones
 * vacias es la peor primera impresion posible.
 *
 * <p>Esto es comodidad, no seguridad. Quien se salte la redireccion llega a
 * pantallas sin datos, no a datos ajenos: eso lo impide el backend, que valida
 * el token en cada peticion.
 */
export function OnboardingGate() {
  const user = useCurrentUser();

  if (user !== null && !user.onboardingCompleted) {
    return <Navigate to={paths.onboarding} replace />;
  }

  return <Outlet />;
}
