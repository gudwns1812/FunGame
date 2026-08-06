import React from 'react';
import type { Player } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { getPlayerColor } from '../utils/playerColor';

interface LogListProps {
  logs: string[];
  players: Player[];
  /** 부모가 자동 스크롤을 위해 잡고 있는 ref */
  containerRef?: React.RefObject<HTMLDivElement | null>;
  className?: string;
  emptyText?: string;
}

/** 라운드 시작마다 서버 이벤트로 들어오는 '====…' 구분선 판별 */
const isRoundDivider = (log: string) => /^=+$/.test(log.trim());

/**
 * 대기실 · 게임 · 행맨이 공유하는 로그 렌더러 (표시 전용).
 * 입력창과 전송 로직은 각 화면이 그대로 들고 있다.
 */
const LogList: React.FC<LogListProps> = ({ logs, players, containerRef, className = '', emptyText }) => {
  const renderLine = (log: string, i: number) => {
    // 라운드 구분선
    if (isRoundDivider(log)) {
      return (
        <div key={i} className="flex items-center gap-2 py-1.5">
          <span className="px-rule flex-1" />
          <span className="px-label text-[10px] text-ink-soft shrink-0">다음 문제</span>
          <span className="px-rule flex-1" />
        </div>
      );
    }

    // 시스템 계열 로그
    if (log.startsWith('[시스템]') || log.startsWith('[오류]') || log.startsWith('[알림]') || log.startsWith('[실패]') || log.startsWith('[게임 종료]')) {
      const tone = log.startsWith('[오류]') || log.startsWith('[실패]')
        ? 'text-cherry'
        : log.startsWith('[게임 종료]')
          ? 'text-sea'
          : log.startsWith('[알림]')
            ? 'text-grass'
            : 'text-ink-soft';

      return (
        <p key={i} className={`text-xs leading-5 ${tone}`}>
          {log}
        </p>
      );
    }

    // 일반 채팅 (닉네임: 메시지)
    const colonIdx = log.indexOf(':');
    if (colonIdx > 0) {
      const senderName = log.substring(0, colonIdx);
      const rest = log.substring(colonIdx + 1);
      const player = players.find((p) => stripTag(p.name) === senderName || p.name === senderName);
      const color = getPlayerColor(player?.colorIndex ?? null) || '#0c6780';

      return (
        <p key={i} className="text-[13px] leading-5 text-ink break-words">
          <span className="font-display font-bold" style={{ color }}>
            {senderName}
          </span>
          <span className="text-ink-soft/60 mx-1.5">▸</span>
          {rest}
        </p>
      );
    }

    return (
      <p key={i} className="text-[13px] leading-5 text-ink break-words">
        {log}
      </p>
    );
  };

  return (
    <div ref={containerRef} className={`scroll-y custom-scrollbar px-3 py-2 space-y-1 ${className}`}>
      {logs.length === 0 && emptyText ? (
        <p className="px-label text-center py-4">{emptyText}</p>
      ) : (
        logs.map(renderLine)
      )}
    </div>
  );
};

export default LogList;
