import React from 'react';
import ResultBgm from '../components/ResultBgm';
import type { Player } from '../types/game';

interface HangmanResultPageProps {
    rankings: Player[];
    onBackToLobby: () => void;
}

const MAX_TRIES = 6;

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
        <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center p-4">
            <ResultBgm />

            <div className="px-card w-full max-w-xs p-6 text-center space-y-4 animate-scale-up">
                <p className={`px-title text-2xl ${isWin ? 'text-grass' : 'text-cherry'}`}>
                    {isWin ? '단어 해독 완료' : '모든 기회를 소진했습니다.'}
                </p>

                {isWin && actualAnswer && (
                    <div className="px-inset py-3">
                        <p className="px-label">정답</p>
                        <p className="px-title text-xl mt-1 tracking-wide">{actualAnswer}</p>
                    </div>
                )}

                <div className="px-inset py-3">
                    <p className="px-label">남은 기회</p>
                    <p className="px-title text-2xl mt-1 num">
                        {remainingTries} <span className="text-base text-ink-soft">/ {MAX_TRIES}</span>
                    </p>
                </div>

                <button onClick={onBackToLobby} className="px-btn px-btn-primary w-full py-3">
                    대기실로 돌아가기
                </button>
            </div>
        </div>
    );
};

export default HangmanResultPage;
