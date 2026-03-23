import React from 'react';
import type { Player } from '../types/game';

interface HangmanResultPageProps {
    rankings: Player[];
    onBackToLobby: () => void;
}

const HangmanResultPage: React.FC<HangmanResultPageProps> = ({ rankings, onBackToLobby }) => {
    const firstRank = rankings?.[0];
    const secondRank = rankings?.[1];
    let isWin = false;
    let remainingTries = 0;
    let actualAnswer = '';

    if (firstRank) {
        if (firstRank.name.includes(':')) {
            const parts = firstRank.name.split(':');
            isWin = parts[0] === '성공';
            remainingTries = parseInt(parts[1] || '0', 10);
        } else {
            isWin = firstRank.name === '성공';
            remainingTries = firstRank.score;
        }
    }

    if (secondRank) {
        actualAnswer = secondRank.name.includes(':') ? secondRank.name.split(':')[0] : secondRank.name;
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-background-dark text-white p-4">
            <div className={`p-10 md:p-14 rounded-[3rem] border-2 shadow-[0_0_80px_rgba(0,0,0,0.5)] text-center space-y-8 max-w-md w-full mx-4 transform animate-scale-up ${isWin ? 'border-primary/30 bg-primary/5' : 'border-red-500/30 bg-red-500/5'}`}>

                <div className="space-y-4">
                    {isWin ? (
                        <>
                            <h1 className="text-5xl md:text-6xl font-black tracking-tighter uppercase text-primary neon-glow drop-shadow-[0_0_20px_rgba(37,192,244,0.6)]">VICTORY</h1>
                            <p className="text-xl font-bold text-slate-300 tracking-widest mt-4">단어 해독 완료</p>
                        </>
                    ) : (
                        <>
                            <h1 className="text-5xl md:text-6xl font-black tracking-tighter uppercase text-red-500 animate-pulse drop-shadow-[0_0_20px_rgba(239,68,68,0.8)]">GAME OVER</h1>
                            <p className="text-lg md:text-xl font-bold text-slate-300 tracking-widest mt-4">모든 기회를 소진했습니다.</p>
                        </>
                    )}
                </div>

                {isWin && actualAnswer && (
                    <div className="bg-slate-950 py-6 px-8 rounded-2xl border border-white/5 space-y-1">
                        <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Correct Word</span>
                        <div className="text-3xl font-black text-white tracking-widest neon-glow-subtle">{actualAnswer}</div>
                    </div>
                )}

                <div className="bg-slate-950/80 py-8 px-8 rounded-3xl border border-white/5 space-y-2 my-10 shadow-inner">
                    <span className="text-[12px] font-black text-slate-500 uppercase tracking-widest">남은 기회</span>
                    <div className="text-5xl font-black text-white">{remainingTries} <span className="text-2xl opacity-40">/ 6</span></div>
                </div>

                <button
                    onClick={onBackToLobby}
                    className={`w-full py-5 font-black uppercase tracking-widest rounded-2xl transition-all shadow-lg active:scale-95 text-lg ${isWin
                        ? 'bg-primary text-background-dark hover:bg-primary/90 shadow-[0_10px_30px_rgba(37,192,244,0.3)]'
                        : 'bg-red-500/20 border-2 border-red-500/50 text-red-100 hover:bg-red-500/30 shadow-[0_10px_30px_rgba(239,68,68,0.2)]'
                        }`}>
                    대기실로 돌아가기
                </button>
            </div>
        </div>
    );
};

export default HangmanResultPage;
