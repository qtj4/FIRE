import type { ManagerProfile } from '@/types';

export const mockManagerProfile: ManagerProfile = {
  id: 'mgr-1',
  fullName: 'Иванов Иван',
  role: 'Старший менеджер',
  office: 'Алматы Центр',
  department: 'Обращения',
  status: 'online',
  email: 'ivanov@example.kz',
  phone: '+7 777 123 4567',
  shift: '09:00 – 18:00',
  languages: ['RU', 'KZ'],
  skills: ['Консультация', 'Жалобы', 'Претензии'],
  stats: {
    assignedToday: 12,
    inProgress: 5,
    slaBreaches: 0,
    avgHandleTimeMin: 18
  }
};
