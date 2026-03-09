import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // sockjs-client 등 일부 라이브러리는 Node의 global 객체를 기대하므로 브라우저에서 global을 globalThis로 매핑
  define: {
    global: 'globalThis',
  },
  server: {
    allowedHosts: ['localhost', 'songquiz.duckdns.org'],
  },
});
