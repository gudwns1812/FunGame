import React from 'react';
import type { HaliGaliPlayerInfo } from '../types/game';
import HaliGaliCard from './HaliGaliCard';

interface PlayerZoneProps {
  player: HaliGaliPlayerInfo;
  isMe: boolean;
  onFlip?: () => void;
}

const HaliGaliPlayer: React.FC<PlayerZoneProps> = ({ player, isMe, onFlip }) => {
  const isDead = player.deckSize === 0 && player.fruit === 'NONE';
  
  // 위치에 따른 덱과 카드의 배치 순서 (덱은 항상 외곽, 오픈 카드는 중앙쪽)
  const getLayoutConfig = () => {
    switch (player.position) {
      case 'TOP': return 'flex-col';
      case 'BOTTOM': return 'flex-col-reverse';
      case 'LEFT': return 'flex-row';
      case 'RIGHT': return 'flex-row-reverse';
      default: return 'flex-col';
    }
  };

  // 닉네임 위치 (최외곽)
  const getNamePosition = () => {
    switch (player.position) {
      case 'TOP': return '-top-16';
      case 'BOTTOM': return '-bottom-16';
      case 'LEFT': return '-left-24 rotate-[-90deg]';
      case 'RIGHT': return '-right-24 rotate-[90deg]';
      default: return '-top-12';
    }
  };

  return (
    <div className={`relative flex items-center justify-center transition-all duration-500 ${isDead ? 'opacity-20 grayscale' : ''} ${player.isTurn ? 'scale-105' : 'scale-100'}`}>
      
      {/* 닉네임 라벨 (최외곽 배치) */}
      <div className={`absolute ${getNamePosition()} left-1/2 -translate-x-1/2 px-6 py-1.5 rounded-full text-[10px] font-black shadow-xl z-20 whitespace-nowrap border-2 transition-colors ${
        player.isTurn ? 'bg-yellow-400 text-white border-yellow-200 shadow-yellow-200/50' : 'bg-slate-900 text-slate-400 border-slate-800'
      }`}>
        {player.nickname} {player.isTurn && '●'}
      </div>

      {/* 덱과 카드의 배치 구역 */}
      <div className={`flex items-center gap-4 ${getLayoutConfig()}`}>
        
        {/* 덱 (카드 더미) - 내 턴이고 나인 경우 클릭 가능 */}
        <div 
          onClick={() => isMe && player.isTurn && onFlip?.()}
          className={`relative group ${isMe && player.isTurn ? 'cursor-pointer hover:-translate-y-1' : 'cursor-default'}`}
        >
          <div className={`w-16 h-24 rounded-lg shadow-2xl border-2 flex items-center justify-center overflow-hidden transition-all ${
            isMe && player.isTurn ? 'bg-indigo-500 border-white ring-4 ring-yellow-400 animate-pulse' : 'bg-indigo-900 border-indigo-700 opacity-80'
          }`}>
            <div className="absolute inset-0 opacity-10 bg-[repeating-linear-gradient(45deg,#fff,#fff_5px,#000_5px,#000_10px)]"></div>
            <span className="text-xl font-black text-white z-10">{player.deckSize}</span>
          </div>
          {/* 카드 겹침 효과 */}
          <div className="absolute top-0.5 left-0.5 w-full h-full bg-indigo-950 rounded-lg -z-10"></div>
          {isMe && player.isTurn && (
            <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 text-[8px] font-bold text-yellow-400 animate-bounce whitespace-nowrap">
              CLICK TO FLIP!
            </div>
          )}
        </div>

        {/* 오픈 카드 */}
        <div className={`transition-all duration-300 ${player.isTurn ? 'ring-4 ring-yellow-400/20 rounded-xl' : ''}`}>
          <HaliGaliCard fruit={player.fruit} count={player.count} />
        </div>

      </div>
    </div>
  );
};

export default HaliGaliPlayer;
