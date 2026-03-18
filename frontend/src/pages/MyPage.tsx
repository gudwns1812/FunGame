import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';

type PromotionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | null;

const MyPage: React.FC = () => {
  const { user, updateNickname } = useAuth();
  const [newNickname, setNewNickname] = useState(user?.nickname || '');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [promotionStatus, setPromotionStatus] = useState<PromotionStatus>(null);
  const [isRequestingPromotion, setIsRequestingPromotion] = useState(false);
  const navigate = useNavigate();

  // 승급 상태 조회
  const fetchPromotionStatus = useCallback(async () => {
    try {
      const response = await axios.get('/api/promotions/status');
      if (response.data.result === 'SUCCESS') {
        setPromotionStatus(response.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch promotion status:', err);
    }
  }, []);

  useEffect(() => {
    fetchPromotionStatus();
  }, [fetchPromotionStatus]);

  const handleUpdateNickname = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNickname.trim() || newNickname === user?.nickname) return;

    setIsLoading(true);
    setMessage({ text: '', type: '' });

    try {
      await updateNickname(newNickname.trim());
      setMessage({ text: '닉네임이 성공적으로 변경되었습니다.', type: 'success' });
    } catch (err: any) {
      setMessage({ text: err.message || '닉네임 변경에 실패했습니다.', type: 'error' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleRequestPromotion = async () => {
    if (!window.confirm('관리자 승급을 신청하시겠습니까?')) return;

    setIsRequestingPromotion(true);
    try {
      const response = await axios.post('/api/promotions');
      if (response.data.result === 'SUCCESS') {
        window.alert('승급 신청이 완료되었습니다. MASTER의 승인을 기다려주세요.');
        fetchPromotionStatus();
      }
    } catch (err: any) {
      window.alert(err.response?.data?.error?.message || '승급 신청 중 오류가 발생했습니다.');
    } finally {
      setIsRequestingPromotion(false);
    }
  };

  const renderPromotionSection = () => {
    if (user?.role !== 'USER') return null;

    return (
      <section className="pt-8 border-t border-white/5 space-y-6">
        <h2 className="text-lg font-bold flex items-center gap-2 uppercase tracking-wide">
          <span className="material-symbols-outlined text-yellow-500">military_tech</span>
          권한 승급 관리
        </h2>

        <div className="bg-slate-950/30 border border-white/5 rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold text-slate-300">ADMIN 권한 신청</p>
              <p className="text-xs text-slate-500 mt-1">노래 추가 및 방 관리 권한을 얻을 수 있습니다.</p>
            </div>
            
            {promotionStatus === 'PENDING' ? (
              <div className="px-4 py-2 bg-yellow-500/10 border border-yellow-500/30 rounded-xl text-yellow-500 text-xs font-black uppercase tracking-widest flex items-center gap-2">
                <span className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"></span>
                검토 대기 중
              </div>
            ) : promotionStatus === 'REJECTED' ? (
              <button
                onClick={handleRequestPromotion}
                disabled={isRequestingPromotion}
                className="px-6 py-2 bg-slate-800 hover:bg-slate-700 border border-white/10 rounded-xl text-xs font-bold transition-all"
              >
                {isRequestingPromotion ? '처리 중...' : '재신청 하기'}
              </button>
            ) : (
              <button
                onClick={handleRequestPromotion}
                disabled={isRequestingPromotion}
                className="px-6 py-2 bg-yellow-500 hover:bg-yellow-400 text-background-dark rounded-xl text-xs font-black transition-all shadow-[0_0_15px_rgba(234,179,8,0.2)]"
              >
                {isRequestingPromotion ? '신청 중...' : '승급 신청하기'}
              </button>
            )}
          </div>

          {promotionStatus === 'REJECTED' && (
            <div className="flex items-center gap-2 text-[10px] font-bold text-red-400 bg-red-400/5 p-2 rounded-lg">
              <span className="material-symbols-outlined text-sm">info</span>
              이전 신청이 거절되었습니다. 사유를 확인하거나 나중에 다시 신청해주세요.
            </div>
          )}
        </div>
      </section>
    );
  };

  return (
    <div className="min-h-screen bg-background-dark text-white p-6 relative overflow-hidden">
      <div className="absolute inset-0 grid-overlay opacity-10 pointer-events-none"></div>
      
      <div className="max-w-2xl mx-auto relative z-10 animate-fade-in">
        <button 
          onClick={() => navigate('/rooms')}
          className="flex items-center gap-2 text-primary hover:text-primary/80 transition-colors mb-8 font-bold uppercase tracking-widest text-xs"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          로비로 돌아가기
        </button>

        <div className="panel-border bg-slate-900/80 backdrop-blur-xl p-8 md:p-12 rounded-3xl shadow-2xl space-y-10">
          <div className="flex items-center gap-6 border-b border-white/10 pb-8">
            <div className="w-24 h-24 rounded-full border-4 border-primary/30 flex items-center justify-center bg-slate-800 shadow-[0_0_20px_rgba(37,192,244,0.2)]">
              <span className="material-symbols-outlined text-5xl text-primary">person</span>
            </div>
            <div>
              <h1 className="text-3xl font-black tracking-tighter uppercase mb-1">My <span className="text-primary">Profile</span></h1>
              <p className="text-slate-400 text-sm font-medium">{user?.loginId} 계정으로 접속 중</p>
              <div className="mt-2 inline-block px-3 py-1 bg-primary/10 border border-primary/30 rounded text-[10px] font-black text-primary uppercase tracking-widest">
                {user?.role} Access
              </div>
            </div>
          </div>

          <section className="space-y-6">
            <h2 className="text-lg font-bold flex items-center gap-2 uppercase tracking-wide">
              <span className="material-symbols-outlined text-primary">edit_square</span>
              닉네임 설정
            </h2>
            
            <form onSubmit={handleUpdateNickname} className="space-y-4">
              <div className="space-y-2">
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest pl-1">현재 닉네임</label>
                <div className="bg-slate-950/50 border border-white/5 rounded-xl py-4 px-6 text-slate-400 font-bold">
                  {user?.nickname}
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">새 닉네임 입력</label>
                <input
                  type="text"
                  className="w-full bg-slate-950 border border-primary/20 rounded-xl py-4 px-6 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-bold"
                  placeholder="변경할 닉네임을 입력하세요"
                  value={newNickname}
                  onChange={(e) => setNewNickname(e.target.value)}
                  maxLength={16}
                />
              </div>

              {message.text && (
                <div className={`p-4 rounded-xl text-xs font-bold flex items-center gap-2 ${
                  message.type === 'success' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
                }`}>
                  <span className="material-symbols-outlined text-sm">
                    {message.type === 'success' ? 'check_circle' : 'error'}
                  </span>
                  {message.text}
                </div>
              )}

              <button
                type="submit"
                disabled={isLoading || !newNickname.trim() || newNickname === user?.nickname}
                className="w-full bg-primary hover:bg-primary/90 text-background-dark font-black py-4 rounded-xl transition-all transform hover:scale-[1.01] shadow-[0_0_20px_rgba(37,192,244,0.3)] disabled:opacity-30 disabled:grayscale disabled:scale-100 flex items-center justify-center gap-2 mt-4 uppercase tracking-tighter"
              >
                {isLoading ? '저장 중...' : '정보 업데이트'}
              </button>
            </form>
          </section>

          {renderPromotionSection()}

          <section className="pt-8 border-t border-white/5 space-y-4">
            <h2 className="text-xs font-bold text-slate-500 uppercase tracking-[0.2em]">계정 보안 안내</h2>
            <p className="text-xs text-slate-500 leading-relaxed">
              닉네임은 게임 내 서비스 이용 시 본인을 식별하는 용도로 사용됩니다. <br/>
              불쾌감을 주는 닉네임은 관리자에 의해 임의로 변경되거나 제재를 받을 수 있습니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};

export default MyPage;
