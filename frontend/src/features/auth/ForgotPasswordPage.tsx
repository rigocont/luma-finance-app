import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { requestPasswordReset } from './passwordApi';

const FIELDS: FieldSpec[] = [
  {
    name: 'email',
    label: 'Correo',
    type: 'email',
    autoComplete: 'email',
    testId: testIds.auth.emailInput,
  },
];

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const { mutate, isPending, isSuccess, error } = useMutation({
    mutationFn: requestPasswordReset,
  });

  if (isSuccess) {
    return <ResetLinkSent email={email} />;
  }

  return (
    <AuthLayout
      title="Recupera tu acceso"
      subtitle="Te enviamos un enlace para elegir una contrasena nueva."
      footer={
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={paths.login}
            data-testid={testIds.auth.goToLogin}
            underline="hover"
          >
            Volver a iniciar sesion
          </Link>
        </Typography>
      }
    >
      <div data-testid={testIds.auth.forgotPasswordPage}>
        <AuthForm
          fields={FIELDS}
          values={{ email }}
          onChange={(_, value) => setEmail(value)}
          onSubmit={() => mutate({ email })}
          submitLabel="Enviar enlace"
          submitTestId={testIds.auth.submitButton}
          pending={isPending}
          error={error}
          errorTestId={testIds.auth.formError}
        />
      </div>
    </AuthLayout>
  );
}

/**
 * Confirmacion despues de pedir el enlace.
 *
 * El mensaje es el mismo exista o no una cuenta con ese correo. Decir "no
 * encontramos esa cuenta" permitiria averiguar quien usa LUMA probando correos.
 */
function ResetLinkSent({ email }: { email: string }) {
  return (
    <AuthLayout
      title="Revisa tu correo"
      subtitle="Si existe una cuenta con ese correo, ya va en camino un enlace para recuperarla."
    >
      <Stack spacing={5} data-testid={testIds.auth.resetLinkSent}>
        <Alert severity="success" icon={false}>
          <Typography variant="body2">
            Enviado a <strong>{email}</strong>
          </Typography>
        </Alert>

        <Typography variant="body2" color="text.secondary">
          El enlace vence en una hora y sirve una sola vez. Si no llega en unos minutos, revisa la
          carpeta de correo no deseado.
        </Typography>

        <Button
          component={RouterLink}
          to={paths.login}
          variant="contained"
          size="large"
          data-testid={testIds.auth.goToLogin}
        >
          Volver a iniciar sesion
        </Button>
      </Stack>
    </AuthLayout>
  );
}
