import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { resetPassword } from './passwordApi';

const FIELDS: FieldSpec[] = [
  {
    name: 'newPassword',
    label: 'Contrasena nueva',
    type: 'password',
    autoComplete: 'new-password',
    testId: testIds.auth.newPasswordInput,
    helperText: 'Al menos 8 caracteres',
  },
  {
    name: 'confirmPassword',
    label: 'Confirma la contrasena',
    type: 'password',
    autoComplete: 'new-password',
    testId: testIds.auth.confirmPasswordInput,
  },
];

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [values, setValues] = useState<Record<string, string>>({
    newPassword: '',
    confirmPassword: '',
  });
  const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

  const { mutate, isPending, error } = useMutation({
    mutationFn: resetPassword,
    onSuccess: () => {
      // La contrasena cambio y todas las sesiones quedaron cerradas: el unico
      // camino posible es volver a entrar.
      navigate(paths.login, {
        replace: true,
        state: { notice: 'Tu contrasena quedo lista. Entra con la nueva.' },
      });
    },
  });

  if (!token) {
    return <InvalidLink />;
  }

  function handleSubmit() {
    if (values.newPassword !== values.confirmPassword) {
      setLocalErrors({ confirmPassword: 'Las contrasenas no coinciden' });
      return;
    }
    setLocalErrors({});
    mutate({ token: token as string, newPassword: values.newPassword ?? '' });
  }

  return (
    <AuthLayout
      title="Elige una contrasena nueva"
      subtitle="Al guardarla se cerraran todas las sesiones abiertas de tu cuenta."
    >
      <div data-testid={testIds.auth.resetPasswordPage}>
        <AuthForm
          fields={FIELDS}
          values={values}
          onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
          onSubmit={handleSubmit}
          submitLabel="Guardar contrasena"
          submitTestId={testIds.auth.submitButton}
          pending={isPending}
          error={error}
          errorTestId={testIds.auth.formError}
          fieldErrors={localErrors}
        />
      </div>
    </AuthLayout>
  );
}

/** El enlace llego sin codigo, o alguien abrio la ruta a mano. */
function InvalidLink() {
  return (
    <AuthLayout
      title="Este enlace no sirve"
      subtitle="Le falta el codigo de recuperacion, o ya se uso."
    >
      <Stack spacing={5} data-testid={testIds.auth.invalidResetLink}>
        <Alert severity="error" icon={false}>
          <Typography variant="body2">
            Los enlaces de recuperacion vencen en una hora y sirven una sola vez.
          </Typography>
        </Alert>

        <Button
          component={RouterLink}
          to={paths.forgotPassword}
          variant="contained"
          size="large"
          data-testid={testIds.auth.requestNewLink}
        >
          Pedir un enlace nuevo
        </Button>

        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          <Link component={RouterLink} to={paths.login} underline="hover">
            Volver a iniciar sesion
          </Link>
        </Typography>
      </Stack>
    </AuthLayout>
  );
}
