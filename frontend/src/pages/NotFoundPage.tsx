import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { paths } from '@/routes/paths';

export function NotFoundPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader title={t('notFound.title')} />
      <Card>
        <EmptyState
          title={t('notFound.heading')}
          description={t('notFound.description')}
          action={
            <Button variant="contained" onClick={() => navigate(paths.dashboard)}>
              {t('notFound.goToDashboard')}
            </Button>
          }
        />
      </Card>
    </div>
  );
}
