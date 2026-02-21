import { api } from './api';
import type { IntakeDataset, IntakeResponse } from '@/types';

const intakeConfig: Record<IntakeDataset, { endpoint: string; title: string; columns: string[]; sampleRow: string[] }> = {
  offices: {
    endpoint: '/api/intake/evaluation/offices',
    title: 'Офисы',
    columns: ['Офис', 'Адрес', 'Широта', 'Долгота'],
    sampleRow: ['Алматы Центр', 'Кунаева 12', '43.238949', '76.889709']
  },
  managers: {
    endpoint: '/api/intake/evaluation/managers',
    title: 'Менеджеры',
    columns: ['ФИО', 'Должность', 'Офис', 'Навыки', 'Количество обращений в работе'],
    sampleRow: ['Аида Нурланова', 'Senior Routing Manager', 'Алматы Центр', 'VIP Routing;Fraud Escalation;KZ', '4']
  },
  tickets: {
    endpoint: '/api/intake/ticket-intake/tickets',
    title: 'Обращения',
    columns: [
      'GUID клиента',
      'Пол клиента',
      'Дата рождения',
      'Описание',
      'Вложения',
      'Сегмент клиента',
      'Страна',
      'Область',
      'Населённый пункт',
      'Улица',
      'Дом'
    ],
    sampleRow: [
      '507f1f77-bcfa-4d7a-9f21-7a7f2e6f183a',
      'Жен',
      '1995-03-10 12:30',
      'Не работает вход в приложение',
      'screenshot.png',
      'VIP',
      'Казахстан',
      'Алматинская область',
      'Алматы',
      'Абая',
      '22'
    ]
  }
};

export function getIntakeConfig(dataset: IntakeDataset) {
  return intakeConfig[dataset];
}

export function getAllIntakeConfigs() {
  return intakeConfig;
}

export async function uploadCsv(dataset: IntakeDataset, file: File): Promise<IntakeResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const { data } = await api.post<IntakeResponse>(intakeConfig[dataset].endpoint, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });

  return data;
}
