import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ForgotPasswordPage from './ForgotPasswordPage';
import { useAuth } from '../contexts/AuthContext';

vi.mock('../contexts/AuthContext', async () => {
  const actual = await vi.importActual('../contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn(),
  };
});

describe('ForgotPasswordPage', () => {
  const mockRequestPasswordReset = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (useAuth as any).mockReturnValue({
      requestPasswordReset: mockRequestPasswordReset,
      isAuthenticated: false,
      isInitialLoading: false,
      user: null,
    });
  });

  const setup = () =>
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>,
    );

  const submit = () => {
    fireEvent.change(screen.getByPlaceholderText('아이디 입력'), { target: { value: 'tester' } });
    fireEvent.change(screen.getByPlaceholderText(/가입할 때 등록한 이메일/), {
      target: { value: 'tester@fun-game.club' },
    });
    fireEvent.click(screen.getByRole('button', { name: /재설정 링크 받기/ }));
  };

  it('아이디와 이메일을 제출하면 재설정을 요청하고 안내 문구로 바뀐다', async () => {
    mockRequestPasswordReset.mockResolvedValue(undefined);
    setup();

    submit();

    await waitFor(() => {
      expect(mockRequestPasswordReset).toHaveBeenCalledWith('tester', 'tester@fun-game.club');
    });
    expect(await screen.findByText(/일치하는 계정이 있다면/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /재설정 링크 받기/ })).not.toBeInTheDocument();
  });

  it('아이디만 입력하면 요청하지 않고 안내를 보여준다', async () => {
    setup();

    fireEvent.change(screen.getByPlaceholderText('아이디 입력'), { target: { value: 'tester' } });
    fireEvent.click(screen.getByRole('button', { name: /재설정 링크 받기/ }));

    expect(await screen.findByText(/아이디와 이메일을 모두 입력해주세요./)).toBeInTheDocument();
    expect(mockRequestPasswordReset).not.toHaveBeenCalled();
  });
});
