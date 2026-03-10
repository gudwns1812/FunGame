import React from 'react';
import type { FruitType } from '../types/game';

interface HaliGaliCardProps {
  fruit: FruitType;
  count: number;
}

const FruitIcon: React.FC<{ type: FruitType }> = ({ type }) => {
  switch (type) {
    case 'STRAWBERRY':
      return (
        <svg width="40" height="40" viewBox="0 0 40 40">
          <path d="M20 35 C10 35 5 25 5 15 C5 8 12 5 20 5 C28 5 35 8 35 15 C35 25 30 35 20 35" fill="#FF4D4D" />
          <path d="M15 5 C17 2 23 2 25 5" fill="#4CAF50" stroke="#2E7D32" strokeWidth="2" />
          <circle cx="15" cy="15" r="1.5" fill="#FFD700" />
          <circle cx="25" cy="18" r="1.5" fill="#FFD700" />
          <circle cx="18" cy="25" r="1.5" fill="#FFD700" />
          <circle cx="22" cy="12" r="1.5" fill="#FFD700" />
        </svg>
      );
    case 'BANANA':
      return (
        <svg width="40" height="40" viewBox="0 0 40 40">
          <path d="M5 10 C15 5 35 15 35 30 C35 35 30 35 25 30 C20 20 10 20 5 10" fill="#FFD700" stroke="#FBC02D" strokeWidth="1" />
          <path d="M5 10 L7 8" stroke="#795548" strokeWidth="3" />
        </svg>
      );
    case 'LIME':
      return (
        <svg width="40" height="40" viewBox="0 0 40 40">
          <circle cx="20" cy="20" r="15" fill="#8BC34A" stroke="#689F38" strokeWidth="2" />
          <circle cx="20" cy="20" r="12" fill="none" stroke="rgba(255,255,255,0.4)" strokeWidth="1" />
          {[0, 60, 120, 180, 240, 300].map(deg => (
            <line key={deg} x1="20" y1="20" x2={20 + 13 * Math.cos(deg * Math.PI / 180)} y2={20 + 13 * Math.sin(deg * Math.PI / 180)} stroke="rgba(255,255,255,0.4)" strokeWidth="1.5" />
          ))}
        </svg>
      );
    case 'GRAPE':
      return (
        <svg width="40" height="40" viewBox="0 0 40 40">
          <circle cx="15" cy="15" r="7" fill="#6A1B9A" />
          <circle cx="25" cy="15" r="7" fill="#7B1FA2" />
          <circle cx="20" cy="22" r="7" fill="#8E24AA" />
          <circle cx="20" cy="30" r="7" fill="#9C27B0" />
          <path d="M20 10 L20 4" stroke="#4CAF50" strokeWidth="3" />
        </svg>
      );
    default:
      return null;
  }
};

const HaliGaliCard: React.FC<HaliGaliCardProps> = ({ fruit, count }) => {
  if (fruit === 'NONE' || !fruit) return <div className="w-24 h-36 rounded-lg border-2 border-dashed border-gray-300 flex items-center justify-center text-[10px] text-gray-400">Card Area</div>;

  // count가 음수이거나 잘못된 값일 경우 방어
  const displayCount = Math.max(0, isNaN(count) ? 0 : count);

  return (
    <div className="w-24 h-36 bg-white rounded-lg shadow-md border border-gray-100 flex flex-wrap items-center justify-center p-2 relative overflow-hidden transition-all hover:scale-105">
      <div className="absolute top-1 left-1 text-[8px] font-bold text-gray-300">{fruit}</div>
      <div className="flex flex-wrap justify-center items-center gap-1">
        {Array.from({ length: displayCount }).map((_, i) => (
          <div key={i} className="animate-fade-in scale-75">
            <FruitIcon type={fruit} />
          </div>
        ))}
      </div>
      <div className="absolute bottom-1 right-1 text-[8px] font-bold text-gray-300">{displayCount}</div>
    </div>
  );
};

export default HaliGaliCard;
