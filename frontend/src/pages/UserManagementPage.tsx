import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopBar from '../components/layout/TopBar';
import { notifyError, notifySuccess } from '../utils/toast';
import { askConfirm } from '../utils/confirm';

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
    if (!(await askConfirm(confirmMsg))) return;

    try {
      const response = await axios.patch(`/api/master/promotions/${id}/${action}`);
      if (response.data.result === 'SUCCESS') {
        notifySuccess(action === 'approve' ? '승급이 완료되었습니다.' : '요청이 거절되었습니다.');
        fetchRequests();
      }
    } catch (err: any) {
      notifyError(err.response?.data?.error?.message || '처리에 실패했습니다.');
    }
  };

  return (
    <div className="app-frame">
      <TopBar
        title="유저 관리"
        onLogoClick={() => navigate('/rooms')}
        right={
          <button onClick={() => navigate('/rooms')} className="px-btn px-btn-sm px-btn-paper">
            ◀ 로비
          </button>
        }
      />

      <main className="flex-1 min-h-0 scroll-y custom-scrollbar p-4 sm:p-6">
        <div className="w-full max-w-4xl mx-auto animate-pop">
          <div className="px-card">
            <div className="px-head">
              <span>승급 요청</span>
              <span className="px-label text-[10px] num">{requests.length}건 대기</span>
            </div>

            {isLoading ? (
              <p className="px-title text-sm text-center py-16 animate-blink">요청 데이터 로드 중...</p>
            ) : requests.length === 0 ? (
              <div className="p-4">
                <div className="border-2 border-dashed border-ink/35 py-16 text-center">
                  <p className="px-title text-sm">현재 대기 중인 승급 요청이 없습니다.</p>
                </div>
              </div>
            ) : (
              <div className="p-3 space-y-2.5">
                {requests.map((req) => (
                  <div
                    key={req.id}
                    className="px-card-sm p-3 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="px-chip num shrink-0">#{req.id}</span>
                      <div className="min-w-0">
                        <p className="px-title text-sm truncate">{req.nickname}</p>
                        <p className="px-label truncate">{req.loginId}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="px-label hidden sm:inline">{new Date(req.createdAt).toLocaleString()}</span>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleAction(req.id, 'reject')}
                          className="px-btn px-btn-sm px-btn-paper text-cherry">
                          거절
                        </button>
                        <button
                          onClick={() => handleAction(req.id, 'approve')}
                          className="px-btn px-btn-sm px-btn-grass">
                          승인
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default UserManagementPage;
