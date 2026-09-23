import { describe, expect, it } from 'vitest';

import { formatMoney, formatMoneyCompact, isNegative, isPositive } from './index';

describe('formato de dinero', () => {
  it('siempre muestra dos decimales', () => {
    const formatted = formatMoney({ amount: '1100', currency: 'MXN' });
    expect(formatted).toContain('1,100.00');
  });

  it('conserva el signo de un deficit', () => {
    const value = { amount: '-740.00', currency: 'MXN' };
    expect(isNegative(value)).toBe(true);
    expect(formatMoney(value)).toContain('740.00');
  });

  it('la version compacta omite los decimales', () => {
    const formatted = formatMoneyCompact({ amount: '12500.00', currency: 'MXN' });
    expect(formatted).toContain('12,500');
    expect(formatted).not.toContain('.00');
  });

  it('reconoce un remanente positivo', () => {
    expect(isPositive({ amount: '1100.00', currency: 'MXN' })).toBe(true);
  });
});
