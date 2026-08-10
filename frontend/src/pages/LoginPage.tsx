import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const APP_TITLE = 'FUNGAME';
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
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
