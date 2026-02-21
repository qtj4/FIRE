import type { DashboardStats } from '@/types';
import { mockStats } from '@/mocks/stats';
import { api, useMocks } from './api';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function fetchDashboardStats(): Promise<DashboardStats> {
  if (useMocks) {
    await sleep(300);
    return mockStats;
  }

  try {
    const { data } = await api.get<DashboardStats>('/api/evaluation/stats');
    return data;
  } catch (error) {
    console.warn('API недоступен, используется mock-аналитика.', error);
    await sleep(300);
    return mockStats;
  }
}
