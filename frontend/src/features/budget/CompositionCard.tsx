import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';

import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';
import { chartPalette } from '@/theme/tokens';

import { ratePercent, type BudgetTotals } from './types';

interface CompositionCardProps {
  totals: BudgetTotals;
  savingsRate: number;
  /** Los montos de los gastos variables todavia no estan confirmados. */
  hasEstimates: boolean;
}

/** Un segmento de la barra. El orden es el del relato: sale, se aparta, sobra. */
interface Segmento {
  key: string;
  label: string;
  /** Solo para el ancho. Es geometria, no una cifra que se muestre. */
  value: number;
  /** El monto ya formateado por formatMoney, para el tooltip. */
  formatted: string;
  color: string;
  /** Trama diagonal: el monto es una estimacion, no un dato. */
  hatched: boolean;
}

/**
 * En que se reparte lo que entra.
 *
 * <p>Una sola barra apilada y no cuatro tarjetas con numeros: la pregunta es de
 * parte y todo —cuanto de mi ingreso se va en cada cosa— y una longitud responde
 * eso de un vistazo, mientras que cuatro cifras obligan a dividir mentalmente.
 *
 * <p><b>Lo disponible es el hueco, no un color.</b> La barra se dibuja sobre una
 * pista y lo que no se llena es lo que queda. Se intento como cuarto segmento
 * gris y el validador de la paleta lo rechazo: un gris no alcanza el piso de
 * croma y contra el verde del ahorro era indistinguible en vision deutan. Salio
 * mejor diseno de una comprobacion tecnica.
 *
 * <p><b>Con deficit la barra se pasa de la pista.</b> No se recorta ni se
 * normaliza: se escala al total que salga y se marca donde termino el ingreso.
 * Ver mas de lo que entra es exactamente lo que esta ocurriendo, y esconderlo
 * para que la barra quede bonita seria mentir con una imagen.
 */
export function CompositionCard({ totals, savingsRate, hasEstimates }: CompositionCardProps) {
  const theme = useTheme();
  const c = chartPalette[theme.palette.mode];

  const ingreso = Number(totals.income.amount);
  const fijos = Number(totals.fixedExpenses.amount);
  const variables = Number(totals.variableExpenses.amount);
  const ahorro = Number(totals.savings.amount);
  const salidas = Number(totals.totalOutflow.amount);

  // La escala es el mayor de los dos. Con remanente manda el ingreso y sobra
  // pista; con deficit manda la salida y el ingreso queda marcado antes del
  // final.
  const escala = Math.max(ingreso, salidas);
  const seExcede = salidas > ingreso;

  const segmentos: Segmento[] = [
    {
      key: 'fixed',
      label: 'Gastos fijos',
      value: fijos,
      formatted: formatMoney(totals.fixedExpenses),
      color: c.expense,
      hatched: false,
    },
    {
      key: 'variable',
      label: 'Gastos que cambian',
      value: variables,
      formatted: formatMoney(totals.variableExpenses),
      color: c.expense,
      hatched: hasEstimates,
    },
    {
      key: 'savings',
      label: 'Ahorro',
      value: ahorro,
      formatted: formatMoney(totals.savings),
      color: c.saving,
      hatched: false,
    },
  ].filter((segmento) => segmento.value > 0);

  return (
    <Card data-testid={testIds.dashboard.composition}>
      <CardContent>
        <Stack spacing={5}>
          <Stack spacing={1}>
            <Typography variant="h3">En que se reparte</Typography>
            <Typography variant="body2" color="text.secondary">
              De {formatMoney(totals.income)} que entran este ciclo.
            </Typography>
          </Stack>

          {escala > 0 && (
            <Box
              data-testid={testIds.dashboard.compositionBar}
              role="img"
              aria-label={`De ${formatMoney(totals.income)}: ${segmentos
                .map((segmento) => `${segmento.label} ${segmento.value}`)
                .join(', ')}`}
              sx={{
                position: 'relative',
                display: 'flex',
                height: 28,
                borderRadius: 1,
                backgroundColor: c.track,
                overflow: 'hidden',
              }}
            >
              {segmentos.map((segmento, index) => (
                <Tooltip key={segmento.key} title={`${segmento.label}: ${segmento.formatted}`}>
                  <Box
                    data-testid={testIds.dashboard.compositionSegment(segmento.key)}
                    sx={{
                      width: `${(segmento.value / escala) * 100}%`,
                      backgroundColor: segmento.color,
                      // 2px de superficie entre rellenos: sin esa separacion dos
                      // segmentos del mismo tono se leen como uno solo.
                      ...(index > 0 && {
                        borderLeft: `2px solid ${theme.palette.background.paper}`,
                      }),
                      ...(segmento.hatched && {
                        backgroundImage: `repeating-linear-gradient(135deg, ${theme.palette.background.paper}00 0 5px, ${theme.palette.background.paper}59 5px 7px)`,
                      }),
                    }}
                  />
                </Tooltip>
              ))}

              {seExcede && (
                // Donde termino el ingreso. Lo que sigue es lo que no cabe.
                <Box
                  aria-hidden
                  sx={{
                    position: 'absolute',
                    top: 0,
                    bottom: 0,
                    left: `${(ingreso / escala) * 100}%`,
                    width: 2,
                    backgroundColor: c.over,
                  }}
                />
              )}
            </Box>
          )}

          <Stack
            direction="row"
            spacing={4}
            useFlexGap
            sx={{ flexWrap: 'wrap', rowGap: 3, columnGap: 6 }}
          >
            <Renglon
              label="Gastos fijos"
              value={formatMoney(totals.fixedExpenses)}
              color={c.expense}
              testId={testIds.dashboard.totalFixed}
            />
            <Renglon
              label="Gastos que cambian"
              value={formatMoney(totals.variableExpenses)}
              color={c.expense}
              hatched={hasEstimates}
              testId={testIds.dashboard.totalVariable}
            />
            <Renglon
              label="Ahorro"
              value={formatMoney(totals.savings)}
              color={c.saving}
              testId={testIds.dashboard.totalSavings}
            />
            <Renglon
              label={seExcede ? 'Lo que no cabe' : 'Disponible'}
              // El balance ya viene calculado: es ingreso menos salidas. Restarlo
              // aqui seria hacer aritmetica de dinero en el cliente, y con
              // deficit llega en negativo, que es justo lo que hay que mostrar.
              value={formatMoney(totals.balance)}
              color={seExcede ? c.over : c.track}
              testId={testIds.dashboard.totalIncome}
            />
          </Stack>

          <Typography
            variant="body2"
            color="text.secondary"
            data-testid={testIds.dashboard.savingsRate}
          >
            Estas apartando el {ratePercent(savingsRate)}% de lo que entra.
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

/**
 * Una entrada de la leyenda.
 *
 * <p>La leyenda no es opcional: la barra tiene dos tonos cuya separacion en
 * vision protan queda en la banda que solo se admite con una segunda
 * codificacion. Estas etiquetas SON esa codificacion. El texto va con color de
 * texto, nunca del color de la serie; el cuadrito de al lado lleva la identidad.
 */
function Renglon({
  label,
  value,
  color,
  hatched = false,
  testId,
}: {
  label: string;
  value: string;
  color: string;
  hatched?: boolean;
  testId: string;
}) {
  const theme = useTheme();

  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Box
        aria-hidden
        sx={{
          mt: 0.75,
          width: 10,
          height: 10,
          flexShrink: 0,
          borderRadius: 0.5,
          backgroundColor: color,
          ...(hatched && {
            backgroundImage: `repeating-linear-gradient(135deg, ${theme.palette.background.paper}00 0 3px, ${theme.palette.background.paper}80 3px 4px)`,
          }),
        }}
      />
      <Stack spacing={0.25}>
        <Typography variant="caption" color="text.disabled">
          {label}
        </Typography>
        <Typography
          variant="body2"
          sx={{ fontVariantNumeric: 'tabular-nums' }}
          data-testid={testId}
        >
          {value}
        </Typography>
      </Stack>
    </Stack>
  );
}
