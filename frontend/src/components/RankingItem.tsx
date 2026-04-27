import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';

interface RankingItemProps {
  player: Player;
  rank: number;
  isFirst: boolean;
  isWinner: boolean;
  color: string;
}

const RankingItem: React.FC<RankingItemProps> = ({
  player,
  rank,
  isFirst,
  isWinner,
  color,
}) => {
  return (
    <div
      className={`flex justify-between items-center p-4 rounded-lg transition-all duration-300
        ${isFirst ? 'bg-slate-900/60' : 'bg-slate-950/40 hover:bg-slate-800/60'}
        ${isWinner ? 'animate-shimmer' : ''}`}
    >
      <div className="flex items-center gap-4 min-w-0">
        <span className="text-xs font-mono font-bold opacity-40 shrink-0">#{rank}</span>
        <span
          className="font-bold truncate uppercase text-sm"
          style={{
            color: isFirst ? '#ffffff' : color,
            textShadow: isFirst ? `0 0 10px ${color}` : 'none',
          }}
        >
          {stripTag(player.name)}
        </span>
      </div>
      <span className="font-mono font-bold text-white text-base shrink-0">{player.score}</span>
    </div>
  );
};

export default RankingItem;
