import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { FinancialInsightsCard } from './FinancialInsightsCard';
import type { FinancialInsights } from './types';

const vacio: FinancialInsights = {
  deficitCause: null,
  surplusAllocation: null,
  categoryGrowth: [],
};

describe('FinancialInsightsCard', () => {
  it('no dibuja nada mientras carga si no hay nada previo que mostrar', () => {
    render(<FinancialInsightsCard insights={undefined} loading />);

    expect(screen.getByTestId('dashboard-insights')).toBeInTheDocument();
  });

  it('no dibuja nada cuando no hay ninguna senal que respaldar', () => {
    render(<FinancialInsightsCard insights={vacio} loading={false} />);

    expect(screen.queryByTestId('dashboard-insights')).not.toBeInTheDocument();
  });

  it('explica la causa del deficit con las cifras que la respaldan', () => {
    const insights: FinancialInsights = {
      ...vacio,
      deficitCause: {
        cycleId: 'ciclo-1',
        missing: { amount: '500.00', currency: 'MXN' },
        categoryName: 'Despensa',
        previousAmount: { amount: '1000.00', currency: 'MXN' },
        currentAmount: { amount: '1500.00', currency: 'MXN' },
        increase: { amount: '500.00', currency: 'MXN' },
      },
    };

    render(<FinancialInsightsCard insights={insights} loading={false} />);

    const bloque = screen.getByTestId('dashboard-insights-deficit-cause');
    expect(bloque.textContent).toContain('Despensa');
    expect(bloque.textContent).toContain('500.00');
    expect(bloque.textContent).toContain('1,000.00');
    expect(bloque.textContent).toContain('1,500.00');
  });

  it('reparte el remanente entre las metas en el orden que llego', () => {
    const insights: FinancialInsights = {
      ...vacio,
      surplusAllocation: {
        cycleId: 'ciclo-1',
        surplus: { amount: '2000.00', currency: 'MXN' },
        shares: [
          {
            goalId: 'meta-1',
            goalName: 'Vacaciones',
            amount: { amount: '1500.00', currency: 'MXN' },
          },
          { goalId: 'meta-2', goalName: 'Fondo', amount: { amount: '500.00', currency: 'MXN' } },
        ],
      },
    };

    render(<FinancialInsightsCard insights={insights} loading={false} />);

    const filas = screen.getAllByTestId('dashboard-insights-surplus-row');
    expect(filas).toHaveLength(2);
    expect(filas.at(0)?.textContent).toContain('Vacaciones');
    expect(filas.at(1)?.textContent).toContain('Fondo');
  });

  it('dice que no hay meta activa cuando el remanente no tiene a donde ir', () => {
    const insights: FinancialInsights = {
      ...vacio,
      surplusAllocation: {
        cycleId: 'ciclo-1',
        surplus: { amount: '2000.00', currency: 'MXN' },
        shares: [],
      },
    };

    render(<FinancialInsightsCard insights={insights} loading={false} />);

    expect(screen.getByText(/no tienes ninguna meta activa/i)).toBeInTheDocument();
  });

  it('lista las categorias con una racha sostenida', () => {
    const insights: FinancialInsights = {
      ...vacio,
      categoryGrowth: [
        {
          categoryName: 'Salud',
          firstAmount: { amount: '100.00', currency: 'MXN' },
          lastAmount: { amount: '300.00', currency: 'MXN' },
          cycles: 3,
        },
      ],
    };

    render(<FinancialInsightsCard insights={insights} loading={false} />);

    const fila = screen.getByTestId('dashboard-insights-growth-row');
    expect(fila.textContent).toContain('Salud');
    expect(fila.textContent).toContain('3');
  });
});
