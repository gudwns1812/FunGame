import React from 'react';

interface AlphabetKeyboardProps {
  onGuess: (letter: string) => void;
  disabled: boolean;
  wrongLetters: string[];
  currentDisplay: string;
}

const ALPHABETS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

const AlphabetKeyboard: React.FC<AlphabetKeyboardProps> = ({ onGuess, disabled, wrongLetters, currentDisplay }) => {
  const isGuessed = (letter: string) => {
    return wrongLetters.includes(letter) || currentDisplay.includes(letter);
  };

  return (
    <div className="grid grid-cols-7 gap-2 max-w-2xl mx-auto p-4 bg-slate-900/50 backdrop-blur-md rounded-2xl border border-white/10">
      {ALPHABETS.map((letter) => (
        <button
          key={letter}
          onClick={() => onGuess(letter as any)}
          disabled={disabled || isGuessed(letter)}
          className={`h-12 rounded-lg font-bold transition-all ${
            isGuessed(letter)
              ? 'bg-slate-800 text-slate-600 cursor-not-allowed opacity-50'
              : disabled
              ? 'bg-slate-700 text-slate-400 cursor-not-allowed'
              : 'bg-primary/20 text-primary border border-primary/30 hover:bg-primary hover:text-background-dark active:scale-95'
          }`}
        >
          {letter}
        </button>
      ))}
    </div>
  );
};

export default AlphabetKeyboard;
