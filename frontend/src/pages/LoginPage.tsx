import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

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
    <div className="min-h-screen flex items-center justify-center bg-background-dark p-6 overflow-hidden relative">
      {/* 배경 장식 */}
      <div className="absolute inset-0 grid-overlay opacity-20 pointer-events-none"></div>
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 rounded-full blur-[100px] pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-primary/5 rounded-full blur-[120px] pointer-events-none"></div>

      <div className="w-full max-w-md z-10 animate-fade-in">
        <div className="panel-border bg-slate-900/80 backdrop-blur-xl p-10 rounded-3xl shadow-2xl space-y-8">
          <div className="text-center space-y-2">
            <div className="inline-flex items-center justify-center p-3 bg-primary/10 rounded-2xl mb-4">
              <span className="material-symbols-outlined text-4xl text-primary neon-glow">rocket_launch</span>
            </div>
            <h1 className="text-4xl font-black text-white tracking-tighter uppercase">
              FunGame <span className="text-primary neon-glow">Login</span>
            </h1>
            <p className="text-slate-400 text-sm font-medium tracking-wide">계정에 로그인하여 게임을 즐기세요.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">아이디</label>
              <div className="relative group">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-primary/40 group-focus-within:text-primary transition-colors">
                  person
                </span>
                <input
                  type="text"
                  className="w-full bg-slate-950 border border-primary/20 rounded-xl py-4 pl-12 pr-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  placeholder="아이디 입력"
                  value={loginId}
                  onChange={(e) => setLoginId(e.target.value)}
                  autoFocus
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">비밀번호</label>
              <div className="relative group">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-primary/40 group-focus-within:text-primary transition-colors">
                  lock
                </span>
                <input
                  type="password"
                  className="w-full bg-slate-950 border border-primary/20 rounded-xl py-4 pl-12 pr-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  placeholder="비밀번호 입력"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 text-red-400 text-xs font-bold bg-red-400/10 p-3 rounded-lg border border-red-400/20">
                <span className="material-symbols-outlined text-sm">error</span>
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-primary hover:bg-primary/90 text-background-dark font-black py-4 rounded-xl transition-all transform hover:scale-[1.02] shadow-[0_0_20px_rgba(37,192,244,0.3)] disabled:opacity-50 disabled:scale-100 flex items-center justify-center gap-2">
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-background-dark border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <>
                  <span className="material-symbols-outlined text-sm">login</span>
                  로그인
                </>
              )}
            </button>
          </form>

          <div className="pt-4 text-center space-y-4">
            <p className="text-slate-500 text-xs font-medium">
              계정이 없으신가요?{' '}
              <Link to="/signup" className="text-primary hover:underline font-bold">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
