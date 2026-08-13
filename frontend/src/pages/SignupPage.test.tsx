import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import SignupPage from './SignupPage';
import { useAuth } from '../contexts/AuthContext';

// useAuth 훅 모킹
vi.mock('../contexts/AuthContext', async () => {
  const actual = await vi.importActual('../contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn(),
  };
});

// useNavigate 모킹
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('SignupPage 닉네임 중복 확인 기능', () => {
  const mockCheckNickname = vi.fn();
  const mockCheckId = vi.fn();
  const mockSignup = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (useAuth as any).mockReturnValue({
      signup: mockSignup,
      checkId: mockCheckId,
      checkNickname: mockCheckNickname,
      isAuthenticated: false,
      isInitialLoading: false,
      user: null,
    });
  });

  const setup = () => {
    return render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );
  };

  it('닉네임 입력 후 중복 확인 버튼을 클릭하면 로딩 상태가 표시되고 API가 호출되어야 한다', async () => {
    // Given
    mockCheckNickname.mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve(false), 100)));
    setup();
    const nicknameInput = screen.getByPlaceholderText(/닉네임 입력/);
    // '중복 확인' 버튼은 닉네임용/아이디용 두 개이므로 첫 번째(닉네임)를 사용한다
    const [checkButton] = screen.getAllByRole('button', { name: /중복 확인/ });

    // When
    fireEvent.change(nicknameInput, { target: { value: 'testUser' } });
    fireEvent.click(checkButton);

    // Then
    expect(checkButton).toHaveTextContent('확인 중...');
    expect(mockCheckNickname).toHaveBeenCalledWith('testUser');

    await waitFor(() => {
      expect(checkButton).not.toHaveTextContent('확인 중...');
    });
  });

  it('중복된 닉네임인 경우 에러 메시지가 출력되고 가입 버튼이 비활성화 상태여야 한다', async () => {
    // Given
    mockCheckNickname.mockResolvedValue(true); // true means duplicated
    setup();
    const nicknameInput = screen.getByPlaceholderText(/닉네임 입력/);
    const [checkButton] = screen.getAllByRole('button', { name: /중복 확인/ });
    const signupButton = screen.getByRole('button', { name: /계정 생성하기/ });

    // When
    fireEvent.change(nicknameInput, { target: { value: 'duplicateNick' } });
    fireEvent.click(checkButton);

    // Then
    await waitFor(() => {
      expect(screen.getByText('이미 사용 중인 닉네임입니다.')).toBeInTheDocument();
    });
    expect(signupButton).toBeDisabled();
  });

  it('사용 가능한 닉네임인 경우 성공 메시지가 출력되고, 아이디 중복 확인도 완료되었다면 가입 버튼이 활성화되어야 한다', async () => {
    // Given
    mockCheckNickname.mockResolvedValue(false); // false means not duplicated (available)
    mockCheckId.mockResolvedValue(false); // available
    setup();
    
    const nicknameInput = screen.getByPlaceholderText(/닉네임 입력/);
    const idInput = screen.getByPlaceholderText(/아이디 입력/);
    const checkButtons = screen.getAllByRole('button', { name: /중복 확인/ });
    const nicknameCheckButton = checkButtons[0];
    const idCheckButton = checkButtons[1];
    const signupButton = screen.getByRole('button', { name: /계정 생성하기/ });

    // When & Then - 닉네임 중복 확인
    fireEvent.change(nicknameInput, { target: { value: 'newNick' } });
    fireEvent.click(nicknameCheckButton);
    
    await waitFor(() => {
      expect(screen.getByText('사용 가능한 닉네임입니다.')).toBeInTheDocument();
    });
    expect(signupButton).toBeDisabled(); // ID check not done yet

    // When & Then - 아이디 중복 확인
    fireEvent.change(idInput, { target: { value: 'newId' } });
    fireEvent.click(idCheckButton);
    
    await waitFor(() => {
      expect(screen.getByText('사용 가능한 아이디입니다.')).toBeInTheDocument();
    });

    // 최종적으로 가입 버튼 활성화 확인 (비밀번호 입력 여부와 무관하게 버튼 활성화 여부만 체크)
    expect(signupButton).not.toBeDisabled();
  });

  it('닉네임 중복 확인 API 호출 중 에러가 발생하면 에러 메시지를 출력해야 한다', async () => {
    // Given
    mockCheckNickname.mockRejectedValue(new Error('API Error'));
    setup();
    const nicknameInput = screen.getByPlaceholderText(/닉네임 입력/);
    const [checkButton] = screen.getAllByRole('button', { name: /중복 확인/ });

    // When
    fireEvent.change(nicknameInput, { target: { value: 'errorUser' } });
    fireEvent.click(checkButton);

    // Then
    // 컴포넌트는 예외의 message를 그대로 노출하고, 없을 때만 기본 문구로 대체한다
    await waitFor(() => {
      expect(screen.getByText('API Error')).toBeInTheDocument();
    });
  });

  it('이메일까지 채워 제출하면 이메일을 함께 회원가입 API 로 넘긴다', async () => {
    // Given
    mockCheckNickname.mockResolvedValue(false);
    mockCheckId.mockResolvedValue(false);
    mockSignup.mockResolvedValue(undefined);
    vi.spyOn(window, 'alert').mockImplementation(() => {});
    setup();

    const checkButtons = screen.getAllByRole('button', { name: /중복 확인/ });

    // When
    fireEvent.change(screen.getByPlaceholderText(/닉네임 입력/), { target: { value: 'newNick' } });
    fireEvent.click(checkButtons[0]);
    await waitFor(() => expect(screen.getByText('사용 가능한 닉네임입니다.')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText(/아이디 입력/), { target: { value: 'newId' } });
    fireEvent.click(checkButtons[1]);
    await waitFor(() => expect(screen.getByText('사용 가능한 아이디입니다.')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('이메일 입력'), {
      target: { value: 'tester@fun-game.club' },
    });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 입력'), { target: { value: 'pw1234' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 다시 입력'), { target: { value: 'pw1234' } });
    fireEvent.click(screen.getByRole('button', { name: /계정 생성하기/ }));

    // Then
    await waitFor(() => {
      expect(mockSignup).toHaveBeenCalledWith('newId', 'pw1234', 'newNick', 'tester@fun-game.club');
    });
  });

  it('브라우저 검증은 통과하지만 도메인에 점이 없는 이메일은 회원가입을 요청하지 않는다', async () => {
    // Given
    mockCheckNickname.mockResolvedValue(false);
    mockCheckId.mockResolvedValue(false);
    setup();

    const checkButtons = screen.getAllByRole('button', { name: /중복 확인/ });

    // When
    fireEvent.change(screen.getByPlaceholderText(/닉네임 입력/), { target: { value: 'newNick' } });
    fireEvent.click(checkButtons[0]);
    await waitFor(() => expect(screen.getByText('사용 가능한 닉네임입니다.')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText(/아이디 입력/), { target: { value: 'newId' } });
    fireEvent.click(checkButtons[1]);
    await waitFor(() => expect(screen.getByText('사용 가능한 아이디입니다.')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('이메일 입력'), {
      target: { value: 'tester@fun-game' },
    });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 입력'), { target: { value: 'pw1234' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 다시 입력'), { target: { value: 'pw1234' } });
    fireEvent.click(screen.getByRole('button', { name: /계정 생성하기/ }));

    // Then
    expect(await screen.findByText('이메일 형식이 올바르지 않습니다.')).toBeInTheDocument();
    expect(mockSignup).not.toHaveBeenCalled();
  });
});
