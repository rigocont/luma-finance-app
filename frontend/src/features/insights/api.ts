import { api } from '@/lib/api/client';

import type { FinancialInsights } from './types';

export const insightsKeys = {
  current: ['insights', 'current'] as const,
};

export async function fetchInsights(): Promise<FinancialInsights> {
  const { data } = await api.get<FinancialInsights>('/insights');
  return data;
}
