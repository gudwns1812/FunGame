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
    <div className="grid grid-cols-7 gap-1.5 w-full max-w-md">
      {ALPHABETS.map((letter) => {
        const guessed = isGuessed(letter);
        const isWrong = wrongLetters.includes(letter);

        return (
          <button
            key={letter}
            onClick={() => onGuess(letter)}
            disabled={disabled || isGuessed(letter)}
            className={`px-title h-9 border-2 border-ink text-sm ${
              guessed
                ? isWrong
                  ? 'bg-cherry text-white opacity-60'
                  : 'bg-grass text-white opacity-60'
                : disabled
                  ? 'bg-paper-2 text-ink-soft'
                  : 'bg-white hover:bg-sky-deep shadow-[2px_2px_0_0_#1a1a1a] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none'
            }`}>
            {letter}
          </button>
        );
      })}
    </div>
  );
};

export default AlphabetKeyboard;
