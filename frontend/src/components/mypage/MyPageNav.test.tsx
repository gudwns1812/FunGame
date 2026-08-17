import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import MyPageNav from './MyPageNav';

const renderNav = (isAdmin: boolean) =>
  render(
    <MemoryRouter initialEntries={['/mypage']}>
      <MyPageNav isAdmin={isAdmin} />
    </MemoryRouter>,
  );

describe('MyPageNav', () => {
  it('내 정보와 내 문의로 갈 수 있다', () => {
    renderNav(false);

    expect(screen.getByRole('link', { name: '내 정보' })).toHaveAttribute('href', '/mypage');
    expect(screen.getByRole('link', { name: '내 문의' })).toHaveAttribute('href', '/mypage/reports');
  });

  it('일반 사용자에게는 문의 관리를 보여주지 않는다', () => {
    renderNav(false);

    expect(screen.queryByRole('link', { name: '문의 관리' })).not.toBeInTheDocument();
  });

  it('관리자에게는 문의 관리를 보여준다', () => {
    renderNav(true);

    expect(screen.getByRole('link', { name: '문의 관리' })).toHaveAttribute('href', '/mypage/inquiries');
  });
});
