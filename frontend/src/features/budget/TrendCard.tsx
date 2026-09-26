import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import { EmptyState } from '@/components/ui/EmptyState';
import { LoadingState } from '@/components/ui/LoadingState';
import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { TrendChart } from './TrendChart';
import type { Change, CycleTrend } from './types';

interface TrendCardProps {
  trends: CycleTrend[] | undefined;
  loading: boolean;
}

/**
 * Como va este ciclo comparado con el pasado.
 *
 * <p>La comparacion es contra el ciclo INMEDIATAMENTE anterior, no contra un
 * promedio. Un promedio de seis ciclos diluye justo el salto que importa cuando
 * algo acaba de cambiar.
 *
 * <p>El servidor manda los ciclos del mas antiguo al mas reciente: una
 * comparacion se lee como linea de tiempo y no como lista. Aqui no se reordena.
 */
export function TrendCard({ trends, loading }: TrendCardProps) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <Card data-testid={testIds.dashboard.trend}>
        <CardContent>
          <LoadingState rows={3} />
        </CardContent>
      </Card>
    );
  }

  const ciclos = trends ?? [];

  if (ciclos.length < 2) {
    return (
      <Card data-testid={testIds.dashboard.trend}>
        <CardContent>
          <div data-testid={testIds.dashboard.trendEmpty}>
            <EmptyState
              title={t('dashboard.trend.emptyTitle')}
              description={t('dashboard.trend.emptyDescription')}
            />
          </div>
        </CardContent>
      </Card>
    );
  }

  const actual = ciclos[ciclos.length - 1]!;
  const anterior = ciclos[ciclos.length - 2]!;

  return (
    <Card data-testid={testIds.dashboard.trend}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1}>
            <Typography variant="h3">{t('dashboard.trend.title')}</Typography>
            <Typography
              variant="body1"
              color="text.secondary"
              data-testid={testIds.dashboard.trendDelta}
            >
              {frasePara(t, actual.outflowChange)}
            </Typography>
            <Typography variant="caption" color="text.disabled">
              {t('dashboard.trend.previousOutflow', {
                date: formatDay(anterior.period.start),
                amount: formatMoney(anterior.planned.totalOutflow),
              })}
            </Typography>
          </Stack>

          <TrendChart trends={ciclos} />
        </Stack>
      </CardContent>
    </Card>
  );
}

/**
 * La frase, a partir de lo que mando el servidor.
 *
 * No hay ninguna resta aqui: la direccion y la magnitud llegan calculadas. Fue
 * el lint del proyecto el que lo exigio, y tenia razon — esta funcion empezo
 * restando dos montos y sacandoles el valor absoluto.
 */
function frasePara(
  t: (key: string, opts?: Record<string, unknown>) => string,
  change: Change | null,
): string {
  if (change === null || change.direction === 'SAME') {
    return t('dashboard.trend.same');
  }

  const monto = formatMoney(change.amount);

  return change.direction === 'UP'
    ? t('dashboard.trend.up', { amount: monto })
    : t('dashboard.trend.down', { amount: monto });
}
