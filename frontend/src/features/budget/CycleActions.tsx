import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { testIds } from '@/lib/testids';

import type { BudgetBalance } from './types';

interface CycleActionsProps {
  balance: BudgetBalance;
  closing: boolean;
  onClose: () => void;
}

/**
 * Cerrar el ciclo.
 *
 * <p>La confirmacion dice QUE se congela y CUANTO queda sin confirmar, no
 * "estas seguro". Una pregunta sin sujeto no ayuda a nadie a decidir, y cerrar
 * no se deshace: el ciclo queda inmutable.
 *
 * <p>Abrir el siguiente no vive aqui sino en el estado vacio: mientras haya un
 * ciclo en curso, abrir otro se rechaza del lado del servidor, asi que ofrecer
 * el boton seria ofrecer un error.
 */
export function CycleActions({ balance, closing, onClose }: CycleActionsProps) {
  const { t } = useTranslation();
  const [confirmando, setConfirmando] = useState(false);

  const sinConfirmar = balance.counts.needsReview + balance.counts.pending;

  return (
    <>
      <Stack direction="row" spacing={3}>
        <Button
          variant="outlined"
          onClick={() => setConfirmando(true)}
          disabled={closing}
          data-testid={testIds.dashboard.closeCycleButton}
        >
          {t('dashboard.cycleActions.close')}
        </Button>
      </Stack>

      <ConfirmDialog
        open={confirmando}
        title={t('dashboard.cycleActions.confirmTitle')}
        description={
          sinConfirmar > 0
            ? t('dashboard.cycleActions.confirmWithPending', {
                total: balance.counts.total,
                pending: sinConfirmar,
              })
            : t('dashboard.cycleActions.confirmAllSettled', { total: balance.counts.total })
        }
        confirmLabel={t('dashboard.cycleActions.confirmLabel')}
        destructive
        pending={closing}
        onConfirm={() => {
          onClose();
          setConfirmando(false);
        }}
        onCancel={() => setConfirmando(false)}
      />
    </>
  );
}
