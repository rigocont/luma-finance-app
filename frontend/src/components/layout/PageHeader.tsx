import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

import { testIds } from '@/lib/testids';

interface PageHeaderProps {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function PageHeader({ eyebrow, title, description, action }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={3}
      sx={{
        alignItems: { xs: 'flex-start', sm: 'flex-end' },
        justifyContent: 'space-between',
        pt: 8,
        pb: 6,
      }}
    >
      <Stack spacing={1} sx={{ minWidth: 0 }}>
        {eyebrow && (
          <Typography variant="overline" color="text.disabled">
            {eyebrow}
          </Typography>
        )}
        <Typography variant="h2" data-testid={testIds.layout.pageTitle}>
          {title}
        </Typography>
        {description && (
          <Typography variant="body1" color="text.secondary" sx={{ maxWidth: '60ch' }}>
            {description}
          </Typography>
        )}
      </Stack>
      {action}
    </Stack>
  );
}
