import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { AuthLayout } from './AuthLayout';
import { resetPassword } from './passwordApi';

function getFields(t: TFunction): FieldSpec[] {
  return [
    {
      name: 'newPassword',
      label: t('auth.fields.newPassword'),
      type: 'password',
      autoComplete: 'new-password',
      testId: testIds.auth.newPasswordInput,
      helperText: t('auth.register.passwordHelp'),
    },
    {
      name: 'confirmPassword',
      label: t('auth.fields.confirmPasswordShort'),
      type: 'password',
      autoComplete: 'new-password',
      testId: testIds.auth.confirmPasswordInput,
    },
  ];
}

export function ResetPasswordPage() {
  const { t } = useTranslation();
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
        state: { notice: t('auth.resetPassword.successNotice') },
      });
    },
  });

  if (!token) {
    return <InvalidLink />;
  }

  function handleSubmit() {
    if (values.newPassword !== values.confirmPassword) {
      setLocalErrors({ confirmPassword: t('auth.resetPassword.mismatch') });
      return;
    }
    setLocalErrors({});
    mutate({ token: token as string, newPassword: values.newPassword ?? '' });
  }

  return (
    <AuthLayout title={t('auth.resetPassword.title')} subtitle={t('auth.resetPassword.subtitle')}>
      <div data-testid={testIds.auth.resetPasswordPage}>
        <AuthForm
          fields={getFields(t)}
          values={values}
          onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
          onSubmit={handleSubmit}
          submitLabel={t('auth.resetPassword.submit')}
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
  const { t } = useTranslation();

  return (
    <AuthLayout
      title={t('auth.resetPassword.invalidTitle')}
      subtitle={t('auth.resetPassword.invalidSubtitle')}
    >
      <Stack spacing={5} data-testid={testIds.auth.invalidResetLink}>
        <Alert severity="error" icon={false}>
          <Typography variant="body2">{t('auth.resetPassword.invalidMessage')}</Typography>
        </Alert>

        <Button
          component={RouterLink}
          to={paths.forgotPassword}
          variant="contained"
          size="large"
          data-testid={testIds.auth.requestNewLink}
        >
          {t('auth.resetPassword.requestNew')}
        </Button>

        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          <Link component={RouterLink} to={paths.login} underline="hover">
            {t('auth.forgotPassword.backToLogin')}
          </Link>
        </Typography>
      </Stack>
    </AuthLayout>
  );
}
