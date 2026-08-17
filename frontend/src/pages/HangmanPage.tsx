import React, { useMemo, useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import HangmanDrawing from '../components/hangman/HangmanDrawing';
import WordDisplay from '../components/hangman/WordDisplay';
import AlphabetKeyboard from '../components/hangman/AlphabetKeyboard';
import LeaveGameConfirm from '../components/LeaveGameConfirm';
import LogList from '../components/LogList';
import ReportButton from '../components/ReportButton';
import TopBar from '../components/layout/TopBar';
import type { HangmanStatus, Player } from '../types/game';

interface HangmanPageProps {
  status: HangmanStatus | null;
  onGuess: (letter: string) => void;
  myMemberId: number | null;
  logs: string[];
  players: Player[];
  onSendMessage: (msg: string) => void;
  roomId: string;
  onLeave: () => void;
}

const MAX_TRIES = 6;

const HangmanPage: React.FC<HangmanPageProps> = ({
  status,
  onGuess,
  myMemberId,
  logs,
  players,
  onSendMessage,
  roomId,
  onLeave,
}) => {
  const navigate = useNavigate();
  const [chatMessage, setChatMessage] = useState('');
  const [isLeaveAsked, setIsLeaveAsked] = useState(false);
  const logContainerRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLInputElement>(null);

  // 피드백 이펙트 상태
  const [effect, setEffect] = useState<'CORRECT' | 'WRONG' | null>(null);
  const prevStatusRef = useRef<HangmanStatus | null>(null);

  useEffect(() => {
    if (status && prevStatusRef.current) {
      const prev = prevStatusRef.current;
      const currentRevealed = status.currentDisplay.replace(/_/g, '').length;
      const prevRevealed = prev.currentDisplay.replace(/_/g, '').length;

      if (currentRevealed > prevRevealed) {
        setEffect('CORRECT');
        setTimeout(() => setEffect(null), 800);
      } else if (status.wrongLetters.length > prev.wrongLetters.length) {
        setEffect('WRONG');
        setTimeout(() => setEffect(null), 800);
      }
    }
    prevStatusRef.current = status;
  }, [status]);

  // 채팅 전역 엔터키 포커스
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Enter') {
        const activeTag = document.activeElement?.tagName;
        if (activeTag !== 'INPUT' && activeTag !== 'TEXTAREA') {
          e.preventDefault();
          chatInputRef.current?.focus();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // 채팅 자동 스크롤
  useEffect(() => {
    setTimeout(() => {
      if (logContainerRef.current) {
        logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
      }
    }, 100);
  }, [logs]);

  const handleChatSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (chatMessage.trim()) {
      onSendMessage(chatMessage.trim());
      setChatMessage('');
    }
  };

  // 시스템 로그에서 정답 추출 (백엔드 GAME_RESULT 메시지 기반)
  const actualAnswer = useMemo(() => {
    const resultLog = [...logs].reverse().find((log) => log.includes('[게임 종료] 정답:'));
    return resultLog ? resultLog.split('정답:')[1]?.trim() : null;
  }, [logs]);

  if (!status) {
    return (
      <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center">
        <p className="px-title text-sm animate-blink">게임 데이터를 동기화하는 중...</p>
      </div>
    );
  }

  const isMyTurn = myMemberId !== null && myMemberId === status.currentTurnMemberId;

  return (
    <div className="app-frame">
      <TopBar
        title="행맨"
        right={
          <>
            <ReportButton roomId={Number(roomId) || null} gameType="HANGMAN" />
            <span className={`px-chip num ${status.remainingTries <= 2 ? 'px-chip-cherry' : ''}`}>
              기회 {status.remainingTries} / {MAX_TRIES}
            </span>
            <button type="button" className="px-btn px-btn-sm px-btn-paper" onClick={() => setIsLeaveAsked(true)}>
              나가기
            </button>
          </>
        }
      />

      <main className="flex-1 min-h-0 p-3 sm:p-4 flex">
        <div className="h-full min-h-0 w-full max-w-5xl mx-auto flex flex-col gap-3">
          {/* 현재 턴 */}
          <div className="px-card shrink-0 px-3 py-2.5 flex items-center justify-between gap-3">
            <p className={`px-title text-sm truncate ${isMyTurn ? 'text-cherry' : ''}`}>
              {isMyTurn ? '당신의 차례입니다!' : `${status.currentTurnPlayer} 님의 차례`}
            </p>
            <div className="flex items-center gap-1 shrink-0">
              {Array.from({ length: MAX_TRIES }).map((_, i) => (
                <span
                  key={i}
                  className={`w-3 h-3 border-2 border-ink ${i < status.remainingTries ? 'bg-grass' : 'bg-paper-2'}`}
                />
              ))}
            </div>
          </div>

          {/* 게임 보드 */}
          <div
            className={`px-card flex-1 min-h-0 p-4 flex flex-col sm:flex-row items-center justify-center gap-5 relative scroll-y custom-scrollbar
              ${effect === 'WRONG' ? 'bg-[#ffe3e0]' : effect === 'CORRECT' ? 'bg-[#e2f7e4]' : ''}`}>
            {effect && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-20 animate-pop">
                <span className={`px-title text-[7rem] ${effect === 'CORRECT' ? 'text-grass' : 'text-cherry'} opacity-25`}>
                  {effect === 'CORRECT' ? 'O' : 'X'}
                </span>
              </div>
            )}

            <HangmanDrawing remainingTries={status.remainingTries} />

            <div className="flex flex-col items-center gap-5 w-full sm:w-auto min-w-0">
              <WordDisplay currentDisplay={status.currentDisplay} />
              <AlphabetKeyboard
                onGuess={onGuess}
                disabled={!isMyTurn || status.isGameOver}
                wrongLetters={status.wrongLetters}
                currentDisplay={status.currentDisplay}
              />
            </div>
          </div>

          {/* 로그 + 채팅 입력 */}
          <div className="px-card shrink-0 h-[24vh] min-h-[130px] flex flex-col overflow-hidden">
            <div className="px-head shrink-0">
              <span>진행 로그</span>
              <span className="w-2 h-2 bg-grass animate-blink" />
            </div>

            <LogList
              logs={logs}
              players={players}
              containerRef={logContainerRef}
              className="flex-1 min-h-0"
              emptyText="아직 기록이 없습니다"
            />

            <form onSubmit={handleChatSubmit} className="shrink-0 border-t-[3px] border-ink bg-paper-2 p-2 flex gap-2">
              <input
                ref={chatInputRef}
                type="text"
                className="px-input flex-1 py-2 border-2"
                placeholder="메시지 입력..."
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
              />
              <button type="submit" className="px-btn px-btn-sm px-btn-sea">
                전송
              </button>
            </form>
          </div>
        </div>
      </main>

      {isLeaveAsked && <LeaveGameConfirm onConfirm={onLeave} onCancel={() => setIsLeaveAsked(false)} />}

      {/* 게임 종료 */}
      {status.isGameOver && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 animate-fade-in">
          <div className="px-card w-full max-w-xs p-6 text-center space-y-4 animate-scale-up">
            <p className={`px-title text-2xl ${status.isWin ? 'text-grass' : 'text-cherry'}`}>
              {status.isWin ? '단어를 맞췄습니다!' : '기회를 모두 소진했습니다.'}
            </p>

            {actualAnswer && (
              <div className="px-inset py-3">
                <p className="px-label">정답</p>
                <p className="px-title text-xl mt-1 tracking-wide">{actualAnswer}</p>
              </div>
            )}

            <button onClick={() => navigate('/rooms')} className="px-btn px-btn-primary w-full py-3">
              로비로 돌아가기
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default HangmanPage;
