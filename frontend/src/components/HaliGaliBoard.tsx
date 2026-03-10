import React, { useMemo, useEffect } from 'react';
import type { HaliGaliPlayerInfo, FruitType } from '../types/game';
import HaliGaliPlayer from './HaliGaliPlayer';
import Arena from './Arena';

interface HaliGaliBoardProps {
  status: string[];
  myNickname: string;
  onAction: (type: 'FLIP_CARD' | 'PRESS_BELL') => void;
}

const HaliGaliBoard: React.FC<HaliGaliBoardProps> = ({ status, myNickname, onAction }) => {
  // 1. 상태 데이터 파싱 (NaN 방지 보강)
  const parsedPlayers = useMemo(() => {
    if (!status || status.length < 4) return []; // TURN, ROUND, BELL 이후부터 플레이어

    const turnInfo = status[0]?.split(':')[1] || '';
    
    // 플레이어 데이터는 인덱스 3부터 시작 (BELL 정보 추가됨)
    const playersData = status.slice(3).map(item => {
      const parts = item.split(':');
      if (parts.length < 4) return null;

      const [nickname, fruit, countStr, deckSizeStr] = parts;
      const count = parseInt(countStr, 10);
      const deckSize = parseInt(deckSizeStr, 10);

      return {
        nickname,
        fruit: fruit as FruitType,
        count: isNaN(count) ? 0 : count,
        deckSize: isNaN(deckSize) ? 0 : deckSize,
        isTurn: nickname === turnInfo
      };
    }).filter(p => p !== null) as any[];

    // 2. 나를 기준으로 위치 배정 (BOTTOM 고정 회전)
    const myIndex = playersData.findIndex(p => p.nickname === myNickname);
    let sorted = [...playersData];
    
    if (myIndex !== -1) {
      const beforeMe = sorted.slice(0, myIndex);
      const fromMe = sorted.slice(myIndex);
      sorted = [...fromMe, ...beforeMe];
    }

    const positions: ('BOTTOM' | 'LEFT' | 'TOP' | 'RIGHT')[] = ['BOTTOM', 'LEFT', 'TOP', 'RIGHT'];
    return sorted.map((p, i) => ({
      ...p,
      position: positions[i % 4]
    } as HaliGaliPlayerInfo));
  }, [status, myNickname]);

  // 스페이스바 단축키 지원
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space') {
        const me = parsedPlayers.find(p => p.nickname === myNickname);
        if (me?.isTurn) {
          onAction('FLIP_CARD');
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [parsedPlayers, myNickname, onAction]);

  const currentPlayer = parsedPlayers.find(p => p.nickname === myNickname);

  return (
    <div className="relative w-full h-[90vh] max-w-full mx-auto flex items-center justify-center bg-emerald-950/20 rounded-[4rem] shadow-inner border-[12px] border-white/10 backdrop-blur-3xl overflow-hidden select-none">
      
      {/* 바닥 장식 */}
      <div className="absolute inset-0 opacity-5 pointer-events-none bg-[radial-gradient(circle,#fff_1px,transparent_1px)] bg-[size:50px_50px]"></div>

      {/* 메인 게임 그리드 (화면 꽉 채움) */}
      <div className="grid grid-cols-3 grid-rows-3 w-full h-full p-16">
        
        {/* 상단 (TOP) */}
        <div className="col-start-2 row-start-1 flex items-start justify-center">
          {parsedPlayers.find(p => p.position === 'TOP') && (
            <HaliGaliPlayer player={parsedPlayers.find(p => p.position === 'TOP')!} isMe={false} />
          )}
        </div>

        {/* 좌측 (LEFT) */}
        <div className="col-start-1 row-start-2 flex items-center justify-start">
          {parsedPlayers.find(p => p.position === 'LEFT') && (
            <HaliGaliPlayer player={parsedPlayers.find(p => p.position === 'LEFT')!} isMe={false} />
          )}
        </div>

        {/* 중앙 아레나 (서버 동기화 종) */}
        <div className="col-start-2 row-start-2 relative">
          <Arena onPressBell={() => onAction('PRESS_BELL')} actionTrigger={status} />
        </div>

        {/* 우측 (RIGHT) */}
        <div className="col-start-3 row-start-2 flex items-center justify-end">
          {parsedPlayers.find(p => p.position === 'RIGHT') && (
            <HaliGaliPlayer player={parsedPlayers.find(p => p.position === 'RIGHT')!} isMe={false} />
          )}
        </div>

        {/* 하단 (나 - BOTTOM) */}
        <div className="col-start-2 row-start-3 flex items-end justify-center">
          {currentPlayer && (
            <HaliGaliPlayer 
              player={currentPlayer} 
              isMe={true} 
              onFlip={() => onAction('FLIP_CARD')} 
            />
          )}
        </div>
      </div>

      {/* 내 턴 알림 오버레이 (옵션) */}
      {currentPlayer?.isTurn && (
        <div className="absolute bottom-12 right-12 bg-yellow-400 text-yellow-950 px-6 py-2 rounded-full font-black text-sm shadow-2xl animate-pulse z-50 border-2 border-white">
          YOUR TURN! CLICK YOUR DECK
        </div>
      )}
    </div>
  );
};

export default HaliGaliBoard;
