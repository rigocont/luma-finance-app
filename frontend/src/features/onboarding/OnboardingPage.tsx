import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Stepper from '@mui/material/Stepper';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate } from 'react-router';

import { LumaMark } from '@/components/layout/LumaMark';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { useCurrentUser } from '@/features/auth/useAuth';
import { useIncomes } from '@/features/incomes/useIncomes';
import { ApiError } from '@/lib/api/types';
import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { CycleStep } from './steps/CycleStep';
import { ExpensesStep } from './steps/ExpensesStep';
import { IncomesStep } from './steps/IncomesStep';
import { SavingsStep } from './steps/SavingsStep';
import { SummaryStep } from './steps/SummaryStep';
import {
  nextStep,
  previousStep,
  OPTIONAL_STEPS,
  STEPS,
  stepLabel,
  type Step as WizardStep,
} from './types';
import { useCyclePreferences } from '../preferences/usePreferences';

import { useCompleteOnboarding, useOnboardingState, useSkipOnboarding } from './useOnboarding';

/**
 * Los mismos filtros que usa el paso de ingresos.
 *
 * No es casualidad: al coincidir la clave, las dos pantallas comparten la
 * respuesta en cache. Capturar un ingreso invalida esa clave y el pie del
 * asistente se entera en el mismo instante que la lista.
 */
const FILTROS_DE_INGRESOS = { sort: 'NEWEST', page: 0, size: 50 } as const;

/**
 * El alta guiada.
 *
 * <p>Vive fuera del shell: sin menu lateral ni encabezado. Todavia no hay nada
 * que navegar, y ofrecer secciones vacias durante el alta invita a irse a
 * medias.
 *
 * <p>Lo capturado se guarda paso a paso con los endpoints normales, asi que
 * cerrar la pestana no pierde nada: al volver, el servidor ya sabe que hay.
 */
export function OnboardingPage() {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const estado = useOnboardingState();
  const ingresos = useIncomes(FILTROS_DE_INGRESOS);
  const preferencias = useCyclePreferences();
  const terminar = useCompleteOnboarding();
  const posponer = useSkipOnboarding();

  const [paso, setPaso] = useState<WizardStep | null>(null);

  // Un paso puede registrar algo que guardar antes de avanzar. Hoy solo lo usa
  // el del ciclo: los demas guardan al capturar cada renglon.
  const guardarDelPaso = useRef<(() => Promise<void>) | null>(null);
  const registrarGuardado = useCallback((save: (() => Promise<void>) | null) => {
    guardarDelPaso.current = save;
  }, []);

  const [avanzando, setAvanzando] = useState(false);
  const [errorAlAvanzar, setErrorAlAvanzar] = useState<unknown>(null);

  // El primer paso lo decide el servidor: quien vuelve retoma donde tenia algo
  // que hacer. Despues manda la navegacion local.
  useEffect(() => {
    if (paso === null && estado.data) {
      setPaso(estado.data.resumeStep);
    }
  }, [paso, estado.data]);

  if (user?.onboardingCompleted) {
    return <Navigate to={paths.dashboard} replace />;
  }

  if (estado.isPending || paso === null) {
    return (
      <Container maxWidth="sm" sx={{ py: 16 }}>
        <LoadingState rows={4} />
      </Container>
    );
  }

  if (estado.isError) {
    return (
      <Container maxWidth="sm" sx={{ py: 16 }}>
        <ErrorState error={estado.error} onRetry={() => estado.refetch()} />
      </Container>
    );
  }

  const datos = estado.data;
  const indice = STEPS.indexOf(paso);
  const esUltimo = paso === 'SUMMARY';
  const esOpcional = OPTIONAL_STEPS.has(paso);

  // Se mira la lista viva, no solo el conteo del servidor. Ese conteo solo se
  // refresca al cambiar de paso, asi que agregar el primer ingreso dejaria el
  // boton deshabilitado hasta que la persona hiciera algo mas. Quien decide de
  // verdad sigue siendo el servidor al terminar; esto solo habilita el boton.
  const hayIngresos = (ingresos.data?.content.length ?? 0) > 0 || datos.canFinish;
  const bloqueadoPorIngresos = paso === 'INCOMES' && !hayIngresos;

  async function irA(destino: WizardStep) {
    setErrorAlAvanzar(null);
    const guardar = guardarDelPaso.current;

    if (guardar) {
      setAvanzando(true);
      try {
        await guardar();
      } catch (error) {
        setErrorAlAvanzar(error);
        setAvanzando(false);
        return;
      }
      setAvanzando(false);
    }

    await estado.refetch();
    setPaso(destino);
  }

  const errorDelPie = errorAlAvanzar ?? terminar.error ?? posponer.error;
  const mensajeDelPie =
    errorDelPie instanceof ApiError
      ? errorDelPie.message
      : errorDelPie
        ? t('onboarding.genericError')
        : null;

  const ocupado = avanzando || terminar.isPending || posponer.isPending;

  return (
    <Box
      data-testid={testIds.onboarding.page}
      sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column' }}
    >
      <Container maxWidth="sm" sx={{ flex: 1, py: { xs: 8, sm: 12 } }}>
        <Stack spacing={8}>
          <Stack direction="row" spacing={2.5} sx={{ alignItems: 'center' }}>
            <LumaMark />
            <Typography
              component="span"
              sx={{ fontWeight: 800, letterSpacing: '0.14em', fontSize: '1.0625rem' }}
            >
              LUMA
            </Typography>
          </Stack>

          <Stepper
            activeStep={indice}
            alternativeLabel
            data-testid={testIds.onboarding.stepper}
            sx={{ display: { xs: 'none', sm: 'flex' } }}
          >
            {STEPS.map((value) => (
              <Step key={value}>
                <StepLabel>{stepLabel(value)}</StepLabel>
              </Step>
            ))}
          </Stepper>

          <Typography
            variant="caption"
            color="text.disabled"
            sx={{ display: { xs: 'block', sm: 'none' } }}
          >
            {t('onboarding.stepIndicator', {
              current: indice + 1,
              total: STEPS.length,
              label: stepLabel(paso),
            })}
          </Typography>

          <Stack spacing={2}>
            <Typography variant="h2">{t(`onboarding.titles.${paso}`)}</Typography>
          </Stack>

          {paso === 'CYCLE' && <CycleStep onRegisterSave={registrarGuardado} />}
          {paso === 'INCOMES' && <IncomesStep />}
          {paso === 'EXPENSES' && <ExpensesStep />}
          {paso === 'SAVINGS' && <SavingsStep />}
          {paso === 'SUMMARY' && <SummaryStep state={datos} preferences={preferencias.data} />}

          {mensajeDelPie && (
            <Alert severity="error" data-testid={testIds.onboarding.error}>
              {mensajeDelPie}
            </Alert>
          )}

          {bloqueadoPorIngresos && <Alert severity="info">{t('onboarding.incomesRequired')}</Alert>}

          <Stack
            direction={{ xs: 'column-reverse', sm: 'row' }}
            spacing={3}
            sx={{ alignItems: 'center', justifyContent: 'space-between', pt: 2 }}
          >
            <Button
              onClick={() => posponer.mutate()}
              disabled={ocupado}
              data-testid={testIds.onboarding.skipAllButton}
            >
              {t('onboarding.skipAll')}
            </Button>

            <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
              {indice > 0 && (
                <Button
                  onClick={() => void irA(previousStep(paso))}
                  disabled={ocupado}
                  data-testid={testIds.onboarding.backButton}
                >
                  {t('onboarding.back')}
                </Button>
              )}

              {esOpcional && (
                <Button
                  onClick={() => void irA(nextStep(paso))}
                  disabled={ocupado}
                  data-testid={testIds.onboarding.skipStepButton}
                >
                  {t('onboarding.skipStep')}
                </Button>
              )}

              {esUltimo ? (
                <Button
                  variant="contained"
                  onClick={() => terminar.mutate()}
                  disabled={ocupado || !hayIngresos}
                  data-testid={testIds.onboarding.finishButton}
                >
                  {terminar.isPending ? t('onboarding.finishing') : t('onboarding.finish')}
                </Button>
              ) : (
                <Button
                  variant="contained"
                  onClick={() => void irA(nextStep(paso))}
                  disabled={ocupado || bloqueadoPorIngresos}
                  data-testid={testIds.onboarding.nextButton}
                >
                  {avanzando ? t('common.oneMoment') : t('onboarding.continue')}
                </Button>
              )}
            </Stack>
          </Stack>
        </Stack>
      </Container>
    </Box>
  );
}
