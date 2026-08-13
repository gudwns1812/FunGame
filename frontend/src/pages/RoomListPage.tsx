import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import RoomList from '../components/RoomList';
import OnlineUserList from '../components/OnlineUserList';
import TopBar from '../components/layout/TopBar';
import type { CreateRoomInput, Room } from '../types/game';
import { useAuth } from '../contexts/AuthContext';

interface RoomListPageProps {
  rooms: Room[];
  nickname: string;
  onJoinRoom: (room: Room) => void;
  onCreateRoom: (input: CreateRoomInput) => void;
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

  const menuItem = 'w-full text-left px-3 py-2.5 text-sm font-display hover:bg-paper-2 transition-none';

  return (
    <div className="app-frame">
      <TopBar
        onLogoClick={() => navigate('/rooms')}
        right={
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
              className={`px-btn px-btn-sm ${isDropdownOpen ? 'px-btn-sea' : 'px-btn-paper'}`}>
              <span className="max-w-[7rem] truncate">{user?.nickname || nickname}</span>
              <span className="text-[10px]">{isDropdownOpen ? '▲' : '▼'}</span>
            </button>

            {isDropdownOpen && (
              <div className="px-card absolute right-0 top-full mt-2 w-48 z-50 animate-pop">
                <div className="px-3 py-2 border-b-[3px] border-ink">
                  <p className="px-label truncate">{user?.loginId}</p>
                </div>

                <button
                  onClick={() => {
                    navigate('/mypage');
                    setIsDropdownOpen(false);
                  }}
                  className={menuItem}>
                  마이페이지
                </button>

                {/* 어드민 전용 메뉴 (ADMIN, MASTER 공통) */}
                {isAdmin && (
                  <button
                    onClick={() => {
                      navigate('/admin/songs');
                      setIsDropdownOpen(false);
                    }}
                    className={menuItem}>
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
                    className={menuItem}>
                    유저 관리
                  </button>
                )}

                <button onClick={handleLogout} className={`${menuItem} text-cherry border-t-2 border-ink`}>
                  로그아웃
                </button>
              </div>
            )}
          </div>
        }
      />

      <main className="flex-1 min-h-0 p-4 sm:p-6 flex flex-col md:flex-row gap-4">
        <div className="flex-1 min-h-0 scroll-y custom-scrollbar">
          <RoomList
            rooms={rooms}
            onJoinRoom={onJoinRoom}
            onCreateRoom={onCreateRoom}
            onRefreshRooms={onRefreshRooms}
          />
        </div>

        <OnlineUserList />
      </main>
    </div>
  );
};

export default RoomListPage;
