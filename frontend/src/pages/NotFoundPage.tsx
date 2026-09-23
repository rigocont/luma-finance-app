import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import { useNavigate } from 'react-router';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { paths } from '@/routes/paths';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader title="No encontramos esta pagina" />
      <Card>
        <EmptyState
          title="La direccion no existe"
          description="Puede que el enlace este mal escrito o que la seccion todavia no exista."
          action={
            <Button variant="contained" onClick={() => navigate(paths.dashboard)}>
              Ir al resumen
            </Button>
          }
        />
      </Card>
    </div>
  );
}
