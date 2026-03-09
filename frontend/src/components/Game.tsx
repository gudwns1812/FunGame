import React, { useState, useEffect, useRef } from 'react';
import YouTube from 'react-youtube';
import type { Player, GameStartInfo, RoundEndInfo } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

interface GameProps {
  players: Player[];
  roomId: string;
  timeLeft: number;
  totalTime: number;
  currentVideoId: string;
  onAnswerSubmit: (answer: string) => void;
  onSkipRound: () => void;
  onFetchRank: () => Promise<void>;
  gameStartInfo: GameStartInfo | null;
  gameType: string | null;
  roundEndInfo: RoundEndInfo | null;
  currentRound: number;
  totalRound: number;
  logs: string[];
}

const Game: React.FC<GameProps> = ({
  players,
  timeLeft,
  totalTime,
  currentVideoId,
  onAnswerSubmit,
  onSkipRound,
  onFetchRank,
  gameStartInfo,
  gameType,
  roundEndInfo,
  currentRound,
  totalRound,
  logs,
}) => {
  const [answer, setAnswer] = useState('');
  const logContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const progressPercent = Math.max(0, Math.min(100, (timeLeft / totalTime) * 100));

  useEffect(() => {
    onFetchRank();
  }, []);

  useEffect(() => {
    // 채팅이나 로그가 추가된 후 확실하게 스크롤을 내리도록 타임아웃 100ms 적용
    setTimeout(() => {
      if (logContainerRef.current) {
        logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
      }
    }, 100);
  }, [logs]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && document.activeElement !== inputRef.current) {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (answer.trim()) {
      onAnswerSubmit(answer.trim());
      setAnswer('');
    }
  };

  const sortedPlayers = [...players].sort((a, b) => b.score - a.score);

  const renderChatLog = (log: string, i: number) => {
    if (log.startsWith('[시스템]') || log.startsWith('[오류]')) {
      return (
        <p key={i} className={`font-mono text-xs py-1 ${log.startsWith('[오류]') ? 'text-red-400' : 'text-slate-400'}`}>
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
        <p key={i} className="font-mono text-sm py-1.5 text-slate-200 border-b border-white/5 last:border-0">
          <span className="opacity-30 text-[10px]">[{new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}]</span>{' '}
          <span style={{ color }} className="font-bold">{senderName}</span>
          <span className="opacity-50 mx-1">:</span> {rest}
        </p>
      );
    }
    return <p key={i} className="font-mono text-sm py-1 text-slate-200">{log}</p>;
  };

  const renderSongPanel = () => {
    if (roundEndInfo) {
      return (
        <div className="flex flex-col items-center justify-center gap-8 h-full p-12 text-center animate-in fade-in duration-500">
          <span className="material-symbols-outlined text-8xl text-green-400 drop-shadow-[0_0_20px_rgba(74,222,128,0.6)]">check_circle</span>
          <div className="space-y-4">
            <p className="text-sm text-green-400 uppercase tracking-[0.3em] font-bold">TARGET_IDENTIFIED</p>
            <p className="text-4xl md:text-2xl font-semibold text-white tracking-tight uppercase drop-shadow-lg">{roundEndInfo.answer}</p>
          </div>
          {roundEndInfo.winner && roundEndInfo.winner !== '없음' ? (
            <div className="mt-6 inline-flex items-center gap-4 bg-primary/10 border border-primary/30 px-8 py-4 rounded-full">
              <span className="text-xs text-primary uppercase tracking-widest font-bold">맞춘 사람</span>
              <span className="text-2xl font-black text-white uppercase">{stripTag(roundEndInfo.winner)}</span>
            </div>
          ) : (
            <div className="mt-6 inline-flex items-center gap-3 bg-red-500/10 border border-red-500/30 px-8 py-4 rounded-full">
              <span className="text-sm font-bold text-red-400 uppercase tracking-widest">정답자 없음</span>
            </div>
          )}
        </div>
      );
    }

    if (gameStartInfo) {
      return (
        <div className="flex flex-col items-center justify-center gap-10 h-full p-12 text-center animate-in fade-in duration-1000">
          <span className="material-symbols-outlined text-7xl text-primary animate-pulse neon-glow">graphic_eq</span>
          <div className="max-w-3xl">
            <p className="text-3xl font-black text-white tracking-widest uppercase leading-tight neon-glow">{gameStartInfo.message}</p>
          </div>
          <div className="grid grid-cols-3 gap-12 text-center mt-10 w-full max-w-2xl border-t border-primary/20 pt-10">
            <div className="flex flex-col gap-2">
              <p className="text-xs text-primary/60 uppercase tracking-widest font-bold">카테고리</p>
              <p className="text-2xl font-black text-primary">{gameStartInfo.category || '전체'}</p>
            </div>
            <div className="flex flex-col gap-2 border-x border-primary/20 px-4">
              <p className="text-xs text-primary/60 uppercase tracking-widest font-bold">총 문제 수</p>
              <p className="text-2xl font-black text-primary">{gameStartInfo.songCount}문항</p>
            </div>
            <div className="flex flex-col gap-2">
              <p className="text-xs text-primary/60 uppercase tracking-widest font-bold">게임 모드</p>
              <p className="text-2xl font-black text-primary">{gameStartInfo.gameType === 'SONG' ? '음악' : 'CS'}</p>
            </div>
          </div>
        </div>
      );
    }

    if (gameType === 'CS' && currentVideoId) {
      return (
        // justify-start -> justify-center, text-left -> text-center 로 변경되었습니다.
        <div className="flex flex-col items-center justify-center gap-4 h-full p-8 overflow-y-auto custom-scrollbar text-center w-full">
          <p className="text-[10px] text-primary uppercase tracking-[0.3em] font-normal shrink-0">CS QUIZ</p>
          <div className="text-2xl md:text-xl font-black text-slate-300 leading-relaxed max-w-4xl break-keep whitespace-pre-wrap">
            {currentVideoId}
          </div>
        </div>
      );
    }

    return (
      <div className="flex flex-col items-center justify-center gap-6 h-full relative w-full overflow-hidden group">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(37,192,244,0.15)_0%,transparent_70%)] opacity-50 group-hover:opacity-100 transition-opacity duration-1000 pointer-events-none"></div>
        <div className="relative z-10 flex flex-col items-center">
          <span className="material-symbols-outlined text-9xl text-primary/20 mb-6 animate-pulse">music_note</span>
          <p className="text-primary/30 text-3xl font-mono tracking-[0.6em] font-black uppercase">Analyzing signal...</p>
        </div>
      </div>
    );
  };

  return (
    <div className="w-full max-w-7xl mx-auto h-full flex flex-col lg:flex-row gap-6 overflow-hidden">

      {/* ── 좌측: 랭킹 및 정보 패널 ── */}
      <div className="lg:w-80 shrink-0 flex flex-col gap-6 h-full">
        {/* 타이머 섹션 */}
        <div className="panel-border bg-slate-900/80 rounded-xl p-6 flex flex-col gap-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xs font-bold text-primary uppercase tracking-widest flex items-center gap-2">
              <span className="material-symbols-outlined text-sm">timer</span> 시스템 카운트다운
            </h2>
            <span className={`text-2xl font-mono font-black ${timeLeft <= 5 ? 'text-red-500 animate-pulse' : 'text-white'}`}>
              {timeLeft}s
            </span>
          </div>
          <div className="w-full h-3 bg-slate-800 rounded-full overflow-hidden border border-white/5">
            <div
              className={`h-full transition-all duration-1000 ease-linear ${progressPercent < 30 ? 'bg-red-500 shadow-[0_0_12px_rgba(239,68,68,0.8)]' : 'bg-primary shadow-[0_0_12px_rgba(37,192,244,0.8)]'}`}
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <div className="flex justify-between items-center mt-1">
            <span className="text-[10px] font-mono text-slate-500 uppercase tracking-widest">Sys_Readiness</span>
            <span className="text-[10px] font-mono text-slate-500">{progressPercent.toFixed(1)}%</span>
          </div>
        </div>

        {/* 랭킹 보드 */}
        <div className="panel-border bg-slate-900/60 rounded-xl flex flex-col flex-1 overflow-hidden min-h-0">
          <div className="bg-primary/10 border-b border-primary/30 p-5 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">leaderboard</span>
              <h3 className="text-xs font-bold text-primary tracking-widest uppercase">실시간 랭킹</h3>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-3 space-y-2 custom-scrollbar">
            {sortedPlayers.map((p, idx) => {
              const color = getPlayerColor(p.colorIndex ?? null) || '#25c0f4';
              const isFirst = idx === 0 && p.score > 0;
              return (
                <div
                  key={p.id}
                  className={`flex justify-between items-center p-4 rounded-lg border-l-4 transition-all duration-300
                    ${isFirst ? 'bg-primary/20 border-primary shadow-[inset_0_0_15px_rgba(37,192,244,0.2)]' : 'bg-slate-950/40 border-transparent hover:bg-slate-800/60'}`}
                  style={!isFirst ? { borderLeftColor: color } : undefined}
                >
                  <div className="flex items-center gap-4 min-w-0">
                    <span className="text-xs font-mono font-bold opacity-40 shrink-0">#{idx + 1}</span>
                    <span className="font-bold truncate uppercase text-sm" style={{ color: isFirst ? '#ffffff' : color, textShadow: isFirst ? `0 0 10px ${color}` : 'none' }}>
                      {stripTag(p.name)}
                    </span>
                  </div>
                  <span className="font-mono font-bold text-white text-base shrink-0">{p.score}</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* ── 우측: 메인 디스플레이 & 채팅 ── */}
      <div className="flex-1 flex flex-col gap-6 min-w-0 h-full overflow-hidden">

        {/* 노래 정보 패널 (550px 고정) */}
        <div className="panel-border bg-slate-950 rounded-2xl relative flex flex-col overflow-hidden h-[550px] shrink-0 shadow-[0_0_40px_rgba(0,0,0,0.7)]">
          <div className="absolute top-0 left-0 w-16 h-16 border-l-2 border-t-2 border-primary/40 m-6"></div>
          <div className="absolute top-0 right-0 w-16 h-16 border-r-2 border-t-2 border-primary/40 m-6"></div>
          <div className="absolute bottom-0 left-0 w-16 h-16 border-l-2 border-b-2 border-primary/40 m-6"></div>
          <div className="absolute bottom-0 right-0 w-16 h-16 border-r-2 border-b-2 border-primary/40 m-6"></div>

          <div className="flex-1 relative z-10">
            {renderSongPanel()}
          </div>

          <div className="absolute bottom-0 w-full bg-slate-900/95 border-t border-primary/30 p-4 flex justify-between items-center px-10 z-20">
            <div className="flex items-center gap-4">
              <span className="w-3 h-3 rounded-full bg-red-500 animate-pulse shadow-[0_0_8px_rgba(239,68,68,0.8)]"></span>
              <span className="text-xs font-mono text-slate-400 uppercase tracking-[0.3em] font-black">Signal Decoder: ONLINE</span>
            </div>
            {currentRound > 0 && (
              <span className="text-sm font-black text-primary tracking-widest uppercase neon-glow">
                TARGET {currentRound} <span className="opacity-40">/ {totalRound}</span>
              </span>
            )}
          </div>

          <div style={{ position: 'absolute', width: 1, height: 1, opacity: 0, pointerEvents: 'none', overflow: 'hidden' }}>
            <YouTube
              key={currentVideoId}
              videoId={currentVideoId}
              opts={{ height: '1', width: '1', playerVars: { autoplay: 1, controls: 0, mute: 0, origin: window.location.origin, host: 'https://www.youtube.com' } }}
              onReady={(e) => { e.target.playVideo(); }}
            />
          </div>
        </div>

        {/* 채팅 터미널 */}
        <div className="panel-border bg-slate-900/60 rounded-xl flex flex-col h-[350px] shrink-0 overflow-hidden">
          <div className="bg-primary/10 border-b border-primary/30 p-4 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-base">terminal</span>
              <h3 className="text-xs font-bold text-primary tracking-widest uppercase"> 채팅 로그</h3>
            </div>
            <span className="text-[10px] font-mono text-primary/40 uppercase font-bold tracking-widest">Secure Link Active</span>
          </div>

          <div ref={logContainerRef} className="flex-1 overflow-y-auto p-6 flex flex-col gap-1 bg-black/50 custom-scrollbar">
            {logs.map((log, i) => renderChatLog(log, i))}
            {logs.length === 0 && (
              <div className="flex flex-col items-center justify-center h-full opacity-20">
                <span className="material-symbols-outlined text-5xl mb-4">sensors</span>
                <p className="italic uppercase tracking-[0.4em] text-xs font-bold">신호를 기다리는 중...</p>
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="p-4 border-t border-primary/30 bg-slate-950 flex gap-4 shrink-0 shadow-[0_-10px_30px_rgba(0,0,0,0.5)]">
            <button
              type="button"
              onClick={onSkipRound}
              className="px-6 border-2 border-primary/30 text-primary hover:bg-primary/10 rounded-xl font-bold transition-all flex items-center gap-2 shrink-0 group"
              title="문제 건너뛰기 투표"
            >
              <span className="material-symbols-outlined group-hover:rotate-12 transition-transform">skip_next</span>
              스킵
            </button>
            <div className="flex-1 relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-primary/50 font-mono text-lg font-bold">{'>'}</span>
              <input
                ref={inputRef}
                type="text"
                className="w-full bg-slate-900 border-2 border-primary/30 rounded-xl pl-10 pr-4 py-4 text-base text-white focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none font-mono transition-all placeholder:text-slate-700"
                placeholder="코드나 정답을 입력하세요..."
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                autoFocus
              />
            </div>
            <button type="submit" className="bg-primary hover:bg-primary/80 text-background-dark font-black px-10 rounded-xl transition-all transform hover:scale-[1.02] shadow-[0_0_20px_rgba(37,192,244,0.4)] tracking-[0.2em] text-lg">
              입력
            </button>
          </form>
        </div>

      </div>
    </div>
  );
};

export default Game;
