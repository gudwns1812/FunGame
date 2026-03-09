import React, { useState, useEffect, useRef } from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

interface WaitingRoomProps {
  players: Player[];
  onStart: () => void;
  onLeave: () => void;
  isHost: boolean;
  logs: string[];
  onSendMessage: (message: string) => void;
}

const WaitingRoom: React.FC<WaitingRoomProps> = ({ players, onStart, onLeave, isHost, logs, onSendMessage }) => {
  const [chatInput, setChatInput] = useState('');
  const logContainerRef = useRef<HTMLDivElement>(null);
  const SLOTS = 8;
  const slotsArray = Array.from({ length: SLOTS }, (_, i) => players[i] || null);

  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs]);

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
        <p key={i} className={`font-mono text-xs ${log.startsWith('[오류]') ? 'text-red-400' : 'text-slate-400'}`}>
          <span className="opacity-50">[{new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute:'2-digit', second:'2-digit' })}]</span> {log}
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
          <span className="opacity-30">[{new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute:'2-digit', second:'2-digit' })}]</span>{' '}
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
        
        <div 
          ref={logContainerRef}
          className="flex-1 overflow-y-auto p-4 flex flex-col gap-1 space-y-1"
        >
          <div className="text-xs font-mono text-primary/50 mb-4 border-b border-primary/20 pb-2">
            보안 채널이 연결되었습니다.<br/>
            메시지 수신 대기 중...
          </div>
          {logs.map((log, i) => renderLog(log, i))}
        </div>

        <form onSubmit={handleChatSubmit} className="p-3 border-t border-primary/30 bg-black/40 flex gap-2">
          <div className="flex-1 relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-primary/50 font-mono text-sm">{'>'}</span>
            <input
              type="text"
              className="w-full bg-slate-950 border border-primary/30 rounded pl-8 pr-3 py-2 text-sm text-white focus:border-primary outline-none font-mono transition-colors"
              placeholder="메시지 입력..."
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              autoFocus
            />
          </div>
          <button type="submit" className="bg-primary/20 hover:bg-primary/40 text-primary border border-primary/50 rounded px-4 transition-colors flex items-center justify-center">
            <span className="material-symbols-outlined text-sm">send</span>
          </button>
        </form>
      </div>

      {/* 오른쪽: 플레이어 슬롯 및 컨트롤 */}
      <div className="lg:col-span-2 flex flex-col gap-6">
        
        {/* 상단 컨트롤 */}
        <div className="panel-border bg-slate-900/60 rounded-xl p-6 flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="space-y-1">
            <h2 className="text-2xl font-black text-white tracking-tighter uppercase flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-3xl">meeting_room</span>
              게임 <span className="text-primary neon-glow">대기실</span>
            </h2>
            <p className="text-xs text-slate-400 font-mono">참가 인원: {players.length} / {SLOTS}</p>
          </div>
          
          <div className="flex gap-3">
            <button 
              className="px-6 py-3 border border-red-500/50 text-red-400 font-bold rounded hover:bg-red-500/10 transition-colors flex items-center gap-2"
              onClick={onLeave}
            >
              <span className="material-symbols-outlined text-sm">logout</span>
              방 나가기
            </button>
            {isHost && (
              <button 
                className="px-8 py-3 bg-primary text-background-dark font-black rounded hover:bg-primary/90 transition-all transform hover:scale-105 flex items-center gap-2 shadow-[0_0_15px_rgba(37,192,244,0.3)] disabled:opacity-50 disabled:cursor-not-allowed"
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
            
            return (
              <div
                key={index}
                className={`panel-border rounded-xl flex flex-col items-center justify-center p-6 relative overflow-hidden transition-all duration-500 ${isFilled ? 'bg-slate-800/80' : 'bg-slate-900/30 border-dashed opacity-50'}`}
              >
                {isFilled && (
                  <div className="absolute inset-0 opacity-10" style={{ background: `radial-gradient(circle at center, ${color} 0%, transparent 70%)` }}></div>
                )}
                
                <div 
                  className={`w-16 h-16 rounded-full flex items-center justify-center border-2 mb-3 z-10 transition-all duration-500`}
                  style={{ 
                    borderColor: color,
                    backgroundColor: isFilled ? 'rgba(16, 30, 34, 0.8)' : 'transparent',
                    boxShadow: isFilled ? `0 0 15px ${color}40` : 'none'
                  }}
                >
                  <span className="material-symbols-outlined text-3xl" style={{ color: isFilled ? color : 'rgba(37, 192, 244, 0.3)' }}>
                    {isFilled ? 'person' : 'person_off'}
                  </span>
                </div>

                <div className="text-center z-10 w-full">
                  <span 
                    className={`block font-bold text-sm truncate uppercase tracking-wider ${isFilled ? 'text-white' : 'text-primary/30'}`}
                    style={isFilled ? { textShadow: `0 0 10px ${color}80` } : undefined}
                  >
                    {isFilled ? stripTag(player.name) : '비어 있음'}
                  </span>
                  
                  {player?.isHost ? (
                    <div className="mt-2 inline-flex items-center gap-1 text-[9px] font-bold bg-primary/20 text-primary border border-primary/50 px-2 py-0.5 rounded uppercase tracking-widest">
                      <span className="material-symbols-outlined text-[10px]">stars</span> 방장
                    </div>
                  ) : (
                    <div className="mt-2 h-4"></div>
                  )}
                </div>

                <div className="absolute top-2 left-2 text-[8px] font-mono text-primary/40 font-bold">
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
