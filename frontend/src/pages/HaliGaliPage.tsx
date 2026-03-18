import React from 'react';
import HaliGaliBoard from '../components/HaliGaliBoard';

interface HaliGaliPageProps {
  haliGaliStatus: string[];
  onHaliGaliAction: (type: 'FLIP_CARD' | 'PRESS_BELL') => void;
  myNickname: string;
}

const HaliGaliPage: React.FC<HaliGaliPageProps> = ({
  haliGaliStatus,
  onHaliGaliAction,
  myNickname,
}) => {
  return (
    <div className="relative flex flex-col min-h-screen bg-indigo-50/50">
      {/* 할리갈리 전용 헤더 */}
      <header className="flex items-center justify-between border-b border-indigo-200 px-6 py-4 bg-white/90 backdrop-blur-md z-10 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="text-indigo-600">
            <span className="material-symbols-outlined text-4xl">
              playing_cards
            </span>
          </div>
          <div>
            <h1 className="text-xl font-black tracking-tight text-indigo-900">
              할리갈리 익스프레스
            </h1>
            <p className="text-[10px] text-indigo-400 font-bold tracking-widest uppercase">실시간 대결 진행 중</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex flex-col items-end px-4 border-r border-indigo-100">
            <span className="text-[10px] text-indigo-300 uppercase font-bold">현재 정보</span>
            <span className="text-sm font-black text-indigo-600">카드 게임</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col p-6 relative z-10 h-[calc(100vh-140px)] overflow-hidden">
        <HaliGaliBoard status={haliGaliStatus} myNickname={myNickname} onAction={onHaliGaliAction} />
      </main>

      {/* 푸터 */}
      <footer className="mt-auto border-t border-indigo-100 px-6 py-4 bg-white/80 backdrop-blur-sm flex justify-center items-center z-10 shadow-2xl">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 bg-green-500 rounded-full animate-ping"></span>
            <span className="text-xs font-bold text-gray-500">실시간 연동 중</span>
          </div>
          <div className="text-sm font-black text-indigo-600 tracking-tighter">HALIGALLI ACTION LOG</div>
          <div className="text-xs text-gray-400 italic">* 바닥 카드 합계가 5가 되면 빠르게 종을 클릭하세요!</div>
        </div>
      </footer>
    </div>
  );
};

export default HaliGaliPage;
