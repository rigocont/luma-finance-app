import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import type { ReactNode } from 'react';

import { testIds } from '@/lib/testids';
import { palette } from '@/theme/tokens';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  const theme = useTheme();
  const c = palette[theme.palette.mode];

  return (
    <Stack
      spacing={3}
      data-testid={testIds.state.empty}
      sx={{ alignItems: 'center', textAlign: 'center', py: 16, px: 6 }}
    >
      <Box
        aria-hidden
        sx={{
          width: 36,
          height: 36,
          borderRadius: '50%',
          backgroundColor: c.honeyWash,
          border: `1px solid ${c.honeyFill}`,
        }}
      />
      <Typography variant="h4">{title}</Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '40ch' }}>
          {description}
        </Typography>
      )}
      {action}
    </Stack>
  );
}
