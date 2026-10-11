import React from 'react';
import { useMyGameRecords } from '../../hooks/useMyGameRecords';
import { GAME_TYPES } from '../../utils/gameOptions';
import type { GameRecord } from '../../types/record';

const labelOf = (gameType: string) => GAME_TYPES.find((option) => option.value === gameType)?.label;

const GameRecordCard: React.FC<{ record: GameRecord; label: string }> = ({ record, label }) => (
  <section aria-label={`${label} 전적`} className="px-card-sm p-3 space-y-3">
    <div className="flex items-center justify-between gap-2">
      <span className="px-title text-sm">{label}</span>
      <span className="px-chip num">{record.played}판</span>
    </div>

    {record.played === 0 ? (
      <p className="px-label text-[10px] py-2">아직 플레이한 판이 없습니다.</p>
    ) : (
      <div className="px-inset px-3 py-2 flex items-end justify-between gap-2">
        <span className="text-sm num">
          {record.won}승 {record.lost}패
        </span>
        <span className="flex items-baseline gap-1.5">
          <span className="px-label text-[10px]">승률</span>
          <span className="px-title text-2xl num">{record.winRate}%</span>
        </span>
      </div>
    )}
  </section>
);

const MyGameRecordsSection: React.FC = () => {
  const { records, isLoading, errorMessage } = useMyGameRecords();
  const labelled = records.flatMap((record) => {
    const label = labelOf(record.gameType);
    return label ? [{ record, label }] : [];
  });

  return (
    <div className="px-card">
      <div className="px-head">
        <span>내 전적</span>
      </div>

      {isLoading ? (
        <p className="px-title text-sm text-center py-16 animate-blink">전적 로드 중...</p>
      ) : errorMessage ? (
        <p className="text-sm text-cherry text-center py-16">{errorMessage}</p>
      ) : (
        <div className="p-3 space-y-2.5">
          {labelled.map(({ record, label }) => (
            <GameRecordCard key={record.gameType} record={record} label={label} />
          ))}
        </div>
      )}
    </div>
  );
};

export default MyGameRecordsSection;
