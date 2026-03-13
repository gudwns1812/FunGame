import WaitingRoom from '../components/WaitingRoom';
import type { Player } from '../types/game';

interface WaitingRoomPageProps {
  players: Player[];
  logs: string[];
  isHost: boolean;
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  onSendMessage: (message: string) => void;
}

const WaitingRoomPage: React.FC<WaitingRoomPageProps> = ({
  players,
  logs,
  isHost,
  onStart,
  onLeave,
  onToggleReady,
  onSendMessage,
}) => {
  return (
    <div className="relative flex flex-col min-h-screen">
      {/* 한글 헤더 */}
      <header className="flex items-center justify-between border-b border-primary/30 px-6 py-4 bg-background-dark/80 backdrop-blur-md z-10">
        <div className="flex items-center gap-3">
          <div className="text-primary">
            <span className="material-symbols-outlined text-4xl neon-glow">rocket_launch</span>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest uppercase text-primary neon-glow">대기실</h1>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="hidden md:flex flex-col items-end px-4 border-r border-primary/20">
            <span className="text-[10px] text-primary/50 uppercase">방 상태</span>
            <span className="text-sm font-bold text-green-400 uppercase tracking-widest">준비 중</span>
          </div>
          <div className="flex gap-2">
            <button className="p-2 rounded border border-primary/30 hover:bg-primary/10 text-primary transition-colors">
              <span className="material-symbols-outlined">settings</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col items-center p-6 relative z-10">
        <WaitingRoom
          players={players}
          logs={logs}
          isHost={isHost}
          onStart={onStart}
          onLeave={onLeave}
          onToggleReady={onToggleReady}
          onSendMessage={onSendMessage}
        />
      </main>

      {/* 한글 푸터 */}
      <footer className="mt-auto border-t border-primary/10 px-6 py-4 bg-slate-900/40 backdrop-blur-sm flex justify-between items-center z-10">
        <div className="flex items-center gap-6">
          <span className="text-[10px] font-bold tracking-[0.3em] text-primary/40 uppercase tracking-widest">
            Awaiting Launch Sequence...
          </span>
        </div>
        <div className="flex items-center gap-4">
          <div className="w-32 h-2 bg-slate-800 rounded-full overflow-hidden border border-white/5">
            <div className="h-full bg-primary w-[50%] shadow-[0_0_8px_rgba(37,192,244,0.5)] animate-pulse"></div>
          </div>
          <span className="text-[10px] font-bold text-primary uppercase tracking-widest">Readiness: 50%</span>
        </div>
      </footer>
    </div>
  );
};

export default WaitingRoomPage;
