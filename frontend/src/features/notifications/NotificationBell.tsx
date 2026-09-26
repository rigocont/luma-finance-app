import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useState, type MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { useNavigate } from 'react-router';

import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadCount,
} from './useNotifications';
import type { AppNotification } from './types';

/**
 * La oracion de cada alerta. Igual que `STATE_HEADLINE` en el resumen
 * financiero: el cliente arma el texto a partir de una clave, nunca de una
 * cifra sin pasar por el traductor.
 */
function copyFor(t: TFunction, n: AppNotification): { primary: string; secondary: string } {
  switch (n.type) {
    case 'PAYMENT_DUE_SOON':
      return {
        primary: t('notifications.dueSoon', { item: n.itemName }),
        secondary: t('notifications.dueSoonDetail', {
          date: formatDay(n.dueDate!),
          amount: formatMoney(n.amount!),
        }),
      };
    case 'PAYMENT_OVERDUE':
      return {
        primary: t('notifications.overdue', { item: n.itemName }),
        secondary: t('notifications.overdueSince', {
          date: formatDay(n.dueDate!),
          amount: formatMoney(n.amount!),
        }),
      };
    case 'CYCLE_DEFICIT':
      return {
        primary: t('notifications.deficitTitle'),
        secondary: t('notifications.deficitBody', { amount: formatMoney(n.amount!) }),
      };
    default:
      return { primary: '', secondary: '' };
  }
}

/**
 * La campana del encabezado: cuenta lo que la persona no ha visto y lo lista
 * en un menu desplegable.
 *
 * Sin pantalla propia en esta fase (Fase 11): cada alerta manda a "Este ciclo",
 * que es donde las tres condiciones —pago proximo, vencido, deficit— se
 * resuelven de verdad.
 */
export function NotificationBell() {
  const { t } = useTranslation();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const abierto = Boolean(anchor);

  const noLeidas = useUnreadCount();
  const { data } = useNotifications();
  const alertas = data?.content ?? [];

  const marcarLeida = useMarkNotificationRead();
  const marcarTodas = useMarkAllNotificationsRead();
  const navigate = useNavigate();

  function abrir(event: MouseEvent<HTMLElement>) {
    setAnchor(event.currentTarget);
  }

  function cerrar() {
    setAnchor(null);
  }

  function irAlCiclo(alerta: AppNotification) {
    if (!alerta.read) {
      marcarLeida.mutate(alerta.id);
    }
    cerrar();
    navigate(paths.currentCycle);
  }

  return (
    <>
      <Tooltip title={t('notifications.alerts')}>
        <IconButton
          aria-label={t('notifications.alerts')}
          data-testid={testIds.notifications.bell}
          onClick={abrir}
        >
          <Badge badgeContent={noLeidas} color="primary" data-testid={testIds.notifications.badge}>
            <NotificationsNoneOutlinedIcon />
          </Badge>
        </IconButton>
      </Tooltip>

      <Menu
        anchorEl={anchor}
        open={abierto}
        onClose={cerrar}
        data-testid={testIds.notifications.menu}
        slotProps={{ paper: { sx: { width: 360, maxWidth: '100%' } } }}
      >
        <Stack
          direction="row"
          sx={{ alignItems: 'center', justifyContent: 'space-between', px: 3, py: 1.5 }}
        >
          <Typography variant="subtitle2">{t('notifications.alerts')}</Typography>
          {noLeidas > 0 && (
            <Button
              size="small"
              data-testid={testIds.notifications.markAllRead}
              onClick={() => marcarTodas.mutate()}
            >
              {t('notifications.markAllRead')}
            </Button>
          )}
        </Stack>

        <Divider />

        {alertas.length === 0 && (
          <Box sx={{ px: 3, py: 4, textAlign: 'center' }} data-testid={testIds.notifications.empty}>
            <Typography variant="body2" color="text.secondary">
              {t('notifications.empty')}
            </Typography>
          </Box>
        )}

        <Box data-testid={testIds.notifications.list} sx={{ maxHeight: 400, overflowY: 'auto' }}>
          {alertas.map((alerta) => {
            const { primary, secondary } = copyFor(t, alerta);

            return (
              <MenuItem
                key={alerta.id}
                data-testid={testIds.notifications.row(alerta.id)}
                onClick={() => irAlCiclo(alerta)}
                sx={{ whiteSpace: 'normal', alignItems: 'flex-start', py: 1.5 }}
              >
                <Stack spacing={0.25} sx={{ minWidth: 0 }}>
                  <Typography variant="body2" sx={{ fontWeight: alerta.read ? 400 : 600 }}>
                    {primary}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {secondary}
                  </Typography>
                </Stack>
              </MenuItem>
            );
          })}
        </Box>
      </Menu>
    </>
  );
}
