import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import type { FormEvent, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { ApiError } from '@/lib/api/types';

export interface FieldSpec {
  name: string;
  label: string;
  type?: string;
  autoComplete?: string;
  testId: string;
  helperText?: string;
}

interface AuthFormProps {
  fields: FieldSpec[];
  values: Record<string, string>;
  onChange: (name: string, value: string) => void;
  onSubmit: () => void;
  submitLabel: string;
  submitTestId: string;
  pending: boolean;
  error: unknown;
  errorTestId: string;
  /**
   * Errores que decide la propia interfaz, no el servidor. El unico caso
   * legitimo es la confirmacion de contrasena: el backend nunca ve ese campo.
   */
  fieldErrors?: Record<string, string>;
  children?: ReactNode;
}

/**
 * Formulario de sesion.
 *
 * La validacion de campos la manda el backend: llega en {@code errors[]} y se
 * reparte por nombre de campo. Asi no hay dos definiciones de las mismas reglas
 * que puedan separarse con el tiempo.
 */
export function AuthForm({
  fields,
  values,
  onChange,
  onSubmit,
  submitLabel,
  submitTestId,
  pending,
  error,
  errorTestId,
  fieldErrors: localFieldErrors,
  children,
}: AuthFormProps) {
  const { t } = useTranslation();
  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = { ...(apiError?.fieldErrorMap ?? {}), ...(localFieldErrors ?? {}) };

  // Si el error trae campos, cada uno se muestra en su input. El mensaje general
  // solo aparece cuando el problema no es de un campo concreto.
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;
  const hasLocalErrors = Object.keys(localFieldErrors ?? {}).length > 0;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <Stack spacing={4}>
        {generalMessage && !hasLocalErrors && (
          <Alert severity="error" data-testid={errorTestId}>
            {generalMessage}
          </Alert>
        )}

        {fields.map((field) => (
          <TextField
            key={field.name}
            id={`auth-field-${field.name}`}
            name={field.name}
            label={field.label}
            type={field.type ?? 'text'}
            autoComplete={field.autoComplete}
            value={values[field.name] ?? ''}
            onChange={(event) => onChange(field.name, event.target.value)}
            error={Boolean(fieldErrors[field.name])}
            helperText={fieldErrors[field.name] ?? field.helperText}
            disabled={pending}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': field.testId } }}
          />
        ))}

        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={pending}
          data-testid={submitTestId}
        >
          {pending ? t('common.oneMoment') : submitLabel}
        </Button>

        {children}
      </Stack>
    </form>
  );
}
