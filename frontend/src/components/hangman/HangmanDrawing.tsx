import React from 'react';

interface HangmanDrawingProps {
  remainingTries: number;
}

const STROKE = 'stroke-ink stroke-[9px]';

const HEAD = <circle key="head" cx="200" cy="80" r="30" className={`fill-none ${STROKE}`} />;
const BODY = <line key="body" x1="200" y1="110" x2="200" y2="210" className={STROKE} />;
const LEFT_ARM = <line key="left-arm" x1="200" y1="130" x2="150" y2="180" className={STROKE} />;
const RIGHT_ARM = <line key="right-arm" x1="200" y1="130" x2="250" y2="180" className={STROKE} />;
const LEFT_LEG = <line key="left-leg" x1="200" y1="210" x2="160" y2="280" className={STROKE} />;
const RIGHT_LEG = <line key="right-leg" x1="200" y1="210" x2="240" y2="280" className={STROKE} />;

const BODY_PARTS = [HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG];

const HangmanDrawing: React.FC<HangmanDrawingProps> = ({ remainingTries }) => {
  const partsToShow = 6 - remainingTries;

  return (
    <svg viewBox="0 0 300 400" className="w-full max-w-[130px] sm:max-w-[170px] shrink-0" aria-hidden>
      {/* 거치대 */}
      <g className="stroke-cherry stroke-[10px]">
        <line x1="24" y1="378" x2="276" y2="378" />
        <line x1="62" y1="378" x2="62" y2="22" />
        <line x1="62" y1="22" x2="200" y2="22" />
        <line x1="200" y1="22" x2="200" y2="50" />
      </g>

      {/* 행맨 신체 부위 */}
      {BODY_PARTS.slice(0, partsToShow)}
    </svg>
  );
};

export default HangmanDrawing;
