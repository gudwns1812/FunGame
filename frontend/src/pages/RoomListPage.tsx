import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import RoomList from '../components/RoomList';
import type { Room } from '../types/game';
import { useAuth } from '../contexts/AuthContext';

interface RoomListPageProps {
  rooms: Room[];
  nickname: string;
  onJoinRoom: (room: Room) => void;
  onCreateRoom: (title: string, maxPlayers: number, category: string, songCount: number, gameType: string) => void;
  onRefreshRooms: () => void;
  onChangeNickname: (newName: string) => void;
}

const RoomListPage: React.FC<RoomListPageProps> = ({
  rooms,
  nickname,
  onJoinRoom,
  onCreateRoom,
  onRefreshRooms,
}) => {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // 외부 클릭 시 드롭다운 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = async () => {
    if (window.confirm('로그아웃 하시겠습니까?')) {
      await logout();
      navigate('/login');
    }
  };

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'MASTER';
  const isMaster = user?.role === 'MASTER';

  return (
    <div className="relative flex flex-col min-h-screen">
      {/* 간소화된 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-8 py-4 bg-background-dark/80 backdrop-blur-md z-50">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow cursor-pointer" onClick={() => navigate('/rooms')}>rocket_launch</span>
          </div>
          <div>
            <h1 className="text-xl font-black tracking-widest uppercase text-primary neon-glow cursor-pointer" onClick={() => navigate('/rooms')}>FunGame</h1>
          </div>
        </div>

        <div className="flex items-center gap-6 relative" ref={dropdownRef}>
          <div 
            className="flex items-center gap-3 cursor-pointer group select-none"
            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          >
            <div className="flex flex-col items-end">
              <span className="text-[10px] text-primary/50 uppercase font-bold tracking-widest">Player</span>
              <span className="text-sm font-black text-white group-hover:text-primary transition-colors uppercase">
                {user?.nickname || nickname}
              </span>
            </div>
            <div className={`w-10 h-10 rounded-full border-2 transition-all duration-300 flex items-center justify-center bg-slate-800 ${isDropdownOpen ? 'border-primary shadow-[0_0_15px_rgba(37,192,244,0.4)]' : 'border-primary/30 group-hover:border-primary/60'}`}>
              <span className="material-symbols-outlined text-primary">person</span>
            </div>
            <span className={`material-symbols-outlined text-primary/40 transition-transform duration-300 ${isDropdownOpen ? 'rotate-180' : ''}`}>
              expand_more
            </span>
          </div>

          {/* 드롭다운 메뉴 */}
          {isDropdownOpen && (
            <div className="absolute top-full right-0 mt-3 w-48 bg-slate-900 border border-primary/20 rounded-2xl shadow-2xl overflow-hidden animate-fade-in py-2 backdrop-blur-xl">
              <div className="px-4 py-3 border-b border-white/5 mb-1">
                <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">인증됨</p>
                <p className="text-xs font-bold text-slate-300 truncate">{user?.loginId}</p>
              </div>
              
              <button
                onClick={() => {
                  navigate('/mypage');
                  setIsDropdownOpen(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-slate-300 hover:bg-primary/10 hover:text-primary transition-colors"
              >
                <span className="material-symbols-outlined text-xl">account_circle</span>
                마이페이지
              </button>

              {/* 어드민 전용 메뉴 (ADMIN, MASTER 공통) */}
              {isAdmin && (
                <button
                  onClick={() => {
                    navigate('/admin/songs');
                    setIsDropdownOpen(false);
                  }}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-slate-300 hover:bg-primary/10 hover:text-primary transition-colors"
                >
                  <span className="material-symbols-outlined text-xl">library_music</span>
                  노래 관리
                </button>
              )}

              {/* MASTER 전용 메뉴 */}
              {isMaster && (
                <button
                  onClick={() => {
                    navigate('/master/users');
                    setIsDropdownOpen(false);
                  }}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-slate-300 hover:bg-primary/10 hover:text-primary transition-colors"
                >
                  <span className="material-symbols-outlined text-xl">manage_accounts</span>
                  유저 관리
                </button>
              )}

              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-3 px-4 py-3 text-sm font-bold text-red-400 hover:bg-red-500/10 transition-colors"
              >
                <span className="material-symbols-outlined text-xl">logout</span>
                로그아웃
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center p-6 relative z-10">
        <RoomList
          rooms={rooms}
          onJoinRoom={onJoinRoom}
          onCreateRoom={onCreateRoom}
          onRefreshRooms={onRefreshRooms}
        />
      </main>

      {/* 간소화된 푸터 */}
      <footer className="mt-auto border-t border-primary/10 px-8 py-6 bg-slate-900/40 backdrop-blur-sm flex justify-between items-center z-10">
        <div className="flex gap-8">
          <span className="text-[10px] text-slate-500 uppercase font-black tracking-[0.2em]">Live Connection</span>
        </div>
        <div className="flex items-center gap-4">
          <div className="w-24 h-1 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-primary w-[100%] shadow-[0_0_8px_rgba(37,192,244,0.5)]"></div>
          </div>
          <span className="text-[10px] font-black text-primary/60 uppercase tracking-widest flex items-center gap-2">
            <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
            System Stable
          </span>
        </div>
      </footer>
    </div>
  );
};

export default RoomListPage;
