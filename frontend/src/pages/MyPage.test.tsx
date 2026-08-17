import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import MyPage from './MyPage';
import { useAuth } from '../contexts/AuthContext';
import type { UserRole } from '../types/auth';

vi.mock('axios');
vi.mock('../contexts/AuthContext', async () => {
  const actual = await vi.importActual('../contexts/AuthContext');
  return { ...actual, useAuth: vi.fn() };
});

const mockedAxios = axios as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };
const mockedUseAuth = vi.mocked(useAuth);

const givenUser = (role: UserRole) => {
  mockedUseAuth.mockReturnValue({
    user: { id: 1, loginId: 'tester', nickname: '테스터', email: 'tester@fun-game.club', role },
    isAuthenticated: true,
    isInitialLoading: false,
    updateNickname: vi.fn(),
  } as unknown as ReturnType<typeof useAuth>);
};

const renderMyPage = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/mypage/*" element={<MyPage />} />
      </Routes>
    </MemoryRouter>,
  );

describe('MyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
    givenUser('USER');
  });

  it('기본 경로에서는 내 정보를 보여준다', () => {
    renderMyPage('/mypage');

    expect(screen.getByText('닉네임 설정')).toBeInTheDocument();
  });

  it('내 문의 경로에서는 접수 목록을 보여준다', async () => {
    renderMyPage('/mypage/reports');

    expect(await screen.findByText(/접수한 문의가 없습니다/)).toBeInTheDocument();
  });

  it('일반 사용자가 문의 관리로 들어오면 내 정보로 되돌린다', () => {
    renderMyPage('/mypage/inquiries');

    expect(screen.getByText('닉네임 설정')).toBeInTheDocument();
  });

  it('관리자는 문의 관리를 열 수 있다', async () => {
    givenUser('ADMIN');

    renderMyPage('/mypage/inquiries');

    expect(await screen.findByText(/처리할 문의가 없습니다/)).toBeInTheDocument();
  });
});
