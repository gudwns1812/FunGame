import React from 'react';

interface TopBarProps {
  /** 현재 화면 이름 (예: 대기실) */
  title?: string;
  /** 화면 이름 오른쪽에 붙는 부가 정보 (예: 방 이름) */
  subtitle?: string;
  /** 우측 영역 (상태 칩, 계정 메뉴 등) */
  right?: React.ReactNode;
  /** 로고 클릭 동작 (없으면 클릭 불가) */
  onLogoClick?: () => void;
}

/**
 * 모든 화면 상단의 얇은 바.
 * 화면 높이를 아끼기 위해 장식 문구 없이 로고 · 화면 이름 · 액션만 둔다.
 */
const TopBar: React.FC<TopBarProps> = ({ title, subtitle, right, onLogoClick }) => {
  return (
    <header className="shrink-0 bg-paper border-b-[4px] border-ink px-3 sm:px-5 h-14 flex items-center justify-between gap-3">
      <div className="flex items-center gap-2.5 min-w-0">
        <button
          type="button"
          onClick={onLogoClick}
          disabled={!onLogoClick}
          className="flex items-center gap-2.5 shrink-0 disabled:cursor-default">
          <span className="w-8 h-8 shrink-0 bg-cherry border-2 border-ink flex items-center justify-center">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.8">
              <circle cx="8" cy="18" r="3" />
              <circle cx="18" cy="15" r="3" />
              <path d="M11 18V6l10-2v11" />
            </svg>
          </span>
          <span className="px-title text-base sm:text-lg whitespace-nowrap">펀게임</span>
        </button>

        {title && (
          <>
            <span className="w-2 h-2 bg-ink shrink-0" />
            <span className="px-title text-sm text-ink-soft shrink-0">{title}</span>
          </>
        )}

        {/* 방 이름처럼 길이를 알 수 없는 값: 남는 폭만 쓰고 넘치면 말줄임 */}
        {subtitle && (
          <span className="px-chip px-chip-sea min-w-0 max-w-[10rem] sm:max-w-[24rem]">
            <span className="block truncate">{subtitle}</span>
          </span>
        )}
      </div>

      {right && <div className="flex items-center gap-2 shrink-0">{right}</div>}
    </header>
  );
};

export default TopBar;
