import Box from '@mui/material/Box';
import { useTheme } from '@mui/material/styles';
import { BarChart } from '@mui/x-charts/BarChart';

import { formatMoneyCompact } from '@/lib/money';
import { testIds } from '@/lib/testids';
import { chartPalette } from '@/theme/tokens';

import type { CycleTrend } from './types';

interface TrendChartProps {
  trends: CycleTrend[];
}

/**
 * El balance de los ultimos ciclos.
 *
 * <p><b>Una sola medida, un solo eje.</b> No se grafican ingresos y salidas como
 * dos series: la pregunta es "voy mejor o peor", y eso lo responde el balance.
 * Dos series obligarian a restarlas con la vista para contestar lo mismo.
 *
 * <p><b>El color NO es lo que dice el signo.</b> Lo dice la posicion respecto al
 * cero, y lo confirma la etiqueta de cada barra. El verde y el rojo son refuerzo
 * convencional, y hacen falta como refuerzo y no como encoding porque esa pareja
 * falla la comprobacion de daltonismo —separacion de 3.3 en vision protan, muy
 * por debajo del piso—. Quien no distingue verde de rojo lee el signo igual: la
 * barra apunta hacia abajo y el numero trae su menos.
 *
 * <p>Las barras van de mas antiguo a mas reciente, que es el orden en que las
 * manda el servidor. Aqui no se reordena.
 */
export function TrendChart({ trends }: TrendChartProps) {
  const theme = useTheme();
  const c = chartPalette[theme.palette.mode];

  const etiquetas = trends.map((trend) => etiquetaDe(trend.period.start));
  const balances = trends.map((trend) => Number(trend.planned.balance.amount));
  const moneda = trends[0]?.planned.balance.currency ?? 'MXN';

  return (
    <Box data-testid={testIds.dashboard.trendChart} sx={{ width: '100%' }}>
      <BarChart
        height={240}
        series={[
          {
            data: balances,
            label: 'Lo que te quedo',
            // El tooltip formatea con la misma funcion que el resto de la
            // aplicacion: una sola forma de escribir dinero.
            valueFormatter: (value) =>
              value === null ? '' : formatMoneyCompact({ amount: String(value), currency: moneda }),
            // La etiqueta sobre cada barra es la segunda codificacion del signo.
            barLabel: (item) =>
              item.value === null
                ? null
                : formatMoneyCompact({ amount: String(item.value), currency: moneda }),
          },
        ]}
        xAxis={[{ scaleType: 'band', data: etiquetas }]}
        yAxis={[
          {
            // El corte en cero pinta hacia abajo con el color del deficit.
            colorMap: { type: 'piecewise', thresholds: [0], colors: [c.over, c.saving] },
            valueFormatter: (value: number) =>
              formatMoneyCompact({ amount: String(value), currency: moneda }),
          },
        ]}
        // Una sola serie no lleva leyenda: el titulo de la tarjeta ya la nombra.
        hideLegend
        grid={{ horizontal: true }}
        borderRadius={4}
      />
    </Box>
  );
}

/** "15 mar" — corto, porque van seis en el eje. */
function etiquetaDe(iso: string): string {
  const partes = iso.split('-');
  if (partes.length !== 3) return iso;

  const [ano, mes, dia] = partes.map(Number) as [number, number, number];

  return new Intl.DateTimeFormat('es-MX', { day: 'numeric', month: 'short' }).format(
    new Date(ano, mes - 1, dia),
  );
}
