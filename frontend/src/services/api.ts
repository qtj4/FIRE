import axios from 'axios';

export const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function ensureHttpUrl(value: string | undefined, fallback: string): string {
  const url = (value || fallback).trim();
  if (!url || (!url.startsWith('http://') && !url.startsWith('https://'))) {
    return fallback;
  }
  return url;
}

export const api = axios.create({
  baseURL: ensureHttpUrl(import.meta.env.VITE_API_BASE_URL, 'http://localhost:8092'),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

/** API for ticket-intake-service (CSV upload, intake pipeline) */
export const intakeApi = axios.create({
  baseURL: ensureHttpUrl(import.meta.env.VITE_INTAKE_API_BASE_URL, 'http://localhost:8082'),
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
});
