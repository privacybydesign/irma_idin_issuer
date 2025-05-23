import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  return {
    plugins: [
      react(),
    ],
    server: mode === 'development' ? {
      port: 3000,
      host: "0.0.0.0",
      proxy: {
        '/irma_idin_server': {
          target: 'http://localhost:8080/',
          changeOrigin: true,
        },
        '/conf.json': {
          target: 'http://localhost:8080/',
          changeOrigin: true,
        }
      }
    } : undefined,
    build: {
      outDir: "build",
    },
  }
});
