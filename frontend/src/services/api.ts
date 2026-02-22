import axios from 'axios';

export const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function resolveBaseUrl(value: string | undefined, fallback: string): string {
  const url = (value ?? '').trim();
  if (!url) {
    return fallback;
  }

  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) {
    return url.replace(/\/+$/, '');
  }

  return fallback;
}

export const api = axios.create({
  baseURL: resolveBaseUrl(import.meta.env.VITE_API_BASE_URL, ''),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

/** API for ticket-intake-service (CSV upload, intake pipeline) */
export const intakeApi = axios.create({
  baseURL: resolveBaseUrl(import.meta.env.VITE_INTAKE_API_BASE_URL, ''),
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
});
