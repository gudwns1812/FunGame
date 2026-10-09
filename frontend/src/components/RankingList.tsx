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
  const sortedPlayers = useMemo(
    () =>
      [...players].sort(
        (a, b) => (a.rank ?? Number.MAX_SAFE_INTEGER) - (b.rank ?? Number.MAX_SAFE_INTEGER) || a.memberId - b.memberId,
      ),
    [players],
  );

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
    <div className="flex-1 min-h-0 scroll-y custom-scrollbar p-2 space-y-1.5">
      {sortedPlayers.map((p) => {
        const color = getPlayerColor(p.colorIndex ?? null) || '#0c6780';
        const isWinner = winnerNames.has(stripTag(p.name));

        return (
          <RankingItem
            key={p.memberId}
            player={p}
            rank={p.score > 0 ? p.rank : undefined}
            isWinner={isWinner}
            color={color}
          />
        );
      })}
    </div>
  );
};

export default RankingList;
