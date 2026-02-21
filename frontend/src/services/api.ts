import axios from 'axios';

export const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://2.133.130.153',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});
