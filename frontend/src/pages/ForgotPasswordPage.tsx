import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const SENT_GUIDE = '입력하신 정보와 일치하는 계정이 있다면 재설정 링크를 메일로 보내드렸습니다. 링크는 5분 뒤에 만료됩니다.';

const ForgotPasswordPage: React.FC = () => {
  const [loginId, setLoginId] = useState('');
  const [email, setEmail] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { requestPasswordReset } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId || !email) {
      setError('아이디와 이메일을 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await requestPasswordReset(loginId, email);
      setIsSubmitted(true);
    } catch (err: any) {
      setError(err.message || '잠시 후 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center p-5">
      <div className="w-full max-w-sm animate-pop">
        <h1 className="px-title text-2xl text-center mb-4">비밀번호 찾기</h1>

        {isSubmitted ? (
          <div className="px-card p-5 space-y-4">
            <p className="text-xs leading-relaxed">{SENT_GUIDE}</p>
            <Link to="/login" className="px-btn px-btn-primary w-full py-3 block text-center">
              로그인으로 돌아가기
            </Link>
          </div>
        ) : (
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
              <label className="px-label block mb-1.5">이메일</label>
              <input
                type="email"
                className="px-input"
                placeholder="가입할 때 등록한 이메일"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            {error && (
              <div className="border-2 border-ink bg-cherry text-white px-3 py-2 text-xs font-display">{error}</div>
            )}

            <button type="submit" disabled={isLoading} className="px-btn px-btn-primary w-full py-3">
              {isLoading ? '요청 중...' : '재설정 링크 받기'}
            </button>

            <p className="px-label text-center">
              <Link to="/login" className="text-cherry underline">
                로그인으로 돌아가기
              </Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
