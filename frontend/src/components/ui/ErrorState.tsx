import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import { ApiError } from '@/lib/api/types';
import { testIds } from '@/lib/testids';

interface ErrorStateProps {
  error: unknown;
  onRetry?: () => void;
}

/**
 * Estado de error.
 *
 * Al usuario le llega que paso y que puede hacer. El detalle tecnico no se
 * muestra; en su lugar se ofrece el identificador de la peticion, que es lo
 * unico util para diagnosticar despues.
 */
export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const { t } = useTranslation();
  const apiError = error instanceof ApiError ? error : null;
  const message = apiError?.message ?? t('state.errorDefault');

  return (
    <Stack
      spacing={3}
      data-testid={testIds.state.error}
      role="alert"
      sx={{ alignItems: 'center', textAlign: 'center', py: 16, px: 6 }}
    >
      <Typography variant="h4">{t('state.errorTitle')}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '44ch' }}>
        {message}
      </Typography>

      {onRetry && (
        <Button variant="contained" onClick={onRetry} data-testid={testIds.state.errorRetry}>
          {t('state.retry')}
        </Button>
      )}

      {apiError?.traceId && (
        <Typography variant="caption" color="text.disabled">
          {t('state.errorReference', { traceId: apiError.traceId })}
        </Typography>
      )}
    </Stack>
  );
}
