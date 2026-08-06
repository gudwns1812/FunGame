import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';

// 이미지 import (레트로 톤에 맞춘 픽셀 메달, 배경 투명)
import firstBadge from '../images/medal-first.svg';
import secondBadge from '../images/medal-second.svg';
import thirdBadge from '../images/medal-third.svg';

interface RankingItemProps {
  player: Player;
  rank: number;
  isWinner: boolean;
  color: string;
}

const BadgeTile: React.FC<{ src: string; alt: string }> = ({ src, alt }) => (
  <img src={src} alt={alt} className="w-9 h-9 shrink-0 object-contain" />
);

const RankingItem: React.FC<RankingItemProps> = ({ player, rank, isWinner, color }) => {
  const getBadge = () => {
    switch (rank) {
      case 1:
        return <BadgeTile src={firstBadge} alt="1st Badge" />;
      case 2:
        return <BadgeTile src={secondBadge} alt="2nd Badge" />;
      case 3:
        return <BadgeTile src={thirdBadge} alt="3rd Badge" />;
      default:
        return <span className="px-label text-[10px] num shrink-0">#{rank}</span>;
    }
  };

  return (
    <div
      className={`flex items-center gap-2 border-2 border-ink bg-white px-2 py-1.5
        ${isWinner ? 'animate-shimmer' : ''}`}>
      <div className="w-9 flex justify-center shrink-0">{getBadge()}</div>

      <span className="w-2.5 h-2.5 shrink-0 border border-ink" style={{ background: color }} />

      <span
        className="name-text font-display flex-1 min-w-0 truncate text-[13px]"
        style={
          {
            color: color,
            '--p-color': color,
          } as React.CSSProperties
        }>
        {stripTag(player.name)}
      </span>

      <span className="px-title text-sm shrink-0 num">{player.score}</span>
    </div>
  );
};

export default RankingItem;
