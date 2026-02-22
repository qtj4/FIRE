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
  backendId?: number;
  rawTicketId?: number;
  clientId?: string;
  gender?: 'Муж' | 'Жен' | string;
  birthDate?: string;
  segment: Segment;
  description: string;
  type: TicketType;
  priority: number;
  attachments?: string[];
  country?: string;
  region?: string;
  city?: string;
  street?: string;
  house?: string;
  office?: string;
  language?: Language;
  sentiment?: Sentiment;
  summary?: string;
  latitude?: number;
  longitude?: number;
  assignedManager?: string;
  createdAt?: string;
}

export interface TicketMutationPayload {
  rawTicketId?: number;
  clientGuid?: string;
  type: string;
  priority: number;
  summary: string;
  language?: string;
  sentiment?: string;
  latitude?: number;
  longitude?: number;
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
  byLanguage: Array<{ language: string; count: number }>;
}

export interface ServiceHealth {
  status: string;
  timestamp: string;
  ticketsTotal: number;
  assignedTotal: number;
  unassignedTotal: number;
  highPriorityUnassigned: number;
}

export interface InsightsResponse {
  generatedAt: string;
  items: Array<{
    severity: 'low' | 'medium' | 'high' | string;
    title: string;
    detail: string;
  }>;
}

export interface ManagerProfile {
  id: string;
  fullName: string;
  role: string;
  office: string;
  department: string;
  status: 'online' | 'offline' | string;
  email: string;
  phone: string;
  shift: string;
  languages: string[];
  skills: string[];
  stats: {
    assignedToday: number;
    inProgress: number;
    slaBreaches: number;
    avgHandleTimeMin: number;
  };
}

export type IntakeDataset = 'offices' | 'managers' | 'tickets';

export interface IntakeResponse {
  status: string;
  message: string;
  processedCount: number;
  failedCount: number;
  results?: TicketProcessingResult[];
}

export interface TicketProcessingResult {
  clientGuid: string;
  rawTicketId?: number;
  enrichedTicketId?: number;
  status: string;
  message: string;
  assignedOfficeName?: string;
  assignedManagerName?: string;
  priority?: number;
  language?: string;
  type?: string;
}

export interface CsvUploadRecord extends IntakeResponse {
  id: string;
  dataset: IntakeDataset;
  endpoint: string;
  fileName: string;
  uploadedAt: string;
  durationMs: number;
}
