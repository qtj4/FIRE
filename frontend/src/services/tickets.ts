import type { Ticket, TicketMutationPayload } from '@/types';
import { mockTickets } from '@/mocks/tickets';
import { api, useMocks } from './api';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

interface EvaluationTicketApi {
  id: number;
  rawTicketId?: number;
  clientGuid?: string;
  type?: string;
  priority?: number;
  summary?: string;
  language?: string;
  sentiment?: string;
  latitude?: number;
  longitude?: number;
  assignedOfficeName?: string;
  assignedManagerName?: string;
  enrichedAt?: string;
}

function deriveSegment(type: string | undefined, priority: number | undefined): string {
  const normalizedType = (type ?? '').toLowerCase();
  if (normalizedType.includes('vip') || normalizedType.includes('претенз') || (priority ?? 0) >= 8) {
    return 'VIP';
  }
  if ((priority ?? 0) >= 6) {
    return 'Priority';
  }
  return 'Mass';
}

function toTicket(dto: EvaluationTicketApi): Ticket {
  const safePriority = dto.priority ?? 0;
  return {
    id: String(dto.id),
    backendId: dto.id,
    rawTicketId: dto.rawTicketId,
    clientId: dto.clientGuid,
    segment: deriveSegment(dto.type, dto.priority),
    description: dto.summary ?? 'Описание отсутствует',
    type: dto.type ?? 'Не указан',
    priority: safePriority,
    office: dto.assignedOfficeName,
    language: dto.language,
    sentiment: dto.sentiment,
    summary: dto.summary,
    latitude: dto.latitude,
    longitude: dto.longitude,
    assignedManager: dto.assignedManagerName,
    createdAt: dto.enrichedAt
  };
}

export async function fetchTickets(): Promise<Ticket[]> {
  if (useMocks) {
    await sleep(300);
    return mockTickets;
  }

  try {
    const { data } = await api.get<EvaluationTicketApi[]>('/api/evaluation/tickets');
    return Array.isArray(data) ? data.map(toTicket) : [];
  } catch (error) {
    console.warn('API недоступен, используется mock-список обращений.', error);
    await sleep(300);
    return mockTickets;
  }
}

export async function createTicket(payload: TicketMutationPayload): Promise<Ticket> {
  const { data } = await api.post<EvaluationTicketApi>('/api/evaluation/tickets', payload);
  return toTicket(data);
}

export async function updateTicket(id: number, payload: TicketMutationPayload): Promise<Ticket> {
  const { data } = await api.put<EvaluationTicketApi>(`/api/evaluation/tickets/${id}`, payload);
  return toTicket(data);
}

export async function deleteTicket(id: number): Promise<void> {
  await api.delete(`/api/evaluation/tickets/${id}`);
}

export async function assignTicket(id: number): Promise<void> {
  await api.post(`/api/evaluation/tickets/${id}/assign`);
}
