import React, { useState, useEffect } from 'react';
import type { Room } from '../types/game';
import { stripTag } from '../utils/stringUtils';

interface RoomListProps {
  rooms: Room[];
  onJoinRoom: (room: Room) => void;
  onCreateRoom: (title: string, maxPlayers: number, category: string, songCount: number, gameType: string) => void;
  onRefreshRooms: () => void;
  onChangeNickname: (newName: string) => void;
}

const RoomList: React.FC<RoomListProps> = ({ rooms, onJoinRoom, onCreateRoom, onRefreshRooms }) => {
  const [showCreate, setShowCreate] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const [maxPlayers, setMaxPlayers] = useState(8);
  const [songCount, setSongCount] = useState(10);
  const [category, setCategory] = useState('KPOP');
  const [gameType, setGameType] = useState('SONG');

  const categories = [
    { value: 'KPOP', label: 'K-POP' },
    { value: 'POP', label: 'POP' },
    { value: 'BALLADE', label: '발라드' },
    { value: 'RAP', label: '랩/힙합' },
    { value: 'OST', label: 'OST' },
  ];

  const gameTypes = [
    { value: 'SONG', label: '음악 퀴즈' },
    { value: 'CS', label: 'CS 퀴즈' },
  ];

  const songCountOptions = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100];

  // CS 퀴즈 선택 시 카테고리 및 문제 수 자동 조정
  useEffect(() => {
    if (gameType === 'CS') {
      setCategory('DEFAULT');
      if (songCount > 50) {
        setSongCount(50);
      }
    } else {
      if (category === 'DEFAULT') {
        setCategory('KPOP');
      }
    }
  }, [gameType]);

  const filteredSongCountOptions = gameType === 'CS' ? songCountOptions.filter((n) => n <= 50) : songCountOptions;

  return (
    <div className="w-full max-w-6xl flex flex-col gap-8">
      {/* 상단 컨트롤 패널 */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div className="space-y-2">
          <div className="inline-block px-3 py-1 bg-primary/10 border border-primary/30 rounded text-[10px] font-bold text-primary tracking-widest uppercase">
            활성화된 게임 방: {rooms.length}
          </div>
          <h2 className="text-3xl font-black text-white tracking-tighter uppercase flex items-center gap-3">
            게임 <span className="text-primary neon-glow">로비</span>
          </h2>
        </div>

        <div className="flex gap-3 w-full md:w-auto">
          <button
            onClick={onRefreshRooms}
            className="flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2 border border-primary/30 rounded text-primary hover:bg-primary/10 transition-colors font-bold text-sm tracking-wider">
            <span className="material-symbols-outlined text-lg">refresh</span> 새로고침
          </button>
          <button
            onClick={() => setShowCreate(true)}
            className="flex-1 md:flex-none flex items-center justify-center gap-2 px-5 py-2 bg-primary text-background-dark rounded hover:bg-primary/90 transition-all transform hover:scale-105 font-black text-sm tracking-wider shadow-[0_0_15px_rgba(37,192,244,0.3)]">
            <span className="material-symbols-outlined text-lg">add_circle</span> 방 만들기
          </button>
        </div>
      </div>

      {/* 방 생성 폼 */}
      {showCreate && (
        <div className="panel-border bg-slate-900/50 p-6 rounded-xl space-y-6 animate-in fade-in duration-300">
          <div className="flex items-center gap-2 border-b border-primary/20 pb-3">
            <span className="material-symbols-outlined text-primary">add_box</span>
            <h3 className="text-lg font-bold text-white uppercase tracking-widest">새 게임 방 설정</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
            <div className="md:col-span-2 space-y-2">
              <label className="block text-[10px] font-bold text-primary/70 uppercase tracking-widest">방 제목</label>
              <input
                className="w-full bg-slate-950 border border-primary/30 rounded-lg p-3 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="방 제목을 입력하세요..."
                value={newRoomName}
                onChange={(e) => setNewRoomName(e.target.value)}
                autoFocus
              />
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-primary/70 uppercase tracking-widest">게임 모드</label>
              <select
                className="w-full bg-slate-950 border border-primary/30 rounded-lg p-3 text-white focus:border-primary outline-none appearance-none"
                value={gameType}
                onChange={(e) => setGameType(e.target.value)}>
                {gameTypes.map((type) => (
                  <option key={type.value} value={type.value} className="bg-background-dark">
                    {type.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-primary/70 uppercase tracking-widest">
                장르/카테고리
              </label>
              {gameType === 'CS' ? (
                <div className="w-full bg-slate-900/50 border border-primary/10 rounded-lg p-3 text-primary/50 font-bold">
                  종합 CS
                </div>
              ) : (
                <select
                  className="w-full bg-slate-950 border border-primary/30 rounded-lg p-3 text-white focus:border-primary outline-none appearance-none"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}>
                  {categories.map((cat) => (
                    <option key={cat.value} value={cat.value} className="bg-background-dark">
                      {cat.label}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-primary/70 uppercase tracking-widest">최대 인원</label>
              <select
                className="w-full bg-slate-950 border border-primary/30 rounded-lg p-3 text-white focus:border-primary outline-none appearance-none"
                value={maxPlayers}
                onChange={(e) => setMaxPlayers(parseInt(e.target.value))}>
                {[2, 3, 4, 5, 6, 7, 8].map((n) => (
                  <option key={n} value={n} className="bg-background-dark">
                    {n}명
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-primary/70 uppercase tracking-widest">퀴즈 수</label>
              <select
                className="w-full bg-slate-950 border border-primary/30 rounded-lg p-3 text-white focus:border-primary outline-none appearance-none"
                value={songCount}
                onChange={(e) => setSongCount(parseInt(e.target.value))}>
                {filteredSongCountOptions.map((n) => (
                  <option key={n} value={n} className="bg-background-dark">
                    {n}문제
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              className="px-6 py-2 border border-primary/30 text-primary font-bold rounded hover:bg-primary/10 transition-colors"
              onClick={() => setShowCreate(false)}>
              취소
            </button>
            <button
              className="px-8 py-2 bg-primary text-background-dark font-black rounded hover:bg-primary/90 transition-colors flex items-center gap-2"
              onClick={() => {
                if (newRoomName.trim()) {
                  onCreateRoom(newRoomName.trim(), maxPlayers, category, songCount, gameType);
                  setShowCreate(false);
                  setNewRoomName('');
                }
              }}>
              <span className="material-symbols-outlined text-sm">check_circle</span>방 생성
            </button>
          </div>
        </div>
      )}

      {/* 방 목록 그리드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {rooms.length === 0 ? (
          <div className="col-span-full panel-border bg-slate-900/30 flex flex-col items-center justify-center py-20 rounded-xl opacity-60">
            <span className="material-symbols-outlined text-6xl text-primary/50 mb-4">search_off</span>
            <p className="uppercase tracking-widest text-primary/70 font-bold">현재 생성된 방이 없습니다.</p>
            <p className="text-xs text-slate-500 mt-2">새로운 방을 만들어 게임을 시작해보세요.</p>
          </div>
        ) : (
          rooms.map((room) => {
            const isPlaying = room.status === 'PLAYING';
            return (
              <div
                key={room.id}
                className={`panel-border bg-slate-900/60 rounded-xl p-5 flex flex-col gap-4 group transition-all duration-300 ${isPlaying ? 'opacity-50 grayscale' : 'hover:bg-slate-800/80 hover:-translate-y-1 cursor-pointer hover:shadow-[0_5px_20px_rgba(37,192,244,0.15)]'}`}
                onClick={() => !isPlaying && onJoinRoom(room)}>
                <div className="flex justify-between items-start gap-2">
                  <div className="flex flex-col gap-1 min-w-0">
                    <h3 className="text-lg font-bold text-white uppercase truncate group-hover:text-primary transition-colors">
                      {room.name}
                    </h3>
                    {isPlaying && (
                      <span className="inline-block px-2 py-0.5 bg-red-500/20 border border-red-500/50 text-red-400 text-[10px] font-bold tracking-wider rounded w-fit">
                        게임 진행 중
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-1 text-xs font-bold text-primary bg-primary/10 border border-primary/30 px-2 py-1 rounded shrink-0">
                    <span className="material-symbols-outlined text-sm">groups</span>
                    {room.playerCount} / {room.maxPlayers}
                  </div>
                </div>

                <div className="mt-auto border-t border-primary/10 pt-3 flex justify-between items-center">
                  <div className="flex items-center gap-2 text-slate-400">
                    <span className="material-symbols-outlined text-sm">account_circle</span>
                    <span className="text-[10px] uppercase font-bold tracking-wider truncate max-w-[120px]">
                      {stripTag(room.hostName)}
                    </span>
                  </div>

                  {!isPlaying && (
                    <div className="opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 text-primary text-[10px] font-bold tracking-widest">
                      입장하기 <span className="material-symbols-outlined text-sm">arrow_forward</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default RoomList;
