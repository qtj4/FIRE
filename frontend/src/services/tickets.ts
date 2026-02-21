import type { Ticket } from '@/types';
import { mockTickets } from '@/mocks/tickets';
import { api, useMocks } from './api';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function fetchTickets(): Promise<Ticket[]> {
  if (useMocks) {
    await sleep(300);
    return mockTickets;
  }

  try {
    const { data } = await api.get<Ticket[]>('/api/evaluation/tickets');
    return data;
  } catch (error) {
    console.warn('API недоступен, используется mock-список обращений.', error);
    await sleep(300);
    return mockTickets;
  }
}
