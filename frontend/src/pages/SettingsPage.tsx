import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { PageHeader } from '@/components/layout/PageHeader';
import { ChangePasswordCard } from '@/features/auth/ChangePasswordCard';
import { useCurrentUser } from '@/features/auth/useAuth';
import { testIds } from '@/lib/testids';
import { useUiStore, type ThemePreference } from '@/store/uiStore';

const THEME_OPTIONS: { value: ThemePreference; label: string }[] = [
  { value: 'light', label: 'Claro' },
  { value: 'dark', label: 'Oscuro' },
  { value: 'system', label: 'Como el sistema' },
];

export function SettingsPage() {
  const user = useCurrentUser();

  return (
    <div data-testid={testIds.settings.page}>
      <PageHeader
        eyebrow="Fase 2"
        title="Ajustes"
        description="Tu cuenta y la apariencia de la aplicacion. El ciclo presupuestal, la moneda y la zona horaria llegan con el motor presupuestal."
      />

      <Stack spacing={6}>
        <Card>
          <CardContent>
            <Stack spacing={4}>
              <Typography variant="h3">Tu cuenta</Typography>
              <Stack direction="row" spacing={10} sx={{ flexWrap: 'wrap' }} useFlexGap>
                <Field label="Nombre" value={user?.name ?? '—'} />
                <Field label="Correo" value={user?.email ?? '—'} testId={testIds.settings.email} />
              </Stack>
            </Stack>
          </CardContent>
        </Card>

        <ThemeCard />

        <ChangePasswordCard />
      </Stack>
    </div>
  );
}

function ThemeCard() {
  const themePreference = useUiStore((state) => state.themePreference);
  const setThemePreference = useUiStore((state) => state.setThemePreference);

  return (
    <Card data-testid={testIds.settings.themeCard}>
      <CardContent>
        <Stack spacing={4}>
          <Stack spacing={1.5}>
            <Typography variant="h3">Apariencia</Typography>
            <Typography variant="body2" color="text.secondary">
              Esta preferencia se guarda en este navegador.
            </Typography>
          </Stack>

          <Stack direction="row" spacing={2.5} sx={{ flexWrap: 'wrap' }} useFlexGap>
            {THEME_OPTIONS.map((option) => (
              <Button
                key={option.value}
                variant={themePreference === option.value ? 'contained' : 'outlined'}
                onClick={() => setThemePreference(option.value)}
                data-testid={testIds.settings.themeOption(option.value)}
              >
                {option.label}
              </Button>
            ))}
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
