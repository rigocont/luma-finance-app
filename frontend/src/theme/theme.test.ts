import { describe, expect, it } from 'vitest';

import { createLumaTheme } from './index';
import { palette } from './tokens';

describe('tema de LUMA', () => {
  it('usa tinta como accion primaria en ambos modos', () => {
    expect(createLumaTheme('light').palette.primary.main).toBe(palette.light.ink);
    expect(createLumaTheme('dark').palette.primary.main).toBe(palette.dark.ink);
  });

  it('reserva verde y rojo para el resultado del dinero', () => {
    const light = createLumaTheme('light');
    expect(light.palette.success.main).toBe(palette.light.positive);
    expect(light.palette.error.main).toBe(palette.light.negative);
  });

  it('pinta un fondo explicito en cada modo', () => {
    expect(createLumaTheme('light').palette.background.default).toBe(palette.light.ground);
    expect(createLumaTheme('dark').palette.background.default).toBe(palette.dark.ground);
  });

  it('usa una escala de espacio con base 4', () => {
    expect(createLumaTheme('light').spacing(4)).toBe('16px');
  });
});
