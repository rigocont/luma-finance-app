import { api } from './client';
import type { SystemInfo } from './types';

export const systemKeys = {
  info: ['system', 'info'] as const,
};

export async function fetchSystemInfo(): Promise<SystemInfo> {
  const { data } = await api.get<SystemInfo>('/system/info');
  return data;
}
