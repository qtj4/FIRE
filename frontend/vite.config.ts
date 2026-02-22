import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api/v1/intake': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/evaluation': {
        target: 'http://localhost:8092',
        changeOrigin: true
      }
    }
  }
});

