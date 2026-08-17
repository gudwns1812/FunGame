import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { useAuth } from '../../contexts/AuthContext';

type PromotionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | null;

const ProfileSection: React.FC = () => {
  const { user, updateNickname } = useAuth();
  const [newNickname, setNewNickname] = useState(user?.nickname || '');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [promotionStatus, setPromotionStatus] = useState<PromotionStatus>(null);
  const [isRequestingPromotion, setIsRequestingPromotion] = useState(false);

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
    } catch (err) {
      setMessage({ text: (err as Error).message || '닉네임 변경에 실패했습니다.', type: 'error' });
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
    } catch (err) {
      const message = (err as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error
        ?.message;
      window.alert(message ?? '승급 신청 중 오류가 발생했습니다.');
    } finally {
      setIsRequestingPromotion(false);
    }
  };

  const renderPromotionSection = () => {
    if (user?.role !== 'USER') return null;

    return (
      <div className="px-card">
        <div className="px-head">
          <span>권한 승급</span>
        </div>

        <div className="p-4 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="px-title text-sm">관리자 권한 신청</p>
              <p className="px-label mt-1">노래 추가 및 방 관리 권한을 얻을 수 있습니다.</p>
            </div>

            {promotionStatus === 'PENDING' ? (
              <span className="px-chip px-chip-gold">검토 대기 중</span>
            ) : promotionStatus === 'REJECTED' ? (
              <button
                onClick={handleRequestPromotion}
                disabled={isRequestingPromotion}
                className="px-btn px-btn-sm px-btn-paper">
                {isRequestingPromotion ? '처리 중...' : '재신청 하기'}
              </button>
            ) : (
              <button
                onClick={handleRequestPromotion}
                disabled={isRequestingPromotion}
                className="px-btn px-btn-sm px-btn-sea">
                {isRequestingPromotion ? '신청 중...' : '승급 신청하기'}
              </button>
            )}
          </div>

          {promotionStatus === 'REJECTED' && (
            <p className="px-inset px-3 py-2 text-xs text-cherry">
              이전 신청이 거절되었습니다. 사유를 확인하거나 나중에 다시 신청해주세요.
            </p>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="px-card p-4 flex items-center gap-4">
        <span className="w-16 h-16 shrink-0 border-[3px] border-ink bg-cherry flex items-center justify-center">
          <span className="px-title text-2xl text-white">{(user?.nickname || '?').slice(0, 1)}</span>
        </span>

        <div className="min-w-0">
          <p className="px-title text-xl truncate">{user?.nickname}</p>
          <p className="px-label mt-1 truncate">{user?.loginId}</p>
          <span className="px-chip px-chip-sea mt-2">{user?.role}</span>
        </div>
      </div>

      <div className="px-card">
        <div className="px-head">
          <span>닉네임 설정</span>
        </div>

        <form onSubmit={handleUpdateNickname} className="p-4 space-y-3">
          <div>
            <label className="px-label block mb-1.5">현재 닉네임</label>
            <div className="px-input bg-paper-2 text-ink-soft">{user?.nickname}</div>
          </div>

          <div>
            <label className="px-label block mb-1.5">이메일</label>
            <div className="px-input bg-paper-2 text-ink-soft">{user?.email}</div>
          </div>

          <div>
            <label className="px-label block mb-1.5">새 닉네임</label>
            <input
              type="text"
              className="px-input"
              placeholder="변경할 닉네임을 입력하세요"
              value={newNickname}
              onChange={(e) => setNewNickname(e.target.value)}
              maxLength={16}
            />
          </div>

          {message.text && (
            <div
              className={`border-2 border-ink px-3 py-2 text-xs font-display ${
                message.type === 'success' ? 'bg-grass text-white' : 'bg-cherry text-white'
              }`}>
              {message.text}
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading || !newNickname.trim() || newNickname === user?.nickname}
            className="px-btn px-btn-primary w-full py-3">
            {isLoading ? '저장 중...' : '닉네임 변경'}
          </button>
        </form>
      </div>

      {renderPromotionSection()}

      <p className="px-label leading-5 px-1">
        닉네임은 게임 내에서 본인을 식별하는 용도로 사용됩니다.
        <br />
        불쾌감을 주는 닉네임은 관리자에 의해 변경되거나 제재를 받을 수 있습니다.
      </p>
    </div>
  );
};

export default ProfileSection;
