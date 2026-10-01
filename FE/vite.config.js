import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { cpSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectRoot = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'copy-legacy-auth-assets',
      closeBundle() {
        for (const directory of ['css', 'js']) {
          cpSync(resolve(projectRoot, directory), resolve(projectRoot, 'dist', directory), { recursive: true });
        }
      }
    }
  ],
  build: {
    rollupOptions: {
      input: {
        main: resolve(projectRoot, 'index.html'),
        login: resolve(projectRoot, 'login.html'),
        changePassword: resolve(projectRoot, 'change-password.html')
      }
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:5000',
        changeOrigin: true
      }
    }
  }
});
