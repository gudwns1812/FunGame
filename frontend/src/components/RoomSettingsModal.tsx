import React, { useState } from 'react';
import type { RoomSettings } from '../types/game';
import {
  CATEGORIES,
  GAME_TYPES,
  applyGameTypeConstraints,
  canHold,
  capacityOf,
  isSingleRound,
  maxPlayerOptionsFor,
  roundOptionsFor,
} from '../utils/gameOptions';

interface RoomSettingsModalProps {
  settings: RoomSettings;
  currentPlayers: number;
  onSubmit: (changes: Omit<RoomSettings, 'title' | 'host'>) => void;
  onClose: () => void;
}

const RoomSettingsModal: React.FC<RoomSettingsModalProps> = ({ settings, currentPlayers, onSubmit, onClose }) => {
  const [gameType, setGameType] = useState(settings.gameType);
  const [category, setCategory] = useState(settings.category ?? 'KPOP');
  const [totalRound, setTotalRound] = useState(settings.totalRound);
  const [difficulty, setDifficulty] = useState(settings.difficulty || 3);
  const [maxPlayers, setMaxPlayers] = useState(settings.maxPlayers);

  const changeGameType = (nextGameType: string) => {
    const adjusted = applyGameTypeConstraints(nextGameType, { category, totalRound, maxPlayers }, currentPlayers);

    setGameType(nextGameType);
    setCategory(adjusted.category);
    setTotalRound(adjusted.totalRound);
    setMaxPlayers(adjusted.maxPlayers);
  };

  const selectableMaxPlayers = maxPlayerOptionsFor(gameType).filter((count) => count >= currentPlayers);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ gameType, maxPlayers, category, totalRound, difficulty });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 animate-fade-in">
      <form className="px-card w-full max-w-sm p-6 space-y-4 animate-scale-up" onSubmit={submit}>
        <h2 className="px-title text-xl border-b-[3px] border-ink pb-2">방 설정 변경</h2>

        <div>
          <label className="px-label block mb-1.5">방 이름</label>
          <div className="px-input bg-paper-2 text-ink-soft truncate">{settings.title}</div>
        </div>

        <div>
          <label className="px-label block mb-1.5" htmlFor="room-settings-game-type">게임 모드</label>
          <select id="room-settings-game-type" className="px-input" value={gameType} onChange={(e) => changeGameType(e.target.value)}>
            {GAME_TYPES.map((type) => (
              <option key={type.value} value={type.value} disabled={!canHold(type.value, currentPlayers)}>
                {canHold(type.value, currentPlayers)
                  ? type.label
                  : `${type.label} (최대 ${capacityOf(type.value)}명)`}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="px-label block mb-1.5" htmlFor="room-settings-category">{gameType === 'HANGMAN' ? '난이도' : '장르'}</label>
          {gameType === 'CS' ? (
            <div className="px-input bg-paper-2 text-ink-soft">CS 종합</div>
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
            <select id="room-settings-category" className="px-input" value={category} onChange={(e) => setCategory(e.target.value)}>
              {CATEGORIES.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          )}
        </div>

        <div>
          <label className="px-label block mb-1.5" htmlFor="room-settings-max-players">최대 인원</label>
          <select id="room-settings-max-players" className="px-input" value={maxPlayers} onChange={(e) => setMaxPlayers(parseInt(e.target.value))}>
            {selectableMaxPlayers.map((count) => (
              <option key={count} value={count}>
                {count}명
              </option>
            ))}
          </select>
          <p className="px-label text-[10px] mt-1">현재 인원({currentPlayers}명)보다 적게 줄일 수 없습니다.</p>
        </div>

        <div>
          <label className="px-label block mb-1.5" htmlFor="room-settings-total-round">{isSingleRound(gameType) ? '게임 방식' : '퀴즈 수'}</label>
          <select id="room-settings-total-round" className="px-input" value={totalRound} onChange={(e) => setTotalRound(parseInt(e.target.value))}>
            {roundOptionsFor(gameType).map((count) => (
              <option key={count} value={count}>
                {isSingleRound(gameType) ? '단판' : `${count}문제`}
              </option>
            ))}
          </select>
        </div>

        <p className="px-label text-[10px]">설정을 바꾸면 모든 참가자의 준비 상태가 해제됩니다.</p>

        <div className="flex items-center gap-2 pt-1">
          <button type="submit" className="px-btn px-btn-primary flex-1 py-2.5">
            저장
          </button>
          <button type="button" className="px-btn px-btn-paper flex-1 py-2.5" onClick={onClose}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
};

export default RoomSettingsModal;
