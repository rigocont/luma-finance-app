import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';

import { PageHeader } from '@/components/layout/PageHeader';
import { ChangePasswordCard } from '@/features/auth/ChangePasswordCard';
import { useCurrentUser } from '@/features/auth/useAuth';
import { CyclePreferenceCard } from '@/features/preferences/CyclePreferenceCard';
import { SystemStatusCard } from '@/features/preferences/SystemStatusCard';
import { useCyclePreferences, useUpdateLanguage } from '@/features/preferences/usePreferences';
import { SUPPORTED_LANGUAGES, type SupportedLanguage } from '@/i18n';
import { testIds } from '@/lib/testids';
import { useTourStore } from '@/store/tourStore';
import { useUiStore, type ThemePreference } from '@/store/uiStore';

const THEME_OPTIONS: ThemePreference[] = ['light', 'dark', 'system'];

export function SettingsPage() {
  const { t } = useTranslation();
  const user = useCurrentUser();

  return (
    <div data-testid={testIds.settings.page}>
      <PageHeader
        eyebrow={t('settings.eyebrow')}
        title={t('settings.title')}
        description={t('settings.description')}
      />

      <Stack spacing={6}>
        <Card>
          <CardContent>
            <Stack spacing={4}>
              <Typography variant="h3">{t('settings.account.title')}</Typography>
              <Stack direction="row" spacing={10} sx={{ flexWrap: 'wrap' }} useFlexGap>
                <Field label={t('settings.account.name')} value={user?.name ?? '—'} />
                <Field
                  label={t('settings.account.email')}
                  value={user?.email ?? '—'}
                  testId={testIds.settings.email}
                />
              </Stack>
            </Stack>
          </CardContent>
        </Card>

        <CyclePreferenceCard />

        <LanguageCard />

        <ThemeCard />

        <TourCard />

        <ChangePasswordCard />

        <SystemStatusCard />
      </Stack>
    </div>
  );
}

function ThemeCard() {
  const { t } = useTranslation();
  const themePreference = useUiStore((state) => state.themePreference);
  const setThemePreference = useUiStore((state) => state.setThemePreference);

  return (
    <Card data-testid={testIds.settings.themeCard}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1.5}>
            <Typography variant="h3">{t('settings.theme.title')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {t('settings.theme.description')}
            </Typography>
          </Stack>

          <Stack direction="row" spacing={2.5} sx={{ flexWrap: 'wrap' }} useFlexGap>
            {THEME_OPTIONS.map((option) => (
              <Button
                key={option}
                variant={themePreference === option ? 'contained' : 'outlined'}
                onClick={() => setThemePreference(option)}
                data-testid={testIds.settings.themeOption(option)}
              >
                {t(`settings.theme.${option}`)}
              </Button>
            ))}
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

/**
 * El idioma de la interfaz (Fase 15).
 *
 * <p>Se guarda en la cuenta, no en este navegador: por eso vive junto al
 * ciclo presupuestal y no junto a Apariencia, que si es de este dispositivo.
 */
function LanguageCard() {
  const { t, i18n } = useTranslation();
  const preferencias = useCyclePreferences();
  const cambiar = useUpdateLanguage();

  const actual = (preferencias.data?.uiLanguage as SupportedLanguage | undefined) ?? i18n.language;

  return (
    <Card data-testid={testIds.settings.languageCard}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1.5}>
            <Typography variant="h3">{t('settings.language.title')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {t('settings.language.description')}
            </Typography>
          </Stack>

          <Stack direction="row" spacing={2.5} sx={{ flexWrap: 'wrap' }} useFlexGap>
            {SUPPORTED_LANGUAGES.map((value) => (
              <Button
                key={value}
                variant={actual === value ? 'contained' : 'outlined'}
                disabled={cambiar.isPending}
                onClick={() => cambiar.mutate(value)}
                data-testid={testIds.settings.languageOption(value)}
              >
                {t(`settings.language.${value}`)}
              </Button>
            ))}
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

function TourCard() {
  const { t } = useTranslation();
  const start = useTourStore((state) => state.start);

  return (
    <Card data-testid={testIds.settings.tourCard}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1.5}>
            <Typography variant="h3">{t('settings.tour.title')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {t('settings.tour.description')}
            </Typography>
          </Stack>

          <Stack direction="row">
            <Button variant="outlined" onClick={start} data-testid={testIds.settings.tourRestart}>
              {t('settings.tour.restart')}
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

function Field({ label, value, testId }: { label: string; value: string; testId?: string }) {
  return (
    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
      <Typography variant="caption" color="text.disabled">
        {label}
      </Typography>
      <Typography variant="body2" data-testid={testId}>
        {value}
      </Typography>
    </Stack>
  );
}
