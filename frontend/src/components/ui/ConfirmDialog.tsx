import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useTranslation } from 'react-i18next';

import { testIds } from '@/lib/testids';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  pending?: boolean;
  /** Acciones que no se deshacen se piden en rojo, no en tinta. */
  destructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * Confirmacion de una accion que no se deshace.
 *
 * El texto dice QUE se va a hacer y sobre QUE, no "estas seguro": una pregunta
 * sin sujeto no ayuda a nadie a decidir.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  pending = false,
  destructive = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog
      open={open}
      onClose={pending ? undefined : onCancel}
      aria-labelledby="confirm-dialog-title"
      data-testid={testIds.confirm.dialog}
    >
      <DialogTitle id="confirm-dialog-title">{title}</DialogTitle>

      <DialogContent>
        <DialogContentText>{description}</DialogContentText>
      </DialogContent>

      <DialogActions>
        <Button onClick={onCancel} disabled={pending} data-testid={testIds.confirm.cancel}>
          {t('common.cancel')}
        </Button>
        <Button
          variant="contained"
          color={destructive ? 'error' : 'primary'}
          onClick={onConfirm}
          disabled={pending}
          data-testid={testIds.confirm.accept}
        >
          {pending ? t('common.oneMoment') : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
