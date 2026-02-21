import type { IntakeResponse } from '@/types';
import { intakeApi } from './api';

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
