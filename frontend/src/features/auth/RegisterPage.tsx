import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { useRegister } from './useAuth';

const FIELDS: FieldSpec[] = [
  {
    name: 'name',
    label: 'Como te llamas',
    autoComplete: 'name',
    testId: testIds.auth.nameInput,
  },
  {
    name: 'email',
    label: 'Correo',
    type: 'email',
    autoComplete: 'email',
    testId: testIds.auth.emailInput,
  },
  {
    name: 'password',
    label: 'Contrasena',
    type: 'password',
    autoComplete: 'new-password',
    testId: testIds.auth.passwordInput,
    helperText: 'Al menos 8 caracteres',
  },
];

export function RegisterPage() {
  const [values, setValues] = useState<Record<string, string>>({
    name: '',
    email: '',
    password: '',
  });
  const { mutate, isPending, error } = useRegister();

  return (
    <AuthLayout
      title="Empecemos"
      subtitle="Crea tu cuenta y arma tu primer presupuesto."
      footer={
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          Ya tienes cuenta?{' '}
          <Link
            component={RouterLink}
            to={paths.login}
            data-testid={testIds.auth.goToLogin}
            underline="hover"
          >
            Entrar
          </Link>
        </Typography>
      }
    >
      <div data-testid={testIds.auth.registerPage}>
        <AuthForm
          fields={FIELDS}
          values={values}
          onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
          onSubmit={() =>
            mutate({
              name: values.name ?? '',
              email: values.email ?? '',
              password: values.password ?? '',
            })
          }
          submitLabel="Crear cuenta"
          submitTestId={testIds.auth.submitButton}
          pending={isPending}
          error={error}
          errorTestId={testIds.auth.formError}
        />
      </div>
    </AuthLayout>
  );
}
