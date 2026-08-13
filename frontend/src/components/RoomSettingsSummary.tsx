import React from 'react';
import type { RoomSettings } from '../types/game';
import { CATEGORIES, CS_DIFFICULTIES, GAME_TYPES, isSingleRound } from '../utils/gameOptions';

interface RoomSettingsSummaryProps {
  settings: RoomSettings;
  isHost: boolean;
  onEdit: () => void;
}

const labelOf = (options: { value: string; label: string }[], value: string | null) =>
  options.find((option) => option.value === value)?.label ?? value;

const RoomSettingsSummary: React.FC<RoomSettingsSummaryProps> = ({ settings, isHost, onEdit }) => {
  return (
    <div className="px-card shrink-0 px-3 py-2.5 flex flex-wrap items-center justify-between gap-2">
      <div className="min-w-0 flex flex-wrap items-center gap-2">
        <span className="px-title text-sm truncate">{settings.title}</span>
        <span className="px-chip px-chip-sea">{labelOf(GAME_TYPES, settings.gameType)}</span>
        <span className="px-chip">
          정원 <span className="num">{settings.maxPlayers}</span>
        </span>

        {settings.gameType === 'SONG' && <span className="px-chip">{labelOf(CATEGORIES, settings.category)}</span>}

        {settings.gameType === 'CS' && (
          <span className="px-chip">난이도 {labelOf(CS_DIFFICULTIES, settings.csDifficulty)}</span>
        )}

        {settings.gameType === 'HANGMAN' ? (
          <span className="px-chip">
            난이도 <span className="num">{settings.difficulty}</span>
          </span>
        ) : (
          <span className="px-chip">
            {isSingleRound(settings.gameType) ? '단판' : <><span className="num">{settings.totalRound}</span>문제</>}
          </span>
        )}
      </div>

      {isHost && (
        <button className="px-btn px-btn-sm px-btn-paper" onClick={onEdit}>
          설정 변경
        </button>
      )}
    </div>
  );
};

export default RoomSettingsSummary;
