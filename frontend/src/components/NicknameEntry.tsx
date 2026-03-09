import React, { useState } from 'react';

interface NicknameEntryProps {
  onEnter: (nickname: string) => void;
}

const NicknameEntry: React.FC<NicknameEntryProps> = ({ onEnter }) => {
  const [nickname, setNickname] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (nickname.trim()) {
      onEnter(nickname.trim());
    }
  };

  return (
    <div className="panel-border bg-background-dark/90 p-8 rounded-xl relative overflow-hidden group w-full max-w-md mx-auto">
      <div className="absolute top-0 right-0 p-2 opacity-20 group-hover:opacity-100 transition-opacity">
        <span className="material-symbols-outlined text-primary text-4xl">memory</span>
      </div>
      
      <div className="space-y-8">
        <div>
          <h3 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">account_circle</span>
            플레이어 등록
          </h3>
          <div className="h-1 w-20 bg-primary"></div>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-xs font-bold text-primary/70 uppercase tracking-widest mb-2">
              닉네임 입력
            </label>
            <div className="relative">
              <input
                type="text"
                className="w-full bg-slate-900 border-2 border-primary/30 rounded-lg px-4 py-4 text-xl font-bold tracking-wider focus:ring-2 focus:ring-primary focus:border-transparent outline-none text-white placeholder:text-slate-700 transition-all"
                placeholder="사용할 이름을 입력하세요"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                autoFocus
              />
              <div className="absolute right-4 top-1/2 -translate-y-1/2 text-primary/30">
                <span className="material-symbols-outlined">fingerprint</span>
              </div>
            </div>
          </div>
          
          <button
            type="submit"
            disabled={!nickname.trim()}
            className="w-full bg-primary hover:bg-primary/80 disabled:bg-primary/30 disabled:cursor-not-allowed text-background-dark font-black text-xl py-5 rounded-lg transition-all transform hover:scale-[1.02] flex items-center justify-center gap-3 shadow-[0_0_20px_rgba(37,192,244,0.3)]"
          >
            <span className="material-symbols-outlined font-bold">login</span>
            게임 접속
          </button>
          
          <p className="text-[10px] text-center text-slate-500 leading-tight uppercase">
            접속 시 시스템 이용 약관 및 데이터 로깅 정책에 동의하는 것으로 간주됩니다.
          </p>
        </form>
      </div>
      <div className="absolute bottom-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-primary to-transparent opacity-50"></div>
    </div>
  );
};

export default NicknameEntry;
