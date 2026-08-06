import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// 테스트 전용 설정. 빌드 설정(vite.config.ts)은 건드리지 않는다.
export default defineConfig({
  plugins: [react()],
  // sockjs-client 등 일부 라이브러리는 Node의 global 객체를 기대한다
  define: {
    global: 'globalThis',
  },
  test: {
    environment: 'jsdom',
    // globals: true 여야 @testing-library/react의 자동 cleanup이 등록된다
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
});
