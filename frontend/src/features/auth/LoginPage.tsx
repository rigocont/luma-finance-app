import Alert from '@mui/material/Alert';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Link as RouterLink, useLocation } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { useLogin } from './useAuth';

function getFields(t: TFunction): FieldSpec[] {
  return [
    {
      name: 'email',
      label: t('auth.fields.email'),
      type: 'email',
      autoComplete: 'email',
      testId: testIds.auth.emailInput,
    },
    {
      name: 'password',
      label: t('auth.fields.password'),
      type: 'password',
      autoComplete: 'current-password',
      testId: testIds.auth.passwordInput,
    },
  ];
}

export function LoginPage() {
  const { t } = useTranslation();
  const location = useLocation();
  const state = location.state as { from?: string; notice?: string } | null;
  const from = state?.from ?? paths.dashboard;
  const notice = state?.notice;

  const [values, setValues] = useState<Record<string, string>>({ email: '', password: '' });
  const { mutate, isPending, error } = useLogin(from);

  return (
    <AuthLayout
      title={t('auth.login.title')}
      subtitle={t('auth.login.subtitle')}
      footer={
        <Stack spacing={2.5}>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
            <Link
              component={RouterLink}
              to={paths.forgotPassword}
              data-testid={testIds.auth.goToForgotPassword}
              underline="hover"
            >
              {t('auth.login.forgotPassword')}
            </Link>
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
            {t('auth.login.noAccount')}{' '}
            <Link
              component={RouterLink}
              to={paths.register}
              data-testid={testIds.auth.goToRegister}
              underline="hover"
            >
              {t('auth.login.createOne')}
            </Link>
          </Typography>
        </Stack>
      }
    >
      <div data-testid={testIds.auth.loginPage}>
        {notice && (
          <Alert severity="success" data-testid={testIds.auth.loginNotice} sx={{ mb: 4 }}>
            {notice}
          </Alert>
        )}
        <AuthForm
          fields={getFields(t)}
          values={values}
          onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
          onSubmit={() => mutate({ email: values.email ?? '', password: values.password ?? '' })}
          submitLabel={t('auth.login.submit')}
          submitTestId={testIds.auth.submitButton}
          pending={isPending}
          error={error}
          errorTestId={testIds.auth.formError}
        />
      </div>
    </AuthLayout>
  );
}
