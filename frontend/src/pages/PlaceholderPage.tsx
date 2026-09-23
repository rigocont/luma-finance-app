import Card from '@mui/material/Card';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';

interface PlaceholderPageProps {
  title: string;
  phase: string;
  description: string;
}

/**
 * Seccion todavia no construida.
 *
 * Se muestra de forma explicita en lugar de ocultar el enlace: la navegacion
 * completa desde la Fase 1 deja ver hacia donde va el producto.
 */
export function PlaceholderPage({ title, phase, description }: PlaceholderPageProps) {
  return (
    <div>
      <PageHeader eyebrow={phase} title={title} description={description} />
      <Card>
        <EmptyState
          title="Esta seccion llega pronto"
          description={`Se construye en la ${phase}. Mientras tanto, el resto de la aplicacion ya funciona.`}
        />
      </Card>
    </div>
  );
}
