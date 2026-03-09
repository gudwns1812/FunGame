import Result from '../components/Result';
import type { Player } from '../types/game';

interface ResultPageProps {
  rankings: Player[];
  onBackToLobby: () => void;
}

const ResultPage: React.FC<ResultPageProps> = ({ rankings, onBackToLobby }) => {
  return (
    <div className="relative flex flex-col min-h-screen">
      {/* 한글 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-6 py-4 bg-background-dark/80 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow">rocket_launch</span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest uppercase text-primary neon-glow">FunGame 결과</h1>
            <p className="text-[10px] text-primary/60 tracking-[0.2em]">시스템 상태: 게임 종료</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-primary/20">
            <span className="text-[10px] text-primary/50 uppercase">상태</span>
            <span className="text-sm font-bold text-slate-400">데이터 기록 완료</span>
          </div>
          <div className="flex gap-2">
            <button className="p-2 rounded border border-primary/30 hover:bg-primary/10 text-primary transition-colors">
              <span className="material-symbols-outlined">settings</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center justify-center p-6 relative z-10">
        <Result rankings={rankings} onBackToLobby={onBackToLobby} />
      </main>
      
      {/* 한글 푸터 */}
      <footer className="mt-auto border-t border-primary/10 px-6 py-4 bg-slate-900/40 backdrop-blur-sm flex justify-between items-center z-10">
        <div className="flex items-center gap-6">
          <span className="text-[10px] font-bold tracking-[0.3em] text-primary/40 uppercase">다음 지시를 기다리는 중...</span>
        </div>
        <div className="flex items-center gap-4">
          <div className="w-32 h-2 bg-slate-800 rounded-full overflow-hidden">
            <div className="h-full bg-primary w-[100%] shadow-[0_0_8px_rgba(37,192,244,0.5)]"></div>
          </div>
          <span className="text-[10px] font-bold text-primary">시스템 수치: 100%</span>
        </div>
      </footer>
    </div>
  );
};

export default ResultPage;
