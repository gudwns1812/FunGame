import React, { useMemo, useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import HangmanDrawing from '../components/hangman/HangmanDrawing';
import WordDisplay from '../components/hangman/WordDisplay';
import AlphabetKeyboard from '../components/hangman/AlphabetKeyboard';
import type { HangmanStatus, Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

interface HangmanPageProps {
  status: HangmanStatus | null;
  onGuess: (letter: string) => void;
  myNickname: string;
  logs: string[];
  players: Player[];
  onSendMessage: (msg: string) => void;
}

const HangmanPage: React.FC<HangmanPageProps> = ({ status, onGuess, myNickname, logs, players, onSendMessage }) => {
  const navigate = useNavigate();
  const [chatMessage, setChatMessage] = useState('');
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

  const renderChatLog = (log: string, i: number) => {
    if (log.startsWith('[시스템]') || log.startsWith('[오류]')) {
      return (
        <p key={i} className={`font-mono text-xs py-1 ${log.startsWith('[오류]') ? 'text-red-400' : 'text-slate-400'}`}>
          <span className="opacity-50">
            [
            {new Date().toLocaleTimeString('en-US', {
              hour12: false,
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            })}
            ]
          </span>{' '}
          {log}
        </p>
      );
    }
    const colonIdx = log.indexOf(':');
    if (colonIdx > 0) {
      const senderName = log.substring(0, colonIdx);
      const rest = log.substring(colonIdx + 1);
      const player = players.find((p) => stripTag(p.name) === senderName || p.name === senderName);
      const color = getPlayerColor(player?.colorIndex ?? null) || '#25c0f4';
      return (
        <p key={i} className="font-mono text-sm py-1.5 text-slate-200 border-b border-white/5 last:border-0">
          <span style={{ color }} className="font-bold">
            {senderName}
          </span>
          <span className="opacity-50 mx-1">:</span> {rest}
        </p>
      );
    }
    return (
      <p key={i} className="font-mono text-sm py-1 text-slate-200">
        {log}
      </p>
    );
  };

  // 시스템 로그에서 정답 추출 (백엔드 GAME_RESULT 메시지 기반)
  const actualAnswer = useMemo(() => {
    const resultLog = [...logs].reverse().find((log) => log.includes('[게임 종료] 정답:'));
    return resultLog ? resultLog.split('정답:')[1]?.trim() : null;
  }, [logs]);

  if (!status) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background-dark text-white">
        <div className="text-2xl font-bold animate-pulse">게임 데이터를 동기화하는 중...</div>
      </div>
    );
  }

  const isMyTurn = myNickname === status.currentTurnPlayer;

  return (
    <div className="min-h-screen bg-background-dark p-8 flex flex-col items-center">
      <div className="w-full max-w-4xl space-y-8 animate-fade-in">
        {/* 상단 정보 */}
        <div className="flex justify-between items-center bg-slate-900/80 p-6 rounded-3xl border border-white/5 shadow-2xl backdrop-blur-xl">
          <div className="space-y-1">
            <h2 className="text-slate-400 text-[10px] font-bold uppercase tracking-[0.2em]">Current Turn</h2>
            <div
              className={`text-2xl font-black transition-all ${isMyTurn ? 'text-primary neon-glow scale-105 origin-left' : 'text-slate-300'}`}>
              {isMyTurn ? '당신의 차례입니다!' : `${status.currentTurnPlayer} 님의 차례`}
            </div>
          </div>
          <div className="text-right space-y-1">
            <h2 className="text-slate-400 text-[10px] font-bold uppercase tracking-[0.2em]">남은 기회</h2>
            <div
              className={`text-2xl font-black ${status.remainingTries <= 2 ? 'text-red-500 animate-pulse' : 'text-white'}`}>
              {status.remainingTries} / 6
            </div>
          </div>
        </div>

        {/* 메인 게임 영역 */}
        <div className={`grid grid-cols-1 md:grid-cols-2 gap-8 items-center bg-slate-900/40 p-10 rounded-[2.5rem] border shadow-inner transition-all duration-300 relative overflow-hidden ${effect === 'WRONG' ? 'border-red-500 shadow-[0_0_50px_rgba(239,68,68,0.3)] bg-red-500/10 translate-x-[5px]' :
          effect === 'CORRECT' ? 'border-primary shadow-[0_0_50px_rgba(37,192,244,0.3)] bg-primary/10' :
            'border-white/5'
          }`}>
          {/* 정답/오답 팝업 아이콘 */}
          {effect && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-30">
              {effect === 'CORRECT' ? (
                <span className="material-symbols-outlined text-[10rem] md:text-[15rem] text-primary/80 drop-shadow-[0_0_50px_rgba(37,192,244,1)] animate-ping">
                  task_alt
                </span>
              ) : (
                <span className="material-symbols-outlined text-[10rem] md:text-[15rem] text-red-500/80 drop-shadow-[0_0_50px_rgba(239,68,68,1)] animate-pulse">
                  cancel
                </span>
              )}
            </div>
          )}
          <div className="flex justify-center bg-slate-950/50 rounded-3xl p-6 border border-white/5 relative overflow-hidden group">
            <div className="absolute inset-0 bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
            <HangmanDrawing remainingTries={status.remainingTries} />
          </div>
          <div className="flex flex-col items-center justify-center space-y-10">
            <WordDisplay currentDisplay={status.currentDisplay} />
            <div className="w-full space-y-6">
              <h3 className="text-slate-500 text-[10px] font-bold uppercase tracking-[0.3em] text-center">
                Select Alphabet
              </h3>
              <AlphabetKeyboard
                onGuess={onGuess}
                disabled={!isMyTurn || status.isGameOver}
                wrongLetters={status.wrongLetters}
                currentDisplay={status.currentDisplay}
              />
            </div>
          </div>
        </div>

        {/* 채팅 터미널 영역 */}
        <div className="panel-border bg-slate-900/60 rounded-xl flex flex-col h-[350px] shrink-0 overflow-hidden w-full border border-primary/20 mt-4">
          <div className="bg-primary/10 border-b border-primary/30 p-3 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-sm">terminal</span>
              <h3 className="text-xs font-bold text-primary tracking-widest uppercase">채팅</h3>
            </div>
            <span className="text-[10px] font-mono text-primary/40 uppercase font-bold tracking-widest">
              Secure Channel
            </span>
          </div>

          <div
            ref={logContainerRef}
            className="flex-1 overflow-y-auto p-4 flex flex-col gap-1 bg-black/50 custom-scrollbar text-left">
            {logs.map((log, i) => renderChatLog(log, i))}
            {logs.length === 0 && (
              <div className="flex flex-col items-center justify-center h-full opacity-20">
                <span className="material-symbols-outlined text-4xl mb-2">sensors</span>
                <p className="italic uppercase tracking-[0.4em] text-[10px] font-bold">신호를 기다리는 중...</p>
              </div>
            )}
          </div>

          <form
            onSubmit={handleChatSubmit}
            className="p-3 border-t border-primary/30 bg-slate-950 flex gap-3 shrink-0">
            <div className="flex-1 relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-primary/50 font-mono text-sm font-bold">
                {'>'}
              </span>
              <input
                ref={chatInputRef}
                type="text"
                className="w-full bg-slate-900 border border-primary/30 rounded-lg pl-8 pr-3 py-2 text-sm text-white focus:border-primary focus:ring-1 focus:ring-primary/20 outline-none font-mono transition-all placeholder:text-slate-700"
                placeholder="팀원과 소통하세요..."
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
              />
            </div>
            <button
              type="submit"
              className="bg-primary hover:bg-primary/80 text-background-dark font-black px-6 rounded-lg transition-all transform hover:scale-[1.02] shadow-[0_0_15px_rgba(37,192,244,0.3)] tracking-widest text-sm">
              전송
            </button>
          </form>
        </div>

        {/* 게임 종료 모달 */}
        {status.isGameOver && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-background-dark/95 backdrop-blur-xl animate-fade-in">
            <div className="bg-slate-900 p-12 rounded-[3rem] border-2 border-primary/30 shadow-[0_0_80px_rgba(37,192,244,0.15)] text-center space-y-8 max-w-sm w-full mx-4 transform animate-scale-up">
              <div className="space-y-2">
                <h2
                  className={`text-6xl font-black tracking-tighter uppercase ${status.isWin ? 'text-primary neon-glow' : 'text-red-500'}`}>
                  {status.isWin ? 'Victory!' : 'Game Over'}
                </h2>
                <p className="text-slate-400 font-bold tracking-tight">
                  {status.isWin ? '단어를 성공적으로 맞췄습니다!' : '기회를 모두 소진했습니다.'}
                </p>
              </div>

              {actualAnswer && (
                <div className="bg-slate-950 py-6 px-8 rounded-2xl border border-white/5 space-y-1">
                  <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Correct Word</span>
                  <div className="text-3xl font-black text-white tracking-widest neon-glow-subtle">{actualAnswer}</div>
                </div>
              )}

              <button
                onClick={() => navigate('/rooms')}
                className="w-full py-5 bg-primary text-background-dark font-black rounded-2xl hover:scale-105 active:scale-95 transition-all shadow-[0_10px_30px_rgba(37,192,244,0.3)]">
                로비로 돌아가기
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default HangmanPage;
