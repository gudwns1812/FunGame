import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface PromotionRequest {
  id: number;
  loginId: string;
  nickname: string;
  status: string;
  createdAt: string;
}

const UserManagementPage: React.FC = () => {
  const [requests, setRequests] = useState<PromotionRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  const fetchRequests = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await axios.get('/api/master/promotions');
      if (response.data.result === 'SUCCESS') {
        setRequests(response.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch promotion requests:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleAction = async (id: number, action: 'approve' | 'reject') => {
    const confirmMsg = action === 'approve' ? '이 사용자를 ADMIN으로 승급시키겠습니까?' : '이 승급 요청을 거절하시겠습니까?';
    if (!window.confirm(confirmMsg)) return;

    try {
      const response = await axios.patch(`/api/master/promotions/${id}/${action}`);
      if (response.data.result === 'SUCCESS') {
        window.alert(action === 'approve' ? '승급이 완료되었습니다.' : '요청이 거절되었습니다.');
        fetchRequests();
      }
    } catch (err: any) {
      window.alert(err.response?.data?.error?.message || '처리에 실패했습니다.');
    }
  };

  return (
    <div className="min-h-screen bg-background-dark text-white p-6 relative overflow-hidden">
      <div className="absolute inset-0 grid-overlay opacity-10 pointer-events-none"></div>
      
      <div className="max-w-5xl mx-auto relative z-10 animate-fade-in">
        <button 
          onClick={() => navigate('/rooms')}
          className="flex items-center gap-2 text-primary hover:text-primary/80 transition-colors mb-8 font-bold uppercase tracking-widest text-xs"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          로비로 돌아가기
        </button>

        <div className="panel-border bg-slate-900/80 backdrop-blur-xl p-8 rounded-3xl shadow-2xl space-y-8">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-white/10 pb-6">
            <div>
              <h1 className="text-3xl font-black tracking-tighter uppercase mb-1">
                User <span className="text-primary">Management</span>
              </h1>
              <p className="text-slate-400 text-sm font-medium uppercase tracking-widest">대기 중인 승급 요청 검토</p>
            </div>
            <div className="px-4 py-2 bg-primary/10 border border-primary/30 rounded-xl text-primary text-xs font-black uppercase tracking-widest">
              Master Control Panel
            </div>
          </div>

          <div className="overflow-x-auto">
            {isLoading ? (
              <div className="py-20 text-center space-y-4">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
                <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">요청 데이터 로드 중...</p>
              </div>
            ) : requests.length === 0 ? (
              <div className="py-20 text-center bg-slate-950/30 rounded-2xl border border-white/5 border-dashed">
                <span className="material-symbols-outlined text-5xl text-slate-700 mb-4">inbox_customize</span>
                <p className="text-slate-500 font-bold">현재 대기 중인 승급 요청이 없습니다.</p>
              </div>
            ) : (
              <table className="w-full text-left border-separate border-spacing-y-3">
                <thead>
                  <tr className="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em] px-4">
                    <th className="pb-2 pl-6 w-16">ID</th>
                    <th className="pb-2">사용자 정보 (ID/닉네임)</th>
                    <th className="pb-2">신청 일시</th>
                    <th className="pb-2 pr-6 text-right">관리 액션</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((req) => (
                    <tr key={req.id} className="bg-slate-950/50 hover:bg-slate-900/80 transition-colors group">
                      <td className="py-4 pl-6 rounded-l-2xl text-xs font-bold text-slate-500">#{req.id}</td>
                      <td className="py-4">
                        <div className="flex flex-col">
                          <span className="text-sm font-black text-white group-hover:text-primary transition-colors">{req.nickname}</span>
                          <span className="text-[10px] text-slate-500 font-bold">{req.loginId}</span>
                        </div>
                      </td>
                      <td className="py-4 text-xs font-medium text-slate-400">
                        {new Date(req.createdAt).toLocaleString()}
                      </td>
                      <td className="py-4 pr-6 rounded-r-2xl text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => handleAction(req.id, 'reject')}
                            className="px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 rounded-xl text-[10px] font-black uppercase transition-all"
                          >
                            거절
                          </button>
                          <button
                            onClick={() => handleAction(req.id, 'approve')}
                            className="px-4 py-2 bg-green-500/10 hover:bg-green-500/20 text-green-400 border border-green-500/30 rounded-xl text-[10px] font-black uppercase transition-all"
                          >
                            승인
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserManagementPage;
