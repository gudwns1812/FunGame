import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ReportModal from './ReportModal';

const openModal = (gameType: string | null, onSubmit = vi.fn()) => {
  render(
    <ReportModal
      gameType={gameType}
      onSubmit={onSubmit}
      onClose={vi.fn()}
      isSubmitting={false}
      errorMessage={null}
      isSubmitted={false}
    />,
  );
  return onSubmit;
};

const reasonLabels = () =>
  screen
    .getAllByRole('button')
    .map((button) => button.textContent ?? '')
    .filter((label) => label !== '닫기');

describe('ReportModal', () => {
  it('노래 퀴즈에서는 노래를 기준으로 사유를 보여준다', () => {
    openModal('SONG');

    expect(reasonLabels()).toEqual(
      expect.arrayContaining(['노래가 안 나와요', '노래가 달라요', '초성 힌트가 이상해요', '답이 이상해요']),
    );
  });

  it('행맨에서는 단어를 기준으로 사유를 보여준다', () => {
    openModal('HANGMAN');

    expect(reasonLabels()).toEqual(
      expect.arrayContaining(['단어가 안 보여요', '단어가 이상해요', '틀린 글자 표시가 이상해요']),
    );
  });

  it('CS 퀴즈에는 힌트가 없으므로 힌트 사유도 없다', () => {
    openModal('CS');

    expect(reasonLabels()).toEqual(expect.arrayContaining(['문제가 안 보여요', '문제가 이상해요', '답이 이상해요']));
    expect(reasonLabels().some((label) => label.includes('힌트'))).toBe(false);
  });

  it('기타가 아닌 사유는 고르는 즉시 접수한다', () => {
    const onSubmit = openModal('SONG');

    fireEvent.click(screen.getByRole('button', { name: '초성 힌트가 이상해요' }));

    expect(onSubmit).toHaveBeenCalledWith('HINT_WRONG', null);
  });

  it('기타는 고르는 것만으로 접수하지 않고 입력창을 연다', () => {
    const onSubmit = openModal('SONG');

    fireEvent.click(screen.getByRole('button', { name: /기타/ }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('기타는 내용이 비어 있으면 접수하지 않는다', () => {
    const onSubmit = openModal('SONG');

    fireEvent.click(screen.getByRole('button', { name: /기타/ }));
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '   ' } });
    fireEvent.click(screen.getByRole('button', { name: '접수' }));

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('기타는 내용을 쓰면 그 내용을 함께 접수한다', () => {
    const onSubmit = openModal('SONG');

    fireEvent.click(screen.getByRole('button', { name: /기타/ }));
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '소리가 안 나요' } });
    fireEvent.click(screen.getByRole('button', { name: '접수' }));

    expect(onSubmit).toHaveBeenCalledWith('ETC', '소리가 안 나요');
  });

  it('접수한 뒤에는 사유를 다시 고를 수 없다', () => {
    render(
      <ReportModal
        gameType="SONG"
        onSubmit={vi.fn()}
        onClose={vi.fn()}
        isSubmitting={false}
        errorMessage={null}
        isSubmitted
      />,
    );

    expect(screen.queryByRole('button', { name: '답이 이상해요' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '닫기' })).toBeInTheDocument();
  });

  it('접수가 실패하면 서버가 준 이유를 보여준다', () => {
    render(
      <ReportModal
        gameType="SONG"
        onSubmit={vi.fn()}
        onClose={vi.fn()}
        isSubmitting={false}
        errorMessage="신고가 너무 잦습니다. 잠시 후 다시 시도해주세요."
        isSubmitted={false}
      />,
    );

    expect(screen.getByText('신고가 너무 잦습니다. 잠시 후 다시 시도해주세요.')).toBeInTheDocument();
  });
});
