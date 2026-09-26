import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/components/ui/EmptyState';
import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { CycleItem } from '../review/types';

interface UpcomingPaymentsProps {
  items: CycleItem[];
  onSettle: (item: CycleItem) => void;
}

/** Cuantos se muestran. El resto vive en la pantalla del ciclo. */
const CUANTOS = 6;

/**
 * Lo que falta por pagar, por fecha.
 *
 * <p>Los vencidos van arriba y marcados, no mezclados en su lugar cronologico:
 * si algo ya se paso de fecha es lo primero que hay que ver, y respetar el orden
 * del calendario lo escondería entre lo que aun no toca.
 */
export function UpcomingPayments({ items, onSettle }: UpcomingPaymentsProps) {
  const { t } = useTranslation();

  const porPagar = items
    .filter((item) => item.itemType !== 'INCOME')
    .filter((item) => item.status === 'PENDING' || item.status === 'OVERDUE')
    .sort(porVencidosYFecha)
    .slice(0, CUANTOS);

  return (
    <Card data-testid={testIds.dashboard.upcoming}>
      <CardContent>
        <Stack spacing={4}>
          <Typography variant="h3">{t('dashboard.upcoming.title')}</Typography>

          {porPagar.length === 0 && (
            <div data-testid={testIds.dashboard.upcomingEmpty}>
              <EmptyState
                title={t('dashboard.upcoming.emptyTitle')}
                description={t('dashboard.upcoming.emptyDescription')}
              />
            </div>
          )}

          {porPagar.length > 0 && (
            <Stack divider={<Divider flexItem />}>
              {porPagar.map((item) => {
                const vencido = item.status === 'OVERDUE';

                return (
                  <Stack
                    key={item.id}
                    direction="row"
                    spacing={3}
                    data-testid={testIds.dashboard.upcomingRow}
                    sx={{ alignItems: 'center', justifyContent: 'space-between', py: 3 }}
                  >
                    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                      <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                        <Typography variant="body1">{item.name}</Typography>
                        {vencido && (
                          <Chip
                            size="small"
                            color="error"
                            label={t('dashboard.upcoming.overdueChip')}
                            data-testid={testIds.dashboard.upcomingOverdue}
                          />
                        )}
                      </Stack>
                      {item.dueDate !== null && (
                        <Typography variant="caption" color="text.secondary">
                          {vencido
                            ? t('dashboard.upcoming.overdueLabel')
                            : t('dashboard.upcoming.dueLabel')}{' '}
                          {formatDay(item.dueDate)}
                        </Typography>
                      )}
                    </Stack>

                    <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
                      <Typography
                        variant="body1"
                        sx={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}
                      >
                        {formatMoney(item.plannedAmount)}
                      </Typography>
                      <Button
                        size="small"
                        onClick={() => onSettle(item)}
                        data-testid={testIds.dashboard.settleAction}
                      >
                        {t('dashboard.upcoming.settle')}
                      </Button>
                    </Stack>
                  </Stack>
                );
              })}
            </Stack>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}

function porVencidosYFecha(a: CycleItem, b: CycleItem): number {
  if (a.status !== b.status) {
    if (a.status === 'OVERDUE') return -1;
    if (b.status === 'OVERDUE') return 1;
  }
  return (a.dueDate ?? '').localeCompare(b.dueDate ?? '');
}
