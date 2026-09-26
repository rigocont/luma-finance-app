import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

/**
 * Sin cuenta de AdSense configurada -el caso normal en desarrollo y en las
 * pruebas- el componente no debe hacer ninguna peticion externa. Cada prueba
 * reimporta el modulo con vi.resetModules() porque los IDs se leen una sola
 * vez, al cargar el archivo.
 */
describe('AdSlot', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('reserva el espacio con un marcador cuando no hay cuenta de AdSense', async () => {
    vi.stubEnv('VITE_ADSENSE_CLIENT_ID', '');
    vi.stubEnv('VITE_ADSENSE_SLOT_ID', '');
    const { AdSlot } = await import('./AdSlot');

    render(<AdSlot />);

    expect(screen.getByText('Espacio publicitario')).toBeInTheDocument();
    expect(document.querySelector('ins.adsbygoogle')).not.toBeInTheDocument();
  });

  it('dibuja el anuncio real cuando la cuenta y el espacio estan configurados', async () => {
    vi.stubEnv('VITE_ADSENSE_CLIENT_ID', 'ca-pub-1234567890123456');
    vi.stubEnv('VITE_ADSENSE_SLOT_ID', '1122334455');
    const { AdSlot } = await import('./AdSlot');

    render(<AdSlot />);

    const anuncio = document.querySelector('ins.adsbygoogle');
    expect(anuncio).toBeInTheDocument();
    expect(anuncio).toHaveAttribute('data-ad-client', 'ca-pub-1234567890123456');
    expect(anuncio).toHaveAttribute('data-ad-slot', '1122334455');
    expect(screen.queryByText('Espacio publicitario')).not.toBeInTheDocument();
  });
});
