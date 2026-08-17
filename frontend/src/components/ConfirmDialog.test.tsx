import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach } from 'vitest';
import ConfirmDialog from './ConfirmDialog';
import { askConfirm, declineWaitingConfirms } from '../utils/confirm';

const ask = (message: string) => {
  let answer: boolean | null = null;
  act(() => {
    void askConfirm(message).then((accepted) => {
      answer = accepted;
    });
  });
  return () => answer;
};

describe('ConfirmDialog', () => {
  beforeEach(() => {
    declineWaitingConfirms();
  });

  it('물어볼 것이 없으면 화면에 아무것도 그리지 않는다', () => {
    const { container } = render(<ConfirmDialog />);

    expect(container).toBeEmptyDOMElement();
  });

  it('물어본 내용을 보여주고 확인 버튼에 바로 포커스를 준다', () => {
    render(<ConfirmDialog />);

    ask('관리자 승급을 신청하시겠습니까?');

    expect(screen.getByRole('dialog')).toHaveAccessibleName('관리자 승급을 신청하시겠습니까?');
    expect(screen.getByRole('button', { name: '확인' })).toHaveFocus();
  });

  it('확인을 누르면 참을 돌려주고 창을 닫는다', async () => {
    const user = userEvent.setup();
    render(<ConfirmDialog />);
    const answer = ask('이 사용자를 ADMIN으로 승급시키겠습니까?');

    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(answer()).toBe(true);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('취소를 누르면 거짓을 돌려주고 창을 닫는다', async () => {
    const user = userEvent.setup();
    render(<ConfirmDialog />);
    const answer = ask('이 승급 요청을 거절하시겠습니까?');

    await user.click(screen.getByRole('button', { name: '취소' }));

    expect(answer()).toBe(false);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('ESC 를 누르면 취소한 것으로 본다', async () => {
    const user = userEvent.setup();
    render(<ConfirmDialog />);
    const answer = ask('이 승급 요청을 거절하시겠습니까?');

    await user.keyboard('{Escape}');

    expect(answer()).toBe(false);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('먼저 물어본 것에 답하면 다음 질문을 보여준다', async () => {
    const user = userEvent.setup();
    render(<ConfirmDialog />);
    const firstAnswer = ask('먼저 물어본 것');
    const secondAnswer = ask('나중에 물어본 것');

    expect(screen.getByRole('dialog')).toHaveAccessibleName('먼저 물어본 것');

    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(firstAnswer()).toBe(true);
    expect(secondAnswer()).toBeNull();
    expect(screen.getByRole('dialog')).toHaveAccessibleName('나중에 물어본 것');
  });
});
