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

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { signup, checkId } = useAuth();
  const navigate = useNavigate();

  // 아이디가 변경되면 중복 확인 상태 초기화
  useEffect(() => {
    setIsIdAvailable(null);
  }, [loginId]);

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

  return (
    <div className="min-h-screen flex items-center justify-center bg-background-dark p-6 overflow-hidden relative">
      {/* 배경 장식 */}
      <div className="absolute inset-0 grid-overlay opacity-20 pointer-events-none"></div>
      
      <div className="w-full max-w-md z-10 animate-fade-in">
        <div className="panel-border bg-slate-900/80 backdrop-blur-xl p-10 rounded-3xl shadow-2xl space-y-8">
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-black text-white tracking-tighter uppercase">
              Join <span className="text-primary neon-glow">FunGame</span>
            </h1>
            <p className="text-slate-400 text-sm font-medium tracking-wide">새로운 계정을 생성하세요.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">닉네임</label>
              <input
                type="text"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-4 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="닉네임 입력 (최대 16자)"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                maxLength={16}
              />
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">아이디</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  className={`flex-1 bg-slate-950 border rounded-xl py-4 px-4 text-white outline-none transition-all focus:ring-1 ${
                    isIdAvailable === true 
                      ? 'border-green-500/50 focus:border-green-500 focus:ring-green-500' 
                      : isIdAvailable === false 
                        ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500'
                        : 'border-primary/20 focus:border-primary focus:ring-primary'
                  }`}
                  placeholder="아이디 입력"
                  value={loginId}
                  onChange={(e) => setLoginId(e.target.value)}
                />
                <button
                  type="button"
                  onClick={handleCheckId}
                  disabled={isCheckingId || !loginId.trim() || isIdAvailable === true}
                  className="px-4 bg-primary/10 text-primary border border-primary/30 rounded-xl font-bold hover:bg-primary/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                >
                  {isCheckingId ? '확인 중...' : '중복 확인'}
                </button>
              </div>
              {isIdAvailable === true && (
                <p className="text-green-400 text-xs font-bold pl-1">사용 가능한 아이디입니다.</p>
              )}
              {isIdAvailable === false && (
                <p className="text-red-400 text-xs font-bold pl-1">이미 사용 중인 아이디입니다.</p>
              )}
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">비밀번호</label>
              <input
                type="password"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-4 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="비밀번호 입력"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">비밀번호 확인</label>
              <input
                type="password"
                className={`w-full bg-slate-950 border rounded-xl py-4 px-4 text-white outline-none transition-all focus:ring-1 ${
                  confirmPassword && password !== confirmPassword 
                    ? 'border-red-500/50 focus:border-red-500 focus:ring-red-500'
                    : 'border-primary/20 focus:border-primary focus:ring-primary'
                }`}
                placeholder="비밀번호 다시 입력"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>

            {error && (
              <div className="flex items-center gap-2 text-red-400 text-xs font-bold bg-red-400/10 p-3 rounded-lg border border-red-400/20">
                <span className="material-symbols-outlined text-sm">error</span>
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading || isIdAvailable !== true}
              className="w-full bg-primary hover:bg-primary/90 text-background-dark font-black py-4 rounded-xl transition-all transform hover:scale-[1.02] shadow-[0_0_20px_rgba(37,192,244,0.3)] disabled:opacity-50 disabled:scale-100 flex items-center justify-center gap-2 mt-4">
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-background-dark border-t-transparent rounded-full animate-spin"></div>
              ) : (
                '계정 생성하기'
              )}
            </button>
          </form>

          <div className="pt-2 text-center">
            <p className="text-slate-500 text-xs font-medium">
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-primary hover:underline font-bold">
                로그인 페이지로
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
