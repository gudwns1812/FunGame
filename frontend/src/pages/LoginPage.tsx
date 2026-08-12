import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const APP_TITLE = 'FUNGAME';
const MEMBER_RESET_NOTICE = `안녕하세요. 이용에 불편을 드려 죄송합니다.

비밀번호 재설정 기능을 준비하면서 회원 정보에 이메일 항목이 새로 추가되었습니다.
기존에 가입해 주신 계정에는 이메일 정보가 없어, 부득이하게 저장되어 있던 회원 정보를 모두 삭제하였습니다.

번거로우시겠지만 회원가입을 다시 한 번 부탁드립니다.
사용하시던 아이디와 닉네임은 그대로 다시 등록하실 수 있습니다.

소중한 시간을 들여 가입해 주셨는데 미리 안내드리지 못한 점 깊이 사과드립니다.
앞으로는 이런 일이 없도록 더욱 신중하게 준비하겠습니다. 너그러이 양해해 주시면 감사하겠습니다.`;
const WAVE_DELAY_PER_LETTER_SECONDS = 0.09;

const WaveTitle: React.FC<{ text: string }> = ({ text }) => (
  <h1 className="px-title text-3xl flex" aria-label={text}>
    {[...text].map((letter, index) => (
      <span
        key={`${letter}-${index}`}
        className="animate-wave"
        style={{ animationDelay: `${index * WAVE_DELAY_PER_LETTER_SECONDS}s` }}
        aria-hidden="true">
        {letter}
      </span>
    ))}
  </h1>
);

const LoginPage: React.FC = () => {
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId || !password) {
      setError('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await login(loginId, password);
      navigate('/rooms');
    } catch (err: any) {
      // 서버에서 구체적인 에러를 주더라도, 클라이언트에서는 보안 및 UX를 위해 포괄적이고 친절한 메시지로 고정
      setError('아이디 또는 비밀번호를 다시 확인해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center p-5">
      <div className="w-full max-w-sm animate-pop">
        {/* 로고 */}
        <div className="flex flex-col items-center gap-2 mb-6">
          <span className="w-16 h-16 bg-cherry border-[3px] border-ink flex items-center justify-center shadow-[5px_5px_0_0_#1a1a1a]">
            <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.8">
              <circle cx="8" cy="18" r="3" />
              <circle cx="18" cy="15" r="3" />
              <path d="M11 18V6l10-2v11" />
            </svg>
          </span>
          <WaveTitle text={APP_TITLE} />
        </div>

        <form onSubmit={handleSubmit} className="px-card p-5 space-y-4">
          <div>
            <label className="px-label block mb-1.5">아이디</label>
            <input
              type="text"
              className="px-input"
              placeholder="아이디 입력"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              autoFocus
            />
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

          {error && (
            <div className="border-2 border-ink bg-cherry text-white px-3 py-2 text-xs font-display">{error}</div>
          )}

          <button type="submit" disabled={isLoading} className="px-btn px-btn-primary w-full py-3">
            {isLoading ? '로그인 중...' : '로그인 ▶'}
          </button>

          <p className="px-label text-center">
            계정이 없으신가요?{' '}
            <Link to="/signup" className="text-cherry underline">
              회원가입
            </Link>
          </p>

          <p className="px-label text-center">
            <Link to="/forgot-password" className="text-cherry underline">
              비밀번호를 잊으셨나요?
            </Link>
          </p>
        </form>

        <div className="px-card p-4 mt-4">
          <p className="px-label mb-2">공지</p>
          <p className="text-[11px] leading-relaxed whitespace-pre-line">{MEMBER_RESET_NOTICE}</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
