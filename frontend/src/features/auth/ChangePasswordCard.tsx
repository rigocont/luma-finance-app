import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { useNavigate } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { useAuthStore } from './authStore';
import { changePassword } from './passwordApi';

function getFields(t: TFunction): FieldSpec[] {
  return [
    {
      name: 'currentPassword',
      label: t('auth.fields.currentPassword'),
      type: 'password',
      autoComplete: 'current-password',
      testId: testIds.auth.currentPasswordInput,
    },
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
      label: t('auth.fields.confirmPasswordFull'),
      type: 'password',
      autoComplete: 'new-password',
      testId: testIds.auth.confirmPasswordInput,
    },
  ];
}

export function ChangePasswordCard() {
  const { t } = useTranslation();
  const clearSession = useAuthStore((state) => state.clearSession);
  const navigate = useNavigate();

  const [values, setValues] = useState<Record<string, string>>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

  const { mutate, isPending, error } = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      // El backend cerro todas las sesiones, incluida esta. Mantener al usuario
      // dentro seria mentirle: la siguiente peticion fallaria.
      clearSession();
      navigate(paths.login, {
        replace: true,
        state: { notice: t('auth.changePassword.successNotice') },
      });
    },
  });

  function handleSubmit() {
    if (values.newPassword !== values.confirmPassword) {
      setLocalErrors({ confirmPassword: t('auth.changePassword.mismatch') });
      return;
    }
    setLocalErrors({});
    mutate({
      currentPassword: values.currentPassword ?? '',
      newPassword: values.newPassword ?? '',
    });
  }

  return (
    <Card data-testid={testIds.settings.changePasswordCard}>
      <CardContent>
        <Stack spacing={5}>
          <Stack spacing={1.5}>
            <Typography variant="h3">{t('auth.changePassword.title')}</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '54ch' }}>
              {t('auth.changePassword.description')}
            </Typography>
          </Stack>

          <AuthForm
            fields={getFields(t)}
            values={values}
            onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
            onSubmit={handleSubmit}
            submitLabel={t('auth.changePassword.submit')}
            submitTestId={testIds.settings.changePasswordSubmit}
            pending={isPending}
            error={error}
            errorTestId={testIds.settings.changePasswordError}
            fieldErrors={localErrors}
          />
        </Stack>
      </CardContent>
    </Card>
  );
}
