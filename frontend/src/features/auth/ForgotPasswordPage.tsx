import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Link as RouterLink } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { requestPasswordReset } from './passwordApi';

function getFields(t: TFunction): FieldSpec[] {
  return [
    {
      name: 'email',
      label: t('auth.fields.email'),
      type: 'email',
      autoComplete: 'email',
      testId: testIds.auth.emailInput,
    },
  ];
}

export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const { mutate, isPending, isSuccess, error } = useMutation({
    mutationFn: requestPasswordReset,
  });

  if (isSuccess) {
    return <ResetLinkSent email={email} />;
  }

  return (
    <AuthLayout
      title={t('auth.forgotPassword.title')}
      subtitle={t('auth.forgotPassword.subtitle')}
      footer={
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={paths.login}
            data-testid={testIds.auth.goToLogin}
            underline="hover"
          >
            {t('auth.forgotPassword.backToLogin')}
          </Link>
        </Typography>
      }
    >
      <div data-testid={testIds.auth.forgotPasswordPage}>
        <AuthForm
          fields={getFields(t)}
          values={{ email }}
          onChange={(_, value) => setEmail(value)}
          onSubmit={() => mutate({ email })}
          submitLabel={t('auth.forgotPassword.submit')}
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
  const { t } = useTranslation();

  return (
    <AuthLayout
      title={t('auth.forgotPassword.sentTitle')}
      subtitle={t('auth.forgotPassword.sentSubtitle')}
    >
      <Stack spacing={5} data-testid={testIds.auth.resetLinkSent}>
        <Alert severity="success" icon={false}>
          <Typography variant="body2">
            {t('auth.forgotPassword.sentTo')} <strong>{email}</strong>
          </Typography>
        </Alert>

        <Typography variant="body2" color="text.secondary">
          {t('auth.forgotPassword.expiry')}
        </Typography>

        <Button
          component={RouterLink}
          to={paths.login}
          variant="contained"
          size="large"
          data-testid={testIds.auth.goToLogin}
        >
          {t('auth.forgotPassword.backToLogin')}
        </Button>
      </Stack>
    </AuthLayout>
  );
}
