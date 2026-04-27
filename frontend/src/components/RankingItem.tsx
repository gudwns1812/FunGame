import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';

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
  return (
    <div
      className={`flex justify-between items-center p-4 rounded-lg transition-all duration-300 bg-slate-950/40 hover:bg-slate-800/60
        ${isWinner ? 'animate-shimmer' : ''}`}
    >
      <div className="flex items-center gap-4 min-w-0">
        <span className="text-xs font-mono font-bold opacity-40 shrink-0">#{rank}</span>
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
