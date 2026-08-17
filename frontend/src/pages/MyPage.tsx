import React from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import TopBar from '../components/layout/TopBar';
import MyPageNav from '../components/mypage/MyPageNav';
import MyReportsSection from '../components/mypage/MyReportsSection';
import ProfileSection from '../components/mypage/ProfileSection';
import ReportAdminSection from '../components/mypage/ReportAdminSection';

const MyPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'MASTER';

  return (
    <div className="app-frame">
      <TopBar
        title="마이페이지"
        onLogoClick={() => navigate('/rooms')}
        right={
          <button onClick={() => navigate('/rooms')} className="px-btn px-btn-sm px-btn-paper">
            ◀ 로비
          </button>
        }
      />

      <main className="flex-1 min-h-0 scroll-y custom-scrollbar p-4 sm:p-6">
        <div className="w-full max-w-3xl mx-auto flex flex-col sm:flex-row gap-4 animate-pop">
          <MyPageNav isAdmin={isAdmin} />

          <div className="flex-1 min-w-0">
            <Routes>
              <Route index element={<ProfileSection />} />
              <Route path="reports" element={<MyReportsSection />} />
              <Route
                path="inquiries"
                element={isAdmin ? <ReportAdminSection /> : <Navigate to="/mypage" replace />}
              />
              <Route path="*" element={<Navigate to="/mypage" replace />} />
            </Routes>
          </div>
        </div>
      </main>
    </div>
  );
};

export default MyPage;
