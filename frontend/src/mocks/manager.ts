import type { ManagerProfile } from '@/types';

export const mockManagerProfile: ManagerProfile = {
  id: 'MGR-0017',
  fullName: 'Аида Нурланова',
  role: 'Senior Routing Manager',
  office: 'Алматы Центр',
  department: 'Customer Operations',
  status: 'online',
  email: 'a.nurlanova@fire.local',
  phone: '+7 701 555 12 44',
  shift: '08:00 - 17:00',
  languages: ['RU', 'KZ', 'ENG'],
  skills: ['VIP Routing', 'Fraud Escalation', 'Data Change'],
  stats: {
    assignedToday: 28,
    inProgress: 6,
    slaBreaches: 1,
    avgHandleTimeMin: 14
  }
};
