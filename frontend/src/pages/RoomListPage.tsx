import React, { useState } from 'react';
import RoomList from '../components/RoomList';
import type { Room } from '../types/game';

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
  onChangeNickname,
}) => {
  const [showNicknameEdit, setShowNicknameEdit] = useState(false);
  const [nicknameInput, setNicknameInput] = useState('');

  return (
    <div className="relative flex flex-col min-h-screen">
      {/* 닉네임 수정 모달 */}
      {showNicknameEdit && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm"
          onClick={() => setShowNicknameEdit(false)}>
          <div
            className="panel-border bg-background-dark/95 p-6 rounded-lg w-80 space-y-4"
            onClick={(e) => e.stopPropagation()}>
            <p className="text-sm font-bold uppercase tracking-widest text-primary flex items-center gap-2">
              <span className="material-symbols-outlined">edit</span> 닉네임 수정
            </p>
            <input
              autoFocus
              type="text"
              className="w-full bg-slate-900 border border-primary/30 rounded p-3 text-white focus:border-primary outline-none transition-colors"
              value={nicknameInput}
              onChange={(e) => setNicknameInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && nicknameInput.trim()) {
                  onChangeNickname(nicknameInput.trim());
                  setShowNicknameEdit(false);
                } else if (e.key === 'Escape') {
                  setShowNicknameEdit(false);
                }
              }}
              maxLength={16}
              placeholder="새 닉네임 입력"
            />
            <div className="flex gap-2 pt-2">
              <button
                className="flex-1 bg-primary text-background-dark font-bold py-2 rounded hover:bg-primary/80 transition-colors"
                onClick={() => {
                  if (nicknameInput.trim()) {
                    onChangeNickname(nicknameInput.trim());
                    setShowNicknameEdit(false);
                  }
                }}>
                확인
              </button>
              <button
                className="flex-1 border border-primary/30 text-primary font-bold py-2 rounded hover:bg-primary/10 transition-colors"
                onClick={() => setShowNicknameEdit(false)}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-6 py-4 bg-background-dark/80 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow">rocket_launch</span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest uppercase text-primary neon-glow">FunGame</h1>
            <p className="text-[10px] text-primary/60 tracking-[0.2em]">서버 상태: 양호</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-primary/20">
            <span className="text-[10px] text-primary/50 uppercase">현재 구역</span>
            <span className="text-sm font-bold uppercase">Lobby-01</span>
          </div>
          <div className="flex gap-3 items-center">
            <div className="flex flex-col items-end">
              <span className="text-[10px] text-primary/50 uppercase">접속 대원</span>
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-white uppercase">{nickname}</span>
                <button
                  onClick={() => {
                    setNicknameInput(nickname);
                    setShowNicknameEdit(true);
                  }}
                  className="text-slate-500 hover:text-primary transition-colors flex items-center"
                  title="닉네임 변경">
                  <span className="material-symbols-outlined text-lg">edit_square</span>
                </button>
              </div>
            </div>
            <div className="w-10 h-10 rounded-full border-2 border-primary overflow-hidden shadow-[0_0_10px_rgba(37,192,244,0.4)] flex items-center justify-center bg-slate-800">
              <span className="material-symbols-outlined text-primary">person</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center p-6 relative z-10">
        <RoomList
          rooms={rooms}
          onJoinRoom={onJoinRoom}
          onCreateRoom={onCreateRoom}
          onRefreshRooms={onRefreshRooms}
          onChangeNickname={onChangeNickname}
        />
      </main>

      {/* 푸터 */}
      <footer className="mt-auto border-t border-primary/10 px-6 py-6 bg-slate-900/40 backdrop-blur-sm flex flex-col md:flex-row justify-between items-center gap-4 z-10">
        <div className="flex gap-8 order-2 md:order-1">
          <span className="text-xs text-slate-500 uppercase font-bold tracking-widest">연습 모드</span>
          <span className="text-xs text-slate-500 uppercase font-bold tracking-widest">전체 랭킹</span>
          <span className="text-xs text-slate-500 uppercase font-bold tracking-widest">로그 확인</span>
        </div>
        <div className="flex items-center gap-4 order-1 md:order-2">
          <div className="w-32 h-2 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-primary w-[100%] shadow-[0_0_8px_rgba(37,192,244,0.5)]"></div>
          </div>
          <span className="text-[10px] font-bold text-primary">네트워크 안정성: 100%</span>
        </div>
      </footer>
    </div>
  );
};

export default RoomListPage;
