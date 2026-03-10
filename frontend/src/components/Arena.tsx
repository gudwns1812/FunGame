import React from 'react';

interface ArenaProps {
  onPressBell: () => void;
  actionTrigger: string[]; // status 데이터를 그대로 받음
}

const Arena: React.FC<ArenaProps> = ({ onPressBell, actionTrigger }) => {
  // status 데이터에서 BELL 정보를 추출 (예: "BELL:45:67")
  const bellInfo = actionTrigger.find(s => s.startsWith('BELL:'));
  const [_, bellX, bellY] = bellInfo ? bellInfo.split(':') : ['BELL', '50', '50'];

  const top = `${bellY}%`;
  const left = `${bellX}%`;

  return (
    <div className="relative w-full h-full min-h-[500px] flex items-center justify-center bg-green-50/10 rounded-full border-2 border-dashed border-green-100/20">
      {/* 중앙 안내 문구 또는 장식 */}
      <div className="absolute inset-0 flex items-center justify-center -z-10">
         <div className="w-80 h-80 bg-green-200/5 rounded-full animate-pulse"></div>
      </div>

      {/* 서버 동기화 위치 종 (Golden Bell) - 크기 축소 (w-24 h-24) */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          onPressBell();
        }}
        style={{ top, left, transform: 'translate(-50%, -50%)' }}
        className="absolute w-24 h-24 bg-yellow-400 hover:bg-yellow-300 active:scale-90 active:rotate-12 rounded-full shadow-[0_6px_0_0_#d4a017,0_10px_15px_rgba(0,0,0,0.3)] border-4 border-yellow-200 flex flex-col items-center justify-center transition-all duration-500 group cursor-pointer z-50"
      >
        <div className="text-3xl group-hover:animate-bounce">🔔</div>
        <div className="mt-0.5 font-black text-yellow-900 drop-shadow-sm text-[10px]">PUSH!</div>
        
        {/* 빛 반사 효과 */}
        <div className="absolute top-3 left-3 w-6 h-3 bg-white/40 rounded-full blur-[1px] -rotate-45"></div>
      </button>

      {/* 중앙 안내 메시지 (옵션) */}
      <div className="absolute bottom-8 text-green-700/30 font-bold text-xs select-none pointer-events-none uppercase tracking-tighter">
        SYNCED ARENA ZONE
      </div>
    </div>
  );
};

export default Arena;
