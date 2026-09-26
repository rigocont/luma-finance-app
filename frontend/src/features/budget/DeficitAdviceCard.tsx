import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

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
  return (
    <Card data-testid={testIds.dashboard.advice}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1}>
            <Typography variant="h3">De donde podria salir</Typography>
            {advice && (
              <Typography variant="body2" color="text.secondary">
                Faltan {formatMoney(advice.missing)}. Esto es lo que se puede mover, de lo que menos
                cuesta a lo que mas.
              </Typography>
            )}
          </Stack>

          {loading && <LoadingState rows={3} />}

          {advice && advice.cuts.length === 0 && (
            <Alert severity="warning" data-testid={testIds.dashboard.adviceShortfall}>
              No encontramos nada que se pueda mover sin tocar tus gastos criticos. Tendrias que
              ajustar un ingreso o revisar si algun gasto critico de verdad lo es.
            </Alert>
          )}

          {advice && advice.cuts.length > 0 && (
            <>
              {!advice.coversTheGap && (
                <Alert severity="warning" data-testid={testIds.dashboard.adviceShortfall}>
                  Aun moviendo todo esto ({formatMoney(advice.covered)}) no alcanza a cubrir lo que
                  falta. Sirve para acercarte, no para cerrar el ciclo.
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
                          <Chip size="small" variant="outlined" label="Ahorro" />
                        )}
                        {cut.estimated && <Chip size="small" variant="outlined" label="Estimado" />}
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {cut.itemType === 'SAVING'
                          ? 'Si no lo apartas este ciclo, la meta lo retoma en el siguiente.'
                          : 'Quitarlo de este ciclo no borra el gasto ni lo desactiva.'}
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
                        {pendingItemId === cut.itemId ? 'Un momento...' : 'Quitar del ciclo'}
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
