import React from 'react';

interface HangmanDrawingProps {
  remainingTries: number;
}

const HEAD = <circle key="head" cx="200" cy="80" r="30" className="stroke-white fill-none stroke-[8px]" />;
const BODY = <line key="body" x1="200" y1="110" x2="200" y2="210" className="stroke-white stroke-[8px]" />;
const LEFT_ARM = <line key="left-arm" x1="200" y1="130" x2="150" y2="180" className="stroke-white stroke-[8px]" />;
const RIGHT_ARM = <line key="right-arm" x1="200" y1="130" x2="250" y2="180" className="stroke-white stroke-[8px]" />;
const LEFT_LEG = <line key="left-leg" x1="200" y1="210" x2="160" y2="280" className="stroke-white stroke-[8px]" />;
const RIGHT_LEG = <line key="right-leg" x1="200" y1="210" x2="240" y2="280" className="stroke-white stroke-[8px]" />;

const BODY_PARTS = [HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG];

const HangmanDrawing: React.FC<HangmanDrawingProps> = ({ remainingTries }) => {
  const partsToShow = 6 - remainingTries;

  return (
    <div className="relative">
      <svg height="400" width="300" className="drop-shadow-neon">
        {/* 거치대 */}
        <line x1="20" y1="380" x2="280" y2="380" className="stroke-primary stroke-[8px]" />
        <line x1="60" y1="380" x2="60" y2="20" className="stroke-primary stroke-[8px]" />
        <line x1="60" y1="20" x2="200" y2="20" className="stroke-primary stroke-[8px]" />
        <line x1="200" y1="20" x2="200" y2="50" className="stroke-primary stroke-[8px]" />
        
        {/* 행맨 신체 부위 */}
        {BODY_PARTS.slice(0, partsToShow)}
      </svg>
    </div>
  );
};

export default HangmanDrawing;
