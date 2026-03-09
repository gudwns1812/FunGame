import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';

interface ResultProps {
  rankings: Player[];
  onBackToLobby: () => void;
}

const Result: React.FC<ResultProps> = ({ rankings, onBackToLobby }) => {
  const sortedRankings = [...rankings].sort((a, b) => b.score - a.score);

  return (
    <div className="w-full max-w-3xl flex flex-col items-center gap-10 animate-in fade-in zoom-in duration-700">
      
      {/* 헤더 섹션 */}
      <div className="text-center space-y-4">
        <div className="inline-flex items-center gap-2 px-4 py-1 bg-primary/20 border border-primary/50 rounded-full text-xs font-bold text-primary tracking-widest uppercase mb-2">
          <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
          게임 종료
        </div>
        <h1 className="text-5xl md:text-7xl font-black text-white leading-none tracking-tighter uppercase">
          CHALLENGE <br/>
          <span className="text-primary neon-glow">COMPLETE</span>
        </h1>
        <p className="text-slate-400 text-sm tracking-widest font-mono">
          최종 플레이 통계가 기록되었습니다.
        </p>
      </div>

      {/* 랭킹 리스트 패널 */}
      <div className="w-full panel-border bg-slate-900/80 rounded-xl p-8 flex flex-col gap-4 relative overflow-hidden">
        <div className="absolute top-0 right-0 p-4 opacity-10">
          <span className="material-symbols-outlined text-9xl text-primary">military_tech</span>
        </div>

        <h3 className="text-sm font-bold text-primary tracking-widest uppercase border-b border-primary/20 pb-4 mb-2 flex items-center gap-2">
          <span className="material-symbols-outlined text-lg">format_list_numbered</span>
          플레이어 성적 리포트
        </h3>

        <div className="w-full flex flex-col gap-3 z-10">
          {sortedRankings.map((p, idx) => {
            const isFirst = idx === 0 && p.score > 0;
            return (
              <div
                key={p.id}
                className={`flex items-center justify-between p-4 rounded-lg transition-all duration-300
                  ${isFirst 
                    ? 'bg-primary/20 border border-primary shadow-[inset_0_0_20px_rgba(37,192,244,0.3)] transform hover:scale-[1.02]' 
                    : 'bg-slate-950/50 border border-primary/20 hover:border-primary/50'
                  }`}
              >
                <div className="flex items-center gap-6">
                  <div className="w-12 text-center flex justify-center">
                    {isFirst ? (
                      <span className="material-symbols-outlined text-4xl text-white drop-shadow-[0_0_10px_rgba(255,255,255,0.8)]">military_tech</span>
                    ) : (
                      <span className="text-2xl font-mono font-bold text-slate-500 opacity-50">{idx + 1}</span>
                    )}
                  </div>
                  <span className={`text-2xl font-bold uppercase tracking-wider ${isFirst ? 'text-white neon-glow' : 'text-slate-300'}`}>
                    {stripTag(p.name)}
                  </span>
                </div>
                
                <div className="text-right flex flex-col items-end">
                  <span className={`text-[10px] font-mono tracking-widest uppercase ${isFirst ? 'text-primary/80' : 'text-slate-500'}`}>
                    최종 점수
                  </span>
                  <span className={`text-3xl font-black font-mono ${isFirst ? 'text-primary neon-glow' : 'text-white'}`}>
                    {p.score}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 하단 컨트롤 */}
      <div className="w-full flex flex-col items-center gap-4">
        <button 
          className="w-full max-w-md bg-primary hover:bg-primary/80 text-background-dark font-black text-xl py-5 rounded-lg transition-all transform hover:scale-[1.02] flex items-center justify-center gap-3 shadow-[0_0_15px_rgba(37,192,244,0.3)] tracking-widest" 
          onClick={onBackToLobby}
        >
          <span className="material-symbols-outlined">first_page</span>
          로비로 돌아가기
        </button>
        <p className="text-[10px] text-slate-500 uppercase tracking-widest font-mono">
          모든 데이터가 세션 아카이브에 저장되었습니다.
        </p>
      </div>

    </div>
  );
};

export default Result;
