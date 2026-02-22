import axios from 'axios';

export const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function resolveTimeoutMs(value: string | undefined, fallback: number): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback;
  return Math.floor(parsed);
}

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
  timeout: resolveTimeoutMs(import.meta.env.VITE_API_TIMEOUT_MS, 20000),
  headers: {
    'Content-Type': 'application/json'
  }
});

/** API for ticket-intake-service (CSV upload, intake pipeline) */
export const intakeApi = axios.create({
  baseURL: resolveBaseUrl(import.meta.env.VITE_INTAKE_API_BASE_URL, ''),
  timeout: resolveTimeoutMs(import.meta.env.VITE_INTAKE_API_TIMEOUT_MS, 300000),
  headers: {
    'Content-Type': 'application/json'
  }
});

/** API for AI assistant backend (Flask/OpenAI proxy) */
export const assistantApi = axios.create({
  baseURL: resolveBaseUrl(import.meta.env.VITE_ASSISTANT_API_BASE_URL, 'http://localhost:5000'),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

