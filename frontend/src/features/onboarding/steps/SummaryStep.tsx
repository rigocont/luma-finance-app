import Alert from '@mui/material/Alert';
import Card from '@mui/material/Card';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { testIds } from '@/lib/testids';

import { CYCLE_TYPE_LABELS, type CyclePreferences } from '../../preferences/types';
import type { OnboardingState } from '../types';

interface SummaryStepProps {
  state: OnboardingState;
  preferences: CyclePreferences | undefined;
}

/**
 * Lo que quedo configurado, antes de entrar.
 *
 * <p>Se muestran CONTEOS, no un balance. El balance lo calcula el motor a partir
 * de un ciclo, y el ciclo todavia no existe: se abre justo al presionar
 * "Empezar". Inventar aqui una cifra derivada en el cliente seria la primera
 * vez que el frontend calcula dinero, y no va a ser en una pantalla de alta.
 */
export function SummaryStep({ state, preferences }: SummaryStepProps) {
  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('summary')}>
      <Typography variant="body1" color="text.secondary">
        Con esto ya podemos abrir tu primer ciclo y decirte cuanto te queda.
      </Typography>

      <Card variant="outlined" sx={{ p: 5 }}>
        <Stack spacing={4}>
          <Linea
            etiqueta="Cada cuanto presupuestas"
            valor={
              preferences
                ? `${CYCLE_TYPE_LABELS[preferences.budgetCycleType]}, desde el dia ${preferences.cycleAnchorDay}`
                : '—'
            }
            testId={testIds.onboarding.summaryCycle}
          />
          <Linea
            etiqueta="Ingresos"
            valor={contar(state.incomeCount, 'ingreso', 'ingresos')}
            testId={testIds.onboarding.summaryIncomes}
          />
          <Linea
            etiqueta="Gastos"
            valor={contar(state.expenseCount, 'gasto', 'gastos')}
            testId={testIds.onboarding.summaryExpenses}
          />
          <Linea
            etiqueta="Metas de ahorro"
            valor={contar(state.goalCount, 'meta', 'metas')}
            testId={testIds.onboarding.summaryGoals}
          />
        </Stack>
      </Card>

      {state.expenseCount === 0 && (
        <Alert severity="info">
          No capturaste gastos. Puedes empezar asi y agregarlos desde la seccion de Gastos; tu ciclo
          en curso los va a tomar en cuanto existan.
        </Alert>
      )}

      <Typography variant="body2" color="text.secondary">
        Al empezar se abre tu primer ciclo con estos renglones ya puestos. Todo esto se puede
        cambiar despues desde cada seccion.
      </Typography>
    </Stack>
  );
}

function contar(cuantos: number, singular: string, plural: string): string {
  if (cuantos === 0) return 'Ninguno por ahora';
  return `${cuantos} ${cuantos === 1 ? singular : plural}`;
}

function Linea({ etiqueta, valor, testId }: { etiqueta: string; valor: string; testId: string }) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'baseline' } }}
    >
      <Typography variant="body2" color="text.secondary">
        {etiqueta}
      </Typography>
      <Typography variant="body1" data-testid={testId}>
        {valor}
      </Typography>
    </Stack>
  );
}
