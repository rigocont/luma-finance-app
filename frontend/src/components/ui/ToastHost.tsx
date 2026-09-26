import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';

import { testIds } from '@/lib/testids';
import { useToastStore } from '@/store/toastStore';

/** Duracion suficiente para leer un mensaje corto sin estorbar. */
const AUTO_HIDE_MS = 5000;

/**
 * Muestra el primer aviso de la cola.
 *
 * Se monta una sola vez, junto al tema. Cualquier parte de la aplicacion pide
 * un aviso con {@code showToast} sin tener que recibir nada por props.
 */
export function ToastHost() {
  const toast = useToastStore((state) => state.toasts[0]);
  const dismiss = useToastStore((state) => state.dismiss);

  function handleClose(_event: unknown, reason?: string) {
    // Un clic fuera no debe tragarse un aviso que la persona no alcanzo a leer.
    if (reason === 'clickaway') return;
    if (toast) dismiss(toast.id);
  }

  return (
    <Snackbar
      open={Boolean(toast)}
      autoHideDuration={AUTO_HIDE_MS}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert
        severity={toast?.tone ?? 'success'}
        variant="filled"
        onClose={() => toast && dismiss(toast.id)}
        data-testid={testIds.toast.root}
        sx={{ width: '100%' }}
      >
        {toast?.message ?? ''}
      </Alert>
    </Snackbar>
  );
}
