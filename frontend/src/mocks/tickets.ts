import type { Ticket } from '@/types';

export const mockTickets: Ticket[] = [
  {
    id: 'TCK-001',
    segment: 'VIP',
    description: 'Не проходит перевод между счетами, ошибка 502 после подтверждения.',
    type: 'Неработоспособность приложения',
    priority: 9,
    city: 'Алматы',
    office: 'Алматы Центр',
    language: 'RU',
    sentiment: 'Негативный',
    summary: 'Клиент не может завершить перевод. Рекомендуется проверить статус транзакции и логи API.',
    assignedManager: 'Кожахметова А.',
    createdAt: '2026-02-21T09:12:00Z'
  },
  {
    id: 'TCK-002',
    segment: 'Mass',
    description: 'Как изменить номер телефона в профиле?',
    type: 'Смена данных',
    priority: 6,
    city: 'Астана',
    office: 'Астана БЦ',
    language: 'RU',
    sentiment: 'Нейтральный',
    summary: 'Запрос на смену номера. Нужна верификация личности клиента.',
    assignedManager: 'Омаров Б.',
    createdAt: '2026-02-21T09:26:00Z'
  },
  {
    id: 'TCK-003',
    segment: 'Priority',
    description: 'Card was charged twice, please refund the duplicate payment.',
    type: 'Претензия',
    priority: 8,
    city: 'Shymkent',
    office: 'Шымкент',
    language: 'ENG',
    sentiment: 'Негативный',
    summary: 'Двойное списание. Проверьте платежные подтверждения и запустите возврат.',
    assignedManager: 'Williams J.',
    createdAt: '2026-02-21T09:42:00Z'
  },
  {
    id: 'TCK-004',
    segment: 'Mass',
    description: 'Можно ли открыть счет для ИП дистанционно?',
    type: 'Консультация',
    priority: 4,
    city: 'Актобе',
    office: 'Актобе',
    language: 'RU',
    sentiment: 'Позитивный',
    summary: 'Запрос на консультацию по открытию счета. Нужны условия и список документов.',
    assignedManager: 'Садыкова Л.',
    createdAt: '2026-02-21T10:05:00Z'
  }
];
