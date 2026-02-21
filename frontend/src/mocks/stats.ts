import type { DashboardStats } from '@/types';

export const mockStats: DashboardStats = {
  totals: {
    tickets: 128,
    avgPriority: 6.4,
    vipShare: 0.23,
    inRouting: 18
  },
  byCity: [
    { city: 'Алматы', count: 48 },
    { city: 'Астана', count: 36 },
    { city: 'Шымкент', count: 19 },
    { city: 'Актобе', count: 12 },
    { city: 'Караганда', count: 13 }
  ],
  byType: [
    { type: 'Неработоспособность приложения', count: 31 },
    { type: 'Консультация', count: 29 },
    { type: 'Смена данных', count: 22 },
    { type: 'Жалоба', count: 18 },
    { type: 'Претензия', count: 16 },
    { type: 'Мошеннические действия', count: 12 }
  ],
  byOffice: [
    { office: 'Алматы Центр', count: 44 },
    { office: 'Астана БЦ', count: 33 },
    { office: 'Шымкент', count: 19 },
    { office: 'Актобе', count: 12 },
    { office: 'Караганда', count: 20 }
  ],
  bySentiment: [
    { sentiment: 'Негативный', count: 41 },
    { sentiment: 'Нейтральный', count: 63 },
    { sentiment: 'Позитивный', count: 24 }
  ],
  byLanguage: [
    { language: 'RU', count: 90 },
    { language: 'KZ', count: 24 },
    { language: 'ENG', count: 14 }
  ]
};
