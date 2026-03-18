import React from 'react';
import Game from '../components/Game';
import type { Player, GameStartInfo, RoundEndInfo } from '../types/game';

interface GamePageProps {
  players: Player[];
  roomId: string;
  timeLeft: number;
  totalTime: number;
  currentVideoId: string;
  logs: string[];
  onAnswerSubmit: (answer: string) => void;
  onSkipRound: () => void;
  onFetchRank: () => Promise<void>;
  gameStartInfo: GameStartInfo | null;
  gameType: string | null;
  roundEndInfo: RoundEndInfo | null;
  currentRound: number;
  totalRound: number;
  hint: string;
}

const GamePage: React.FC<GamePageProps> = ({
  players,
  roomId,
  timeLeft,
  totalTime,
  currentVideoId,
  logs,
  onAnswerSubmit,
  onSkipRound,
  onFetchRank,
  gameStartInfo,
  gameType,
  roundEndInfo,
  currentRound,
  totalRound,
  hint,
}) => {
  return (
    <div className="relative flex flex-col min-h-screen bg-slate-900">
      {/* 퀴즈 게임 전용 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-6 py-4 bg-background-dark/80 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow">
              rocket_launch
            </span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest uppercase text-primary neon-glow">
              FunGame 라이브
            </h1>
            <p className="text-[10px] text-primary/60 tracking-[0.2em]">시스템 상태: 게임 진행 중</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-primary/20">
            <span className="text-[10px] text-primary/50 uppercase">현재 정보</span>
            <span className="text-sm font-bold text-red-400">퀴즈 대결</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col p-6 relative z-10 h-[calc(100vh-140px)] overflow-hidden">
        <Game
          players={players}
          roomId={roomId}
          timeLeft={timeLeft}
          totalTime={totalTime}
          currentVideoId={currentVideoId}
          logs={logs}
          onAnswerSubmit={onAnswerSubmit}
          onSkipRound={onSkipRound}
          onFetchRank={onFetchRank}
          gameStartInfo={gameStartInfo}
          gameType={gameType}
          roundEndInfo={roundEndInfo}
          currentRound={currentRound}
          totalRound={totalRound}
          hint={hint}
        />
      </main>

      {/* 푸터 */}
      <footer className="mt-auto border-t border-primary/10 px-6 py-4 bg-slate-900/40 backdrop-blur-sm flex justify-between items-center z-10">
        <div className="flex items-center gap-6">
          <span className="text-[10px] font-bold tracking-[0.3em] text-red-400 uppercase">
            주의: 시간 제한이 임박했습니다.
          </span>
        </div>
        <div className="flex items-center gap-4">
          <div className="w-32 h-2 bg-slate-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.5)] transition-all duration-1000 ease-linear"
              style={{ width: `${Math.max(0, Math.min(100, (timeLeft / totalTime) * 100))}%` }}></div>
          </div>
          <span className="text-[10px] font-bold text-red-400">
            남은 시간: {Math.max(0, Math.min(100, Math.round((timeLeft / totalTime) * 100)))}%
          </span>
        </div>
      </footer>
    </div>
  );
};

export default GamePage;
