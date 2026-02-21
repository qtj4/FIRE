export type Segment = 'Mass' | 'VIP' | 'Priority' | string;
export type TicketType =
  | 'Жалоба'
  | 'Смена данных'
  | 'Консультация'
  | 'Претензия'
  | 'Неработоспособность приложения'
  | 'Мошеннические действия'
  | 'Спам'
  | string;
export type Sentiment = 'Позитивный' | 'Нейтральный' | 'Негативный' | string;
export type Language = 'RU' | 'KZ' | 'ENG' | string;

export interface Ticket {
  id: string;
  segment: Segment;
  description: string;
  type: TicketType;
  priority: number;
  city?: string;
  office?: string;
  language?: Language;
  sentiment?: Sentiment;
  summary?: string;
  assignedManager?: string;
  createdAt?: string;
}

export interface DashboardStats {
  totals: {
    tickets: number;
    avgPriority: number;
    vipShare: number;
    inRouting: number;
  };
  byCity: Array<{ city: string; count: number }>;
  byType: Array<{ type: string; count: number }>;
  byOffice: Array<{ office: string; count: number }>;
  bySentiment: Array<{ sentiment: string; count: number }>;
}
