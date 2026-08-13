import React, { useState } from 'react';
import type { CreateRoomInput, Room } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import {
  CATEGORIES,
  CS_DIFFICULTIES,
  DEFAULT_CS_DIFFICULTY,
  GAME_TYPES,
  applyGameTypeConstraints,
  isSingleRound,
  maxPlayerOptionsFor,
  roundOptionsFor,
} from '../utils/gameOptions';

interface RoomListProps {
  rooms: Room[];
  onJoinRoom: (room: Room) => void;
  onCreateRoom: (input: CreateRoomInput) => void;
  onRefreshRooms: () => void;
}

/** 방 제목 최대 길이 (헤더 칩에 잘리지 않고 들어가는 길이 기준) */
const ROOM_NAME_MAX = 20;

const labelOf = (options: { value: string; label: string }[], value: string | null) =>
  options.find((option) => option.value === value)?.label ?? value;

const RoomList: React.FC<RoomListProps> = ({ rooms, onJoinRoom, onCreateRoom, onRefreshRooms }) => {
  const [showCreate, setShowCreate] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const [maxPlayers, setMaxPlayers] = useState(8);
  const [songCount, setSongCount] = useState(10);
  const [category, setCategory] = useState('KPOP');
  const [gameType, setGameType] = useState('SONG');
  const [difficulty, setDifficulty] = useState(3);
  const [csDifficulty, setCsDifficulty] = useState(DEFAULT_CS_DIFFICULTY);

  const changeGameType = (nextGameType: string) => {
    const adjusted = applyGameTypeConstraints(nextGameType, { category, totalRound: songCount, maxPlayers });

    setGameType(nextGameType);
    setCategory(adjusted.category);
    setSongCount(adjusted.totalRound);
    setMaxPlayers(adjusted.maxPlayers);
  };

  const singleRound = isSingleRound(gameType);

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col gap-4">
      {/* 상단: 제목 + 액션 */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div className="flex items-center gap-2.5">
          <h2 className="px-title text-2xl">게임 방</h2>
          <span className="px-chip num">{rooms.length}</span>
        </div>

        <div className="flex gap-2">
          <button onClick={onRefreshRooms} className="px-btn px-btn-sm px-btn-paper">
            새로고침
          </button>
          <button onClick={() => setShowCreate(true)} className="px-btn px-btn-sm px-btn-primary">
            방 만들기 +
          </button>
        </div>
      </div>

      {/* 방 생성 폼 */}
      {showCreate && (
        <div className="px-card p-4 space-y-3.5 animate-pop">
          <h3 className="px-title text-base border-b-[3px] border-ink pb-2">새 방 만들기</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="md:col-span-2">
              <div className="flex items-center justify-between mb-1.5">
                <label className="px-label">방 제목</label>
                <span className={`px-label num ${newRoomName.length >= ROOM_NAME_MAX ? 'text-cherry' : ''}`}>
                  {newRoomName.length} / {ROOM_NAME_MAX}
                </span>
              </div>
              <input
                className="px-input"
                placeholder="방 제목을 입력하세요"
                value={newRoomName}
                onChange={(e) => setNewRoomName(e.target.value)}
                maxLength={ROOM_NAME_MAX}
                autoFocus
              />
            </div>

            <div>
              <label className="px-label block mb-1.5">게임 모드</label>
              <select className="px-input" value={gameType} onChange={(e) => changeGameType(e.target.value)}>
                {GAME_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="px-label block mb-1.5">
                {gameType === 'HANGMAN' || gameType === 'CS' ? '난이도' : '장르'}
              </label>
              {gameType === 'CS' ? (
                <>
                  <select className="px-input" value={csDifficulty} onChange={(e) => setCsDifficulty(e.target.value)}>
                    {CS_DIFFICULTIES.map((level) => (
                      <option key={level.value} value={level.value}>
                        {level.label}
                      </option>
                    ))}
                  </select>
                  <p className="px-label text-[10px] mt-1">고른 난이도까지의 문제가 출제됩니다.</p>
                </>
              ) : gameType === 'HANGMAN' ? (
                <div className="px-input flex items-center gap-3 py-2.5">
                  <input
                    type="range"
                    min="1"
                    max="4"
                    step="1"
                    value={difficulty}
                    onChange={(e) => setDifficulty(parseInt(e.target.value))}
                    className="flex-1 accent-cherry h-1.5 cursor-pointer"
                  />
                  <span className="px-title text-sm w-4 text-center num">{difficulty}</span>
                </div>
              ) : (
                <select className="px-input" value={category} onChange={(e) => setCategory(e.target.value)}>
                  {CATEGORIES.map((cat) => (
                    <option key={cat.value} value={cat.value}>
                      {cat.label}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label className="px-label block mb-1.5">최대 인원</label>
              <select
                className="px-input"
                value={maxPlayers}
                onChange={(e) => setMaxPlayers(parseInt(e.target.value))}>
                {maxPlayerOptionsFor(gameType).map((n) => (
                  <option key={n} value={n}>
                    {n}명
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="px-label block mb-1.5">{singleRound ? '게임 방식' : '퀴즈 수'}</label>
              <select className="px-input" value={songCount} onChange={(e) => setSongCount(parseInt(e.target.value))}>
                {roundOptionsFor(gameType).map((n) => (
                  <option key={n} value={n}>
                    {singleRound ? '단판' : `${n}문제`}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-1">
            <button className="px-btn px-btn-sm px-btn-paper" onClick={() => setShowCreate(false)}>
              취소
            </button>
            <button
              className="px-btn px-btn-sm px-btn-primary"
              data-sound="roomCreate"
              onClick={() => {
                if (newRoomName.trim()) {
                  onCreateRoom({
                    title: newRoomName.trim(),
                    maxPlayers,
                    category,
                    totalRound: songCount,
                    gameType,
                    difficulty,
                    csDifficulty,
                  });
                  setShowCreate(false);
                  setNewRoomName('');
                }
              }}>
              방 생성
            </button>
          </div>
        </div>
      )}

      {/* 방 목록 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        {rooms.length === 0 ? (
          <div className="col-span-full px-card py-14 flex flex-col items-center gap-2">
            <p className="px-title text-sm">현재 생성된 방이 없습니다.</p>
            <p className="px-label">새 방을 만들어 게임을 시작해보세요</p>
          </div>
        ) : (
          rooms.map((room) => {
            const isPlaying = room.status === 'PLAYING';
            const isFull = room.playerCount >= room.maxPlayers;

            return (
              <div
                key={room.id}
                className="px-card flex flex-col px-tap cursor-pointer"
                onClick={() => onJoinRoom(room)}>
                {/* 카드 헤더 */}
                <div className="flex items-center justify-between border-b-[3px] border-ink px-3 py-2 bg-sky-deep">
                  <span className="w-7 h-7 border-2 border-ink bg-paper flex items-center justify-center">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#1a1a1a" strokeWidth="2.6">
                      <circle cx="8" cy="18" r="3" />
                      <circle cx="18" cy="15" r="3" />
                      <path d="M11 18V6l10-2v11" />
                    </svg>
                  </span>

                  {isPlaying ? (
                    <span className="px-chip px-chip-cherry">게임 진행 중</span>
                  ) : (
                    <span className={`px-chip num ${isFull ? 'px-chip-cherry' : ''}`}>
                      {room.playerCount} / {room.maxPlayers}
                    </span>
                  )}
                </div>

                <div className="p-3 flex flex-col gap-2 flex-1">
                  <h3 className="px-title text-base leading-snug break-keep">{room.name}</h3>

                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="px-chip px-chip-sea">{labelOf(GAME_TYPES, room.gameType)}</span>
                    {room.gameType === 'CS' && (
                      <span className="px-chip">난이도 {labelOf(CS_DIFFICULTIES, room.csDifficulty)}</span>
                    )}
                  </div>

                  <div className="mt-auto flex items-center justify-between gap-2">
                    <span className="px-label truncate">방장 {stripTag(room.hostName)}</span>
                    <span className="px-chip px-chip-cherry">{isPlaying ? '재입장 ▶' : '입장 ▶'}</span>
                  </div>
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
