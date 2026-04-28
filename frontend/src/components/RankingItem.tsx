import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';

// 이미지 import
import firstBadge from '../images/first.png';
import secondBadge from '../images/second.png';
import thirdBadge from '../images/third.png';

interface RankingItemProps {
  player: Player;
  rank: number;
  isWinner: boolean;
  color: string;
}

const RankingItem: React.FC<RankingItemProps> = ({
  player,
  rank,
  isWinner,
  color,
}) => {
  const getBadge = () => {
    switch (rank) {
      case 1:
        return <img src={firstBadge} alt="1st Badge" className="w-10 h-10 object-contain drop-shadow-[0_0_8px_rgba(255,215,0,0.5)]" />;
      case 2:
        return <img src={secondBadge} alt="2nd Badge" className="w-10 h-10 object-contain drop-shadow-[0_0_8px_rgba(192,192,192,0.5)]" />;
      case 3:
        return <img src={thirdBadge} alt="3rd Badge" className="w-10 h-10 object-contain drop-shadow-[0_0_8px_rgba(205,127,50,0.5)]" />;
      default:
        return <span className="text-xs font-mono font-bold opacity-40 shrink-0">#{rank}</span>;
    }
  };

  return (
    <div
      className={`flex justify-between items-center p-4 rounded-lg transition-all duration-300 bg-slate-950/40 hover:bg-slate-800/60
        ${isWinner ? 'animate-shimmer' : ''}`}
    >
      <div className="flex items-center gap-4 min-w-0">
        <div className="w-12 flex justify-center shrink-0">
          {getBadge()}
        </div>
        <span
          className="font-bold truncate uppercase text-sm name-text"
          style={{
            color: color,
            '--p-color': color,
          } as React.CSSProperties}
        >
          {stripTag(player.name)}
        </span>
      </div>
      <span className="font-mono font-bold text-white text-base shrink-0">{player.score}</span>
    </div>
  );
};

export default RankingItem;
