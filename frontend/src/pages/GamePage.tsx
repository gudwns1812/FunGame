import React from 'react';
import Game from '../components/Game';
import HaliGaliBoard from '../components/HaliGaliBoard';
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
  // 할리갈리 전용 Props
  haliGaliStatus: string[];
  onHaliGaliAction: (type: 'FLIP_CARD' | 'PRESS_BELL') => void;
  myNickname: string;
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
  haliGaliStatus,
  onHaliGaliAction,
  myNickname
}) => {
  const isHaliGali = gameType === 'HALLIGALLI';

  return (
    <div className="relative flex flex-col min-h-screen bg-slate-50">
      {/* 한글 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-6 py-4 bg-background-dark/80 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow">
              {isHaliGali ? 'playing_cards' : 'rocket_launch'}
            </span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest uppercase text-primary neon-glow">
              {isHaliGali ? '할리갈리 익스프레스' : 'FunGame 라이브'}
            </h1>
            <p className="text-[10px] text-primary/60 tracking-[0.2em]">시스템 상태: 게임 진행 중</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-primary/20">
            <span className="text-[10px] text-primary/50 uppercase">현재 정보</span>
            <span className="text-sm font-bold text-red-400">
              {isHaliGali ? `진행 중` : '퀴즈 대결'}
            </span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col p-6 relative z-10 h-[calc(100vh-140px)] overflow-hidden">
        {isHaliGali ? (
          <HaliGaliBoard 
            status={haliGaliStatus} 
            myNickname={myNickname} 
            onAction={onHaliGaliAction} 
          />
        ) : (
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
          />
        )}
      </main>

      {/* 푸터 - 게임 타입에 따라 다르게 표시 */}
      {!isHaliGali ? (
        <footer className="mt-auto border-t border-primary/10 px-6 py-4 bg-slate-900/40 backdrop-blur-sm flex justify-between items-center z-10">
          <div className="flex items-center gap-6">
            <span className="text-[10px] font-bold tracking-[0.3em] text-red-400 uppercase">주의: 시간 제한이 임박했습니다.</span>
          </div>
          <div className="flex items-center gap-4">
            <div className="w-32 h-2 bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.5)] transition-all duration-1000 ease-linear"
                style={{ width: `${Math.max(0, Math.min(100, (timeLeft / totalTime) * 100))}%` }}
              ></div>
            </div>
            <span className="text-[10px] font-bold text-red-400">남은 시간: {Math.max(0, Math.min(100, Math.round((timeLeft / totalTime) * 100)))}%</span>
          </div>
        </footer>
      ) : (
        <footer className="mt-auto border-t border-indigo-100 px-6 py-4 bg-white/80 backdrop-blur-sm flex justify-center items-center z-10 shadow-2xl">
          <div className="flex items-center gap-8">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 bg-green-500 rounded-full animate-ping"></span>
              <span className="text-xs font-bold text-gray-500">실시간 연동 중</span>
            </div>
            <div className="text-sm font-black text-indigo-600 tracking-tighter">
              HALIGALLI ACTION LOG
            </div>
            <div className="text-xs text-gray-400 italic">
              * 바닥 카드 합계가 5가 되면 빠르게 종을 클릭하세요!
            </div>
          </div>
        </footer>
      )}
    </div>
  );
};

export default GamePage;
