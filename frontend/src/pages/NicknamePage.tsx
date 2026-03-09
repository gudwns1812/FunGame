import NicknameEntry from '../components/NicknameEntry';

interface NicknamePageProps {
  onEnter: (nickname: string) => void;
}

const NicknamePage: React.FC<NicknamePageProps> = ({ onEnter }) => {
  return (
    <main className="flex-1 flex flex-col items-center justify-center p-6 relative min-h-screen">
      <div className="absolute top-10 left-10 w-32 h-32 border-l-2 border-t-2 border-primary/40 opacity-50"></div>
      <div className="absolute bottom-10 right-10 w-32 h-32 border-r-2 border-b-2 border-primary/40 opacity-50"></div>
      
      <div className="w-full max-w-4xl grid grid-cols-1 lg:grid-cols-2 gap-8 items-center z-10">
        <div className="space-y-6 hidden lg:block">
          <div className="inline-block px-3 py-1 bg-primary/20 border border-primary/50 rounded text-xs font-bold text-primary tracking-widest uppercase mb-4">
            시스템 프로토콜 082-베타
          </div>
          <h2 className="text-5xl md:text-7xl font-black text-white leading-none tracking-tighter">
            노래/퀴즈 <br/>
            <span className="text-primary neon-glow">챌린지</span>
          </h2>
          <p className="text-slate-400 max-w-md text-lg leading-relaxed">
            환영합니다. 플레이어 이름을 등록하고 다양한 퀴즈 게임에 참여하여 실력을 증명해보세요.
          </p>
        </div>
        
        <NicknameEntry onEnter={onEnter} />
      </div>

      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-6 text-[10px] font-bold tracking-[0.3em] text-primary/40 uppercase">
        <span>엔진: 활성</span>
        <span className="text-primary">•</span>
        <span>시스템: 안정</span>
        <span className="text-primary">•</span>
        <span>통신: 대기 중</span>
      </div>
    </main>
  );
};

export default NicknamePage;
