import React, { useState } from 'react';
import Game from '../components/Game';
import LeaveGameConfirm from '../components/LeaveGameConfirm';
import ReportButton from '../components/ReportButton';
import TopBar from '../components/layout/TopBar';
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
  onLeave: () => void;
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
  onLeave,
}) => {
  const [isLeaveAsked, setIsLeaveAsked] = useState(false);

  return (
    <div className="app-frame">
      <TopBar
        title={gameType === 'CS' ? 'CS 퀴즈' : '노래 퀴즈'}
        right={
          <>
            <ReportButton roomId={Number(roomId) || null} gameType={gameType} />
            <span className="px-chip px-chip-cherry">
              <span className="w-2 h-2 bg-white animate-blink" />
              진행 중
            </span>
            <button type="button" className="px-btn px-btn-sm px-btn-paper" onClick={() => setIsLeaveAsked(true)}>
              나가기
            </button>
          </>
        }
      />

      <main className="flex-1 min-h-0 p-3 sm:p-4 flex">
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

      {isLeaveAsked && <LeaveGameConfirm onConfirm={onLeave} onCancel={() => setIsLeaveAsked(false)} />}
    </div>
  );
};

export default GamePage;
