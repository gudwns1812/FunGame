import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const MINIMUM_PASSWORD_LENGTH = 4;
const BROKEN_LINK_GUIDE = '잘못된 접근입니다. 메일로 받은 링크로 다시 들어와주세요.';

const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { resetPassword } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      return;
    }

    if (!newPassword || !confirmPassword) {
      setError('새 비밀번호를 두 번 입력해주세요.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('비밀번호가 서로 일치하지 않습니다.');
      return;
    }

    if (newPassword.length < MINIMUM_PASSWORD_LENGTH) {
      setError(`비밀번호는 최소 ${MINIMUM_PASSWORD_LENGTH}자 이상이어야 합니다.`);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await resetPassword(token, newPassword);
      window.alert('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.');
      navigate('/login');
    } catch (err: any) {
      setError(err.message || '비밀번호 재설정에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center p-5">
      <div className="w-full max-w-sm animate-pop">
        <h1 className="px-title text-2xl text-center mb-4">비밀번호 재설정</h1>

        {token ? (
          <form onSubmit={handleSubmit} className="px-card p-5 space-y-4">
            <div>
              <label className="px-label block mb-1.5">새 비밀번호</label>
              <input
                type="password"
                className="px-input"
                placeholder="새 비밀번호 입력"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoFocus
              />
            </div>

            <div>
              <label className="px-label block mb-1.5">새 비밀번호 확인</label>
              <input
                type="password"
                className={`px-input ${confirmPassword && newPassword !== confirmPassword ? 'border-cherry' : ''}`}
                placeholder="새 비밀번호 다시 입력"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>

            {error && (
              <div className="border-2 border-ink bg-cherry text-white px-3 py-2 text-xs font-display">{error}</div>
            )}

            <button type="submit" disabled={isLoading} className="px-btn px-btn-primary w-full py-3">
              {isLoading ? '변경 중...' : '비밀번호 변경하기'}
            </button>
          </form>
        ) : (
          <div className="px-card p-5 space-y-4">
            <p className="text-xs leading-relaxed">{BROKEN_LINK_GUIDE}</p>
            <Link to="/forgot-password" className="px-btn px-btn-primary w-full py-3 block text-center">
              다시 요청하기
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default ResetPasswordPage;
