import type { AssistantQueryRequest, AssistantQueryResponse } from '@/types';
import { assistantApi } from './api';

export async function queryDashboardAssistant(payload: AssistantQueryRequest): Promise<AssistantQueryResponse> {
  const { data } = await assistantApi.post<AssistantQueryResponse>('/api/assistant/dashboard', payload);
  return data;
}
