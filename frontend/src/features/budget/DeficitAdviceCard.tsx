import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import { LoadingState } from '@/components/ui/LoadingState';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { CutSuggestion, DeficitAdvice } from './types';

interface DeficitAdviceCardProps {
  advice: DeficitAdvice | undefined;
  loading: boolean;
  pendingItemId: string | null;
  onSkip: (cut: CutSuggestion) => void;
}

/**
 * De donde podria salir lo que falta.
 *
 * <p>El orden lo decide el servidor —gastos flexibles, luego ahorros del menos
 * prioritario al mas, y al final los importantes— y aqui no se reordena. Los
 * gastos criticos no aparecen nunca: es lo que la aplicacion promete al
 * capturarlos.
 *
 * <p>LUMA no mueve nada. Cada renglon ofrece la accion; quien decide es la
 * persona. Una aplicacion de dinero que recorta por su cuenta deja de ser
 * confiable el primer dia que se equivoca.
 */
export function DeficitAdviceCard({
  advice,
  loading,
  pendingItemId,
  onSkip,
}: DeficitAdviceCardProps) {
  const { t } = useTranslation();

  return (
    <Card data-testid={testIds.dashboard.advice}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1}>
            <Typography variant="h3">{t('dashboard.advice.title')}</Typography>
            {advice && (
              <Typography variant="body2" color="text.secondary">
                {t('dashboard.advice.subtitle', { amount: formatMoney(advice.missing) })}
              </Typography>
            )}
          </Stack>

          {loading && <LoadingState rows={3} />}

          {advice && advice.cuts.length === 0 && (
            <Alert severity="warning" data-testid={testIds.dashboard.adviceShortfall}>
              {t('dashboard.advice.noOptions')}
            </Alert>
          )}

          {advice && advice.cuts.length > 0 && (
            <>
              {!advice.coversTheGap && (
                <Alert severity="warning" data-testid={testIds.dashboard.adviceShortfall}>
                  {t('dashboard.advice.shortfall', { covered: formatMoney(advice.covered) })}
                </Alert>
              )}

              <Stack divider={<Divider flexItem />}>
                {advice.cuts.map((cut) => (
                  <Stack
                    key={cut.itemId}
                    direction="row"
                    spacing={3}
                    data-testid={testIds.dashboard.adviceRow}
                    sx={{ alignItems: 'center', justifyContent: 'space-between', py: 3 }}
                  >
                    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                      <Stack
                        direction="row"
                        spacing={2}
                        sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}
                      >
                        <Typography variant="body1">{cut.name}</Typography>
                        {cut.itemType === 'SAVING' && (
                          <Chip
                            size="small"
                            variant="outlined"
                            label={t('dashboard.advice.savingChip')}
                          />
                        )}
                        {cut.estimated && (
                          <Chip
                            size="small"
                            variant="outlined"
                            label={t('dashboard.advice.estimatedChip')}
                          />
                        )}
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {cut.itemType === 'SAVING'
                          ? t('dashboard.advice.savingNote')
                          : t('dashboard.advice.expenseNote')}
                      </Typography>
                    </Stack>

                    <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
                      <Typography
                        variant="body1"
                        sx={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}
                      >
                        {formatMoney(cut.amount)}
                      </Typography>
                      <Button
                        size="small"
                        disabled={pendingItemId !== null}
                        onClick={() => onSkip(cut)}
                        data-testid={testIds.dashboard.adviceSkipAction}
                      >
                        {pendingItemId === cut.itemId
                          ? t('common.oneMoment')
                          : t('dashboard.advice.skip')}
                      </Button>
                    </Stack>
                  </Stack>
                ))}
              </Stack>
            </>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}
