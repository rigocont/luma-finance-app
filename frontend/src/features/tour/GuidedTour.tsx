import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Popper from '@mui/material/Popper';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';

import { testIds } from '@/lib/testids';
import { useTourStore } from '@/store/tourStore';
import { palette, shadows } from '@/theme/tokens';

import { tourStepDescription, tourStepTitle, tourSteps, type TourStep } from './tourSteps';

const SPOTLIGHT_PADDING = 8;

/**
 * Rect en vivo del elemento del paso actual, o `null` si el paso es centrado
 * o el elemento no esta visible ahora mismo.
 *
 * El Sidebar se renderiza DOS veces (drawer temporal en movil, permanente en
 * escritorio; ver components/layout/AppShell.tsx), asi que el mismo
 * `data-testid` existe dos veces en el DOM. `querySelectorAll` + quedarse con
 * la primera copia de tamano distinto de cero evita senalar la copia oculta
 * por CSS del viewport que no aplica.
 */
function useTargetRect(testId: string | null, active: boolean): DOMRect | null {
  const [rect, setRect] = useState<DOMRect | null>(null);

  useEffect(() => {
    if (!active || !testId) {
      setRect(null);
      return;
    }

    function measure() {
      const candidatos = document.querySelectorAll(`[data-testid="${testId}"]`);
      for (const candidato of candidatos) {
        const medida = candidato.getBoundingClientRect();
        if (medida.width > 0 && medida.height > 0) {
          setRect(medida);
          return;
        }
      }
      setRect(null);
    }

    measure();
    window.addEventListener('resize', measure);
    window.addEventListener('scroll', measure, true);
    return () => {
      window.removeEventListener('resize', measure);
      window.removeEventListener('scroll', measure, true);
    };
  }, [testId, active]);

  return rect;
}

/**
 * Tour guiado de bienvenida (Fase 14).
 *
 * Vive montado una sola vez dentro de AppShell -como AdSlot- para que
 * "Volver a tomar el tour" desde Ajustes lo pueda arrancar sin importar en
 * que pantalla este montado. No navega entre rutas: cada paso senala su
 * elemento en la barra lateral, que esta siempre presente (ver tourSteps.ts).
 *
 * El texto de cada paso sigue el idioma de la interfaz desde la Fase 15: no
 * vive en `tourSteps`, se busca por clave con `tourStepTitle`/
 * `tourStepDescription`.
 */
export function GuidedTour() {
  const theme = useTheme();
  const c = palette[theme.palette.mode];
  const s = shadows[theme.palette.mode];

  const completed = useTourStore((state) => state.completed);
  const running = useTourStore((state) => state.running);
  const stepIndex = useTourStore((state) => state.stepIndex);
  const start = useTourStore((state) => state.start);
  const next = useTourStore((state) => state.next);
  const prev = useTourStore((state) => state.prev);
  const skip = useTourStore((state) => state.skip);
  const finish = useTourStore((state) => state.finish);

  // Cuenta nueva en este navegador: ni lo termino ni lo omitio antes.
  // Es un efecto y no un valor inicial del store porque el arranque debe
  // decidirse cuando AppShell monta -es decir, cuando ya hay sesion-, no al
  // cargar el modulo.
  useEffect(() => {
    if (!completed && !running) {
      start();
    }
  }, [completed, running, start]);

  const step: TourStep | undefined = tourSteps[stepIndex];
  const rect = useTargetRect(step?.targetTestId ?? null, running);

  if (!running || !step) {
    return null;
  }

  const isFirst = stepIndex === 0;
  const isLast = stepIndex === tourSteps.length - 1;
  const virtualAnchor = rect ? { getBoundingClientRect: () => rect } : null;

  const card = (
    <TourCard
      step={step}
      index={stepIndex}
      isFirst={isFirst}
      isLast={isLast}
      onPrev={prev}
      onNext={next}
      onSkip={skip}
      onFinish={finish}
      boxShadow={s.level2}
    />
  );

  return (
    <Box
      data-testid={testIds.tour.root}
      sx={{ position: 'fixed', inset: 0, zIndex: theme.zIndex.tooltip + 1 }}
    >
      {/* El recorte del spotlight: una sombra enorme con un hueco del tamano
          del objetivo. Un solo elemento, sin overlay aparte para el fondo. */}
      <Box
        sx={{
          position: 'fixed',
          top: rect ? rect.top - SPOTLIGHT_PADDING : 0,
          left: rect ? rect.left - SPOTLIGHT_PADDING : 0,
          width: rect ? rect.width + SPOTLIGHT_PADDING * 2 : '100%',
          height: rect ? rect.height + SPOTLIGHT_PADDING * 2 : '100%',
          borderRadius: rect ? 2 : 0,
          boxShadow: rect
            ? `0 0 0 9999px rgba(0, 0, 0, 0.6), 0 0 0 2px ${c.honey}`
            : '0 0 0 9999px rgba(0, 0, 0, 0.6)',
          transition: 'top 0.2s ease, left 0.2s ease, width 0.2s ease, height 0.2s ease',
          pointerEvents: 'none',
        }}
      />

      {virtualAnchor ? (
        <Popper
          open
          anchorEl={virtualAnchor}
          placement="right-start"
          modifiers={[
            { name: 'offset', options: { offset: [0, 16] } },
            { name: 'preventOverflow', options: { padding: 16 } },
            { name: 'flip', options: { padding: 16 } },
          ]}
          sx={{ zIndex: theme.zIndex.tooltip + 2 }}
        >
          {card}
        </Popper>
      ) : (
        <Box
          sx={{
            position: 'fixed',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: theme.zIndex.tooltip + 2,
          }}
        >
          {card}
        </Box>
      )}
    </Box>
  );
}

interface TourCardProps {
  step: TourStep;
  index: number;
  isFirst: boolean;
  isLast: boolean;
  onPrev: () => void;
  onNext: () => void;
  onSkip: () => void;
  onFinish: () => void;
  boxShadow: string;
}

function TourCard({
  step,
  index,
  isFirst,
  isLast,
  onPrev,
  onNext,
  onSkip,
  onFinish,
  boxShadow,
}: TourCardProps) {
  const { t } = useTranslation();

  return (
    <Paper
      variant="elevation"
      elevation={0}
      data-testid={testIds.tour.card}
      sx={{ p: 5, width: 320, maxWidth: '90vw', boxShadow }}
    >
      <Stack spacing={3}>
        <Stack spacing={1}>
          <Typography variant="overline" color="text.disabled" data-testid={testIds.tour.progress}>
            {t('tour.progress', { current: index + 1, total: tourSteps.length })}
          </Typography>
          <Typography variant="h4" data-testid={testIds.tour.title}>
            {tourStepTitle(step)}
          </Typography>
          <Typography variant="body2" color="text.secondary" data-testid={testIds.tour.description}>
            {tourStepDescription(step)}
          </Typography>
        </Stack>

        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Button size="small" onClick={onSkip} data-testid={testIds.tour.skipButton}>
            {t('tour.skip')}
          </Button>

          <Stack direction="row" spacing={2}>
            {!isFirst && (
              <Button
                size="small"
                variant="outlined"
                onClick={onPrev}
                data-testid={testIds.tour.prevButton}
              >
                {t('tour.prev')}
              </Button>
            )}
            {isLast ? (
              <Button
                size="small"
                variant="contained"
                onClick={onFinish}
                data-testid={testIds.tour.finishButton}
              >
                {t('tour.finish')}
              </Button>
            ) : (
              <Button
                size="small"
                variant="contained"
                onClick={onNext}
                data-testid={testIds.tour.nextButton}
              >
                {t('tour.next')}
              </Button>
            )}
          </Stack>
        </Stack>
      </Stack>
    </Paper>
  );
}
