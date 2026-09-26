import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { STATE_HEADLINE, STATE_TONE, type BudgetBalance } from './types';

interface BalanceHeroProps {
  balance: BudgetBalance;
  onReviewClick: () => void;
}

const TONO_A_COLOR = {
  positive: 'success.main',
  neutral: 'text.primary',
  negative: 'error.main',
} as const;

/**
 * La cifra que responde la pregunta con la que alguien abre LUMA.
 *
 * <p>El numero va primero y grande; debajo, que significa. Al reves obligaria a
 * leer una frase para enterarse de algo que se puede ver.
 *
 * <p>Cuando hay gastos por revisar, ese numero es una ESTIMACION y se dice ahi
 * mismo. Es el riesgo de poner la cifra primero, y taparlo seria dar por firme lo
 * que no lo es.
 */
export function BalanceHero({ balance, onReviewClick }: BalanceHeroProps) {
  const tono = STATE_TONE[balance.state];

  return (
    <Stack spacing={3} data-testid={testIds.dashboard.hero}>
      <Stack spacing={1}>
        <Typography variant="overline" color="text.disabled">
          {balance.state === 'DEFICIT' ? 'Balance del ciclo' : 'Te queda disponible'}
        </Typography>

        <Typography
          data-testid={testIds.dashboard.heroAmount}
          sx={{
            fontSize: { xs: '2.5rem', sm: '3.25rem' },
            fontWeight: 700,
            lineHeight: 1.05,
            letterSpacing: '-0.02em',
            fontVariantNumeric: 'tabular-nums',
            color: TONO_A_COLOR[tono],
          }}
        >
          {formatMoney(
            balance.state === 'DEFICIT'
              ? {
                  ...balance.planned.balance,
                  amount: balance.planned.balance.amount.replace('-', ''),
                }
              : balance.planned.balance,
          )}
        </Typography>
      </Stack>

      <Stack spacing={1}>
        <Typography variant="h3" data-testid={testIds.dashboard.heroHeadline}>
          {STATE_HEADLINE[balance.state]}
        </Typography>
        <Typography
          variant="body1"
          color="text.secondary"
          data-testid={testIds.dashboard.heroDetail}
          sx={{ maxWidth: '52ch' }}
        >
          {detalleDe(balance)}
        </Typography>
      </Stack>

      {balance.requiresReview && (
        <Alert
          severity="warning"
          data-testid={testIds.dashboard.heroEstimateNotice}
          action={
            <Typography
              component="button"
              onClick={onReviewClick}
              sx={{
                background: 'none',
                border: 0,
                p: 0,
                cursor: 'pointer',
                font: 'inherit',
                textDecoration: 'underline',
                color: 'inherit',
              }}
            >
              Revisar
            </Typography>
          }
        >
          {balance.counts.needsReview === 1
            ? 'Un gasto todavia no tiene monto confirmado, asi que esta cifra es una estimacion.'
            : `${balance.counts.needsReview} gastos todavia no tienen monto confirmado, asi que esta cifra es una estimacion.`}
        </Alert>
      )}
    </Stack>
  );
}

/** La frase que acompana a la cifra. Dice QUE HACER o QUE PASO, no repite el numero. */
function detalleDe(balance: BudgetBalance): string {
  const ahorro = Number(balance.planned.savings.amount);

  if (balance.state === 'DEFICIT') {
    return 'Tus gastos y tu ahorro suman mas de lo que entra este ciclo. Abajo esta de donde podria salir la diferencia.';
  }

  if (balance.state === 'BALANCED') {
    return 'Lo que entra alcanza justo para lo que planeaste. No hay margen, pero tampoco falta.';
  }

  return ahorro > 0
    ? 'Ya esta contemplado tu ahorro de este ciclo: esto es lo que queda libre despues de apartarlo.'
    : 'Este es tu margen del ciclo. Si quieres, parte de esto puede ir a una meta de ahorro.';
}
