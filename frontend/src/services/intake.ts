import type { IntakeResponse, TicketProcessingResult } from '@/types';
import { api, intakeApi } from './api';

export async function uploadTicketsCsv(file: File): Promise<IntakeResponse> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await intakeApi.post<IntakeResponse>('/api/v1/intake/tickets', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return data;
}

export async function uploadOfficesCsv(file: File): Promise<IntakeResponse> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post<IntakeResponse>('/api/evaluation/intake/offices', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return data;
}

export async function uploadManagersCsv(file: File): Promise<IntakeResponse> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post<IntakeResponse>('/api/evaluation/intake/managers', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return data;
}

/** Опрос результатов назначения по clientGuids (после загрузки CSV). */
export async function fetchIntakeResults(clientGuids: string[]): Promise<TicketProcessingResult[]> {
  if (clientGuids.length === 0) return [];
  const q = clientGuids.join(',');
  const { data } = await intakeApi.get<TicketProcessingResult[]>(`/api/v1/intake/results?clientGuids=${encodeURIComponent(q)}`);
  return Array.isArray(data) ? data : [];
}

export async function fetchRecentIntakeResults(limit = 50): Promise<TicketProcessingResult[]> {
  const safeLimit = Math.max(1, Math.min(limit, 500));
  const { data } = await intakeApi.get<TicketProcessingResult[]>(`/api/v1/intake/results/recent?limit=${safeLimit}`);
  return Array.isArray(data) ? data : [];
}

export interface IntakeGeocodeResponse {
  address: string;
  lat: number;
  lon: number;
  source_url?: string;
}

export async function geocodeIntakeAddress(address: string): Promise<IntakeGeocodeResponse | null> {
  const normalized = address.trim();
  if (!normalized) return null;
  try {
    const { data } = await intakeApi.get<IntakeGeocodeResponse>(
      `/api/v1/intake/geocode?address=${encodeURIComponent(normalized)}`
    );
    if (!data || typeof data.lat !== 'number' || typeof data.lon !== 'number') {
      return null;
    }
    return data;
  } catch {
    return null;
  }
}
