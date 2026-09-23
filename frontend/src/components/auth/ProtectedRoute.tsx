import { Navigate, Outlet, useLocation } from 'react-router';

import { useAuthStore } from '@/features/auth/authStore';
import { paths } from '@/routes/paths';

/**
 * Deja pasar solo con sesion iniciada.
 *
 * Recuerda a donde iba el usuario para devolverlo ahi despues de entrar, en
 * lugar de dejarlo siempre en el resumen.
 *
 * Esto es comodidad, no seguridad: quien protege los datos es el backend, que
 * valida el token en cada peticion.
 */
export function ProtectedRoute() {
  const status = useAuthStore((state) => state.status);
  const location = useLocation();

  // SessionGate no deja llegar aqui durante el arranque, pero si el estado
  // fuera ese todavia no hay nada que decidir: mandar al login seria un falso
  // negativo.
  if (status === 'bootstrapping') {
    return null;
  }

  if (status !== 'authenticated') {
    return <Navigate to={paths.login} replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
