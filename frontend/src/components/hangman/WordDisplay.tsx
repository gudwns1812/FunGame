import React from 'react';

interface WordDisplayProps {
  currentDisplay: string;
}

const WordDisplay: React.FC<WordDisplayProps> = ({ currentDisplay }) => {
  return (
    <div className="flex flex-wrap justify-center gap-2">
      {currentDisplay.split(' ').map((char, index) => (
        <div
          key={index}
          className={`px-title min-w-[2.25rem] h-12 px-1 flex items-center justify-center border-2 border-ink text-2xl ${
            char === '_' ? 'bg-paper-2 text-transparent' : 'bg-white'
          }`}>
          {char === '_' ? '' : char}
        </div>
      ))}
    </div>
  );
};

export default WordDisplay;
