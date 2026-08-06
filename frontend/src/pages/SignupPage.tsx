import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const SignupPage: React.FC = () => {
  const [nickname, setNickname] = useState('');
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // 중복 확인 상태 (null: 확인 안함, true: 사용 가능, false: 중복됨)
  const [isIdAvailable, setIsIdAvailable] = useState<boolean | null>(null);
  const [isCheckingId, setIsCheckingId] = useState(false);

  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isCheckingNickname, setIsCheckingNickname] = useState(false);

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { signup, checkId, checkNickname } = useAuth();
  const navigate = useNavigate();

  // 아이디가 변경되면 중복 확인 상태 초기화
  useEffect(() => {
    setIsIdAvailable(null);
  }, [loginId]);

  // 닉네임이 변경되면 중복 확인 상태 초기화
  useEffect(() => {
    setIsNicknameAvailable(null);
  }, [nickname]);

  const handleCheckId = async () => {
    if (!loginId.trim()) {
      setError('확인할 아이디를 입력해주세요.');
      return;
    }

    setIsCheckingId(true);
    setError('');

    try {
      const isDuplicated = await checkId(loginId);
      if (isDuplicated) {
        setIsIdAvailable(false);
      } else {
        setIsIdAvailable(true);
      }
    } catch (err: any) {
      setError('아이디 중복 확인 중 오류가 발생했습니다.');
    } finally {
      setIsCheckingId(false);
    }
  };

  const handleCheckNickname = async () => {
    if (!nickname.trim()) {
      setError('확인할 닉네임을 입력해주세요.');
      return;
    }

    setIsCheckingNickname(true);
    setError('');

    try {
      const isDuplicated = await checkNickname(nickname);
      if (isDuplicated) {
        setIsNicknameAvailable(false);
      } else {
        setIsNicknameAvailable(true);
      }
    } catch (err: any) {
      console.error('Nickname check error:', err);
      setError(err.message || '닉네임 중복 확인 중 서버 에러가 발생했습니다.');
    } finally {
      setIsCheckingNickname(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!nickname || !loginId || !password || !confirmPassword) {
      setError('모든 필드를 입력해주세요.');
      return;
    }

    if (isIdAvailable !== true) {
      setError('아이디 중복 확인을 먼저 완료해주세요.');
      return;
    }

    if (isNicknameAvailable !== true) {
      setError('닉네임 중복 확인을 먼저 완료해주세요.');
      return;
    }

    if (password !== confirmPassword) {
      setError('비밀번호가 서로 일치하지 않습니다.');
      return;
    }

    if (password.length < 4) {
      setError('비밀번호는 최소 4자 이상이어야 합니다.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await signup(loginId, password, nickname);
      window.alert('회원가입이 완료되었습니다. 로그인해주세요.');
      navigate('/login');
    } catch (err: any) {
      setError(err.message || '회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  /** 중복 확인 결과에 따른 입력창 테두리 */
  const fieldTone = (state: boolean | null) =>
    state === true ? 'border-grass' : state === false ? 'border-cherry' : '';

  return (
    <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center p-5">
      <div className="w-full max-w-sm animate-pop py-6">
        <h1 className="px-title text-2xl text-center mb-4">회원가입</h1>

        <form onSubmit={handleSubmit} className="px-card p-5 space-y-3.5">
          <div>
            <label className="px-label block mb-1.5">닉네임</label>
            <div className="flex gap-2">
              <input
                type="text"
                className={`px-input flex-1 ${fieldTone(isNicknameAvailable)}`}
                placeholder="닉네임 입력 (최대 16자)"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                maxLength={16}
              />
              <button
                type="button"
                onClick={handleCheckNickname}
                disabled={isCheckingNickname || !nickname.trim() || isNicknameAvailable === true}
                className="px-btn px-btn-sm px-btn-paper shrink-0">
                {isCheckingNickname ? '확인 중...' : '중복 확인'}
              </button>
            </div>
            {isNicknameAvailable === true && (
              <p className="px-label text-grass mt-1.5">사용 가능한 닉네임입니다.</p>
            )}
            {isNicknameAvailable === false && (
              <p className="px-label text-cherry mt-1.5">이미 사용 중인 닉네임입니다.</p>
            )}
          </div>

          <div>
            <label className="px-label block mb-1.5">아이디</label>
            <div className="flex gap-2">
              <input
                type="text"
                className={`px-input flex-1 ${fieldTone(isIdAvailable)}`}
                placeholder="아이디 입력"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
              />
              <button
                type="button"
                onClick={handleCheckId}
                disabled={isCheckingId || !loginId.trim() || isIdAvailable === true}
                className="px-btn px-btn-sm px-btn-paper shrink-0">
                {isCheckingId ? '확인 중...' : '중복 확인'}
              </button>
            </div>
            {isIdAvailable === true && <p className="px-label text-grass mt-1.5">사용 가능한 아이디입니다.</p>}
            {isIdAvailable === false && <p className="px-label text-cherry mt-1.5">이미 사용 중인 아이디입니다.</p>}
          </div>

          <div>
            <label className="px-label block mb-1.5">비밀번호</label>
            <input
              type="password"
              className="px-input"
              placeholder="비밀번호 입력"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div>
            <label className="px-label block mb-1.5">비밀번호 확인</label>
            <input
              type="password"
              className={`px-input ${confirmPassword && password !== confirmPassword ? 'border-cherry' : ''}`}
              placeholder="비밀번호 다시 입력"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && (
            <div className="border-2 border-ink bg-cherry text-white px-3 py-2 text-xs font-display">{error}</div>
          )}

          <button
            type="submit"
            disabled={isLoading || isIdAvailable !== true || isNicknameAvailable !== true}
            className="px-btn px-btn-primary w-full py-3">
            {isLoading ? '처리 중...' : '계정 생성하기'}
          </button>

          <p className="px-label text-center">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-cherry underline">
              로그인
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
};

export default SignupPage;
