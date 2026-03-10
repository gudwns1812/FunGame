import React, { useState, useEffect, useRef } from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

interface WaitingRoomProps {
  players: Player[];
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  isHost: boolean;
  logs: string[];
  onSendMessage: (message: string) => void;
}

const WaitingRoom: React.FC<WaitingRoomProps> = ({ players, onStart, onLeave, onToggleReady, isHost, logs, onSendMessage }) => {
  const [chatInput, setChatInput] = useState('');
  const logContainerRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLInputElement>(null);
  const SLOTS = 8;
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

  const renderLog = (log: string, i: number) => {
    if (log.startsWith('[시스템]') || log.startsWith('[오류]')) {
      return (
        <p key={i} className={`font-mono text-xs py-0.5 ${log.startsWith('[오류]') ? 'text-red-400' : 'text-slate-400'}`}>
          <span className="opacity-50">[{new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}]</span> {log}
        </p>
      );
    }
    const colonIdx = log.indexOf(':');
    if (colonIdx > 0) {
      const senderName = log.substring(0, colonIdx);
      const rest = log.substring(colonIdx + 1);
      const player = players.find(p => stripTag(p.name) === senderName || p.name === senderName);
      const color = getPlayerColor(player?.colorIndex ?? null) || '#25c0f4';
      return (
        <p key={i} className="font-mono text-xs text-slate-200">
          <span className="opacity-30">[{new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}]</span>{' '}
          <span style={{ color }} className="font-bold">{senderName}</span>
          <span className="opacity-50">:</span> {rest}
        </p>
      );
    }
    return <p key={i} className="font-mono text-xs text-slate-200">{log}</p>;
  };

  return (
    <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-3 gap-6">

      {/* 왼쪽: 채팅 터미널 */}
      <div className="lg:col-span-1 panel-border bg-slate-900/60 rounded-xl flex flex-col h-[600px] overflow-hidden">
        <div className="bg-primary/10 border-b border-primary/30 p-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-sm">terminal</span>
            <h3 className="text-xs font-bold text-primary tracking-widest uppercase">채팅 터미널</h3>
          </div>
          <span className="flex h-2 w-2 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
          </span>
        </div>

        <div ref={logContainerRef} className="flex-1 overflow-y-auto p-4 flex flex-col gap-1 space-y-1 bg-black/20">
          <div className="text-xs font-mono text-primary/50 mb-4 border-b border-primary/20 pb-2 uppercase tracking-widest font-bold">
            Secure Link: Established<br />
          </div>
          {logs.map((log, i) => renderLog(log, i))}
        </div>

        <form onSubmit={handleChatSubmit} className="p-3 border-t border-primary/30 bg-slate-950 flex gap-2">
          <div className="flex-1 relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-primary/50 font-mono text-sm">{'>'}</span>
            <input
              ref={chatInputRef}
              type="text"
              className="w-full bg-slate-900 border border-primary/30 rounded pl-8 pr-3 py-2 text-sm text-white focus:border-primary outline-none font-mono transition-colors"
              placeholder="메시지 입력..."
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              autoFocus
            />
          </div>
          <button type="submit" className="bg-primary/20 hover:bg-primary/40 text-primary border border-primary/50 rounded px-4 transition-colors">
            <span className="material-symbols-outlined text-sm">send</span>
          </button>
        </form>
      </div>

      {/* 오른쪽: 플레이어 슬롯 및 컨트롤 */}
      <div className="lg:col-span-2 flex flex-col gap-6">

        {/* 상단 컨트롤 */}
        <div className="panel-border bg-slate-900/60 rounded-xl p-6 flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="space-y-1 text-center sm:text-left">
            <h2 className="text-2xl font-black text-white tracking-tighter uppercase flex items-center justify-center sm:justify-start gap-2">
              <span className="material-symbols-outlined text-primary text-3xl">meeting_room</span>
              게임 <span className="text-primary neon-glow">대기실</span>
            </h2>
            <p className="text-xs text-slate-400 font-mono uppercase tracking-widest">Pilots Boarded: {players.length} / {SLOTS}</p>
          </div>

          <div className="flex gap-3 w-full sm:w-auto">
            <button
              className="flex-1 sm:flex-none px-6 py-3 border border-red-500/50 text-red-400 font-bold rounded-lg hover:bg-red-500/10 transition-colors flex items-center justify-center gap-2 uppercase tracking-widest text-xs"
              onClick={onLeave}
            >
              <span className="material-symbols-outlined text-sm">logout</span>
              방 나가기
            </button>

            {!isHost && (() => {
              const me = players.find(p => stripTag(p.name) === localStorage.getItem('ums_nickname') || p.name === localStorage.getItem('ums_nickname'));
              const amIReady = me?.isReady || false;
              
              return (
                <button
                  className={`flex-1 sm:flex-none px-8 py-3 border-2 font-black rounded-lg transition-all flex items-center justify-center gap-2 shadow-lg uppercase tracking-widest text-xs ${
                    amIReady 
                    ? 'bg-red-500/20 border-red-500/50 text-red-400 hover:bg-red-500/30' 
                    : 'bg-primary/20 border-primary/50 text-primary hover:bg-primary/40'
                  }`}
                  onClick={onToggleReady}
                >
                  <span className="material-symbols-outlined text-sm">
                    {amIReady ? 'cancel' : 'check_circle'}
                  </span>
                  {amIReady ? '준비 취소' : '준비하기'}
                </button>
              );
            })()}

            {isHost && (
              <button
                className="flex-1 sm:flex-none px-10 py-3 bg-primary text-background-dark font-black rounded-lg hover:bg-primary/90 transition-all transform hover:scale-105 flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(37,192,244,0.4)] disabled:opacity-50 disabled:cursor-not-allowed uppercase tracking-widest text-xs"
                onClick={onStart}
                disabled={players.length < 1}
              >
                <span className="material-symbols-outlined text-sm">play_arrow</span>
                게임 시작
              </button>
            )}
          </div>
        </div>

        {/* 슬롯 그리드 */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 flex-1">
          {slotsArray.map((player, index) => {
            const color = player ? (getPlayerColor(player.colorIndex ?? null) || '#25c0f4') : 'rgba(37, 192, 244, 0.1)';
            const isFilled = !!player;
            const isReady = player?.isReady || player?.isHost;

            return (
              <div
                key={index}
                className={`panel-border rounded-2xl flex flex-col items-center justify-center p-6 relative overflow-hidden transition-all duration-500 ${isFilled ? 'bg-slate-800/80 shadow-[inset_0_0_20px_rgba(0,0,0,0.5)]' : 'bg-slate-900/30 border-dashed opacity-40'}`}
              >
                {/* 준비 완료 상태 네온 효과 */}
                {isFilled && isReady && (
                  <div className="absolute inset-0 bg-primary/5 animate-pulse"></div>
                )}

                <div
                  className={`w-16 h-16 rounded-full flex items-center justify-center border-2 mb-4 z-10 transition-all duration-500`}
                  style={{
                    borderColor: isFilled ? (isReady ? '#25c0f4' : color) : 'rgba(37, 192, 244, 0.2)',
                    backgroundColor: isFilled ? 'rgba(16, 30, 34, 0.9)' : 'transparent',
                    boxShadow: isFilled && isReady ? '0 0 20px rgba(37, 192, 244, 0.4)' : 'none'
                  }}
                >
                  <span className={`material-symbols-outlined text-3xl transition-all ${isFilled && isReady ? 'text-primary scale-110' : ''}`} style={{ color: isFilled ? (isReady ? '#25c0f4' : color) : 'rgba(37, 192, 244, 0.2)' }}>
                    {isFilled ? (isReady ? 'verified' : 'person') : 'person_off'}
                  </span>
                </div>

                <div className="text-center z-10 w-full px-2">
                  <span
                    className={`block font-black text-sm truncate uppercase tracking-widest ${isFilled ? (isReady ? 'text-white' : 'text-slate-300') : 'text-primary/20'}`}
                    style={isFilled && isReady ? { textShadow: '0 0 10px rgba(37, 192, 244, 0.8)' } : undefined}
                  >
                    {isFilled ? stripTag(player.name) : 'EMPTY'}
                  </span>

                  {isFilled && (
                    <div className={`mt-3 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-[0.2em] border transition-all
                      ${player.isHost
                        ? 'bg-primary/20 text-primary border-primary/50 shadow-[0_0_10px_rgba(37,192,244,0.2)]'
                        : isReady
                          ? 'bg-green-500/20 text-green-400 border-green-500/50 shadow-[0_0_10px_rgba(74,222,128,0.2)]'
                          : 'bg-slate-900 text-slate-500 border-white/5'
                      }`}
                    >
                      {player.isHost ? (
                        <><span className="material-symbols-outlined text-[10px]">stars</span> Commander</>
                      ) : isReady ? (
                        <><span className="material-symbols-outlined text-[10px]">check_circle</span> Ready</>
                      ) : (
                        'Standby'
                      )}
                    </div>
                  )}
                </div>

                <div className="absolute top-3 left-3 text-[8px] font-mono text-primary/30 font-black">
                  SLOT_0{index + 1}
                </div>
              </div>
            );
          })}
        </div>

      </div>
    </div>
  );
};

export default WaitingRoom;
