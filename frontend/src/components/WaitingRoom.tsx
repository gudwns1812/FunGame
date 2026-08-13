import React, { useState, useEffect, useRef } from 'react';
import type { Player, RoomSettings } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';
import LogList from './LogList';
import RoomSettingsModal from './RoomSettingsModal';
import RoomSettingsSummary from './RoomSettingsSummary';

interface WaitingRoomProps {
  players: Player[];
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  isHost: boolean;
  logs: string[];
  onSendMessage: (message: string) => void;
  maxPlayers: number;
  roomSettings: RoomSettings | null;
  onChangeSettings: (changes: Omit<RoomSettings, 'title' | 'hostMemberId' | 'hostNickname'>) => void;
  myMemberId: number | null;
}

const WaitingRoom: React.FC<WaitingRoomProps> = ({
  players,
  onStart,
  onLeave,
  onToggleReady,
  isHost,
  logs,
  onSendMessage,
  maxPlayers,
  roomSettings,
  onChangeSettings,
  myMemberId,
}) => {
  const [chatInput, setChatInput] = useState('');
  const [isEditingSettings, setIsEditingSettings] = useState(false);
  const logContainerRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLInputElement>(null);
  const SLOTS = maxPlayers || 12;
  const slotsArray = Array.from({ length: SLOTS }, (_, i) => players[i] || null);

  useEffect(() => {
    setTimeout(() => {
      if (logContainerRef.current) {
        logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
      }
    }, 100);
  }, [logs]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && document.activeElement !== chatInputRef.current) {
        e.preventDefault();
        chatInputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleChatSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (chatInput.trim()) {
      onSendMessage(chatInput.trim());
      setChatInput('');
    }
  };

  const readyCount = players.filter((p) => p.isReady || p.isHost).length;

  return (
    <div className="h-full min-h-0 max-w-5xl w-full mx-auto flex flex-col gap-3">
      {roomSettings && (
        <RoomSettingsSummary settings={roomSettings} isHost={isHost} onEdit={() => setIsEditingSettings(true)} />
      )}

      {roomSettings && isEditingSettings && (
        <RoomSettingsModal
          settings={roomSettings}
          currentPlayers={players.length}
          onSubmit={(changes) => {
            onChangeSettings(changes);
            setIsEditingSettings(false);
          }}
          onClose={() => setIsEditingSettings(false)}
        />
      )}

      {/* 상단: 인원 · 준비 현황 + 액션 */}
      <div className="px-card shrink-0 px-3 py-2.5 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="px-chip px-chip-sea">
            인원 <span className="num">{players.length}</span> / <span className="num">{SLOTS}</span>
          </span>
          <span className={`px-chip ${readyCount === players.length && players.length > 0 ? 'px-chip-grass' : ''}`}>
            준비 <span className="num">{readyCount}</span>
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button className="px-btn px-btn-sm px-btn-paper" onClick={onLeave}>
            나가기
          </button>

          {!isHost &&
            (() => {
              const me = players.find((p) => p.memberId === myMemberId);
              const amIReady = me?.isReady || false;

              return (
                <button
                  className={`px-btn px-btn-sm ${amIReady ? 'px-btn-paper' : 'px-btn-grass'}`}
                  onClick={onToggleReady}>
                  {amIReady ? '준비 취소' : '준비 완료'}
                </button>
              );
            })()}

          {isHost && (
            <button className="px-btn px-btn-sm px-btn-primary" onClick={onStart} disabled={players.length < 1}>
              게임 시작 ▶
            </button>
          )}
        </div>
      </div>

      {/* 참가자 슬롯 */}
      <div className="px-card shrink-0 max-h-[42vh] scroll-y custom-scrollbar p-2.5">
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2.5">
          {slotsArray.map((player, index) => {
            const color = player ? getPlayerColor(player.colorIndex ?? null) || '#0c6780' : '#a89f9f';
            const isFilled = !!player;
            const isReady = player?.isReady || player?.isHost;

            if (!isFilled) {
              return (
                <div
                  key={index}
                  className="h-[72px] border-2 border-dashed border-ink/35 bg-white/30 flex items-center justify-center">
                  <span className="px-label text-[10px]">빈 자리</span>
                </div>
              );
            }

            return (
              <div key={index} className="px-card-sm h-[72px] px-2.5 flex items-center gap-2.5 animate-pop relative">
                <span className="absolute top-0 left-0 px-label text-[9px] bg-ink text-paper px-1 leading-4">
                  {String(index + 1).padStart(2, '0')}
                </span>

                <span
                  className="w-9 h-9 shrink-0 border-2 border-ink flex items-center justify-center font-display text-sm text-white"
                  style={{ background: color }}>
                  {stripTag(player.name).slice(0, 1)}
                </span>

                <div className="min-w-0 flex-1 pt-1">
                  <p className="px-title text-[13px] truncate">{stripTag(player.name)}</p>
                  <span
                    className={`px-chip mt-0.5 px-1.5 py-0 text-[10px] ${
                      player.isHost ? 'px-chip-gold' : isReady ? 'px-chip-grass' : ''
                    }`}>
                    {player.isHost ? '방장' : isReady ? '준비' : '대기'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 로그 + 채팅 입력 */}
      <div className="px-card flex-1 min-h-0 flex flex-col overflow-hidden">
        <div className="px-head shrink-0">
          <span>채팅</span>
          <span className="w-2 h-2 bg-grass animate-blink" />
        </div>

        <LogList
          logs={logs}
          players={players}
          containerRef={logContainerRef}
          className="flex-1 min-h-0"
          emptyText="게임 시작 전에 인사라도 한마디"
        />

        <form onSubmit={handleChatSubmit} className="shrink-0 border-t-[3px] border-ink p-2 flex gap-2 bg-paper-2">
          <input
            ref={chatInputRef}
            type="text"
            className="px-input flex-1 py-2 border-2"
            placeholder="메시지 입력..."
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            autoFocus
          />
          <button type="submit" className="px-btn px-btn-sm px-btn-sea">
            전송
          </button>
        </form>
      </div>
    </div>
  );
};

export default WaitingRoom;
