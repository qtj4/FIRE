import axios from 'axios';

export const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});
