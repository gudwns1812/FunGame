import React, { useState, useEffect, useRef } from 'react';
import ReactPlayer from 'react-player';
import type { Player, GameStartInfo, RoundEndInfo } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import RankingList from './RankingList';
import LogList from './LogList';

const CS_BACKGROUND_VIDEO_ID = 'U34kLXjdw90';

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
  hint: string;
  logs: string[];
}

/** 노래 재생 중 표시용 이퀄라이저 */
const Equalizer: React.FC = () => (
  <div className="flex items-end gap-2 h-20">
    {[0, 1, 2, 3, 4, 5, 6, 7].map((i) => (
      <span
        key={i}
        className="px-eq-bar w-3.5 h-full border-2 border-ink bg-cherry"
        style={{ animationDelay: `${i * 0.11}s`, animationDuration: `${0.8 + (i % 3) * 0.2}s` }}
      />
    ))}
  </div>
);

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
  hint,
  logs,
}) => {
  const [answer, setAnswer] = useState('');
  const logContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const progressPercent = Math.max(0, Math.min(100, (timeLeft / totalTime) * 100));
  const isUrgent = timeLeft < 10;
  const playbackVideoId = gameType === 'CS' ? CS_BACKGROUND_VIDEO_ID : gameType === 'SONG' ? currentVideoId : '';

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

  const renderSongPanel = () => {
    /* 라운드 종료: 정답 공개 */
    if (roundEndInfo) {
      const explanation = roundEndInfo.explanation?.trim();
      const shouldSplitRoundEnd = gameType === 'CS' && Boolean(explanation);

      return (
        <div className="h-full p-3.5 sm:p-4 flex flex-col gap-2.5 animate-pop">
          {/* 상태 칩은 한 줄로 모아 세로 공간을 아낀다 */}
          <div className="shrink-0 flex items-center justify-center gap-2 flex-wrap">
            <span className="px-chip px-chip-grass text-xs">정답 공개</span>
            {roundEndInfo.winner && roundEndInfo.winner !== '없음' ? (
              <span className="px-chip px-chip-cherry text-xs gap-1.5">
                맞춘 사람 <span className="px-title text-xs text-white">{stripTag(roundEndInfo.winner)}</span>
              </span>
            ) : (
              <span className="px-chip text-xs">정답자 없음</span>
            )}
          </div>

          {/* 정답·해설 박스가 패널 높이를 채우고, 넓은 화면에서는 좌우로 나눈다 */}
          <div className="flex-1 min-h-0 flex flex-col md:flex-row gap-2.5">
            <div
              className={`border-2 border-cherry bg-[#ffeceb] px-4 py-3 min-w-0 flex flex-col justify-center text-center ${
                shouldSplitRoundEnd ? 'md:w-[32%] md:shrink-0' : 'mx-auto w-full max-w-xl'
              }`}>
              <p className="px-label text-cherry mb-1.5">정답</p>
              {/* 정답이 여러 개거나 긴 영문 한 덩어리일 때 박스 밖으로 뻗지 않도록 강제 줄바꿈을 허용한다 */}
              <p className="px-title text-lg sm:text-xl break-keep wrap-anywhere whitespace-pre-wrap leading-snug">
                {roundEndInfo.answer}
              </p>
            </div>

            {shouldSplitRoundEnd ? (
              <div className="px-inset px-4 py-3 text-left flex-1 min-w-0 flex flex-col">
                <p className="px-label mb-1.5 shrink-0">해설</p>
                {/* 해설은 자르지 않고 그대로 보여주고, 박스를 넘칠 때만 내부에서 스크롤된다 */}
                <div className="flex-1 min-h-0 scroll-y custom-scrollbar">
                  <p className="text-sm leading-6 font-semibold break-keep wrap-anywhere whitespace-pre-wrap text-ink">
                    {explanation}
                  </p>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      );
    }

    /* CS 퀴즈: 문제 텍스트 */
    if (gameType === 'CS' && currentVideoId) {
      return (
        <div className="h-full scroll-y custom-scrollbar p-6 sm:p-8 flex flex-col">
          <div className="m-auto flex flex-col items-center gap-4 text-center">
            <span className="px-chip px-chip-sea text-xs">CS 퀴즈</span>
            <p className="px-title text-xl sm:text-2xl leading-relaxed max-w-3xl break-keep wrap-anywhere whitespace-pre-wrap">
              {currentVideoId}
            </p>
          </div>
        </div>
      );
    }

    /* 게임 시작 안내 */
    if (gameStartInfo) {
      return (
        <div className="h-full p-6 flex flex-col items-center justify-center gap-5 text-center animate-pop">
          <p className="px-title text-xl sm:text-2xl break-keep max-w-xl leading-snug">{gameStartInfo.message}</p>

          <div className="grid grid-cols-3 gap-2 w-full max-w-md">
            <div className="px-inset py-3">
              <p className="px-label">카테고리</p>
              <p className="px-title text-sm mt-1">{gameStartInfo.category || '전체'}</p>
            </div>
            <div className="px-inset py-3">
              <p className="px-label">총 문제 수</p>
              <p className="px-title text-sm mt-1 num">{gameStartInfo.songCount}문항</p>
            </div>
            <div className="px-inset py-3">
              <p className="px-label">게임 모드</p>
              <p className="px-title text-sm mt-1">{gameStartInfo.gameType === 'SONG' ? '음악' : 'CS'}</p>
            </div>
          </div>
        </div>
      );
    }

    /* 힌트 공개 */
    if (gameType === 'SONG' && hint && !roundEndInfo) {
      return (
        <div className="h-full p-6 flex flex-col items-center justify-center gap-4 text-center animate-pop">
          <span className="px-chip px-chip-gold text-xs">힌트</span>
          <p className="px-title text-2xl sm:text-3xl break-keep tracking-wide">{hint}</p>
        </div>
      );
    }

    /* 기본: 노래 재생 중 */
    return (
      <div className="h-full flex flex-col items-center justify-center gap-6">
        <Equalizer />
        <p className="px-title text-sm text-ink-soft">노래를 듣고 정답을 입력하세요</p>
      </div>
    );
  };

  return (
    <div className="h-full min-h-0 w-full max-w-6xl mx-auto flex flex-col gap-3">
      {/* 라운드 · 남은 시간 */}
      <div className="px-card shrink-0 px-3 py-2 flex items-center gap-3">
        <span className="px-chip px-chip-cherry shrink-0">
          {currentRound > 0 ? (
            <>
              <span className="num">{currentRound}</span> / <span className="num">{totalRound}</span>
            </>
          ) : (
            '준비 중'
          )}
        </span>

        <div className="px-bar flex-1 h-5">
          <div
            className={`px-bar-fill ${isUrgent ? 'text-cherry' : 'text-sea'}`}
            style={{ width: `${progressPercent}%`, backgroundColor: 'currentColor' }}
          />
        </div>

        {/* 남은 시간: 1초마다 통통 튀고, 10초 미만이면 빨갛게 강조 */}
        <span className={`px-title text-lg shrink-0 num ${isUrgent ? 'text-cherry' : ''}`}>
          <span key={timeLeft} className={isUrgent ? 'animate-tick-urgent' : 'animate-tick'}>
            {timeLeft}
          </span>
          초
        </span>
      </div>

      {/* 문제 영역 + 순위: 화면 높이의 일부만 쓰고 남은 공간은 로그에 넘긴다 (작은 노트북 대응) */}
      <div className="shrink-0 flex gap-3 h-[38vh] min-h-[190px] max-h-[560px]">
        {/* h-[550px]는 기존 테스트가 참조하는 클래스라 유지하고, 실제 높이는 부모(행) 높이를 따르게 한다 */}
        <div className="px-card flex-1 min-w-0 h-[550px] min-h-full max-h-full relative overflow-hidden">
          {renderSongPanel()}

          {/* 오디오만 사용하는 숨김 플레이어 */}
          <div className="hidden">
            {playbackVideoId && (
              <ReactPlayer
                key={playbackVideoId}
                src={`https://www.youtube.com/watch?v=${playbackVideoId}`}
                playing={true}
                controls={false}
                width={0}
                height={0}
                onStart={() => {
                  console.log('[Player] 유튜브 재생 시작! 미디어 세션 탈취 및 고정 시도');

                  // 1. 무음 오디오 생성 및 재생
                  const overrideAudio = new Audio('/assets/silent.wav');
                  overrideAudio.volume = 0.01; // 0.1보다 더 작게 주셔도 됩니다 (브라우저 인식용)
                  overrideAudio.loop = true;

                  overrideAudio
                    .play()
                    .then(() => {
                      if ('mediaSession' in navigator) {
                        // 2. 유튜브가 자기 정보를 올릴 틈을 주지 않기 위해 0.5초 간격으로 3초간 덮어씁니다.
                        let count = 0;
                        const interval = setInterval(() => {
                          navigator.mediaSession.metadata = new MediaMetadata({
                            title: '🎵 보안 신호 해독 중...',
                            artist: '정답을 맞춰보세요!',
                            album: 'FunGame Live',
                          });
                          navigator.mediaSession.playbackState = 'playing';

                          count++;
                          // 0.5초 * 6번 = 3초 동안 반복 후 종료
                          if (count > 6) clearInterval(interval);
                        }, 500);
                      }
                    })
                    .catch((e) => console.log('탈취 실패:', e));
                }}
                onError={(e) => console.error('[Player] Error:', e)}
              />
            )}
          </div>
        </div>

        {/* 실시간 순위 */}
        <div className="px-card w-[150px] sm:w-[230px] shrink-0 flex flex-col overflow-hidden">
          <div className="px-head shrink-0">
            <span>순위</span>
            <span className="num text-ink-soft">{players.length}명</span>
          </div>
          <RankingList players={players} roundEndInfo={roundEndInfo} />
        </div>
      </div>

      {/* 로그 + 정답 입력 (주관식) — 남은 높이를 모두 차지한다 */}
      <div className="px-card flex-1 min-h-[130px] flex flex-col overflow-hidden">
        <div className="px-head shrink-0">
          <span>진행 로그</span>
          <span className="px-label text-[10px]">엔터로 바로 입력</span>
        </div>

        <LogList
          logs={logs}
          players={players}
          containerRef={logContainerRef}
          className="flex-1 min-h-0"
          emptyText="첫 문제를 기다리는 중"
        />

        <form onSubmit={handleSubmit} className="shrink-0 border-t-[3px] border-ink bg-paper-2 p-2 flex gap-2">
          <input
            ref={inputRef}
            type="text"
            className="px-input flex-1 py-2 border-2"
            placeholder="정답을 입력하세요"
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            autoFocus
          />
          <button type="submit" className="px-btn px-btn-sm px-btn-primary">
            입력
          </button>
          <button type="button" onClick={onSkipRound} className="px-btn px-btn-sm px-btn-paper" title="문제 건너뛰기 투표">
            스킵
          </button>
        </form>
      </div>
    </div>
  );
};

export default Game;
