import Alert from '@mui/material/Alert';
import Card from '@mui/material/Card';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';

import { testIds } from '@/lib/testids';

import { cycleTypeLabel, type CyclePreferences } from '../../preferences/types';
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
  const { t } = useTranslation();

  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('summary')}>
      <Typography variant="body1" color="text.secondary">
        {t('onboarding.summary.intro')}
      </Typography>

      <Card variant="outlined" sx={{ p: 5 }}>
        <Stack spacing={4}>
          <Linea
            etiqueta={t('onboarding.summary.cycleLine')}
            valor={
              preferences
                ? t('onboarding.summary.cycleValue', {
                    type: cycleTypeLabel(preferences.budgetCycleType),
                    day: preferences.cycleAnchorDay,
                  })
                : '—'
            }
            testId={testIds.onboarding.summaryCycle}
          />
          <Linea
            etiqueta={t('onboarding.summary.incomesLine')}
            valor={contar(t, state.incomeCount, 'countIncomes', 'countIncomesPlural')}
            testId={testIds.onboarding.summaryIncomes}
          />
          <Linea
            etiqueta={t('onboarding.summary.expensesLine')}
            valor={contar(t, state.expenseCount, 'countExpenses', 'countExpensesPlural')}
            testId={testIds.onboarding.summaryExpenses}
          />
          <Linea
            etiqueta={t('onboarding.summary.goalsLine')}
            valor={contar(t, state.goalCount, 'countGoals', 'countGoalsPlural')}
            testId={testIds.onboarding.summaryGoals}
          />
        </Stack>
      </Card>

      {state.expenseCount === 0 && (
        <Alert severity="info">{t('onboarding.summary.noExpensesNotice')}</Alert>
      )}

      <Typography variant="body2" color="text.secondary">
        {t('onboarding.summary.footer')}
      </Typography>
    </Stack>
  );
}

function contar(t: TFunction, cuantos: number, singularKey: string, pluralKey: string): string {
  if (cuantos === 0) return t('onboarding.summary.none');
  return t(`onboarding.summary.${cuantos === 1 ? singularKey : pluralKey}`, { count: cuantos });
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
