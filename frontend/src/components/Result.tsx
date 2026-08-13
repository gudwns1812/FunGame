import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

import firstBadge from '../images/medal-first.svg';
import secondBadge from '../images/medal-second.svg';
import thirdBadge from '../images/medal-third.svg';

interface ResultProps {
  rankings: Player[];
  onBackToLobby: () => void;
  onBackToRoom: () => void;
}

const BADGES = [firstBadge, secondBadge, thirdBadge];

const Result: React.FC<ResultProps> = ({ rankings, onBackToLobby, onBackToRoom }) => {
  const sortedRankings = [...rankings].sort((a, b) => b.score - a.score);
  const winner = sortedRankings[0];

  return (
    <div className="w-full max-w-md flex flex-col gap-4 animate-pop">
      {/* 우승자 */}
      {winner && (
        <div className="px-card p-5 flex flex-col items-center gap-2 text-center">
          <img src={BADGES[0]} alt="1st Badge" className="w-24 h-24 object-contain" />
          <p className="px-label">우승</p>
          <p className="px-title text-3xl truncate max-w-full">{stripTag(winner.name)}</p>
          <p className="px-chip px-chip-cherry num">{winner.score}점</p>
        </div>
      )}

      {/* 전체 순위 */}
      <div className="px-card flex flex-col overflow-hidden">
        <div className="px-head shrink-0">
          <span>최종 순위</span>
          <span className="num text-ink-soft">{sortedRankings.length}명</span>
        </div>

        <div className="max-h-[38vh] scroll-y custom-scrollbar p-2 space-y-1.5">
          {sortedRankings.map((p, idx) => {
            const color = getPlayerColor(p.colorIndex ?? null) || '#0c6780';
            const isFirst = idx === 0 && p.score > 0;

            return (
              <div
                key={p.memberId}
                className={`flex items-center gap-2.5 border-2 border-ink px-2.5 py-2 ${
                  isFirst ? 'bg-gold' : 'bg-white'
                }`}>
                <div className="w-9 flex justify-center shrink-0">
                  {idx < 3 ? (
                    <img src={BADGES[idx]} alt={`${idx + 1}위`} className="w-9 h-9 object-contain" />
                  ) : (
                    <span className="px-label text-[10px] num">#{idx + 1}</span>
                  )}
                </div>

                <span className="w-2.5 h-2.5 shrink-0 border border-ink" style={{ background: color }} />
                <span className="px-title flex-1 min-w-0 truncate text-sm">{stripTag(p.name)}</span>
                <span className="px-title text-sm shrink-0 num">{p.score}</span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <button className="px-btn px-btn-primary w-full py-3" onClick={onBackToRoom}>
          게임방으로 돌아가기
        </button>
        <button className="px-btn w-full py-3" onClick={onBackToLobby}>
          ◀ 로비로 돌아가기
        </button>
      </div>
    </div>
  );
};

export default Result;
