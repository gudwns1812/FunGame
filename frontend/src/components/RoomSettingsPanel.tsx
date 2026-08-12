import React, { useEffect, useState } from 'react';
import type { RoomSettings } from '../types/game';

interface RoomSettingsPanelProps {
  settings: RoomSettings;
  isHost: boolean;
  currentPlayers: number;
  onChange: (changes: Omit<RoomSettings, 'gameType' | 'host'>) => void;
}

const CATEGORIES = ['TOTAL', 'KPOP', 'POP', 'BALLAD', 'RAP', 'OST'];
const DIFFICULTIES = [
  { value: 1, label: '쉬움' },
  { value: 2, label: '보통' },
  { value: 3, label: '어려움' },
];

const GAME_TYPE_LABELS: Record<string, string> = {
  SONG: '노래 퀴즈',
  CS: 'CS 퀴즈',
  HANGMAN: '행맨',
};

const RoomSettingsPanel: React.FC<RoomSettingsPanelProps> = ({ settings, isHost, currentPlayers, onChange }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(settings);

  useEffect(() => {
    setDraft(settings);
  }, [settings]);

  const roundLabel = settings.gameType === 'SONG' ? '곡 수' : '문제 수';

  const summary = (
    <div className="flex flex-wrap items-center gap-2">
      <span className="px-chip px-chip-sea">{GAME_TYPE_LABELS[settings.gameType] ?? settings.gameType}</span>
      <span className="px-chip">
        정원 <span className="num">{settings.maxPlayers}</span>
      </span>
      {settings.gameType === 'SONG' && <span className="px-chip">{settings.category}</span>}
      {settings.gameType !== 'HANGMAN' && (
        <span className="px-chip">
          {roundLabel} <span className="num">{settings.totalRound}</span>
        </span>
      )}
      {settings.gameType === 'HANGMAN' && (
        <span className="px-chip">{DIFFICULTIES.find((d) => d.value === settings.difficulty)?.label ?? '보통'}</span>
      )}
    </div>
  );

  if (!isEditing) {
    return (
      <div className="px-card shrink-0 px-3 py-2.5 flex flex-wrap items-center justify-between gap-2">
        <div className="min-w-0 flex flex-wrap items-center gap-2">
          <span className="px-title text-sm truncate">{settings.title}</span>
          {summary}
        </div>

        {isHost && (
          <button className="px-btn px-btn-sm px-btn-paper" onClick={() => setIsEditing(true)}>
            설정 변경
          </button>
        )}
      </div>
    );
  }

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    onChange({
      title: draft.title,
      maxPlayers: draft.maxPlayers,
      category: draft.category,
      totalRound: draft.totalRound,
      difficulty: draft.difficulty,
    });
    setIsEditing(false);
  };

  return (
    <form className="px-card shrink-0 px-3 py-2.5 flex flex-col gap-2.5" onSubmit={submit}>
      <div className="flex flex-col gap-1">
        <label className="px-label text-[10px]">방 제목</label>
        <input
          className="px-input"
          value={draft.title}
          maxLength={30}
          onChange={(e) => setDraft({ ...draft, title: e.target.value })}
        />
      </div>

      <div className="flex flex-wrap gap-2.5">
        <div className="flex flex-col gap-1">
          <label className="px-label text-[10px]">정원</label>
          <input
            className="px-input w-24 num"
            type="number"
            min={currentPlayers}
            max={12}
            value={draft.maxPlayers}
            onChange={(e) => setDraft({ ...draft, maxPlayers: Number(e.target.value) })}
          />
        </div>

        {draft.gameType === 'SONG' && (
          <div className="flex flex-col gap-1">
            <label className="px-label text-[10px]">카테고리</label>
            <select
              className="px-input"
              value={draft.category ?? 'TOTAL'}
              onChange={(e) => setDraft({ ...draft, category: e.target.value })}>
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>
        )}

        {draft.gameType !== 'HANGMAN' && (
          <div className="flex flex-col gap-1">
            <label className="px-label text-[10px]">{roundLabel}</label>
            <input
              className="px-input w-24 num"
              type="number"
              min={1}
              max={30}
              value={draft.totalRound}
              onChange={(e) => setDraft({ ...draft, totalRound: Number(e.target.value) })}
            />
          </div>
        )}

        {draft.gameType === 'HANGMAN' && (
          <div className="flex flex-col gap-1">
            <label className="px-label text-[10px]">난이도</label>
            <select
              className="px-input"
              value={draft.difficulty}
              onChange={(e) => setDraft({ ...draft, difficulty: Number(e.target.value) })}>
              {DIFFICULTIES.map((difficulty) => (
                <option key={difficulty.value} value={difficulty.value}>
                  {difficulty.label}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <p className="px-label text-[10px]">설정을 바꾸면 모든 참가자의 준비 상태가 해제됩니다.</p>

      <div className="flex items-center gap-2">
        <button type="submit" className="px-btn px-btn-sm px-btn-primary">
          저장
        </button>
        <button
          type="button"
          className="px-btn px-btn-sm px-btn-paper"
          onClick={() => {
            setDraft(settings);
            setIsEditing(false);
          }}>
          취소
        </button>
      </div>
    </form>
  );
};

export default RoomSettingsPanel;
