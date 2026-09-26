import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import { useState } from 'react';

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
          Cerrar este ciclo
        </Button>
      </Stack>

      <ConfirmDialog
        open={confirmando}
        title="Cerrar este ciclo"
        description={
          sinConfirmar > 0
            ? `Al cerrarlo, sus ${balance.counts.total} renglones quedan como registro y ya no se ` +
              `pueden cambiar. Todavia hay ${sinConfirmar} sin confirmar: van a quedarse asi para ` +
              `siempre, y el ciclo cerrado es lo que despues explica tu historial.`
            : `Al cerrarlo, sus ${balance.counts.total} renglones quedan como registro y ya no se ` +
              `pueden cambiar. Todo esta confirmado, asi que no vas a perder nada.`
        }
        confirmLabel="Cerrar el ciclo"
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
