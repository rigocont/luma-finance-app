import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { LoadingState } from '@/components/ui/LoadingState';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { FinancialInsights } from './types';

interface FinancialInsightsCardProps {
  insights: FinancialInsights | undefined;
  loading: boolean;
}

/**
 * El analisis financiero sin IA: por que no alcanzo, a donde podria ir lo que
 * sobra, y que categorias llevan una racha al alza.
 *
 * <p>Cada senal es independiente y todas pueden faltar -un ciclo en equilibrio
 * no tiene ni causa de deficit ni reparto, y una cuenta con poco historial no
 * tiene crecimiento que reportar. Cuando no hay ninguna, la tarjeta
 * simplemente no aparece: no hay nada honesto que decir todavia.
 *
 * <p>La redaccion de estas frases es fija, no generada: las cifras vienen del
 * servidor y el texto solo las acomoda. La priorizacion y redaccion con IA
 * (ver docs/00-arquitectura-fase-0.md, S8.4) queda pendiente para una fase
 * futura.
 */
export function FinancialInsightsCard({ insights, loading }: FinancialInsightsCardProps) {
  if (loading) {
    return (
      <Card data-testid={testIds.dashboard.insights}>
        <CardContent>
          <LoadingState rows={2} />
        </CardContent>
      </Card>
    );
  }

  if (!insights) {
    return null;
  }

  const { deficitCause, surplusAllocation, categoryGrowth } = insights;
  const hayAlgoQueDecir =
    deficitCause !== null || surplusAllocation !== null || categoryGrowth.length > 0;

  if (!hayAlgoQueDecir) {
    return null;
  }

  return (
    <Card data-testid={testIds.dashboard.insights}>
      <CardContent>
        <Stack spacing={4} divider={<Divider flexItem />}>
          <Typography variant="h3">Analisis financiero</Typography>

          {deficitCause && (
            <Stack spacing={1} data-testid={testIds.dashboard.insightsDeficitCause}>
              <Typography variant="body1">
                &ldquo;{deficitCause.categoryName}&rdquo; es lo que mas subio este ciclo: de{' '}
                {formatMoney(deficitCause.previousAmount)} a{' '}
                {formatMoney(deficitCause.currentAmount)} (+{formatMoney(deficitCause.increase)}).
              </Typography>
              <Typography variant="caption" color="text.secondary">
                No es la unica razon posible del deficit, pero es la que mas cambio respecto al
                ciclo anterior.
              </Typography>
            </Stack>
          )}

          {surplusAllocation && (
            <Stack spacing={2} data-testid={testIds.dashboard.insightsSurplus}>
              <Typography variant="body1">
                Te sobran {formatMoney(surplusAllocation.surplus)} este ciclo.
              </Typography>
              {surplusAllocation.shares.length > 0 ? (
                <>
                  <Typography variant="caption" color="text.secondary">
                    Esto es a donde alcanzaria, si sigues el orden de tus metas:
                  </Typography>
                  <Stack spacing={1}>
                    {surplusAllocation.shares.map((share) => (
                      <Stack
                        key={share.goalId}
                        direction="row"
                        data-testid={testIds.dashboard.insightsSurplusRow}
                        sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                      >
                        <Typography variant="body2">{share.goalName}</Typography>
                        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                          {formatMoney(share.amount)}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                </>
              ) : (
                <Typography variant="caption" color="text.secondary">
                  No tienes ninguna meta activa a la que asignarlo. Crea una en Ahorros si quieres
                  aprovecharlo.
                </Typography>
              )}
            </Stack>
          )}

          {categoryGrowth.length > 0 && (
            <Stack spacing={1} data-testid={testIds.dashboard.insightsGrowth}>
              <Typography variant="body2" color="text.secondary">
                Categorias con una racha al alza:
              </Typography>
              <Stack spacing={1}>
                {categoryGrowth.map((growth) => (
                  <Stack
                    key={growth.categoryName}
                    direction="row"
                    data-testid={testIds.dashboard.insightsGrowthRow}
                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                  >
                    <Typography variant="body2">{growth.categoryName}</Typography>
                    <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                      {formatMoney(growth.firstAmount)} → {formatMoney(growth.lastAmount)} (
                      {growth.cycles} ciclos)
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            </Stack>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}
