import React from 'react';

interface WordDisplayProps {
  currentDisplay: string;
}

const WordDisplay: React.FC<WordDisplayProps> = ({ currentDisplay }) => {
  return (
    <div className="flex flex-wrap justify-center gap-4 py-8">
      {currentDisplay.split(' ').map((char, index) => (
        <div
          key={index}
          className={`text-5xl font-black min-w-[3rem] h-16 flex items-center justify-center border-b-4 ${char === '_' ? 'border-primary/30 text-primary/30' : 'border-primary text-white neon-glow'
            }`}
        >
          {char === '_' ? '' : char}
        </div>
      ))}
    </div>
  );
};

export default WordDisplay;
