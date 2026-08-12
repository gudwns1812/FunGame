import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ResetPasswordPage from './ResetPasswordPage';
import { useAuth } from '../contexts/AuthContext';

vi.mock('../contexts/AuthContext', async () => {
  const actual = await vi.importActual('../contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn(),
  };
});

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ResetPasswordPage', () => {
  const mockResetPassword = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, 'alert').mockImplementation(() => {});
    (useAuth as any).mockReturnValue({
      resetPassword: mockResetPassword,
      isAuthenticated: false,
      isInitialLoading: false,
      user: null,
    });
  });

  const setup = (search: string) =>
    render(
      <MemoryRouter initialEntries={[`/reset-password${search}`]}>
        <ResetPasswordPage />
      </MemoryRouter>,
    );

  it('토큰이 있으면 새 비밀번호를 제출해 재설정한다', async () => {
    mockResetPassword.mockResolvedValue(undefined);
    setup('?token=raw-token');

    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 입력'), { target: { value: 'new1234' } });
    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 다시 입력'), { target: { value: 'new1234' } });
    fireEvent.click(screen.getByRole('button', { name: /비밀번호 변경하기/ }));

    await waitFor(() => {
      expect(mockResetPassword).toHaveBeenCalledWith('raw-token', 'new1234');
    });
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('두 비밀번호가 다르면 요청하지 않는다', async () => {
    setup('?token=raw-token');

    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 입력'), { target: { value: 'new1234' } });
    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 다시 입력'), { target: { value: 'other1234' } });
    fireEvent.click(screen.getByRole('button', { name: /비밀번호 변경하기/ }));

    expect(await screen.findByText(/비밀번호가 서로 일치하지 않습니다./)).toBeInTheDocument();
    expect(mockResetPassword).not.toHaveBeenCalled();
  });

  it('토큰이 없으면 잘못된 접근으로 안내한다', () => {
    setup('');

    expect(screen.getByText(/잘못된 접근입니다./)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /비밀번호 변경하기/ })).not.toBeInTheDocument();
  });

  it('서버가 만료된 토큰을 알려주면 그 메시지를 보여준다', async () => {
    mockResetPassword.mockRejectedValue(new Error('링크가 만료되었거나 이미 사용되었습니다. 다시 요청해주세요.'));
    setup('?token=expired-token');

    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 입력'), { target: { value: 'new1234' } });
    fireEvent.change(screen.getByPlaceholderText('새 비밀번호 다시 입력'), { target: { value: 'new1234' } });
    fireEvent.click(screen.getByRole('button', { name: /비밀번호 변경하기/ }));

    expect(await screen.findByText(/링크가 만료되었거나 이미 사용되었습니다./)).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
