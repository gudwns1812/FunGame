import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import RoomSettingsModal from './RoomSettingsModal';
import type { RoomSettings } from '../types/game';

const songRoom = (maxPlayers: number): RoomSettings => ({
  title: '테스트 방',
  gameType: 'SONG',
  maxPlayers,
  category: 'KPOP',
  totalRound: 10,
  difficulty: 0,
  csDifficulty: 'HARD',
  hostMemberId: 1,
  hostNickname: '방장',
});

const openModal = (currentPlayers: number, onSubmit = vi.fn()) => {
  render(
    <RoomSettingsModal
      settings={songRoom(12)}
      currentPlayers={currentPlayers}
      onSubmit={onSubmit}
      onClose={vi.fn()}
    />,
  );
  return onSubmit;
};

describe('RoomSettingsModal', () => {
  it('현재 인원보다 적은 정원은 고를 수 없다', () => {
    openModal(5);

    const options = screen.getAllByRole('option').filter((option) => /명$/.test(option.textContent ?? ''));
    const values = options.map((option) => Number((option as HTMLOptionElement).value));

    expect(Math.min(...values)).toBe(5);
  });

  it('현재 인원을 수용할 수 없는 게임 모드는 고를 수 없다', () => {
    openModal(8);

    const hangman = screen.getByRole('option', { name: /행맨/ }) as HTMLOptionElement;

    expect(hangman.disabled).toBe(true);
    expect(hangman.textContent).toContain('최대 6명');
  });

  it('게임 모드를 바꿔도 정원이 현재 인원 아래로 내려가지 않는다', () => {
    const onSubmit = openModal(5);

    fireEvent.change(screen.getByLabelText('게임 모드', { selector: 'select' }), { target: { value: 'HANGMAN' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit.mock.calls[0][0].maxPlayers).toBeGreaterThanOrEqual(5);
    expect(onSubmit.mock.calls[0][0].gameType).toBe('HANGMAN');
  });

  it('CS 를 고르면 난이도를 고를 수 있다', () => {
    openModal(2);

    fireEvent.change(screen.getByLabelText('게임 모드', { selector: 'select' }), { target: { value: 'CS' } });

    const levels = screen.getAllByRole('option').map((option) => option.textContent);
    expect(levels).toEqual(expect.arrayContaining(['쉬움', '보통', '어려움']));
  });

  it('고른 CS 난이도를 저장값 그대로 넘긴다', () => {
    const onSubmit = openModal(2);

    fireEvent.change(screen.getByLabelText('게임 모드', { selector: 'select' }), { target: { value: 'CS' } });
    fireEvent.change(screen.getByLabelText('난이도', { selector: 'select' }), { target: { value: 'EASY' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(onSubmit.mock.calls[0][0].csDifficulty).toBe('EASY');
  });

  it('CS 가 아닌 모드에서는 방의 기존 난이도를 그대로 유지한다', () => {
    const onSubmit = openModal(2);

    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(onSubmit.mock.calls[0][0].csDifficulty).toBe('HARD');
  });
});
