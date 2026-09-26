import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { AuthForm, type FieldSpec } from './AuthForm';
import { useAuthStore } from './authStore';
import { changePassword } from './passwordApi';

const FIELDS: FieldSpec[] = [
  {
    name: 'currentPassword',
    label: 'Contrasena actual',
    type: 'password',
    autoComplete: 'current-password',
    testId: testIds.auth.currentPasswordInput,
  },
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
    label: 'Confirma la contrasena nueva',
    type: 'password',
    autoComplete: 'new-password',
    testId: testIds.auth.confirmPasswordInput,
  },
];

export function ChangePasswordCard() {
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
        state: { notice: 'Cambiamos tu contrasena. Entra con la nueva.' },
      });
    },
  });

  function handleSubmit() {
    if (values.newPassword !== values.confirmPassword) {
      setLocalErrors({ confirmPassword: 'Las contrasenas no coinciden' });
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
            <Typography variant="h3">Contrasena</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '54ch' }}>
              Al cambiarla se cerraran todas tus sesiones, incluida esta, y tendras que entrar de
              nuevo.
            </Typography>
          </Stack>

          <AuthForm
            fields={FIELDS}
            values={values}
            onChange={(name, value) => setValues((prev) => ({ ...prev, [name]: value }))}
            onSubmit={handleSubmit}
            submitLabel="Cambiar contrasena"
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
