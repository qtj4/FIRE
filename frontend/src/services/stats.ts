import type { DashboardStats, InsightsResponse, ServiceHealth } from '@/types';
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

export async function fetchServiceHealth(): Promise<ServiceHealth> {
  if (useMocks) {
    await sleep(200);
    return {
      status: 'UP',
      timestamp: new Date().toISOString(),
      ticketsTotal: mockStats.totals.tickets,
      assignedTotal: mockStats.totals.tickets - mockStats.totals.inRouting,
      unassignedTotal: mockStats.totals.inRouting,
      highPriorityUnassigned: 3
    };
  }

  const { data } = await api.get<ServiceHealth>('/api/evaluation/health');
  return data;
}

export async function fetchInsights(): Promise<InsightsResponse> {
  if (useMocks) {
    await sleep(200);
    return {
      generatedAt: new Date().toISOString(),
      items: [
        { severity: 'medium', title: 'Очередь маршрутизации', detail: 'Есть накопление в очереди. Проверьте баланс офисов.' },
        { severity: 'high', title: 'Срочные обращения', detail: 'Есть нераспределенные high-priority кейсы.' },
        { severity: 'low', title: 'Языковая нагрузка', detail: 'Распределение языков в норме.' }
      ]
    };
  }

  const { data } = await api.get<InsightsResponse>('/api/evaluation/insights');
  return data;
}
