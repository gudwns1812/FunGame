import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import MaintenanceRoutes from './MaintenanceRoutes';

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <MaintenanceRoutes />
    </MemoryRouter>,
  );

describe('점검 모드 라우팅', () => {
  it('서비스 경로로 들어오면 점검 화면을 보여준다', () => {
    renderAt('/rooms');

    expect(screen.getByRole('heading', { name: '서버 점검 중' })).toBeTruthy();
  });

  it('처음 보는 경로로 들어와도 점검 화면을 보여준다', () => {
    renderAt('/waiting');

    expect(screen.getByRole('heading', { name: '서버 점검 중' })).toBeTruthy();
  });

  it('약관과 개인정보처리방침은 점검 중에도 그대로 열린다', () => {
    renderAt('/terms');
    expect(screen.getByRole('heading', { name: '이용약관' })).toBeTruthy();

    renderAt('/privacy');
    expect(screen.getByRole('heading', { name: '개인정보처리방침' })).toBeTruthy();
  });

  it('게임 방법도 점검 중에 열린다', () => {
    renderAt('/how-to-play');

    expect(screen.queryByRole('heading', { name: '서버 점검 중' })).toBeNull();
  });

  it('점검 화면은 검색 노출을 막는다', () => {
    renderAt('/rooms');

    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex');
  });
});
