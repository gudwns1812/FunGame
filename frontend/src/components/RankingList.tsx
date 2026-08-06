import React, { useMemo } from 'react';
import type { Player, RoundEndInfo } from '../types/game';
import RankingItem from './RankingItem';
import { getPlayerColor } from '../utils/playerColor';
import { stripTag } from '../utils/stringUtils';

interface RankingListProps {
  players: Player[];
  roundEndInfo: RoundEndInfo | null;
}

const RankingList: React.FC<RankingListProps> = ({ players, roundEndInfo }) => {
  const sortedPlayers = useMemo(() => {
    return [...players].sort((a, b) => b.score - a.score);
  }, [players]);

  const winnerNames = useMemo(() => {
    if (!roundEndInfo?.winner || roundEndInfo.winner === '없음') return new Set<string>();
    
    // 다중 우승자 지원 (쉼표 구분)
    return new Set(
      roundEndInfo.winner
        .split(',')
        .map((name) => stripTag(name.trim()))
        .filter((name) => name !== '')
    );
  }, [roundEndInfo]);

  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-2 custom-scrollbar">
      {sortedPlayers.map((p, idx) => {
        const color = getPlayerColor(p.colorIndex ?? null) || '#25c0f4';
        const isWinner = winnerNames.has(stripTag(p.name));

        return (
          <RankingItem
            key={p.id}
            player={p}
            rank={idx + 1}
            isWinner={isWinner}
            color={color}
          />
        );
      })}
    </div>
  );
};

export default RankingList;
