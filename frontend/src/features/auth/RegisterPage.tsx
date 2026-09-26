import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Link as RouterLink } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { useRegister } from './useAuth';

function getFields(t: TFunction): FieldSpec[] {
  return [
    {
      name: 'name',
      label: t('auth.register.nameLabel'),
      autoComplete: 'name',
      testId: testIds.auth.nameInput,
    },
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
      autoComplete: 'new-password',
      testId: testIds.auth.passwordInput,
      helperText: t('auth.register.passwordHelp'),
    },
  ];
}

export function RegisterPage() {
  const { t } = useTranslation();
  const [values, setValues] = useState<Record<string, string>>({
    name: '',
    email: '',
    password: '',
  });
  const { mutate, isPending, error } = useRegister();

  return (
    <AuthLayout
      title={t('auth.register.title')}
      subtitle={t('auth.register.subtitle')}
      footer={
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          {t('auth.register.haveAccount')}{' '}
          <Link
            component={RouterLink}
            to={paths.login}
            data-testid={testIds.auth.goToLogin}
            underline="hover"
          >
            {t('auth.register.enter')}
          </Link>
        </Typography>
      }
    >
      <div data-testid={testIds.auth.registerPage}>
        <AuthForm
          fields={getFields(t)}
          values={values}
          onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
          onSubmit={() =>
            mutate({
              name: values.name ?? '',
              email: values.email ?? '',
              password: values.password ?? '',
            })
          }
          submitLabel={t('auth.register.submit')}
          submitTestId={testIds.auth.submitButton}
          pending={isPending}
          error={error}
          errorTestId={testIds.auth.formError}
        />
      </div>
    </AuthLayout>
  );
}
